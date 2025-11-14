package top.yumbo.ai.reviewer.application.hackathon.cli;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.reviewer.adapter.output.archive.ZipArchiveAdapter;
import top.yumbo.ai.reviewer.adapter.output.filesystem.LocalFileSystemAdapter;
import top.yumbo.ai.reviewer.adapter.output.repository.GitRepositoryAdapter;
import top.yumbo.ai.reviewer.adapter.output.storage.S3StorageAdapter;
import top.yumbo.ai.reviewer.adapter.output.storage.S3StorageConfig;
import top.yumbo.ai.reviewer.application.service.S3StorageService;
import top.yumbo.ai.reviewer.domain.model.S3DownloadResult;
import top.yumbo.ai.reviewer.application.hackathon.service.HackathonScoringService;
import top.yumbo.ai.reviewer.application.port.output.CloneRequest;
import top.yumbo.ai.reviewer.application.port.output.RepositoryPort;
import top.yumbo.ai.reviewer.application.service.ProjectAnalysisService;
import top.yumbo.ai.reviewer.application.service.ReportGenerationService;
import top.yumbo.ai.reviewer.domain.hackathon.model.HackathonScore;
import top.yumbo.ai.reviewer.domain.hackathon.model.HackathonScoringConfig;
import top.yumbo.ai.reviewer.domain.hackathon.model.DimensionScoringRegistry;
import top.yumbo.ai.reviewer.domain.model.*;
import top.yumbo.ai.reviewer.infrastructure.config.Configuration;
import top.yumbo.ai.reviewer.infrastructure.config.ConfigurationLoader;
import top.yumbo.ai.reviewer.infrastructure.di.ApplicationModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 黑客松命令行应用
 * 专门用于黑客松项目评审的命令行入口
 *
 * <p>职责：
 * <ul>
 *   <li>解析黑客松特定的命令行参数</li>
 *   <li>协调 Git 克隆、项目扫描、评分流程</li>
 *   <li>生成黑客松评分报告和排行榜</li>
 * </ul>
 *
 * @author AI-Reviewer Team
 * @version 2.0 (六边形架构重构版)
 * @since 2025-11-13
 */
@Slf4j
public class HackathonCommandLineApp {

    private final ProjectAnalysisService analysisService;
    private final ReportGenerationService reportService;
    private final LocalFileSystemAdapter fileSystemAdapter;
    private final HackathonScoringService scoringService;
    private final HackathonScoringConfig scoringConfig;
    private final ZipArchiveAdapter zipArchiveAdapter;
    private final Configuration configuration;
    private S3StorageService s3StorageService;

