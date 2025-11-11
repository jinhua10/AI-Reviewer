package top.yumbo.ai.refactor.adapter.output.visualization;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.refactor.domain.model.ReviewReport;

import java.util.Map;

/**
 * 图表生成器
 * 生成文本形式的可视化图表
 */
@Slf4j
public class ChartGenerator {

    /**
     * 生成ASCII雷达图
     */
    public String generateRadarChart(ReviewReport report) {
        StringBuilder sb = new StringBuilder();

        sb.append("## 📊 评分雷达图\n\n");
        sb.append("```\n");

        Map<String, Integer> scores = report.getDimensionScores();
        if (scores != null && !scores.isEmpty()) {
            for (Map.Entry<String, Integer> entry : scores.entrySet()) {
                String dimension = entry.getKey();
                int score = entry.getValue();

                sb.append(String.format("%-20s ", dimension));
                sb.append(generateBar(score, 100));
                sb.append(String.format(" %d/100\n", score));
            }
        }

        sb.append("```\n\n");
        return sb.toString();
    }

    /**
     * 生成趋势折线图（ASCII）
     */
    public String generateTrendChart(Map<String, Integer> history) {
        StringBuilder sb = new StringBuilder();

        sb.append("## 📈 质量趋势\n\n");
        sb.append("```\n");

        if (history != null && !history.isEmpty()) {
            for (Map.Entry<String, Integer> entry : history.entrySet()) {
                String date = entry.getKey();
                int score = entry.getValue();

                sb.append(String.format("%-12s ", date));
                sb.append(generateBar(score, 100));
                sb.append(String.format(" %d\n", score));
            }
        }

        sb.append("```\n\n");
        return sb.toString();
    }

    /**
     * 生成进度条
     */
    private String generateBar(int value, int max) {
        int barLength = 50;
        int filled = (int) ((value * barLength) / (double) max);

        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < barLength; i++) {
            if (i < filled) {
                bar.append("█");
            } else {
                bar.append("░");
            }
        }
        bar.append("]");

        return bar.toString();
    }

    /**
     * 生成评分徽章（Markdown）
     */
    public String generateScoreBadge(int score) {
        String color = getScoreColor(score);
        String label = getScoreLabel(score);

        return String.format("![Score](https://img.shields.io/badge/Score-%d--%s?style=flat-square) "
                + "![Grade](https://img.shields.io/badge/Grade-%s-%s?style=flat-square)",
                score, color, label, color);
    }

    /**
     * 获取评分颜色
     */
    private String getScoreColor(int score) {
        if (score >= 90) return "brightgreen";
        if (score >= 80) return "green";
        if (score >= 70) return "yellow";
        if (score >= 60) return "orange";
        return "red";
    }

    /**
     * 获取评分标签
     */
    private String getScoreLabel(int score) {
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "F";
    }
}

