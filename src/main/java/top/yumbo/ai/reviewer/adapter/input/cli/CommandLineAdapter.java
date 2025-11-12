package top.yumbo.ai.reviewer.adapter.input.cli;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.reviewer.adapter.output.repository.GiteeRepositoryAdapter;
import top.yumbo.ai.reviewer.adapter.output.repository.GitHubRepositoryAdapter;
import top.yumbo.ai.reviewer.adapter.output.ai.AIServiceConfig;
import top.yumbo.ai.reviewer.adapter.output.ai.DeepSeekAIAdapter;
import top.yumbo.ai.reviewer.adapter.output.cache.FileCacheAdapter;
import top.yumbo.ai.reviewer.adapter.output.filesystem.LocalFileSystemAdapter;
import top.yumbo.ai.reviewer.application.port.input.ProjectAnalysisUseCase;
import top.yumbo.ai.reviewer.application.port.input.ReportGenerationUseCase;
import top.yumbo.ai.reviewer.application.service.ProjectAnalysisService;
import top.yumbo.ai.reviewer.application.service.ReportGenerationService;
import top.yumbo.ai.reviewer.domain.hackathon.model.*;
import top.yumbo.ai.reviewer.domain.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

/**
 * 命令行适配器
 * 提供命令行接口，连接用户输入和应用服务
 */
@Slf4j
public class CommandLineAdapter {

    private final ProjectAnalysisUseCase analysisUseCase;
    private final ReportGenerationUseCase reportUseCase;

    public CommandLineAdapter() {
        // 初始化适配器
        DeepSeekAIAdapter aiAdapter = createAIAdapter();
        FileCacheAdapter cacheAdapter = new FileCacheAdapter();
        LocalFileSystemAdapter fileSystemAdapter = createFileSystemAdapter();

        // 初始化服务
        this.analysisUseCase = new ProjectAnalysisService(
                aiAdapter, cacheAdapter, fileSystemAdapter);
        this.reportUseCase = new ReportGenerationService(fileSystemAdapter);
    }

    /**
     * 主入口
     */
    public static void main(String[] args) {
        CommandLineAdapter cli = new CommandLineAdapter();

        try {
            if (args.length > 0 && "hackathon".equals(args[0])) {
                // Hackathon mode
                HackathonArguments hackArgs = cli.parseHackathonArguments(args);
                cli.executeHackathon(hackArgs);
            } else {
                // Regular mode
                CLIArguments cliArgs = cli.parseArguments(args);
                cli.execute(cliArgs);
            }
        } catch (Exception e) {
            log.error("执行失败", e);
            System.err.println("错误: " + e.getMessage());
            cli.printUsage();
            System.exit(1);
        }
    }

