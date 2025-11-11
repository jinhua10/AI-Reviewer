package top.yumbo.ai.reviewer.adapter.input.hackathon.application;

import top.yumbo.ai.reviewer.adapter.input.hackathon.domain.model.HackathonProject;
import top.yumbo.ai.reviewer.adapter.input.hackathon.domain.model.HackathonScore;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 排行榜服务
 *
 * 负责维护和查询黑客松项目排行榜
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-12
 */
public class LeaderboardService {

    // 排行榜缓存（实际应使用 Redis）
    private final Map<String, List<LeaderboardEntry>> leaderboardCache = new ConcurrentHashMap<>();

    /**
     * 排行榜条目
     */
    public static class LeaderboardEntry {
        private final String projectId;
        private final String projectName;
        private final String teamName;
        private final int totalScore;
        private final String grade;
        private final HackathonScore detailedScore;
        private int rank;

        public LeaderboardEntry(HackathonProject project) {
            this.projectId = project.getId();
            this.projectName = project.getName();
            this.teamName = project.getTeam().getName();

            Integer bestScore = project.getBestScore();
            this.totalScore = bestScore != null ? bestScore : 0;

            HackathonScore score = project.getBestSubmission() != null
                ? project.getBestSubmission().getScore()
                : null;
            this.grade = score != null ? score.getGrade() : "N/A";
            this.detailedScore = score;
        }

        // Getters
        public String getProjectId() { return projectId; }
        public String getProjectName() { return projectName; }
        public String getTeamName() { return teamName; }
        public int getTotalScore() { return totalScore; }
        public String getGrade() { return grade; }
        public HackathonScore getDetailedScore() { return detailedScore; }
        public int getRank() { return rank; }

        public void setRank(int rank) { this.rank = rank; }

        @Override
        public String toString() {
            return String.format(
                "#%d - %s (团队: %s) - %d分 (%s)",
                rank, projectName, teamName, totalScore, grade
            );
        }
    }

    /**
     * 更新排行榜
     *
     * @param projects 所有项目列表
     */
    public void updateLeaderboard(List<HackathonProject> projects) {
        if (projects == null || projects.isEmpty()) {
            return;
        }

        // 总排行榜
        List<LeaderboardEntry> overallRanking = calculateRanking(projects);
        leaderboardCache.put("overall", overallRanking);

        // 按语言分类的排行榜
        Map<String, List<HackathonProject>> projectsByLanguage = projects.stream()
            .filter(p -> p.getBestSubmission() != null)
            .collect(Collectors.groupingBy(this::getProjectLanguage));

        projectsByLanguage.forEach((language, projectList) -> {
            List<LeaderboardEntry> ranking = calculateRanking(projectList);
            leaderboardCache.put("language:" + language, ranking);
        });
    }

    /**
     * 计算排名
     */
    private List<LeaderboardEntry> calculateRanking(List<HackathonProject> projects) {
        List<LeaderboardEntry> entries = projects.stream()
            .filter(p -> p.getBestScore() != null)
            .map(LeaderboardEntry::new)
            .sorted((e1, e2) -> Integer.compare(e2.getTotalScore(), e1.getTotalScore()))
            .collect(Collectors.toList());

        // 设置排名
        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setRank(i + 1);
        }

