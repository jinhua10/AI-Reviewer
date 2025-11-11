package top.yumbo.ai.refactor.adapter.input.cli;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.refactor.adapter.output.cache.FileCacheAdapter;
import top.yumbo.ai.refactor.adapter.output.filesystem.LocalFileSystemAdapter;
import top.yumbo.ai.refactor.application.port.output.AIServicePort;
import top.yumbo.ai.refactor.application.service.ProjectAnalysisService;
import top.yumbo.ai.refactor.application.service.ReportGenerationService;
import top.yumbo.ai.refactor.domain.model.AnalysisConfiguration;
import top.yumbo.ai.refactor.domain.model.AnalysisProgress;
import top.yumbo.ai.refactor.domain.model.AnalysisTask;
import top.yumbo.ai.refactor.domain.model.AnalysisTask.AnalysisStatus;
import top.yumbo.ai.refactor.domain.model.Project;
import top.yumbo.ai.refactor.domain.model.ProjectType;
import top.yumbo.ai.refactor.domain.model.ReviewReport;
import top.yumbo.ai.refactor.domain.model.SourceFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

/**
 * 命令行界面适配器
 * 提供用户友好的命令行交互
 */
@Slf4j
public class CommandLineInterface {

    private final ProjectAnalysisService analysisService;
    private final ReportGenerationService reportService;
    private final LocalFileSystemAdapter fileSystemAdapter;
    private final Scanner scanner;

    public CommandLineInterface(
            ProjectAnalysisService analysisService,
            ReportGenerationService reportService,
            LocalFileSystemAdapter fileSystemAdapter) {
        this.analysisService = analysisService;
        this.reportService = reportService;
        this.fileSystemAdapter = fileSystemAdapter;
        this.scanner = new Scanner(System.in);
    }