    /**
     * 执行分析
     */
    public void execute(CLIArguments args) {
        log.info("开始项目分析: {}", args.projectPath());

        // 1. 扫描项目
        System.out.println("正在扫描项目...");
        LocalFileSystemAdapter fsAdapter = createFileSystemAdapter();
        Path projectRoot = Paths.get(args.projectPath());

        List<SourceFile> sourceFiles = fsAdapter.scanProjectFiles(projectRoot);
        String structureTree = fsAdapter.generateProjectStructure(projectRoot);

        // 2. 构建项目对象
        Project project = Project.builder()
                .name(projectRoot.getFileName().toString())
                .rootPath(projectRoot)
                .type(detectProjectType(sourceFiles))
                .sourceFiles(sourceFiles)
                .structureTree(structureTree)
                .build();

        System.out.println("项目信息:");
        System.out.println("  - 名称: " + project.getName());
        System.out.println("  - 类型: " + project.getType().getDisplayName());
        System.out.println("  - 文件数: " + project.getSourceFiles().size());
        System.out.println("  - 代码行数: " + project.getTotalLines());

        // 3. 执行分析
        System.out.println("\n正在分析项目...");
        AnalysisTask task;

        if (args.async()) {
            String taskId = analysisUseCase.analyzeProjectAsync(project);
            System.out.println("异步分析任务已启动: " + taskId);

            // 轮询任务状态
            do {
                task = analysisUseCase.getTaskStatus(taskId);
                if (task.getProgress() != null) {
                    System.out.printf("\r进度: %.1f%% - %s",
                            task.getProgress().getPercentage(),
                            task.getProgress().getCurrentTask());
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } while (task.isRunning());

            System.out.println("\n");

        } else {
            task = analysisUseCase.analyzeProject(project);
        }

        // 4. 获取结果
        if (task.isCompleted()) {
            ReviewReport report = analysisUseCase.getAnalysisResult(task.getTaskId());

            System.out.println("分析完成！");
            System.out.println("\n=== 分析结果 ===");
            System.out.println("总体评分: " + report.getOverallScore() + "/100 (" + report.getGrade() + ")");
            System.out.println("\n各维度评分:");
            report.getDimensionScores().forEach((dimension, score) ->
                    System.out.println("  - " + dimension + ": " + score + "/100"));

            // 5. 保存报告
            if (args.outputPath() != null) {
                Path outputPath = Paths.get(args.outputPath());
                reportUseCase.saveReport(report, outputPath, args.format());
                System.out.println("\n报告已保存到: " + outputPath);
            } else {
                // 打印Markdown报告到控制台
                System.out.println("\n=== 详细报告 ===");
                String markdownReport = reportUseCase.generateMarkdownReport(report);
                System.out.println(markdownReport);
            }

            System.out.println("\n分析耗时: " + task.getDurationMillis() + " 毫秒");

        } else if (task.isFailed()) {
            System.err.println("分析失败: " + task.getErrorMessage());
            System.exit(1);
        }
    }

    /**
     * 执行黑客松评审
     */
    public void executeHackathon(HackathonArguments args) {
        log.info("开始黑客松项目评审: {}", args.gitUrl() != null ? args.gitUrl() : args.directory());

        Path projectPath = null;
        boolean needsCleanup = false;

        try {
            // 1. 获取项目路径
            if (args.gitUrl() != null) {
                // 从Git URL克隆
                System.out.println("正在克隆项目: " + args.gitUrl());
                top.yumbo.ai.reviewer.application.port.output.RepositoryPort repoPort = detectGitRepositoryAdapter(args.gitUrl());

                String branch = args.branch() != null ? args.branch() : "main";
                top.yumbo.ai.reviewer.application.port.output.CloneRequest cloneRequest =
                    top.yumbo.ai.reviewer.application.port.output.CloneRequest.builder()
                        .url(args.gitUrl())
                        .branch(branch)
                        .timeoutSeconds(300)
                        .build();

                projectPath = repoPort.cloneRepository(cloneRequest);
                needsCleanup = true;
                System.out.println("项目克隆完成: " + projectPath);
            } else if (args.directory() != null) {
                // 使用本地目录
                projectPath = Paths.get(args.directory());
                if (!Files.exists(projectPath)) {
                    throw new IllegalArgumentException("目录不存在: " + args.directory());
                }
                System.out.println("使用本地目录: " + projectPath);
            }

            // 2. 扫描项目
            System.out.println("\n正在扫描项目...");
            LocalFileSystemAdapter fsAdapter = createFileSystemAdapter();
            List<SourceFile> sourceFiles = fsAdapter.scanProjectFiles(projectPath);
            String structureTree = fsAdapter.generateProjectStructure(projectPath);

            // 3. 构建项目对象
            Project project = Project.builder()
                    .name(projectPath.getFileName().toString())
                    .rootPath(projectPath)
                    .type(detectProjectType(sourceFiles))
                    .sourceFiles(sourceFiles)
                    .structureTree(structureTree)
                    .build();

            System.out.println("项目信息:");
            System.out.println("  - 团队: " + args.team());
            System.out.println("  - 名称: " + project.getName());
            System.out.println("  - 类型: " + project.getType().getDisplayName());
            System.out.println("  - 文件数: " + project.getSourceFiles().size());
            System.out.println("  - 代码行数: " + project.getTotalLines());

            // 4. 执行分析
            System.out.println("\n正在分析项目...");
            AnalysisTask task = analysisUseCase.analyzeProject(project);

            // 5. 获取结果
            if (task.isCompleted()) {
                ReviewReport report = analysisUseCase.getAnalysisResult(task.getTaskId());

                System.out.println("\n分析完成！");
                System.out.println("\n=== 黑客松评审结果 ===");
                System.out.println("团队: " + args.team());
                System.out.println("总体评分: " + report.getOverallScore() + "/100 (" + report.getGrade() + ")");
                System.out.println("\n各维度评分:");
                report.getDimensionScores().forEach((dimension, score) ->
                        System.out.println("  - " + dimension + ": " + score + "/100"));

                // 6. 生成黑客松评分
                HackathonScore hackathonScore = calculateHackathonScore(report);
                System.out.println("\n=== 黑客松评分细则 ===");
                System.out.println("代码质量: " + hackathonScore.getCodeQuality() + "/100 (权重40%)");
                System.out.println("创新性: " + hackathonScore.getInnovation() + "/100 (权重30%)");
                System.out.println("完整性: " + hackathonScore.getCompleteness() + "/100 (权重20%)");
                System.out.println("文档质量: " + hackathonScore.getDocumentation() + "/100 (权重10%)");
                System.out.println("----------------------------------------");
                System.out.println("总分: " + hackathonScore.calculateTotalScore() + "/100 (" + hackathonScore.getGrade() + ")");

                // 7. 保存报告
                if (args.output() != null) {
                    Path outputPath = Paths.get(args.output());
                    reportUseCase.saveReport(report, outputPath, "json");
                    System.out.println("\n评分结果已保存到: " + outputPath);
                }

                if (args.report() != null) {
                    Path reportPath = Paths.get(args.report());
                    reportUseCase.saveReport(report, reportPath, "markdown");
                    System.out.println("详细报告已保存到: " + reportPath);
                }

                System.out.println("\n分析耗时: " + task.getDurationMillis() + " 毫秒");

            } else if (task.isFailed()) {
                System.err.println("分析失败: " + task.getErrorMessage());
                System.exit(1);
            }

        } catch (Exception e) {
            log.error("黑客松评审失败", e);
            System.err.println("评审失败: " + e.getMessage());
            System.exit(1);
        } finally {
            // 清理克隆的临时目录
            if (needsCleanup && projectPath != null) {
                try {
                    deleteDirectory(projectPath);
                } catch (IOException e) {
                    log.warn("清理临时目录失败: {}", projectPath, e);
                }
            }
        }
    }

    /**
     * 计算黑客松评分
     */
    private HackathonScore calculateHackathonScore(ReviewReport report) {
        double overallScore = report.getOverallScore();

        // 基于总体评分分配到各个维度（各维度满分100）
        int codeQuality = (int) Math.min(100, overallScore * 1.1);  // 稍微提高代码质量权重
        int innovation = (int) Math.min(100, overallScore * 0.9);
        int completeness = (int) Math.min(100, overallScore * 0.95);
        int documentation = (int) Math.min(100, overallScore * 0.85);

        return HackathonScore.builder()
                .codeQuality(codeQuality)
                .innovation(innovation)
                .completeness(completeness)
                .documentation(documentation)
                .build();
    }

    /**
     * 检测仓库适配器（GitHub或Gitee）
     */
    private top.yumbo.ai.reviewer.application.port.output.RepositoryPort detectGitRepositoryAdapter(String url) {
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "hackathon-repos");
        return new GiteeRepositoryAdapter(tempDir);
    }

