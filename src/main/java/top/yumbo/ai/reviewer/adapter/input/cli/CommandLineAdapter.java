package top.yumbo.ai.reviewer.adapter.input.cli;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.reviewer.adapter.output.filesystem.LocalFileSystemAdapter;
import top.yumbo.ai.reviewer.application.port.input.ProjectAnalysisUseCase;
import top.yumbo.ai.reviewer.application.port.input.ReportGenerationUseCase;
import top.yumbo.ai.reviewer.domain.model.*;
import top.yumbo.ai.reviewer.infrastructure.config.Configuration;
import top.yumbo.ai.reviewer.infrastructure.config.ConfigurationLoader;
import top.yumbo.ai.reviewer.infrastructure.di.ApplicationModule;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 命令行适配器 - 通用代码审查入口
 * 提供标准的项目代码审查命令行接口
 *
 * <p>职责：
 * <ul>
 *   <li>解析通用代码审查的命令行参数</li>
 *   <li>执行标准的项目分析流程</li>
 *   <li>生成多格式代码审查报告</li>
 * </ul>
 *
 * <p><b>注意：</b>黑客松评审请使用 {@link top.yumbo.ai.reviewer.application.hackathon.cli.HackathonCommandLineApp}
 *
 * @author AI-Reviewer Team
 * @version 2.0 (六边形架构重构版)
 * @since 2025-11-13
 */
@Slf4j
public class CommandLineAdapter {

    private final ProjectAnalysisUseCase analysisUseCase;
    private final ReportGenerationUseCase reportUseCase;
    private final Configuration configuration;
    private final LocalFileSystemAdapter fileSystemAdapter;

    /**
     * 构造函数注入
     */
    @Inject
    public CommandLineAdapter(
            ProjectAnalysisUseCase analysisUseCase,
            ReportGenerationUseCase reportUseCase,
            Configuration configuration,
            LocalFileSystemAdapter fileSystemAdapter) {
        this.analysisUseCase = analysisUseCase;
        this.reportUseCase = reportUseCase;
        this.configuration = configuration;
        this.fileSystemAdapter = fileSystemAdapter;
    }

    /**
     * 通用代码审查主入口
     */
    public static void main(String[] args) {
        try {
            // 1. 加载配置
            log.info("正在加载配置...");
            Configuration config = ConfigurationLoader.load();

            // 2. 创建依赖注入容器
            log.debug("正在初始化依赖注入容器...");
            Injector injector = Guice.createInjector(new ApplicationModule(config));

            // 3. 获取 CLI 适配器实例
            CommandLineAdapter cli = injector.getInstance(CommandLineAdapter.class);

            log.info("🤖 AI-Reviewer (通用代码审查) 已启动");
            log.info("AI 服务: {} (model: {})", config.getAiProvider(), config.getAiModel());

            // 4. 解析并执行命令
            CLIArguments cliArgs = cli.parseArguments(args);
            cli.execute(cliArgs);

        } catch (Configuration.ConfigurationException e) {
            log.error("配置错误: {}", e.getMessage());
            System.err.println("❌ 配置错误: " + e.getMessage());
            System.err.println("\n请检查:");
            System.err.println("  1. 环境变量 AI_API_KEY 是否设置");
            System.err.println("  2. config.yaml 文件是否正确配置");
            System.exit(1);
        } catch (IllegalArgumentException e) {
            log.error("参数错误: {}", e.getMessage());
            System.err.println("❌ 参数错误: " + e.getMessage());
            printUsage();
            System.exit(1);
        } catch (Exception e) {
            log.error("执行失败", e);
            System.err.println("❌ 错误: " + e.getMessage());
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
        Path projectRoot = Paths.get(args.projectPath());

        List<SourceFile> sourceFiles = fileSystemAdapter.scanProjectFiles(projectRoot);
        String structureTree = fileSystemAdapter.generateProjectStructure(projectRoot);

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
    private static void printUsage() {
        System.out.println("🤖 AI-Reviewer - 通用代码审查工具");
        System.out.println("\n用法:");
        System.out.println("  java -cp ai-reviewer.jar top.yumbo.ai.reviewer.adapter.input.cli.CommandLineAdapter [选项]");
        System.out.println("\n选项:");
        System.out.println("  -p, --project <路径>    要分析的项目根目录路径 (必需)");
        System.out.println("  -o, --output <文件>     输出报告的文件路径 (可选)");
        System.out.println("  -f, --format <格式>     报告格式: markdown/html/json (默认: markdown)");
        System.out.println("  -a, --async             异步执行分析");
        System.out.println("  -h, --help              显示此帮助信息");
        System.out.println("\n示例:");
        System.out.println("  # 分析项目并输出到控制台");
        System.out.println("  java -cp ai-reviewer.jar top.yumbo.ai.reviewer.adapter.input.cli.CommandLineAdapter \\");
        System.out.println("    --project /path/to/project");
        System.out.println("\n  # 生成 Markdown 报告");
        System.out.println("  java -cp ai-reviewer.jar top.yumbo.ai.reviewer.adapter.input.cli.CommandLineAdapter \\");
        System.out.println("    -p . -o report.md -f markdown");
        System.out.println("\n  # 异步分析");
        System.out.println("  java -cp ai-reviewer.jar top.yumbo.ai.reviewer.adapter.input.cli.CommandLineAdapter \\");
        System.out.println("    -p /path/to/project -a");
        System.out.println("\n💡 提示:");
        System.out.println("  - 黑客松项目评审请使用: HackathonCommandLineApp");
        System.out.println("    java -cp ai-reviewer.jar top.yumbo.ai.reviewer.application.hackathon.cli.HackathonCommandLineApp --help");
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
     * CLI参数记录
     */
    private record CLIArguments(
            String projectPath,
            String outputPath,
            String format,
            boolean async
    ) {}
}

