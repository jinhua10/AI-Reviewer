package top.yumbo.ai.refactor.application.service;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.refactor.domain.model.ReviewReport;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 对比报告生成器
 * 比较两次分析结果的差异
 */
@Slf4j
public class ComparisonReportGenerator {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 生成对比报告
     */
    public String generateComparison(ReviewReport oldReport, ReviewReport newReport) {
        log.info("生成对比报告: {} vs {}", oldReport.getReportId(), newReport.getReportId());

        StringBuilder sb = new StringBuilder();

        // 标题
        sb.append("# 📊 代码质量对比分析报告\n\n");
        sb.append(String.format("**项目**: %s\n\n", newReport.getProjectName()));

        // 时间对比
        sb.append("## ⏰ 分析时间\n\n");
        sb.append(String.format("- **之前**: %s\n", oldReport.getGeneratedAt().format(DATE_FORMATTER)));
        sb.append(String.format("- **现在**: %s\n\n", newReport.getGeneratedAt().format(DATE_FORMATTER)));

        // 总体评分对比
        sb.append("## 📈 总体评分变化\n\n");
        appendScoreComparison(sb, oldReport.getOverallScore(), newReport.getOverallScore());

        // 评级变化
        String oldGrade = getGrade(oldReport.getOverallScore());
        String newGrade = getGrade(newReport.getOverallScore());
        if (!oldGrade.equals(newGrade)) {
            sb.append(String.format("\n**评级变化**: %s → %s %s\n\n",
                    oldGrade, newGrade, getGradeTrend(oldGrade, newGrade)));
        }

        // 维度对比
        sb.append("## 📊 维度评分对比\n\n");
        sb.append("| 维度 | 之前 | 现在 | 变化 | 趋势 |\n");
        sb.append("|------|------|------|------|------|\n");

        Map<String, Integer> oldScores = oldReport.getDimensionScores();
        Map<String, Integer> newScores = newReport.getDimensionScores();

        if (oldScores != null && newScores != null) {
            for (String dimension : newScores.keySet()) {
                int oldScore = oldScores.getOrDefault(dimension, 0);
                int newScore = newScores.get(dimension);
                int change = newScore - oldScore;
                String trend = getTrendIcon(change);
                String changeStr = change > 0 ? "+" + change : String.valueOf(change);

                sb.append(String.format("| %s | %d | %d | %s | %s |\n",
                        dimension, oldScore, newScore, changeStr, trend));
            }
        }
        sb.append("\n");

        // 主要改进
        sb.append("## ✅ 主要改进\n\n");
        appendImprovements(sb, oldReport, newReport);

        // 需要关注的问题
        sb.append("## ⚠️ 需要关注\n\n");
        appendConcerns(sb, oldReport, newReport);

        // 建议
        sb.append("## 💡 建议\n\n");
        appendRecommendations(sb, oldReport, newReport);

        return sb.toString();
    }

    /**
     * 添加评分对比
     */
    private void appendScoreComparison(StringBuilder sb, int oldScore, int newScore) {
        int change = newScore - oldScore;
        double changePercent = oldScore > 0 ? (change * 100.0 / oldScore) : 0;

        sb.append(String.format("- **之前**: %d/100\n", oldScore));
        sb.append(String.format("- **现在**: %d/100\n", newScore));
        sb.append(String.format("- **变化**: %+d (%+.1f%%) %s\n",
                change, changePercent, getTrendIcon(change)));
    }

    /**
     * 添加改进项
     */
    private void appendImprovements(StringBuilder sb, ReviewReport oldReport, ReviewReport newReport) {
        Map<String, Integer> oldScores = oldReport.getDimensionScores();
        Map<String, Integer> newScores = newReport.getDimensionScores();

        boolean hasImprovement = false;

        if (oldScores != null && newScores != null) {
            for (Map.Entry<String, Integer> entry : newScores.entrySet()) {
                String dimension = entry.getKey();
                int newScore = entry.getValue();
                int oldScore = oldScores.getOrDefault(dimension, 0);

                if (newScore > oldScore) {
                    sb.append(String.format("- ✅ **%s**提升了 %d 分 (从 %d 到 %d)\n",
                            dimension, newScore - oldScore, oldScore, newScore));
                    hasImprovement = true;
                }
            }
        }

        if (!hasImprovement) {
            sb.append("- 暂无明显改进\n");
        }
        sb.append("\n");
    }

