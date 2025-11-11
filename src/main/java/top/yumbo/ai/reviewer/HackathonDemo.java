package top.yumbo.ai.reviewer;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.reviewer.HackathonReviewer.HackathonScore;

import java.util.Arrays;
import java.util.List;

/**
 * Hackathon 源码评分工具演示
 * 展示如何使用AI Reviewer进行黑客松项目评分
 */
@Slf4j
public class HackathonDemo {

    public static void main(String[] args) {
        log.info("=== AI Reviewer - Hackathon 源码评分工具演示 ===\n");

        try {
            HackathonReviewer reviewer = new HackathonReviewer();

            // 演示1: 单个项目评分
            demonstrateSingleProjectScoring(reviewer);

            // 演示2: 批量项目评分
            demonstrateBatchScoring(reviewer);

            // 演示3: 生成排行榜
            demonstrateLeaderboard(reviewer);

        } catch (Exception e) {
            log.error("Hackathon演示执行失败", e);
            System.err.println("错误: " + e.getMessage());
        }
    }

    private static void demonstrateSingleProjectScoring(HackathonReviewer reviewer) {
        System.out.println("🎯 演示1: 单个项目快速评分");
        System.out.println("-".repeat(50));

        try {
            // 评分当前项目作为示例
            HackathonScore score = reviewer.quickScore(".");

            System.out.println("📊 评分结果:");
            System.out.printf("项目名称: %s%n", score.getProjectName());
            System.out.printf("总评分: %.1f/100%n", score.getTotalScore());
            System.out.printf("评审状态: %s%n", score.getJudgeStatus());
            System.out.println();

            System.out.println("📈 详细评分:");
            System.out.printf("├─ 架构设计: %.1f/100%n", score.getArchitectureScore());
            System.out.printf("├─ 代码质量: %.1f/100%n", score.getCodeQualityScore());
            System.out.printf("├─ 功能完整性: %.1f/100%n", score.getFunctionalityScore());
            System.out.printf("├─ 商业价值: %.1f/100%n", score.getBusinessValueScore());
            System.out.printf("└─ 测试覆盖率: %.1f/100%n", score.getTestCoverageScore());
            System.out.println();

            // 生成报告
            reviewer.generateReport(score, "hackathon-report.md");
            System.out.println("📄 评审报告已生成: hackathon-report.md");

        } catch (Exception e) {
            System.out.println("❌ 评分失败: " + e.getMessage());
        }

        System.out.println();
    }

    private static void demonstrateBatchScoring(HackathonReviewer reviewer) {
        System.out.println("🎯 演示2: 批量项目评分");
        System.out.println("-".repeat(50));

        // 模拟多个项目路径 (实际使用时替换为真实路径)
        List<String> projectPaths = Arrays.asList(
                ".",  // 当前项目
                "."   // 重复用于演示
        );

        try {
            List<HackathonScore> scores = reviewer.batchScore(projectPaths);

            System.out.println("📊 批量评分结果:");
            for (int i = 0; i < scores.size(); i++) {
                HackathonScore score = scores.get(i);
                System.out.printf("%d. %s%n", i + 1, score.toString());
            }

        } catch (Exception e) {
            System.out.println("❌ 批量评分失败: " + e.getMessage());
        }

        System.out.println();
    }

    private static void demonstrateLeaderboard(HackathonReviewer reviewer) {
        System.out.println("🎯 演示3: 生成排行榜");
        System.out.println("-".repeat(50));

        // 创建模拟评分数据
        List<HackathonScore> mockScores = Arrays.asList(
                createMockScore("AI-ChatBot", 92.5, "🏆 优秀 - 进入决赛"),
                createMockScore("Smart-Home", 87.3, "🏆 优秀 - 进入决赛"),
                createMockScore("Edu-Platform", 78.9, "🥈 良好 - 晋级复赛"),
                createMockScore("Health-Tracker", 72.1, "🥈 良好 - 晋级复赛"),
                createMockScore("Game-Engine", 65.4, "🥉 及格 - 基础奖项")
        );

        String leaderboard = reviewer.generateLeaderboard(mockScores);
        System.out.println(leaderboard);
    }

    private static HackathonScore createMockScore(String projectName, double score, String status) {
        HackathonScore hackathonScore = new HackathonScore();
        hackathonScore.setProjectName(projectName);
        hackathonScore.setTotalScore(score);
        hackathonScore.setJudgeStatus(status);
        return hackathonScore;
    }
}
