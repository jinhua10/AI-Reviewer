package top.yumbo.ai.reviewer.application.hackathon.cli;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.reviewer.adapter.output.filesystem.LocalFileSystemAdapter;
import top.yumbo.ai.reviewer.adapter.output.repository.GitRepositoryAdapter;
import top.yumbo.ai.reviewer.application.hackathon.service.*;
import top.yumbo.ai.reviewer.application.port.output.CloneRequest;
import top.yumbo.ai.reviewer.application.port.output.RepositoryPort;
import top.yumbo.ai.reviewer.application.service.ProjectAnalysisService;
import top.yumbo.ai.reviewer.application.service.ReportGenerationService;
import top.yumbo.ai.reviewer.domain.model.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

/**
 * 黑客松交互式命令行应用
 * 提供友好的交互式界面进行黑客松项目评审
 *
 * <p>职责：
 * <ul>
 *   <li>提供交互式黑客松项目评审流程</li>
 *   <li>管理团队和参赛作品</li>
 *   <li>生成排行榜和评审报告</li>
 * </ul>
 *
 * @author AI-Reviewer Team
 * @version 2.0 (六边形架构重构版)
 * @since 2025-11-13
 */
@Slf4j
public class HackathonInteractiveApp {

    private final HackathonIntegrationService integrationService;
    private final TeamManagementService teamService;
    private final LeaderboardService leaderboardService;
    private final ProjectAnalysisService analysisService;
    private final ReportGenerationService reportService;
    private final LocalFileSystemAdapter fileSystemAdapter;
    private final Scanner scanner;

    public HackathonInteractiveApp(
            HackathonIntegrationService integrationService,
            TeamManagementService teamService,
            LeaderboardService leaderboardService,
            ProjectAnalysisService analysisService,
            ReportGenerationService reportService,
            LocalFileSystemAdapter fileSystemAdapter) {
        this.integrationService = integrationService;
        this.teamService = teamService;
        this.leaderboardService = leaderboardService;
        this.analysisService = analysisService;
        this.reportService = reportService;
        this.fileSystemAdapter = fileSystemAdapter;
        this.scanner = new Scanner(System.in);
    }

