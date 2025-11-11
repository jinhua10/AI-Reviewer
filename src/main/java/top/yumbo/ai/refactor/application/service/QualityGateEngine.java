package top.yumbo.ai.refactor.application.service;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.refactor.domain.model.ReviewReport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 质量门禁引擎
 * 根据规则检查代码质量是否达标
 */
@Slf4j
public class QualityGateEngine {

    private final QualityGateConfig config;

    public QualityGateEngine() {
        this(QualityGateConfig.createDefault());
    }

    public QualityGateEngine(QualityGateConfig config) {
        this.config = config;
        log.info("质量门禁引擎初始化: minScore={}, blockerIssues={}",
                config.getMinOverallScore(), config.getMaxCriticalIssues());
    }

    /**
     * 检查质量门禁
     */
    public GateResult checkGates(ReviewReport report) {
        log.info("开始检查质量门禁: project={}", report.getProjectName());

        GateResult result = new GateResult();
        result.setProjectName(report.getProjectName());
        result.setOverallScore(report.getOverallScore());

        // 1. 检查总体评分
        checkOverallScore(report, result);

        // 2. 检查维度评分
        checkDimensionScores(report, result);

        // 3. 检查关键问题
        checkCriticalIssues(report, result);

        // 4. 检查技术债务
        checkTechnicalDebt(report, result);

        // 5. 计算最终结果
        result.setPassed(result.getFailedGates().isEmpty());

        if (result.isPassed()) {
            log.info("✅ 质量门禁通过: project={}, score={}",
                    report.getProjectName(), report.getOverallScore());
        } else {
            log.warn("❌ 质量门禁失败: project={}, failedGates={}",
                    report.getProjectName(), result.getFailedGates().size());
        }

        return result;
    }

    /**
     * 检查总体评分
     */
    private void checkOverallScore(ReviewReport report, GateResult result) {
        int score = report.getOverallScore();

        if (score < config.getMinOverallScore()) {
            FailedGate gate = new FailedGate(
                    "overall_score",
                    "总体评分不达标",
                    String.format("要求: %d, 实际: %d", config.getMinOverallScore(), score),
                    "BLOCKER"
            );
            result.addFailedGate(gate);
        } else if (score < config.getRecommendedScore()) {
            FailedGate gate = new FailedGate(
                    "overall_score",
                    "总体评分低于推荐值",
                    String.format("推荐: %d, 实际: %d", config.getRecommendedScore(), score),
                    "WARNING"
            );
            result.addWarning(gate);
        }
    }

    /**
     * 检查维度评分
     */
    private void checkDimensionScores(ReviewReport report, GateResult result) {
        Map<String, Integer> dimensionScores = report.getDimensionScores();
        if (dimensionScores == null || dimensionScores.isEmpty()) {
            return;
        }

        Map<String, Integer> minScores = config.getMinDimensionScores();
        for (Map.Entry<String, Integer> entry : minScores.entrySet()) {
            String dimension = entry.getKey();
            int minScore = entry.getValue();

            Integer actualScore = dimensionScores.get(dimension);
            if (actualScore == null) {
                continue;
            }

            if (actualScore < minScore) {
                FailedGate gate = new FailedGate(
                        "dimension_score_" + dimension,
                        dimension + "评分不达标",
                        String.format("要求: %d, 实际: %d", minScore, actualScore),
                        "MAJOR"
                );
                result.addFailedGate(gate);
            }
        }
    }

    /**
     * 检查关键问题
     */
    private void checkCriticalIssues(ReviewReport report, GateResult result) {
        List<ReviewReport.Issue> issues = report.getIssues();
        if (issues == null || issues.isEmpty()) {
            return;
        }

        // 简化实现：检查问题数量
        int criticalCount = (int) issues.stream()
                .filter(issue -> issue.getDescription().contains("严重") ||
                               issue.getDescription().contains("critical"))
                .count();

        if (criticalCount > config.getMaxCriticalIssues()) {
            FailedGate gate = new FailedGate(
                    "critical_issues",
                    "严重问题过多",
                    String.format("最多允许: %d, 实际: %d", config.getMaxCriticalIssues(), criticalCount),
                    "BLOCKER"
            );
            result.addFailedGate(gate);
        }

        int majorCount = (int) issues.stream()
                .filter(issue -> issue.getDescription().contains("重要") ||
                               issue.getDescription().contains("major"))
                .count();

        if (majorCount > config.getMaxMajorIssues()) {
            FailedGate gate = new FailedGate(
                    "major_issues",
                    "重要问题过多",
                    String.format("最多允许: %d, 实际: %d", config.getMaxMajorIssues(), majorCount),
                    "MAJOR"
            );
            result.addFailedGate(gate);
        }
    }

    /**
     * 检查技术债务
     */
    private void checkTechnicalDebt(ReviewReport report, GateResult result) {
        // 简化实现：基于问题数量估算技术债务
        List<ReviewReport.Issue> issues = report.getIssues();
        if (issues == null || issues.isEmpty()) {
            return;
        }

        // 假设每个问题平均需要1小时修复
        int debtHours = issues.size();

        if (debtHours > config.getMaxTechnicalDebtHours()) {
            FailedGate gate = new FailedGate(
                    "technical_debt",
                    "技术债务过高",
                    String.format("最多允许: %d小时, 估算: %d小时",
                            config.getMaxTechnicalDebtHours(), debtHours),
                    "MAJOR"
            );
            result.addFailedGate(gate);
        }
    }

