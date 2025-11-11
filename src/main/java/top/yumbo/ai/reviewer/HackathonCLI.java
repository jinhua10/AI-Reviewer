package top.yumbo.ai.reviewer;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.reviewer.HackathonReviewer.HackathonScore;
import top.yumbo.ai.reviewer.HackathonReviewer.ReviewMode;
import top.yumbo.ai.reviewer.HackathonReviewer.Leaderboard;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Hackathon AI 评审工具命令行接口
 * 提供专业的命令行操作界面，支持各种评审场景
 */
@Slf4j
public class HackathonCLI {

    private static final String VERSION = "2.0.0";
    private static HackathonReviewer reviewer;

    public static void main(String[] args) {
        System.out.println("🏆 Hackathon AI 源码评分工具 v" + VERSION);
        System.out.println("专业的AI驱动黑客松评审系统\n");

        try {
            // 解析命令行参数
            if (args.length == 0) {
                showHelp();
                return;
            }

            String command = args[0].toLowerCase();

            // 对于不需要初始化的命令，直接处理
            switch (command) {
                case "help":
                case "h":
                case "-h":
                case "--help":
                    showHelp();
                    return;

                case "version":
                case "v":
                case "-v":
                case "--version":
                    showVersion();
                    return;
            }

            // 对于需要评审器的命令，先初始化
            initializeReviewer();

            switch (command) {
                case "review":
                case "r":
                    handleReviewCommand(args);
                    break;

                case "batch":
                case "b":
                    handleBatchCommand(args);
                    break;

                case "leaderboard":
                case "l":
                    handleLeaderboardCommand(args);
                    break;

                case "stats":
                case "s":
                    handleStatsCommand();
                    break;

                case "demo":
                case "d":
                    runDemo();
                    break;

                default:
                    System.err.println("❌ 未知命令: " + command);
                    showHelp();
                    break;
            }

        } catch (Exception e) {
            log.error("命令执行失败", e);
            System.err.println("❌ 错误: " + e.getMessage());
            System.exit(1);
        } finally {
            if (reviewer != null) {
                reviewer.shutdown();
            }
        }
    }

    private static void initializeReviewer() throws IOException {
        System.out.println("🔧 初始化Hackathon AI评审工具...");
        reviewer = new HackathonReviewer();
        System.out.println("✅ 初始化完成\n");
    }

    private static void handleReviewCommand(String[] args) {
        if (args.length < 2) {
            System.err.println("❌ 缺少项目路径参数");
            System.out.println("用法: java -jar hackathon-reviewer.jar review <项目路径> [模式]");
            return;
        }

        String projectPath = args[1];
        ReviewMode mode = ReviewMode.QUICK; // 默认快速模式

        if (args.length >= 3) {
            try {
                mode = ReviewMode.valueOf(args[2].toUpperCase());
            } catch (IllegalArgumentException e) {
                System.err.println("❌ 无效的评审模式: " + args[2]);
                System.out.println("支持的模式: QUICK, DETAILED, EXPERT");
                return;
            }
        }

        try {
            System.out.println("🚀 开始评审项目: " + projectPath);
            System.out.println("📊 评审模式: " + mode.getDisplayName());
            System.out.println("⏳ " + mode.getDescription() + "\n");

            long startTime = System.currentTimeMillis();
            HackathonScore score = reviewer.review(projectPath, mode);
            long duration = System.currentTimeMillis() - startTime;

            // 显示评审结果
            displayReviewResult(score, duration);

            // 生成报告
            String reportFile = "hackathon-review-" + System.currentTimeMillis() + ".md";
            reviewer.generateReviewReport(score, reportFile, mode);
            System.out.println("📄 评审报告已生成: " + reportFile);

        } catch (Exception e) {
            System.err.println("❌ 评审失败: " + e.getMessage());
        }
    }