    @Inject
    public HackathonCommandLineApp(
            ProjectAnalysisService analysisService,
            ReportGenerationService reportService,
            LocalFileSystemAdapter fileSystemAdapter,
            Configuration configuration) {
        this.analysisService = analysisService;
        this.reportService = reportService;
        this.fileSystemAdapter = fileSystemAdapter;
        this.configuration = configuration;
        // 初始化黑客松评分服务（动态配置版）
        this.scoringService = new HackathonScoringService();
        this.scoringConfig = HackathonScoringConfig.createDefault();
        // 初始化 ZIP 解压适配器
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "hackathon-zip-extract");
        this.zipArchiveAdapter = new ZipArchiveAdapter(tempDir);
        // 初始化 S3 服务（如果配置了）
        initializeS3Service();
        log.info("✅ 黑客松评分服务已初始化（动态配置）");
    }

    /**
     * 黑客松应用主入口
     */
    public static void main(String[] args) {
        try {
            // 1. 加载配置
            log.info("正在加载配置...");
            Configuration config = ConfigurationLoader.load();

            // 2. 创建依赖注入容器
            log.debug("正在初始化依赖注入容器...");
            Injector injector = Guice.createInjector(new ApplicationModule(config));

            // 3. 获取黑客松 CLI 应用实例
            HackathonCommandLineApp app = injector.getInstance(HackathonCommandLineApp.class);

            log.info("🏆 黑客松评审工具已启动");
            log.info("AI 服务: {} (model: {})", config.getAiProvider(), config.getAiModel());

            // 4. 解析并执行命令
            HackathonArguments hackArgs = app.parseArguments(args);
            app.execute(hackArgs);

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
     * 执行黑客松项目评审
     */
    public void execute(HackathonArguments args) {
        log.info("开始黑客松项目评审: {}",
                args.gitUrl() != null ? args.gitUrl() :
                args.s3Path() != null ? "s3://" + configuration.getS3BucketName() + "/" + args.s3Path() :
                args.zipFile() != null ? args.zipFile() :
                args.directory());

        Path projectPath = null;
        boolean needsCleanup = false;

        try {
            // 1. 获取项目路径
            if (args.gitUrl() != null) {
                projectPath = cloneProject(args);
                needsCleanup = true;
            } else if (args.s3Path() != null) {
                projectPath = downloadFromS3(args);
                needsCleanup = true;
            } else if (args.zipFile() != null) {
                projectPath = extractZipFile(args);
                needsCleanup = true;
            } else if (args.directory() != null) {
                projectPath = getLocalProject(args.directory());
            }

            // 2. 扫描和分析项目
            Project project = scanAndBuildProject(projectPath);
            printProjectInfo(args.team(), project);

            // 3. 执行分析
            System.out.println("\n正在分析项目...");
            AnalysisTask task = analysisService.analyzeProject(project);

            // 4. 处理分析结果
            if (task.isCompleted()) {
                ReviewReport report = analysisService.getAnalysisResult(task.getTaskId());
                processAnalysisResult(args, report, task);
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
                cleanupTemporaryDirectory(projectPath);
            }
        }
    }

    /**
     * 克隆项目
     */
    private Path cloneProject(HackathonArguments args) throws RepositoryPort.RepositoryException {
        System.out.println("正在克隆项目: " + args.gitUrl());
        RepositoryPort repoPort = detectGitRepositoryAdapter(args.gitUrl());

        CloneRequest cloneRequest = CloneRequest.builder()
                .url(args.gitUrl())
                .branch(args.branch())
                .timeoutSeconds(300)
                .build();

        Path projectPath = repoPort.cloneRepository(cloneRequest);
        System.out.println("项目克隆完成: " + projectPath);
        return projectPath;
    }

    /**
     * 解压 ZIP 文件
     */
    private Path extractZipFile(HackathonArguments args) throws ZipArchiveAdapter.ZipExtractionException {
        System.out.println("正在解压 ZIP 文件: " + args.zipFile());
        Path zipPath = Paths.get(args.zipFile());

        if (!Files.exists(zipPath)) {
            throw new IllegalArgumentException("ZIP 文件不存在: " + args.zipFile());
        }

        Path projectPath = zipArchiveAdapter.extractZipFile(zipPath);
        System.out.println("ZIP 文件解压完成: " + projectPath);

        // 如果解压后的目录中只有一个子目录，使用该子目录作为项目根目录
        projectPath = findProjectRoot(projectPath);

        return projectPath;
    }

    /**
     * 查找项目根目录
     * 如果解压后只有一个子目录，通常该子目录才是真正的项目根目录
     */
    private Path findProjectRoot(Path extractedDir) {
        try (var stream = Files.list(extractedDir)) {
            List<Path> entries = stream.toList();

            // 如果只有一个子目录，且没有其他文件，使用该子目录
            if (entries.size() == 1 && Files.isDirectory(entries.get(0))) {
                Path subDir = entries.get(0);
                log.info("检测到单一子目录，使用作为项目根: {}", subDir.getFileName());
                return subDir;
            }
        } catch (IOException e) {
            log.warn("无法检查解压目录结构: {}", e.getMessage());
        }

        return extractedDir;
    }

    /**
     * 获取本地项目
     */
    private Path getLocalProject(String directory) {
        Path projectPath = Paths.get(directory);
        if (!Files.exists(projectPath)) {
            throw new IllegalArgumentException("目录不存在: " + directory);
        }
        System.out.println("使用本地目录: " + projectPath);
        return projectPath;
    }

    /**
     * 初始化 S3 服务
     */
    private void initializeS3Service() {
        if (configuration.getS3BucketName() == null || configuration.getS3BucketName().isBlank()) {
            log.debug("S3 存储未配置，跳过初始化");
            return;
        }

        try {
            S3StorageConfig s3Config = S3StorageConfig.builder()
                    .region(configuration.getS3Region())
                    .bucketName(configuration.getS3BucketName())
                    .accessKeyId(configuration.getS3AccessKeyId())
                    .secretAccessKey(configuration.getS3SecretAccessKey())
                    .maxConcurrency(configuration.getS3MaxConcurrency())
                    .connectTimeout(configuration.getS3ConnectTimeout())
                    .readTimeout(configuration.getS3ReadTimeout())
                    .maxRetries(configuration.getS3MaxRetries())
                    .retryDelay(configuration.getS3RetryDelay())
                    .useAccelerateEndpoint(configuration.getS3UseAccelerateEndpoint())
                    .usePathStyleAccess(configuration.getS3UsePathStyleAccess())
                    .endpoint(configuration.getS3Endpoint())
                    .build();

            S3StorageAdapter s3Adapter = new S3StorageAdapter(s3Config);
            this.s3StorageService = new S3StorageService(s3Adapter);
            log.info("✅ S3 存储服务已初始化 - Bucket: {}, Region: {}",
                    configuration.getS3BucketName(), configuration.getS3Region());
        } catch (Exception e) {
            log.error("初始化 S3 服务失败: {}", e.getMessage(), e);
            throw new RuntimeException("初始化 S3 服务失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从 S3 下载项目
     */
    private Path downloadFromS3(HackathonArguments args) {
        if (s3StorageService == null) {
            throw new IllegalStateException("S3 服务未初始化。请在 config.yaml 中配置 s3Storage.bucketName");
        }

        System.out.println("正在从 S3 下载项目: " + args.s3Path());
        System.out.println("Bucket: " + configuration.getS3BucketName());
        System.out.println("路径: " + args.s3Path());

        // 创建临时目录
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "hackathon-s3-download");
        try {
            if (!Files.exists(tempDir)) {
                Files.createDirectories(tempDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("创建临时目录失败: " + e.getMessage(), e);
        }

        // 从 S3 路径提取项目名称
        String s3Path = args.s3Path();
        String projectName = extractProjectNameFromS3Path(s3Path);
        Path localDir = tempDir.resolve(projectName + "-" + System.currentTimeMillis());

        // 下载
        S3DownloadResult result = s3StorageService.downloadProjectForReview(
                configuration.getS3BucketName(),
                s3Path,
                localDir
        );

        if (!result.isSuccess()) {
            System.err.println("⚠️  部分文件下载失败:");
            result.getErrors().forEach(error -> System.err.println("  - " + error));
        }

        System.out.println("S3 项目下载完成:");
        System.out.println("  - 总文件数: " + result.getTotalFileCount());
        System.out.println("  - 成功: " + result.getSuccessCount());
        System.out.println("  - 失败: " + result.getFailureCount());
        System.out.println("  - 总大小: " + String.format("%.2f MB", result.getTotalSizeInMB()));
        System.out.println("  - 耗时: " + String.format("%.2f 秒", result.getDurationSeconds()));
        System.out.println("  - 本地目录: " + localDir);

        // 智能识别项目根目录
        return findProjectRoot(localDir);
    }

    /**
     * 从 S3 路径提取项目名称
     */
    private String extractProjectNameFromS3Path(String s3Path) {
        if (s3Path == null || s3Path.isEmpty()) {
            return "project";
        }
        // 移除尾部的斜杠
        s3Path = s3Path.replaceAll("/+$", "");
        // 获取最后一段作为项目名
        int lastSlash = s3Path.lastIndexOf('/');
        if (lastSlash >= 0) {
            return s3Path.substring(lastSlash + 1);
        }
        return s3Path;
    }

    /**
     * 扫描并构建项目对象
     */
    private Project scanAndBuildProject(Path projectPath) {
        System.out.println("正在扫描项目...");
        List<SourceFile> sourceFiles = fileSystemAdapter.scanProjectFiles(projectPath);
        String structureTree = fileSystemAdapter.generateProjectStructure(projectPath);

        return Project.builder()
                .name(projectPath.getFileName().toString())
                .rootPath(projectPath)
                .type(detectProjectType(sourceFiles))
                .sourceFiles(sourceFiles)
                .structureTree(structureTree)
                .build();
    }

    /**
     * 打印项目信息
     */
    private void printProjectInfo(String team, Project project) {
        System.out.println("项目信息:");
        System.out.println("  - 团队: " + team);
        System.out.println("  - 名称: " + project.getName());
        System.out.println("  - 类型: " + project.getType().getDisplayName());
        System.out.println("  - 文件数: " + project.getSourceFiles().size());
        System.out.println("  - 代码行数: " + project.getTotalLines());
    }

    /**
     * 处理分析结果
     */
    private void processAnalysisResult(HackathonArguments args, ReviewReport report, AnalysisTask task) {
        System.out.println("\n分析完成！");
        System.out.println("\n=== 黑客松评审结果 ===");
        System.out.println("团队: " + args.team());
        System.out.println("总体评分: " + report.getOverallScore() + "/100 (" + report.getGrade() + ")");

        // 显示总体评语
        if (report.getOverallSummary() != null && !report.getOverallSummary().isBlank()) {
            System.out.println("\n总体评语:");
            System.out.println(report.getOverallSummary());
        }

        System.out.println("\n各维度评分:");
        report.getDimensionScores().forEach((dimension, score) -> {
            System.out.println("  - " + dimension + ": " + score + "/100");
            // 显示维度评语
            String comment = report.getDimensionComment(dimension);
            if (comment != null && !comment.isBlank()) {
                System.out.println("    评语: " + comment);
            }
        });

        // 生成黑客松评分
        HackathonScore hackathonScore = calculateHackathonScore(report);
        printHackathonScore(hackathonScore);

        // 保存报告
        saveReports(args, report);

        System.out.println("\n分析耗时: " + task.getDurationMillis() + " 毫秒");
    }

    /**
     * 打印黑客松评分（动态版）
     * 根据配置文件动态显示所有维度
     */
    private void printHackathonScore(HackathonScore score) {
        System.out.println("\n=== 黑客松评分细则（动态配置版）===");

        // 动态显示所有维度
        int index = 1;
        for (String dimensionName : scoringConfig.getAllDimensions()) {
            double weight = scoringConfig.getDimensionWeight(dimensionName);
            String displayName = scoringConfig.getDimensionDisplayName(dimensionName);

            // 获取维度分数（映射到固定字段或使用默认值）
            int dimensionScore = getDimensionScore(score, dimensionName);

            System.out.printf("%d. %s: %d/100 (权重%.0f%%)\n",
                index++, displayName, dimensionScore, weight * 100);
        }

        System.out.println("----------------------------------------");
        System.out.printf("📊 总分: %d/100 (%s)\n", score.calculateTotalScore(), score.getGrade());
        System.out.printf("📝 评价: %s\n", score.getGradeDescription());

        // 显示维度数量
        System.out.printf("\n💡 当前评分维度: %d个\n", scoringConfig.getAllDimensions().size());
        System.out.printf("📋 启用的规则: %d个\n", scoringConfig.getEnabledRules().size());
    }

    /**
     * 获取维度分数（策略模式 - 零硬编码）
     */
    private int getDimensionScore(HackathonScore score, String dimensionName) {
        // 使用注册表获取Score字段值（消除硬编码switch）
        DimensionScoringRegistry registry = DimensionScoringRegistry.createDefault();
        Integer fieldValue = registry.getScoreFieldValue(dimensionName, score);

        if (fieldValue != null) {
            return fieldValue;
        }

        // 自定义维度使用总分
        log.debug("未映射的维度: {}, 使用总分", dimensionName);
        return score.calculateTotalScore();
    }

    /**
     * 保存报告
     */
    private void saveReports(HackathonArguments args, ReviewReport report) {
        if (args.output() != null) {
            Path outputPath = Paths.get(args.output());
            reportService.saveReport(report, outputPath, "json");
            System.out.println("\n评分结果已保存到: " + outputPath);
        }

        if (args.report() != null) {
            Path reportPath = Paths.get(args.report());
            reportService.saveReport(report, reportPath, "markdown");
            System.out.println("详细报告已保存到: " + reportPath);
        }
    }

    /**
     * 计算黑客松评分（动态版）
     * 使用HackathonScoringService进行基于AST和规则的评分
     */
    private HackathonScore calculateHackathonScore(ReviewReport report) {
        try {
            // 使用动态评分服务
            // 注意：这里需要Project对象，但当前上下文没有，所以使用简化方式
            log.info("🎯 使用黑客松评分服务进行评分");

            // 从ReviewReport构建简化的Project对象用于评分
            Project project = buildProjectFromReport(report);

            // 调用动态评分服务
            return scoringService.calculateScore(report, project);

        } catch (Exception e) {
            log.error("动态评分失败，使用降级评分: {}", e.getMessage());
            // 降级：使用简化评分
            return buildFallbackScore(report);
        }
    }

    /**
     * 从ReviewReport构建简化的Project对象
     */
    private Project buildProjectFromReport(ReviewReport report) {
        return Project.builder()
            .name(report.getProjectName())
            .rootPath(Paths.get(report.getProjectPath()))
            .type(ProjectType.UNKNOWN)
            .sourceFiles(new ArrayList<>())
            .build();
    }

    /**
     * 降级评分（当动态评分失败时使用）
     */
    private HackathonScore buildFallbackScore(ReviewReport report) {
        double overallScore = report.getOverallScore();

        // 基于总体评分分配到各个维度
        int codeQuality = (int) Math.min(100, overallScore * 1.1);
        int innovation = (int) Math.min(100, overallScore * 0.9);
        int completeness = (int) Math.min(100, overallScore * 0.95);
        int documentation = (int) Math.min(100, overallScore * 0.85);

        log.warn("⚠️ 使用降级评分方法");

        return HackathonScore.builder()
                .codeQuality(codeQuality)
                .innovation(innovation)
                .completeness(completeness)
                .documentation(documentation)
                .build();
    }

    /**
     * 创建Git仓库适配器（支持 GitHub、Gitee、GitLab）
     */
    private RepositoryPort detectGitRepositoryAdapter(String url) {
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "hackathon-repos");
        return GitRepositoryAdapter.create(tempDir, url);
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
     * 清理临时目录
     */
    private void cleanupTemporaryDirectory(Path directory) {
        try {
            deleteDirectory(directory);
        } catch (IOException e) {
            log.warn("清理临时目录失败: {}", directory, e);
        }
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
     * 解析命令行参数
     */
    private HackathonArguments parseArguments(String[] args) {
        String gitUrl = null;
        String giteeUrl = null;
        String directory = null;
        String zipFile = null;
        String s3Path = null;
        String team = "Unknown Team";
        String branch = "";
        String output = null;
        String report = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--github-url", "--git-url" -> gitUrl = args[++i];
                case "--gitee-url" -> giteeUrl = args[++i];
                case "--directory", "--dir", "-d" -> directory = args[++i];
                case "--zip", "-z" -> zipFile = args[++i];
                case "--s3-path", "--s3", "-s" -> s3Path = args[++i];
                case "--team", "-t" -> team = args[++i];
                case "--branch", "-b" -> branch = args[++i];
                case "--output", "-o" -> output = args[++i];
                case "--report", "-r" -> report = args[++i];
                case "--help", "-h" -> {
                    printUsage();
                    System.exit(0);
                }
                default -> throw new IllegalArgumentException("未知参数: " + args[i]);
            }
        }

        // Gitee URL优先，否则使用Git URL
        String finalUrl = giteeUrl != null ? giteeUrl : gitUrl;

        if (finalUrl == null && directory == null && zipFile == null && s3Path == null) {
            throw new IllegalArgumentException("必须指定 Git URL (--github-url/--gitee-url)、目录 (--directory)、ZIP 文件 (--zip) 或 S3 路径 (--s3-path)");
        }

        return new HackathonArguments(finalUrl, directory, zipFile, s3Path, team, branch, output, report);
    }

    /**
     * 打印使用说明
     */
    private static void printUsage() {
        System.out.println("🏆 黑客松项目评审工具");
        System.out.println("\n用法:");
        System.out.println("  java -jar hackathon-reviewer.jar [选项]");
        System.out.println("\n选项:");
        System.out.println("  --github-url <URL>      GitHub 仓库 URL");
        System.out.println("  --gitee-url <URL>       Gitee 仓库 URL (优先使用)");
        System.out.println("  --directory <路径>      本地项目目录");
        System.out.println("  -d, --dir <路径>        同 --directory");
        System.out.println("  --zip <文件>            ZIP 压缩包文件路径");
        System.out.println("  -z <文件>               同 --zip");
        System.out.println("  --s3-path <路径>        S3 存储路径 (需在 config.yaml 配置 bucket)");
        System.out.println("  -s <路径>               同 --s3-path");
        System.out.println("  --team <团队名>         团队名称 (默认: Unknown Team)");
        System.out.println("  -t <团队名>             同 --team");
        System.out.println("  --branch <分支>         Git 分支名称 (默认: main)");
        System.out.println("  -b <分支>               同 --branch");
        System.out.println("  --output <文件>         输出评分结果的文件路径 (JSON格式)");
        System.out.println("  -o <文件>               同 --output");
        System.out.println("  --report <文件>         输出详细报告的文件路径 (Markdown格式)");
        System.out.println("  -r <文件>               同 --report");
        System.out.println("  -h, --help              显示此帮助信息");
        System.out.println("\n示例:");
        System.out.println("  # 使用 GitHub URL");
        System.out.println("  java -jar hackathon-reviewer.jar \\");
        System.out.println("    --github-url https://github.com/user/project \\");
        System.out.println("    --team \"Team Awesome\" \\");
        System.out.println("    --output score.json \\");
        System.out.println("    --report report.md");
        System.out.println("\n  # 使用 Gitee URL");
        System.out.println("  java -jar hackathon-reviewer.jar \\");
        System.out.println("    --gitee-url https://gitee.com/user/project \\");
        System.out.println("    -t \"Team Awesome\" -o score.json");
        System.out.println("\n  # 使用本地目录");
        System.out.println("  java -jar hackathon-reviewer.jar \\");
        System.out.println("    -d /path/to/project \\");
        System.out.println("    -t \"Team Awesome\" \\");
        System.out.println("    -o score.json -r report.md");
        System.out.println("\n  # 使用 ZIP 压缩包");
        System.out.println("  java -jar hackathon-reviewer.jar \\");
        System.out.println("    --zip /path/to/project.zip \\");
        System.out.println("    -t \"Team Awesome\" \\");
        System.out.println("    -o score.json -r report.md");
        System.out.println("\n  # 使用 S3 存储路径");
        System.out.println("  java -jar hackathon-reviewer.jar \\");
        System.out.println("    --s3-path projects/team-awesome/ \\");
        System.out.println("    -t \"Team Awesome\" \\");
        System.out.println("    -o score.json -r report.md");
    }

    /**
     * 黑客松参数记录
     */
    private record HackathonArguments(
            String gitUrl,
            String directory,
            String zipFile,
            String s3Path,
            String team,
            String branch,
            String output,
            String report
    ) {
    }
}