    /**
     * 获取失败的门禁
     */
    public List<FailedGate> getFailedGates(ReviewReport report) {
        return checkGates(report).getFailedGates();
    }

    /**
     * 生成门禁报告
     */
    public String generateGateReport(GateResult result) {
        StringBuilder sb = new StringBuilder();

        sb.append("╔════════════════════════════════════════╗\n");
        sb.append("║        质量门禁检查报告                  ║\n");
        sb.append("╚════════════════════════════════════════╝\n\n");

        sb.append("项目: ").append(result.getProjectName()).append("\n");
        sb.append("总体评分: ").append(result.getOverallScore()).append("/100\n");
        sb.append("检查结果: ").append(result.isPassed() ? "✅ 通过" : "❌ 失败").append("\n\n");

        if (!result.getFailedGates().isEmpty()) {
            sb.append("失败的门禁 (").append(result.getFailedGates().size()).append("):\n");
            for (FailedGate gate : result.getFailedGates()) {
                sb.append(String.format("  ❌ [%s] %s\n", gate.getSeverity(), gate.getName()));
                sb.append(String.format("     原因: %s\n", gate.getReason()));
            }
            sb.append("\n");
        }

        if (!result.getWarnings().isEmpty()) {
            sb.append("警告 (").append(result.getWarnings().size()).append("):\n");
            for (FailedGate warning : result.getWarnings()) {
                sb.append(String.format("  ⚠️  %s: %s\n", warning.getName(), warning.getReason()));
            }
        }

        return sb.toString();
    }

    /**
     * 判断是否通过质量门禁
     */
    public boolean isPassingQuality(ReviewReport report) {
        return checkGates(report).isPassed();
    }

    /**
     * 获取配置
     */
    public QualityGateConfig getConfig() {
        return config;
    }

    /**
     * 质量门禁配置
     */
    public static class QualityGateConfig {
        private int minOverallScore = 70;
        private int recommendedScore = 80;
        private int maxCriticalIssues = 0;
        private int maxMajorIssues = 5;
        private int maxTechnicalDebtHours = 40;
        private Map<String, Integer> minDimensionScores = new HashMap<>();

        public static QualityGateConfig createDefault() {
            QualityGateConfig config = new QualityGateConfig();
            config.minDimensionScores.put("architecture", 60);
            config.minDimensionScores.put("code_quality", 70);
            config.minDimensionScores.put("test_coverage", 60);
            return config;
        }

        // Getters and Setters
        public int getMinOverallScore() { return minOverallScore; }
        public void setMinOverallScore(int minOverallScore) { this.minOverallScore = minOverallScore; }

        public int getRecommendedScore() { return recommendedScore; }
        public void setRecommendedScore(int recommendedScore) { this.recommendedScore = recommendedScore; }

        public int getMaxCriticalIssues() { return maxCriticalIssues; }
        public void setMaxCriticalIssues(int maxCriticalIssues) { this.maxCriticalIssues = maxCriticalIssues; }

        public int getMaxMajorIssues() { return maxMajorIssues; }
        public void setMaxMajorIssues(int maxMajorIssues) { this.maxMajorIssues = maxMajorIssues; }

        public int getMaxTechnicalDebtHours() { return maxTechnicalDebtHours; }
        public void setMaxTechnicalDebtHours(int maxTechnicalDebtHours) {
            this.maxTechnicalDebtHours = maxTechnicalDebtHours;
        }

        public Map<String, Integer> getMinDimensionScores() { return minDimensionScores; }
        public void setMinDimensionScores(Map<String, Integer> minDimensionScores) {
            this.minDimensionScores = minDimensionScores;
        }
    }

    /**
     * 门禁检查结果
     */
    public static class GateResult {
        private String projectName;
        private int overallScore;
        private boolean passed;
        private List<FailedGate> failedGates = new ArrayList<>();
        private List<FailedGate> warnings = new ArrayList<>();

        public void addFailedGate(FailedGate gate) {
            failedGates.add(gate);
        }

        public void addWarning(FailedGate warning) {
            warnings.add(warning);
        }

        // Getters and Setters
        public String getProjectName() { return projectName; }
        public void setProjectName(String projectName) { this.projectName = projectName; }

        public int getOverallScore() { return overallScore; }
        public void setOverallScore(int overallScore) { this.overallScore = overallScore; }

        public boolean isPassed() { return passed; }
        public void setPassed(boolean passed) { this.passed = passed; }

        public List<FailedGate> getFailedGates() { return failedGates; }
        public void setFailedGates(List<FailedGate> failedGates) { this.failedGates = failedGates; }

        public List<FailedGate> getWarnings() { return warnings; }
        public void setWarnings(List<FailedGate> warnings) { this.warnings = warnings; }
    }

    /**
     * 失败的门禁
     */
    public static class FailedGate {
        private final String gateId;
        private final String name;
        private final String reason;
        private final String severity;

        public FailedGate(String gateId, String name, String reason, String severity) {
            this.gateId = gateId;
            this.name = name;
            this.reason = reason;
            this.severity = severity;
        }

        public String getGateId() { return gateId; }
        public String getName() { return name; }
        public String getReason() { return reason; }
        public String getSeverity() { return severity; }
    }
}