    /**
     * 启动CLI主循环
     */
    public void start() {
        printWelcomeBanner();

        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> analyzeProjectInteractive();
                case "2" -> analyzeProjectQuick();
                case "3" -> viewTaskStatus();
                case "4" -> generateReport();
                case "5" -> showHelp();
                case "0" -> running = false;
                default -> System.out.println("❌ 无效选项，请重新选择");
            }
        }

        System.out.println("\n👋 再见！感谢使用 AI-Reviewer!");
        scanner.close();
    }

    /**
     * 打印欢迎横幅
     */
    private void printWelcomeBanner() {
        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║                                                ║");
        System.out.println("║        🤖 AI-Reviewer 2.0 🤖                  ║");
        System.out.println("║                                                ║");
        System.out.println("║        智能代码审查工具                        ║");
        System.out.println("║                                                ║");
        System.out.println("╚════════════════════════════════════════════════╝\n");
    }

    /**
     * 打印主菜单
     */
    private void printMenu() {
        System.out.println("\n📋 主菜单：");
        System.out.println("  1. 🔍 分析项目（交互式）");
        System.out.println("  2. ⚡ 快速分析");
        System.out.println("  3. 📊 查看任务状态");
        System.out.println("  4. 📄 生成报告");
        System.out.println("  5. ❓ 帮助");
        System.out.println("  0. 🚪 退出");
        System.out.print("\n请选择操作 [0-5]: ");
    }

    /**
     * 交互式项目分析
     */
    private void analyzeProjectInteractive() {
        System.out.println("\n🔍 === 项目分析 ===\n");

        // 1. 输入项目路径
        System.out.print("📁 请输入项目路径: ");
        String pathInput = scanner.nextLine().trim();
        Path projectPath = Paths.get(pathInput);

        if (!Files.exists(projectPath)) {
            System.out.println("❌ 错误: 项目路径不存在");
            return;
        }

        // 2. 选择项目类型
        System.out.println("\n请选择项目类型:");
        System.out.println("  1. Java");
        System.out.println("  2. Python");
        System.out.println("  3. JavaScript/React");
        System.out.println("  4. 自动检测");
        System.out.print("选择 [1-4]: ");

        String typeChoice = scanner.nextLine().trim();
        ProjectType projectType = switch (typeChoice) {
            case "1" -> ProjectType.JAVA;
            case "2" -> ProjectType.PYTHON;
            case "3" -> ProjectType.JAVASCRIPT;
            default -> detectProjectType(projectPath);
        };

        // 3. 选择分析模式
        System.out.println("\n选择分析模式:");
        System.out.println("  1. 同步分析（等待完成）");
        System.out.println("  2. 异步分析（后台运行）");
        System.out.print("选择 [1-2]: ");

        String modeChoice = scanner.nextLine().trim();
        boolean async = modeChoice.equals("2");

        // 4. 执行分析
        try {
            System.out.println("\n⏳ 正在扫描项目文件...");
            List<SourceFile> files = fileSystemAdapter.scanProjectFiles(projectPath);
            System.out.println("✅ 找到 " + files.size() + " 个源文件");

            Project project = Project.builder()
                    .name(projectPath.getFileName().toString())
                    .rootPath(projectPath)
                    .type(projectType)
                    .sourceFiles(files)
                    .build();

            if (async) {
                String taskId = analysisService.analyzeProjectAsync(project);
                System.out.println("✅ 分析任务已启动: " + taskId);
                System.out.println("💡 提示: 使用菜单选项 3 查看任务状态");
            } else {
                System.out.println("⏳ 正在分析项目，请稍候...");
                AnalysisTask task = analysisService.analyzeProject(project);

                if (task.isCompleted()) {
                    ReviewReport report = analysisService.getAnalysisResult(task.getTaskId());
                    printReportSummary(report);

                    // 询问是否生成详细报告
                    System.out.print("\n是否生成详细报告？[Y/n]: ");
                    String generateReport = scanner.nextLine().trim();
                    if (generateReport.isEmpty() || generateReport.equalsIgnoreCase("Y")) {
                        generateReportForTask(task.getTaskId());
                    }
                } else {
                    System.out.println("❌ 分析失败");
                }
            }

        } catch (Exception e) {
            System.out.println("❌ 分析失败: " + e.getMessage());
            log.error("Analysis failed", e);
        }
    }

    /**
     * 快速分析（默认选项）
     */
    private void analyzeProjectQuick() {
        System.out.println("\n⚡ === 快速分析 ===\n");

        System.out.print("📁 项目路径: ");
        String pathInput = scanner.nextLine().trim();
        Path projectPath = Paths.get(pathInput);

        if (!Files.exists(projectPath)) {
            System.out.println("❌ 错误: 项目路径不存在");
            return;
        }

        try {
            System.out.println("⏳ 分析中...");

            List<SourceFile> files = fileSystemAdapter.scanProjectFiles(projectPath);
            ProjectType type = detectProjectType(projectPath);

            Project project = Project.builder()
                    .name(projectPath.getFileName().toString())
                    .rootPath(projectPath)
                    .type(type)
                    .sourceFiles(files)
                    .build();

            AnalysisTask task = analysisService.analyzeProject(project);
            ReviewReport report = analysisService.getAnalysisResult(task.getTaskId());

            printReportSummary(report);

            // 自动生成Markdown报告
            String markdown = reportService.generateMarkdownReport(report);
            Path outputPath = Paths.get(project.getName() + "-report.md");
            reportService.saveReport(report, outputPath, "markdown");
            System.out.println("\n📄 报告已保存: " + outputPath.toAbsolutePath());

        } catch (Exception e) {
            System.out.println("❌ 分析失败: " + e.getMessage());
            log.error("Quick analysis failed", e);
        }
    }

    /**
     * 查看任务状态
     */
    private void viewTaskStatus() {
        System.out.println("\n📊 === 任务状态 ===\n");

        System.out.print("请输入任务ID: ");
        String taskId = scanner.nextLine().trim();

        try {
            AnalysisTask task = analysisService.getTaskStatus(taskId);

            if (task == null) {
                System.out.println("❌ 任务不存在: " + taskId);
                return;
            }

            System.out.println("任务ID: " + task.getTaskId());
            System.out.println("项目名: " + task.getProject().getName());
            System.out.println("状态: " + getStatusEmoji(task) + " " + getStatusText(task));

            if (task.getProgress() != null) {
                AnalysisProgress progress = task.getProgress();
                System.out.println("进度: " + progress.getCompletedSteps() + "/" + progress.getTotalSteps());
                System.out.println("当前阶段: " + progress.getCurrentPhase());
            }

            if (task.isCompleted()) {
                ReviewReport report = analysisService.getAnalysisResult(taskId);
                printReportSummary(report);
            }

        } catch (Exception e) {
            System.out.println("❌ 查询失败: " + e.getMessage());
            log.error("Status check failed", e);
        }
    }

    /**
     * 生成报告
     */
    private void generateReport() {
        System.out.println("\n📄 === 生成报告 ===\n");

        System.out.print("请输入任务ID: ");
        String taskId = scanner.nextLine().trim();

        generateReportForTask(taskId);
    }

    /**
     * 为指定任务生成报告
     */
    private void generateReportForTask(String taskId) {
        try {
            ReviewReport report = analysisService.getAnalysisResult(taskId);

            if (report == null) {
                System.out.println("❌ 报告不存在");
                return;
            }

            System.out.println("\n选择报告格式:");
            System.out.println("  1. Markdown");
            System.out.println("  2. HTML");
            System.out.println("  3. JSON");
            System.out.println("  4. 全部格式");
            System.out.print("选择 [1-4]: ");

            String formatChoice = scanner.nextLine().trim();

            System.out.print("输出文件名（不含扩展名）: ");
            String fileName = scanner.nextLine().trim();
            if (fileName.isEmpty()) {
                fileName = report.getProjectName() + "-report";
            }

            switch (formatChoice) {
                case "1" -> {
                    Path path = Paths.get(fileName + ".md");
                    reportService.saveReport(report, path, "markdown");
                    System.out.println("✅ Markdown报告已保存: " + path.toAbsolutePath());
                }
                case "2" -> {
                    Path path = Paths.get(fileName + ".html");
                    reportService.saveReport(report, path, "html");
                    System.out.println("✅ HTML报告已保存: " + path.toAbsolutePath());
                }
                case "3" -> {
                    Path path = Paths.get(fileName + ".json");
                    reportService.saveReport(report, path, "json");
                    System.out.println("✅ JSON报告已保存: " + path.toAbsolutePath());
                }
                case "4" -> {
                    Path mdPath = Paths.get(fileName + ".md");
                    Path htmlPath = Paths.get(fileName + ".html");
                    Path jsonPath = Paths.get(fileName + ".json");

                    reportService.saveReport(report, mdPath, "markdown");
                    reportService.saveReport(report, htmlPath, "html");
                    reportService.saveReport(report, jsonPath, "json");

                    System.out.println("✅ 所有格式报告已保存:");
                    System.out.println("  - " + mdPath.toAbsolutePath());
                    System.out.println("  - " + htmlPath.toAbsolutePath());
                    System.out.println("  - " + jsonPath.toAbsolutePath());
                }
                default -> System.out.println("❌ 无效选项");
            }

        } catch (Exception e) {
            System.out.println("❌ 生成报告失败: " + e.getMessage());
            log.error("Report generation failed", e);
        }
    }

    /**
     * 显示帮助信息
     */
    private void showHelp() {
        System.out.println("\n❓ === 帮助信息 ===\n");
        System.out.println("📖 AI-Reviewer 是一个基于AI的智能代码审查工具\n");

        System.out.println("🎯 主要功能:");
        System.out.println("  • 自动分析项目代码质量");
        System.out.println("  • 识别潜在问题和改进建议");
        System.out.println("  • 生成详细的分析报告");
        System.out.println("  • 支持多种编程语言\n");

        System.out.println("📝 使用流程:");
        System.out.println("  1. 选择 '分析项目' 或 '快速分析'");
        System.out.println("  2. 输入项目路径");
        System.out.println("  3. 等待分析完成");
        System.out.println("  4. 查看结果并生成报告\n");

        System.out.println("💡 提示:");
        System.out.println("  • 交互式分析提供更多选项");
        System.out.println("  • 快速分析使用默认设置");
        System.out.println("  • 异步分析可在后台运行");
        System.out.println("  • 支持Markdown/HTML/JSON报告格式\n");
    }

    /**
     * 打印报告摘要
     */
    private void printReportSummary(ReviewReport report) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("📊 分析结果摘要");
        System.out.println("=".repeat(50));
        System.out.println("项目名称: " + report.getProjectName());
        System.out.println("总体评分: " + getScoreEmoji(report.getOverallScore()) + " " +
                         report.getOverallScore() + "/100");
        System.out.println("评级: " + getGrade(report.getOverallScore()));

        if (report.getDimensionScores() != null && !report.getDimensionScores().isEmpty()) {
            System.out.println("\n维度评分:");
            report.getDimensionScores().forEach((dimension, score) ->
                System.out.println("  • " + dimension + ": " + score + "/100")
            );
        }

        if (report.getKeyFindings() != null && !report.getKeyFindings().isEmpty()) {
            System.out.println("\n关键发现:");
            report.getKeyFindings().stream()
                    .limit(3)
                    .forEach(finding -> System.out.println("  • " + finding));
        }

        System.out.println("=".repeat(50) + "\n");
    }

    /**
     * 检测项目类型
     */
    private ProjectType detectProjectType(Path projectPath) {
        try {
            if (Files.exists(projectPath.resolve("pom.xml")) ||
                Files.exists(projectPath.resolve("build.gradle"))) {
                return ProjectType.JAVA;
            } else if (Files.exists(projectPath.resolve("requirements.txt")) ||
                      Files.exists(projectPath.resolve("setup.py"))) {
                return ProjectType.PYTHON;
            } else if (Files.exists(projectPath.resolve("package.json"))) {
                return ProjectType.JAVASCRIPT;
            }
        } catch (Exception e) {
            log.warn("Failed to detect project type", e);
        }
        return ProjectType.JAVA; // 默认
    }

    /**
     * 获取状态文本
     */
    private String getStatusText(AnalysisTask task) {
        if (task.isCompleted()) return "已完成";
        if (task.isFailed()) return "失败";
        if (task.getStatus() == AnalysisStatus.CANCELLED) return "已取消";
        if (task.isRunning()) return "运行中";
        return "等待中";
    }

    /**
     * 获取状态表情
     */
    private String getStatusEmoji(AnalysisTask task) {
        if (task.isCompleted()) return "✅";
        if (task.isFailed()) return "❌";
        if (task.getStatus() == AnalysisStatus.CANCELLED) return "🚫";
        if (task.isRunning()) return "⏳";
        return "⏸️";
    }

    /**
     * 获取评分表情
     */
    private String getScoreEmoji(int score) {
        if (score >= 90) return "🌟";
        if (score >= 80) return "⭐";
        if (score >= 70) return "👍";
        if (score >= 60) return "👌";
        return "⚠️";
    }

    /**
     * 获取等级
     */
    private String getGrade(int score) {
        if (score >= 90) return "A (优秀)";
        if (score >= 80) return "B (良好)";
        if (score >= 70) return "C (中等)";
        if (score >= 60) return "D (及格)";
        return "F (需改进)";
    }

    /**
     * 主程序入口
     */
    public static void main(String[] args) {
        // 初始化适配器
        LocalFileSystemAdapter.FileSystemConfig fsConfig =
                new LocalFileSystemAdapter.FileSystemConfig(
                        List.of("*.java", "*.py", "*.js", "*.jsx", "*.ts", "*.tsx"),
                        List.of("*.class", "node_modules/*", "__pycache__/*", "target/*", "build/*"),
                        2048,
                        20
                );
        LocalFileSystemAdapter fileSystemAdapter = new LocalFileSystemAdapter(fsConfig);

        // Mock AI服务（实际使用时替换为真实实现）
        AIServicePort aiServicePort = new AIServicePort() {
            @Override
            public String analyze(String prompt) {
                return "这是一个结构良好的项目。代码质量较高，建议继续保持良好的编码规范。";
            }

            @Override
            public java.util.concurrent.CompletableFuture<String> analyzeAsync(String prompt) {
                return java.util.concurrent.CompletableFuture.completedFuture(analyze(prompt));
            }

            @Override
            public java.util.concurrent.CompletableFuture<String[]> analyzeBatchAsync(String[] prompts) {
                String[] results = new String[prompts.length];
                for (int i = 0; i < prompts.length; i++) {
                    results[i] = analyze(prompts[i]);
                }
                return java.util.concurrent.CompletableFuture.completedFuture(results);
            }

            @Override
            public String getProviderName() {
                return "Mock AI Provider";
            }

            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public int getMaxConcurrency() {
                return 5;
            }

            @Override
            public void shutdown() {
                // Mock实现，无需清理资源
            }
        };

        FileCacheAdapter cacheAdapter = new FileCacheAdapter();

        // 初始化服务
        ProjectAnalysisService analysisService =
            new ProjectAnalysisService(aiServicePort, cacheAdapter, fileSystemAdapter);
        ReportGenerationService reportService =
            new ReportGenerationService(fileSystemAdapter);

        // 启动CLI
        CommandLineInterface cli = new CommandLineInterface(
            analysisService, reportService, fileSystemAdapter
        );
        cli.start();
    }
}

