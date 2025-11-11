package top.yumbo.ai.reviewer.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * 详细报告
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetailReport {

    private String title;
    private String content;

    // 各维度详细分析
    private ArchitectureAnalysis architectureAnalysis;
    private CodeQualityAnalysis codeQualityAnalysis;
    private TechnicalDebtAnalysis technicalDebtAnalysis;
    private FunctionalityAnalysis functionalityAnalysis;
    private BusinessValueAnalysis businessValueAnalysis;
    private TestCoverageAnalysis testCoverageAnalysis;

    // 文件级分析
    private List<FileAnalysis> fileAnalyses;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ArchitectureAnalysis {
        private String overview;
        private List<String> strengths;
        private List<String> weaknesses;
        private List<String> recommendations;
        private String layeringAssessment;
        private String couplingAssessment;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CodeQualityAnalysis {
        private String overview;
        private List<String> issues;
        private List<String> bestPractices;
        private String complexityAssessment;
        private String maintainabilityScore;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TechnicalDebtAnalysis {
        private String overview;
        private List<String> debts;
        private String estimatedEffort;
        private List<String> refactoringSuggestions;
        private String priorityAssessment;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FunctionalityAnalysis {
        private String overview;
        private List<String> missingFeatures;
        private List<String> redundantFeatures;
        private String completenessAssessment;
        private List<String> improvementSuggestions;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BusinessValueAnalysis {
        private String overview;
        private List<String> highValueFeatures;
        private List<String> lowValueFeatures;
        private String roiEstimation;
        private List<String> costReductionOpportunities;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TestCoverageAnalysis {
        private String overview;
        private List<String> coveredFeatures;
        private List<String> uncoveredFeatures;
        private String coveragePercentage;
        private List<String> testQualityIssues;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FileAnalysis {
        private String filePath;
        private String fileType;
        private int score;
        private List<String> issues;
        private List<String> highlights;
        private String summary;
    }
}