        return entries;
    }

    /**
     * 获取项目主要语言（简化实现）
     */
    private String getProjectLanguage(HackathonProject project) {
        // 从项目名称或内容推断语言（实际应从代码分析中获取）
        String name = project.getName().toLowerCase();

        if (name.contains("java")) return "Java";
        if (name.contains("python")) return "Python";
        if (name.contains("javascript") || name.contains("react") || name.contains("vue")) return "JavaScript";
        if (name.contains("go") || name.contains("golang")) return "Go";
        if (name.contains("rust")) return "Rust";

        return "Other";
    }

    /**
     * 获取总排行榜
     *
     * @return 排行榜条目列表
     */
    public List<LeaderboardEntry> getOverallLeaderboard() {
        return leaderboardCache.getOrDefault("overall", Collections.emptyList());
    }

    /**
     * 获取指定数量的排行榜
     *
     * @param limit 数量限制
     * @return 排行榜条目列表
     */
    public List<LeaderboardEntry> getTopEntries(int limit) {
        return getOverallLeaderboard().stream()
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * 获取按语言分类的排行榜
     *
     * @param language 语言
     * @return 排行榜条目列表
     */
    public List<LeaderboardEntry> getLeaderboardByLanguage(String language) {
        String key = "language:" + language;
        return leaderboardCache.getOrDefault(key, Collections.emptyList());
    }

    /**
     * 获取项目排名
     *
     * @param projectId 项目ID
     * @return 排名，如果未找到返回 -1
     */
    public int getProjectRank(String projectId) {
        List<LeaderboardEntry> leaderboard = getOverallLeaderboard();

        for (int i = 0; i < leaderboard.size(); i++) {
            if (leaderboard.get(i).getProjectId().equals(projectId)) {
                return i + 1;
            }
        }

        return -1;
    }

    /**
     * 获取项目的详细排名信息
     *
     * @param projectId 项目ID
     * @return 排名信息，如果未找到返回 null
     */
    public LeaderboardEntry getProjectEntry(String projectId) {
        return getOverallLeaderboard().stream()
            .filter(entry -> entry.getProjectId().equals(projectId))
            .findFirst()
            .orElse(null);
    }

    /**
     * 获取排名在指定范围内的项目
     *
     * @param startRank 起始排名（包含）
     * @param endRank 结束排名（包含）
     * @return 排行榜条目列表
     */
    public List<LeaderboardEntry> getEntriesByRankRange(int startRank, int endRank) {
        List<LeaderboardEntry> leaderboard = getOverallLeaderboard();

        if (startRank < 1 || startRank > leaderboard.size()) {
            return Collections.emptyList();
        }

        int actualEnd = Math.min(endRank, leaderboard.size());
        return leaderboard.subList(startRank - 1, actualEnd);
    }

    /**
     * 获取分数在指定范围内的项目
     *
     * @param minScore 最低分数
     * @param maxScore 最高分数
     * @return 排行榜条目列表
     */
    public List<LeaderboardEntry> getEntriesByScoreRange(int minScore, int maxScore) {
        return getOverallLeaderboard().stream()
            .filter(entry -> entry.getTotalScore() >= minScore &&
                           entry.getTotalScore() <= maxScore)
            .collect(Collectors.toList());
    }

    /**
     * 获取指定等级的项目
     *
     * @param grade 等级 (S, A, B, C, D, F)
     * @return 排行榜条目列表
     */
    public List<LeaderboardEntry> getEntriesByGrade(String grade) {
        return getOverallLeaderboard().stream()
            .filter(entry -> entry.getGrade().equals(grade))
            .collect(Collectors.toList());
    }

    /**
     * 获取排行榜统计信息
     *
     * @return 统计信息
     */
    public Map<String, Object> getLeaderboardStatistics() {
        List<LeaderboardEntry> leaderboard = getOverallLeaderboard();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProjects", leaderboard.size());

        if (leaderboard.isEmpty()) {
            return stats;
        }

        // 平均分
        double avgScore = leaderboard.stream()
            .mapToInt(LeaderboardEntry::getTotalScore)
            .average()
            .orElse(0.0);
        stats.put("averageScore", Math.round(avgScore));

        // 最高分
        int maxScore = leaderboard.stream()
            .mapToInt(LeaderboardEntry::getTotalScore)
            .max()
            .orElse(0);
        stats.put("maxScore", maxScore);

        // 最低分
        int minScore = leaderboard.stream()
            .mapToInt(LeaderboardEntry::getTotalScore)
            .min()
            .orElse(0);
        stats.put("minScore", minScore);

        // 等级分布
        Map<String, Long> gradeDistribution = leaderboard.stream()
            .collect(Collectors.groupingBy(
                LeaderboardEntry::getGrade,
                Collectors.counting()
            ));
        stats.put("gradeDistribution", gradeDistribution);

        return stats;
    }

    /**
     * 生成排行榜报告
     *
     * @param topN 显示前N名
     * @return 报告文本
     */
    public String generateLeaderboardReport(int topN) {
        List<LeaderboardEntry> topEntries = getTopEntries(topN);

        if (topEntries.isEmpty()) {
            return "暂无排行榜数据";
        }

        StringBuilder report = new StringBuilder();
        report.append("=".repeat(60)).append("\n");
        report.append("           🏆 黑客松排行榜 TOP ").append(topN).append("\n");
        report.append("=".repeat(60)).append("\n\n");

        for (LeaderboardEntry entry : topEntries) {
            String medal = switch (entry.getRank()) {
                case 1 -> "🥇";
                case 2 -> "🥈";
                case 3 -> "🥉";
                default -> "  ";
            };

            report.append(String.format(
                "%s #%-2d | %-30s | %3d分 (%s) | %s\n",
                medal,
                entry.getRank(),
                truncate(entry.getProjectName(), 30),
                entry.getTotalScore(),
                entry.getGrade(),
                truncate(entry.getTeamName(), 15)
            ));
        }

        report.append("\n").append("=".repeat(60)).append("\n");

        // 添加统计信息
        Map<String, Object> stats = getLeaderboardStatistics();
        report.append(String.format(
            "总项目数: %d | 平均分: %d | 最高分: %d | 最低分: %d\n",
            stats.get("totalProjects"),
            stats.get("averageScore"),
            stats.get("maxScore"),
            stats.get("minScore")
        ));
        report.append("=".repeat(60)).append("\n");

        return report.toString();
    }

    /**
     * 截断字符串
     */
    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }

    /**
     * 清空排行榜缓存
     */
    public void clearCache() {
        leaderboardCache.clear();
    }

    /**
     * 获取所有可用的排行榜类型
     *
     * @return 排行榜类型列表
     */
    public List<String> getAvailableLeaderboards() {
        return new ArrayList<>(leaderboardCache.keySet());
    }
}