    private static void handleBatchCommand(String[] args) {
        if (args.length < 3) {
            System.err.println("❌ 批量评审需要至少2个项目");
            System.out.println("用法: java -jar hackathon-reviewer.jar batch <项目路径1> <项目路径2> ... [模式]");
            return;
        }

        // 解析项目路径和模式
        List<String> projectPaths = Arrays.asList(Arrays.copyOfRange(args, 1, args.length));
        ReviewMode mode = ReviewMode.QUICK;

        // 检查最后一个参数是否为模式
        String lastArg = args[args.length - 1];
        try {
            ReviewMode testMode = ReviewMode.valueOf(lastArg.toUpperCase());
            mode = testMode;
            projectPaths = Arrays.asList(Arrays.copyOfRange(args, 1, args.length - 1));
        } catch (IllegalArgumentException e) {
            // 最后一个参数不是模式，保持默认
        }

        try {
            System.out.println("📦 开始批量评审 " + projectPaths.size() + " 个项目");
            System.out.println("📊 评审模式: " + mode.getDisplayName() + "\n");

            List<HackathonScore> scores = reviewer.batchReview(projectPaths, mode);

            // 显示批量评审结果
            System.out.println("📊 批量评审结果:");
            System.out.println("-".repeat(80));
            for (int i = 0; i < scores.size(); i++) {
                HackathonScore score = scores.get(i);
                System.out.printf("%d. %s%n", i + 1, score.toString());
            }
            System.out.println();

            // 生成排行榜
            Leaderboard leaderboard = reviewer.generateLeaderboard(scores);
            displayLeaderboard(leaderboard);

        } catch (Exception e) {
            System.err.println("❌ 批量评审失败: " + e.getMessage());
        }
    }

    private static void handleLeaderboardCommand(String[] args) {
        // 从评审历史生成排行榜
        List<HackathonReviewer.ReviewRecord> history = reviewer.getReviewHistory();

        if (history.isEmpty()) {
            System.out.println("📊 暂无评审历史记录");
            return;
        }

        List<HackathonScore> scores = history.stream()
                .filter(record -> record.getScore() != null)
                .map(HackathonReviewer.ReviewRecord::getScore)
                .toList();

        if (scores.isEmpty()) {
            System.out.println("📊 暂无完成的评审记录");
            return;
        }

        Leaderboard leaderboard = reviewer.generateLeaderboard(scores);
        displayLeaderboard(leaderboard);
    }

    private static void handleStatsCommand() {
        HackathonReviewer.ReviewStatistics stats = reviewer.getReviewStatistics();

        System.out.println("📈 评审统计信息");
        System.out.println("-".repeat(40));
        System.out.printf("总评审数: %d%n", stats.getTotalReviews());
        System.out.printf("完成评审: %d%n", stats.getCompletedReviews());
        System.out.printf("失败评审: %d%n", stats.getFailedReviews());
        System.out.printf("平均耗时: %.0fms%n", stats.getAverageDuration());
        System.out.println();

        // 显示评审历史摘要
        List<HackathonReviewer.ReviewRecord> history = reviewer.getReviewHistory();
        if (!history.isEmpty()) {
            System.out.println("📋 最近评审记录:");
            history.stream()
                    .limit(5)
                    .forEach(record -> System.out.printf("   %s - %s [%s]%n",
                            record.getProjectPath(),
                            record.getMode().getDisplayName(),
                            record.getStatus().getDisplayName()));
        }
    }

    private static void runDemo() {
        System.out.println("🎯 运行Hackathon评审演示...\n");
        HackathonDemo.main(new String[]{});
    }

    private static void displayReviewResult(HackathonScore score, long duration) {
        System.out.println("📊 评审结果");
        System.out.println("-".repeat(60));
        System.out.printf("项目名称: %s%n", score.getProjectName());
        System.out.printf("评审模式: %s%n", score.getReviewMode().getDisplayName());
        System.out.printf("总评分: %.1f/100%n", score.getTotalScore());
        System.out.printf("评审状态: %s%n", score.getJudgeStatus());
        System.out.printf("评审耗时: %dms%n", duration);
        System.out.printf("评审时间: %s%n", score.getReviewTime());
        System.out.println();

        System.out.println("📈 详细评分:");
        System.out.printf("├─ 架构设计: %.1f/100%n", score.getArchitectureScore());
        System.out.printf("├─ 代码质量: %.1f/100%n", score.getCodeQualityScore());
        System.out.printf("├─ 功能完整性: %.1f/100%n", score.getFunctionalityScore());
        System.out.printf("├─ 商业价值: %.1f/100%n", score.getBusinessValueScore());
        System.out.printf("├─ 测试覆盖率: %.1f/100%n", score.getTestCoverageScore());
        if (score.getInnovationScore() > 0) {
            System.out.printf("└─ 创新性: %.1f/100%n", score.getInnovationScore());
        }
        System.out.println();
    }