    /**
     * 启动交互式界面
     */
    public void start() {
        printWelcomeBanner();

        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> reviewSingleProject();
                case "2" -> reviewBatchProjects();
                case "3" -> manageTeams();
                case "4" -> viewLeaderboard();
                case "5" -> exportResults();
                case "6" -> showHelp();
                case "0" -> running = false;
                default -> System.out.println("❌ 无效选项，请重新选择");
            }
        }

        System.out.println("\n👋 评审完成！感谢使用黑客松评审工具!");
        scanner.close();
    }

    /**
     * 打印欢迎横幅
     */
    private void printWelcomeBanner() {
        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║                                                ║");
        System.out.println("║        🏆 黑客松项目评审工具 🏆              ║");
        System.out.println("║                                                ║");
        System.out.println("║        基于AI的智能评分系统                   ║");
        System.out.println("║                                                ║");
        System.out.println("╚════════════════════════════════════════════════╝\n");
    }

    /**
     * 打印主菜单
     */
    private void printMenu() {
        System.out.println("\n📋 主菜单：");
        System.out.println("  1. 🔍 评审单个项目");
        System.out.println("  2. 📦 批量评审项目");
        System.out.println("  3. 👥 管理团队");
        System.out.println("  4. 🏅 查看排行榜");
        System.out.println("  5. 📊 导出结果");
        System.out.println("  6. ❓ 帮助");
        System.out.println("  0. 🚪 退出");
        System.out.print("\n请选择操作 [0-6]: ");
    }

    /**
     * 评审单个项目
     */
    private void reviewSingleProject() {
        System.out.println("\n🔍 === 评审单个项目 ===\n");

        // 1. 输入团队名称
        System.out.print("📝 团队名称: ");
        String teamName = scanner.nextLine().trim();

        // 2. 选择输入方式
        System.out.println("\n选择项目来源:");
        System.out.println("  1. GitHub URL");
        System.out.println("  2. Gitee URL");
        System.out.println("  3. 本地目录");
        System.out.print("选择 [1-3]: ");

        String sourceChoice = scanner.nextLine().trim();
        String url = null;
        String directory = null;

        switch (sourceChoice) {
            case "1" -> {
                System.out.print("GitHub URL: ");
                url = scanner.nextLine().trim();
            }
            case "2" -> {
                System.out.print("Gitee URL: ");
                url = scanner.nextLine().trim();
            }
            case "3" -> {
                System.out.print("本地目录: ");
                directory = scanner.nextLine().trim();
            }
            default -> {
                System.out.println("❌ 无效选项");
                return;
            }
        }

        // 3. 执行评审
        try {
            System.out.println("\n⏳ 正在评审项目...");

            Path projectPath;
            if (url != null) {
                projectPath = cloneProject(url);
            } else {
                projectPath = Paths.get(directory);
                if (!Files.exists(projectPath)) {
                    System.out.println("❌ 目录不存在");
                    return;
                }
            }

            // 扫描和分析
            List<SourceFile> files = fileSystemAdapter.scanProjectFiles(projectPath);
            Project project = Project.builder()
                    .name(teamName)
                    .rootPath(projectPath)
                    .type(detectProjectType(files))
                    .sourceFiles(files)
                    .build();

            AnalysisTask task = analysisService.analyzeProject(project);

            if (task.isCompleted()) {
                ReviewReport report = analysisService.getAnalysisResult(task.getTaskId());
                printReviewResult(teamName, report);

                // 保存结果
                System.out.print("\n是否保存报告？[Y/n]: ");
                String save = scanner.nextLine().trim();
                if (save.isEmpty() || save.equalsIgnoreCase("Y")) {
                    saveReport(teamName, report);
                }
            } else {
                System.out.println("❌ 评审失败: " + task.getErrorMessage());
            }

        } catch (Exception e) {
            System.out.println("❌ 评审失败: " + e.getMessage());
            log.error("Review failed", e);
        }
    }

    /**
     * 批量评审项目
     */
    private void reviewBatchProjects() {
        System.out.println("\n📦 === 批量评审项目 ===\n");
        System.out.println("请输入包含多个项目URL的文件路径（每行一个URL，格式：团队名,URL）");
        System.out.print("文件路径: ");

        String filePath = scanner.nextLine().trim();
        // TODO: 实现批量评审逻辑
        System.out.println("💡 批量评审功能正在开发中...");
    }

    /**
     * 管理团队
     */
    private void manageTeams() {
        System.out.println("\n👥 === 管理团队 ===\n");
        System.out.println("  1. 注册新团队");
        System.out.println("  2. 查看团队列表");
        System.out.println("  3. 查看团队详情");
        System.out.print("选择 [1-3]: ");

        String choice = scanner.nextLine().trim();
        // TODO: 实现团队管理逻辑
        System.out.println("💡 团队管理功能正在开发中...");
    }

    /**
     * 查看排行榜
     */
    private void viewLeaderboard() {
        System.out.println("\n🏅 === 排行榜 ===\n");
        // TODO: 实现排行榜显示逻辑
        System.out.println("💡 排行榜功能正在开发中...");
    }

    /**
     * 导出结果
     */
    private void exportResults() {
        System.out.println("\n📊 === 导出结果 ===\n");
        System.out.println("选择导出格式:");
        System.out.println("  1. JSON");
        System.out.println("  2. CSV");
        System.out.println("  3. Excel");
        System.out.print("选择 [1-3]: ");

        String choice = scanner.nextLine().trim();
        // TODO: 实现结果导出逻辑
        System.out.println("💡 导出功能正在开发中...");
    }

    /**
     * 显示帮助
     */
    private void showHelp() {
        System.out.println("\n❓ === 帮助信息 ===\n");
        System.out.println("🏆 黑客松项目评审工具");
        System.out.println("\n主要功能:");
        System.out.println("  • 支持GitHub/Gitee仓库自动克隆");
        System.out.println("  • 基于AI的智能评分");
        System.out.println("  • 多维度评分（代码质量、创新性、完整性、文档）");
        System.out.println("  • 自动生成排行榜");
        System.out.println("  • 团队管理和结果导出\n");

        System.out.println("评分维度:");
        System.out.println("  • 代码质量 (40%)");
        System.out.println("  • 创新性 (30%)");
        System.out.println("  • 完整性 (20%)");
        System.out.println("  • 文档质量 (10%)\n");
    }

    /**
     * 克隆项目
     */
    private Path cloneProject(String url) throws RepositoryPort.RepositoryException {
        System.out.println("⏳ 正在克隆项目...");
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "hackathon-repos");
        RepositoryPort repoPort = new GitRepositoryAdapter(tempDir);

        CloneRequest request = CloneRequest.builder()
                .url(url)
                .branch("master")
                .timeoutSeconds(300)
                .build();

        Path projectPath = repoPort.cloneRepository(request);
        System.out.println("✅ 克隆完成");
        return projectPath;
    }

    /**
     * 检测项目类型
     */
    private ProjectType detectProjectType(List<SourceFile> files) {
        int javaCount = 0, pythonCount = 0, jsCount = 0;
        for (SourceFile file : files) {
            switch (file.getProjectType()) {
                case JAVA -> javaCount++;
                case PYTHON -> pythonCount++;
                case JAVASCRIPT, TYPESCRIPT -> jsCount++;
            }
        }

        int max = Math.max(javaCount, Math.max(pythonCount, jsCount));
        if (max == javaCount) return ProjectType.JAVA;
        if (max == pythonCount) return ProjectType.PYTHON;
        if (max == jsCount) return ProjectType.JAVASCRIPT;
        return ProjectType.UNKNOWN;
    }

    /**
     * 打印评审结果
     */
    private void printReviewResult(String teamName, ReviewReport report) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🏆 黑客松评审结果");
        System.out.println("=".repeat(60));
        System.out.println("团队: " + teamName);
        System.out.println("项目: " + report.getProjectName());
        System.out.println("总分: " + report.getOverallScore() + "/100 (" + report.getGrade() + ")");

        if (report.getDimensionScores() != null) {
            System.out.println("\n维度评分:");
            report.getDimensionScores().forEach((dimension, score) ->
                System.out.println("  • " + dimension + ": " + score + "/100")
            );
        }

        System.out.println("=".repeat(60) + "\n");
    }

    /**
     * 保存报告
     */
    private void saveReport(String teamName, ReviewReport report) {
        try {
            String fileName = teamName.replaceAll("[^a-zA-Z0-9-_]", "_") + "-report";
            Path mdPath = Paths.get(fileName + ".md");
            Path jsonPath = Paths.get(fileName + ".json");

            reportService.saveReport(report, mdPath, "markdown");
            reportService.saveReport(report, jsonPath, "json");

            System.out.println("✅ 报告已保存:");
            System.out.println("  - " + mdPath.toAbsolutePath());
            System.out.println("  - " + jsonPath.toAbsolutePath());
        } catch (Exception e) {
            System.out.println("❌ 保存报告失败: " + e.getMessage());
            log.error("Failed to save report", e);
        }
    }

    /**
     * 主程序入口（用于独立运行）
     */
    public static void main(String[] args) {
        System.out.println("💡 提示: 交互式应用需要完整的依赖注入容器");
        System.out.println("请使用 HackathonCommandLineApp 的命令行模式");
        System.out.println("或者通过 Guice 注入器获取 HackathonInteractiveApp 实例");
    }
}

