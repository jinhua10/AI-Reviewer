package top.yumbo.ai.reviewer.entity;

import java.util.Map;

/**
 * 摘要报告：项目级别的整体分析结果
 * 简化版：只保留核心字段
 */
public class SummaryReport {
    private final String projectName;          // 项目名称
    private final String overallAssessment;    // 总体评价
    private final double qualityScore;         // 质量评分 (0-100)
    private final Map<String, Integer> issueCounts; // 问题统计
    private final Map<String, Object> recommendations; // 改进建议

    public SummaryReport(String projectName, String overallAssessment, double qualityScore,
                        Map<String, Integer> issueCounts, Map<String, Object> recommendations) {
        this.projectName = projectName;
        this.overallAssessment = overallAssessment;
        this.qualityScore = qualityScore;
        this.issueCounts = issueCounts;
        this.recommendations = recommendations;
    }

    // Getters
    public String getProjectName() { return projectName; }
    public String getOverallAssessment() { return overallAssessment; }
    public double getQualityScore() { return qualityScore; }
    public Map<String, Integer> getIssueCounts() { return issueCounts; }
    public Map<String, Object> getRecommendations() { return recommendations; }

    public static SummaryReport empty() {
        return new SummaryReport(
            "Unknown Project",
            "No analysis performed",
            0.0,
            Map.of(),
            Map.of()
        );
    }

    @Override
    public String toString() {
        return String.format("SummaryReport{project=%s, score=%.1f, issues=%d}",
            projectName, qualityScore,
            issueCounts.values().stream().mapToInt(Integer::intValue).sum());
    }
}