    private static void displayLeaderboard(Leaderboard leaderboard) {
        System.out.println("🏆 Hackathon排行榜");
        System.out.println("-".repeat(60));
        System.out.printf("总项目数: %d%n", leaderboard.getTotalProjects());
        System.out.printf("平均分: %.1f%n", leaderboard.getAverageScore());
        System.out.printf("最高分: %.1f%n", leaderboard.getHighestScore());
        System.out.printf("最低分: %.1f%n", leaderboard.getLowestScore());
        System.out.printf("生成时间: %s%n", leaderboard.getGeneratedTime());
        System.out.println();

        System.out.println("📊 排名情况:");
        List<HackathonScore> scores = leaderboard.getScores();
        for (int i = 0; i < Math.min(scores.size(), 10); i++) {
            HackathonScore score = scores.get(i);
            System.out.printf("%2d. %-20s %.1f分 %s%n",
                    i + 1,
                    score.getProjectName().length() > 20 ?
                        score.getProjectName().substring(0, 17) + "..." :
                        score.getProjectName(),
                    score.getTotalScore(),
                    score.getJudgeStatus());
        }
        System.out.println();

        System.out.println("📈 状态分布:");
        leaderboard.getStatusStatistics().forEach((status, count) ->
            System.out.printf("   %s: %d 个项目%n", status, count));
        System.out.println();
    }

    private static void showHelp() {
        System.out.println("🏆 Hackathon AI 源码评分工具 v" + VERSION);
        System.out.println("专业的AI驱动黑客松评审系统\n");

        System.out.println("📋 基本用法:");
        System.out.println("  java -jar hackathon-reviewer.jar <命令> [参数...]\n");

        System.out.println("🎯 可用命令:");
        System.out.println("  review <项目路径> [模式]     评审单个项目");
        System.out.println("  batch <路径1> <路径2> ...    批量评审多个项目");
        System.out.println("  leaderboard                  显示排行榜");
        System.out.println("  stats                        显示评审统计");
        System.out.println("  demo                         运行功能演示");
        System.out.println("  help                         显示此帮助信息");
        System.out.println("  version                      显示版本信息\n");

        System.out.println("📊 评审模式:");
        System.out.println("  QUICK      快速评审 (10秒) - 适合大规模初筛");
        System.out.println("  DETAILED   详细评审 (30秒) - 适合复赛评审");
        System.out.println("  EXPERT     专家评审 (60秒) - 适合决赛评审\n");

        System.out.println("💡 使用示例:");
        System.out.println("  # 快速评审当前项目");
        System.out.println("  java -jar hackathon-reviewer.jar review . QUICK");
        System.out.println();
        System.out.println("  # 批量评审多个项目");
        System.out.println("  java -jar hackathon-reviewer.jar batch project1 project2 DETAILED");
        System.out.println();
        System.out.println("  # 查看评审统计");
        System.out.println("  java -jar hackathon-reviewer.jar stats");
        System.out.println();
        System.out.println("  # 运行演示");
        System.out.println("  java -jar hackathon-reviewer.jar demo\n");

        System.out.println("📚 更多信息请查看: HACKATHON-REVIEW-GUIDE.md");
    }

    private static void showVersion() {
        System.out.println("🏆 Hackathon AI 源码评分工具 v" + VERSION);
        System.out.println("基于 AI Reviewer v2.0 构建");
        System.out.println("发布日期: 2025-01-11");
        System.out.println("官方网站: https://github.com/jinhua10/ai-reviewer");
    }
}
