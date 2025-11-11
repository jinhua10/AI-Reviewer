package top.yumbo.ai.reviewer.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * 分析结果实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisResult {

    // 总体评分 (0-100)
    private int overallScore;

    // 各维度评分
    private int architectureScore;
    private int codeQualityScore;
    private int technicalDebtScore;
    private int functionalityScore;
    private int businessValueScore;
    private int testCoverageScore;

    // 报告
    private SummaryReport summaryReport;
    private DetailReport detailReport;

    // 分析时间戳
    private long analysisTimestamp;

    // 项目信息
    private String projectName;
    private String projectPath;
    private String projectType; // java, python, javascript, etc.

    // 分析的维度
    private List<String> analyzedDimensions;

    // 发现的问题列表
    private List<Issue> issues;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Issue {
        private String type; // architecture, quality, debt, functionality
        private String severity; // critical, major, minor, info
        private String title;
        private String description;
        private String filePath;
        private int lineNumber;
        private String suggestion;
        private String category;
    }
}