    /**
     * 添加关注项
     */
    private void appendConcerns(StringBuilder sb, ReviewReport oldReport, ReviewReport newReport) {
        Map<String, Integer> oldScores = oldReport.getDimensionScores();
        Map<String, Integer> newScores = newReport.getDimensionScores();

        boolean hasConcern = false;

        if (oldScores != null && newScores != null) {
            for (Map.Entry<String, Integer> entry : newScores.entrySet()) {
                String dimension = entry.getKey();
                int newScore = entry.getValue();
                int oldScore = oldScores.getOrDefault(dimension, 0);

                if (newScore < oldScore) {
                    sb.append(String.format("- ⚠️ **%s**下降了 %d 分 (从 %d 到 %d)\n",
                            dimension, oldScore - newScore, oldScore, newScore));
                    hasConcern = true;
                }
            }
        }

        if (!hasConcern) {
            sb.append("- 无明显问题\n");
        }
        sb.append("\n");
    }

    /**
     * 添加建议
     */
    private void appendRecommendations(StringBuilder sb, ReviewReport oldReport, ReviewReport newReport) {
        int scoreDiff = newReport.getOverallScore() - oldReport.getOverallScore();

        if (scoreDiff > 5) {
            sb.append("- 👍 继续保持良好的开发实践\n");
            sb.append("- 📈 关注持续改进的方向\n");
        } else if (scoreDiff < -5) {
            sb.append("- ⚠️ 代码质量有所下降，需要关注\n");
            sb.append("- 🔍 建议review最近的代码变更\n");
            sb.append("- 📚 考虑增加代码审查流程\n");
        } else {
            sb.append("- ✅ 代码质量保持稳定\n");
            sb.append("- 💡 可以考虑针对性优化低分维度\n");
        }
    }

    /**
     * 获取趋势图标
     */
    private String getTrendIcon(int change) {
        if (change > 5) return "📈 大幅提升";
        if (change > 0) return "↗️ 提升";
        if (change == 0) return "➡️ 持平";
        if (change > -5) return "↘️ 下降";
        return "📉 大幅下降";
    }

    /**
     * 获取评级
     */
    private String getGrade(int score) {
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "F";
    }

    /**
     * 获取评级趋势
     */
    private String getGradeTrend(String oldGrade, String newGrade) {
        int old = "ABCDF".indexOf(oldGrade);
        int newG = "ABCDF".indexOf(newGrade);
        if (newG < old) return "⬆️ 提升";
        if (newG > old) return "⬇️ 下降";
        return "➡️ 持平";
    }

    /**
     * 生成简化对比
     */
    public ComparisonResult compare(ReviewReport oldReport, ReviewReport newReport) {
        ComparisonResult result = new ComparisonResult();
        result.setOldScore(oldReport.getOverallScore());
        result.setNewScore(newReport.getOverallScore());
        result.setScoreChange(newReport.getOverallScore() - oldReport.getOverallScore());

        // 对比维度
        Map<String, Integer> changes = new HashMap<>();
        Map<String, Integer> oldScores = oldReport.getDimensionScores();
        Map<String, Integer> newScores = newReport.getDimensionScores();

        if (oldScores != null && newScores != null) {
            for (String dimension : newScores.keySet()) {
                int change = newScores.get(dimension) - oldScores.getOrDefault(dimension, 0);
                changes.put(dimension, change);
            }
        }
        result.setDimensionChanges(changes);

        return result;
    }

    /**
     * 对比结果
     */
    public static class ComparisonResult {
        private int oldScore;
        private int newScore;
        private int scoreChange;
        private Map<String, Integer> dimensionChanges;

        // Getters and Setters
        public int getOldScore() { return oldScore; }
        public void setOldScore(int oldScore) { this.oldScore = oldScore; }

        public int getNewScore() { return newScore; }
        public void setNewScore(int newScore) { this.newScore = newScore; }

        public int getScoreChange() { return scoreChange; }
        public void setScoreChange(int scoreChange) { this.scoreChange = scoreChange; }

        public Map<String, Integer> getDimensionChanges() { return dimensionChanges; }
        public void setDimensionChanges(Map<String, Integer> dimensionChanges) {
            this.dimensionChanges = dimensionChanges;
        }

        public boolean isImproved() {
            return scoreChange > 0;
        }

        public boolean isRegressed() {
            return scoreChange < 0;
        }
    }
}