    /**
     * 删除目录及其内容
     */
    private void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            try (var stream = Files.walk(directory)) {
                stream.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                log.warn("删除文件失败: {}", path, e);
                            }
                        });
            }
        }
    }

    /**
     * 解析黑客松命令行参数
     */
    private HackathonArguments parseHackathonArguments(String[] args) {
        String gitUrl = null;
        String giteeUrl = null;
        String directory = null;
        String team = "Unknown Team";
        String branch = "main";
        String output = null;
        String report = null;

        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--github-url", "--git-url" -> gitUrl = args[++i];
                case "--gitee-url" -> giteeUrl = args[++i];
                case "--directory", "--dir", "-d" -> directory = args[++i];
                case "--team", "-t" -> team = args[++i];
                case "--branch", "-b" -> branch = args[++i];
                case "--output", "-o" -> output = args[++i];
                case "--report", "-r" -> report = args[++i];
                case "--help", "-h" -> {
                    printHackathonUsage();
                    System.exit(0);
                }
                default -> throw new IllegalArgumentException("未知参数: " + args[i]);
            }
        }

        // Gitee URL优先，否则使用Git URL
        String finalUrl = giteeUrl != null ? giteeUrl : gitUrl;

        if (finalUrl == null && directory == null) {
            throw new IllegalArgumentException("必须指定 Git URL (--github-url/--gitee-url) 或目录 (--directory)");
        }

        return new HackathonArguments(finalUrl, directory, team, branch, output, report);
    }

    /**
     * 解析命令行参数
     */
    private CLIArguments parseArguments(String[] args) {
        String projectPath = null;
        String outputPath = null;
        String format = "markdown";
        boolean async = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--project", "-p" -> projectPath = args[++i];
                case "--output", "-o" -> outputPath = args[++i];
                case "--format", "-f" -> format = args[++i];
                case "--async", "-a" -> async = true;
                case "--help", "-h" -> {
                    printUsage();
                    System.exit(0);
                }
                default -> throw new IllegalArgumentException("未知参数: " + args[i]);
            }
        }

        if (projectPath == null) {
            throw new IllegalArgumentException("必须指定项目路径 (--project)");
        }

        return new CLIArguments(projectPath, outputPath, format, async);
    }

    /**
     * 打印使用说明
     */
    private void printUsage() {
        System.out.println("AI Reviewer - 六边形架构重构版");
        System.out.println("\n用法:");
        System.out.println("  java -jar hackathon-ai.jar [命令] [选项]");
        System.out.println("\n命令:");
        System.out.println("  hackathon               黑客松项目评审模式");
        System.out.println("  (无命令)                标准项目分析模式");
        System.out.println("\n标准模式选项:");
        System.out.println("  -p, --project <路径>    要分析的项目根目录路径 (必需)");
        System.out.println("  -o, --output <文件>     输出报告的文件路径 (可选)");
        System.out.println("  -f, --format <格式>     报告格式: markdown/html/json (默认: markdown)");
        System.out.println("  -a, --async             异步执行分析");
        System.out.println("  -h, --help              显示此帮助信息");
        System.out.println("\n示例:");
        System.out.println("  java -jar hackathon-ai.jar --project /path/to/project");
        System.out.println("  java -jar hackathon-ai.jar -p . -o report.md -f markdown");
        System.out.println("  java -jar hackathon-ai.jar hackathon --help");
    }

    /**
     * 打印黑客松使用说明
     */
    private void printHackathonUsage() {
        System.out.println("黑客松项目评审工具");
        System.out.println("\n用法:");
        System.out.println("  java -jar hackathon-ai.jar hackathon [选项]");
        System.out.println("\n选项:");
        System.out.println("  --github-url <URL>      GitHub 仓库 URL");
        System.out.println("  --gitee-url <URL>       Gitee 仓库 URL");
        System.out.println("  --directory <路径>      本地项目目录 (替代 Git URL)");
        System.out.println("  --team <团队名>         团队名称 (默认: Unknown Team)");
        System.out.println("  --branch <分支>         Git 分支名称 (默认: main)");
        System.out.println("  --output <文件>         输出评分结果的文件路径 (JSON格式)");
        System.out.println("  --report <文件>         输出详细报告的文件路径 (Markdown格式)");
        System.out.println("  -h, --help              显示此帮助信息");
        System.out.println("\n示例:");
        System.out.println("  # 使用 GitHub URL");
        System.out.println("  java -jar hackathon-ai.jar hackathon \\");
        System.out.println("    --github-url https://github.com/user/project \\");
        System.out.println("    --team \"Team Awesome\" \\");
        System.out.println("    --output score.json \\");
        System.out.println("    --report report.md");
        System.out.println("\n  # 使用 Gitee URL");
        System.out.println("  java -jar hackathon-ai.jar hackathon \\");
        System.out.println("    --gitee-url https://gitee.com/user/project \\");
        System.out.println("    --team \"Team Awesome\" \\");
        System.out.println("    --output score.json");
        System.out.println("\n  # 使用本地目录");
        System.out.println("  java -jar hackathon-ai.jar hackathon \\");
        System.out.println("    --directory /path/to/project \\");
        System.out.println("    --team \"Team Awesome\" \\");
        System.out.println("    --output score.json \\");
        System.out.println("    --report report.md");
    }

    /**
     * 检测项目类型
     */
    private ProjectType detectProjectType(List<SourceFile> files) {
        // 统计各语言文件数
        int javaCount = 0, pythonCount = 0, jsCount = 0;

        for (SourceFile file : files) {
            ProjectType type = file.getProjectType();
            switch (type) {
                case JAVA -> javaCount++;
                case PYTHON -> pythonCount++;
                case JAVASCRIPT, TYPESCRIPT -> jsCount++;
            }
        }

        // 返回主要语言
        int max = Math.max(javaCount, Math.max(pythonCount, jsCount));
        if (max == javaCount) return ProjectType.JAVA;
        if (max == pythonCount) return ProjectType.PYTHON;
        if (max == jsCount) return ProjectType.JAVASCRIPT;

        return ProjectType.UNKNOWN;
    }

    /**
     * 创建AI适配器
     */
    private DeepSeekAIAdapter createAIAdapter() {
        // 从环境变量或配置文件读取
        String apiKey = System.getenv("DEEPSEEK_API_KEY");

        return new DeepSeekAIAdapter(new AIServiceConfig(
                apiKey,
                "https://api.deepseek.com/v1",
                "deepseek-chat",
                4000,
                0.3,
                3,
                3,
                1000,
                30000,
                60000,
                null  // region
        ));
    }

    /**
     * 创建文件系统适配器
     */
    private LocalFileSystemAdapter createFileSystemAdapter() {
        return new LocalFileSystemAdapter(
                new LocalFileSystemAdapter.FileSystemConfig(
                        List.of("*.java", "*.py", "*.js", "*.ts", "*.xml", "*.yaml", "*.md"),
                        List.of("*test*", "*.class", "*.jar", "node_modules", "target", "build"),
                        1024,
                        4
                )
        );
    }

    /**
     * CLI参数记录
     */
    private record CLIArguments(
            String projectPath,
            String outputPath,
            String format,
            boolean async
    ) {}

    /**
     * 黑客松参数记录
     */
    private record HackathonArguments(
            String gitUrl,
            String directory,
            String team,
            String branch,
            String output,
            String report
    ) {}
}

