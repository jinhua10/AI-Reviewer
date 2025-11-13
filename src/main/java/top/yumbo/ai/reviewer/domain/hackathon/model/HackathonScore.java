package top.yumbo.ai.reviewer.domain.hackathon.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 黑客松评分领域模型（动态权重版）
 *
 * 支持动态权重配置
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-13
 */
public class HackathonScore {

    // 代码质量分数 (0-100)
    private final int codeQuality;

    // 创新性分数 (0-100)
    private final int innovation;

    // 完成度分数 (0-100)
    private final int completeness;

    // 文档质量分数 (0-100)
    private final int documentation;

    // 默认权重常量（向后兼容）
    private static final double DEFAULT_WEIGHT_CODE_QUALITY = 0.40;
    private static final double DEFAULT_WEIGHT_INNOVATION = 0.30;
    private static final double DEFAULT_WEIGHT_COMPLETENESS = 0.20;
    private static final double DEFAULT_WEIGHT_DOCUMENTATION = 0.10;

    // 动态权重（可选）
    private final Map<String, Double> customWeights;

    // 私有构造函数，强制使用Builder
    private HackathonScore(Builder builder) {
        this.codeQuality = validateScore(builder.codeQuality, "代码质量");
        this.innovation = validateScore(builder.innovation, "创新性");
        this.completeness = validateScore(builder.completeness, "完成度");
        this.documentation = validateScore(builder.documentation, "文档质量");
        this.customWeights = builder.customWeights;
    }

    /**
     * 验证分数范围
     */
    private int validateScore(int score, String dimension) {
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException(
                dimension + "分数必须在 0-100 之间，当前值: " + score
            );
        }
        return score;
    }

    /**
     * 计算综合得分（支持动态权重）
     */
    public int calculateTotalScore() {
        // 获取权重（自定义或默认）
        double weightCodeQuality = getWeight("code_quality", DEFAULT_WEIGHT_CODE_QUALITY);
        double weightInnovation = getWeight("innovation", DEFAULT_WEIGHT_INNOVATION);
        double weightCompleteness = getWeight("completeness", DEFAULT_WEIGHT_COMPLETENESS);
        double weightDocumentation = getWeight("documentation", DEFAULT_WEIGHT_DOCUMENTATION);

        double total = codeQuality * weightCodeQuality
                     + innovation * weightInnovation
                     + completeness * weightCompleteness
                     + documentation * weightDocumentation;
        return (int) Math.round(total);
    }

    /**
     * 获取权重（自定义或默认）
     */
    private double getWeight(String dimension, double defaultWeight) {
        if (customWeights != null && customWeights.containsKey(dimension)) {
            return customWeights.get(dimension);
        }
        return defaultWeight;
    }

    /**
     * 获取指定维度的权重（用于显示）- 使用映射而非硬编码
     */
    public double getDimensionWeight(String dimension) {
        // 使用映射表（消除硬编码switch）
        Map<String, Double> defaultWeights = Map.of(
            "code_quality", DEFAULT_WEIGHT_CODE_QUALITY,
            "innovation", DEFAULT_WEIGHT_INNOVATION,
            "completeness", DEFAULT_WEIGHT_COMPLETENESS,
            "documentation", DEFAULT_WEIGHT_DOCUMENTATION
        );

        double defaultWeight = defaultWeights.getOrDefault(dimension, 0.0);
        return getWeight(dimension, defaultWeight);
    }
    /**
     * 获取综合得分（getTotalScore是calculateTotalScore的别名）
     */
    public int getTotalScore() {
        return calculateTotalScore();
    }

    /**
     * 获取等级 (S, A, B, C, D, F)
     */
    public String getGrade() {
        int total = calculateTotalScore();
        if (total >= 90) return "S";
        if (total >= 80) return "A";
        if (total >= 70) return "B";
        if (total >= 60) return "C";
        if (total >= 50) return "D";
        return "F";
    }

    /**
     * 获取等级描述
     */
    public String getGradeDescription() {
        return switch (getGrade()) {
            case "S" -> "优秀 (90-100分)";
            case "A" -> "良好 (80-89分)";
            case "B" -> "中等 (70-79分)";
            case "C" -> "及格 (60-69分)";
            case "D" -> "较差 (50-59分)";
            case "F" -> "不及格 (0-49分)";
            default -> "未知";
        };
    }

    /**
     * 检查是否通过（总分 >= 60）
     */
    public boolean isPassed() {
        return calculateTotalScore() >= 60;
    }

    /**
     * 获取最强项
     */
    public String getStrongestDimension() {
        int max = Math.max(Math.max(codeQuality, innovation),
                          Math.max(completeness, documentation));
        if (max == codeQuality) return "代码质量";
        if (max == innovation) return "创新性";
        if (max == completeness) return "完成度";
        return "文档质量";
    }

    /**
     * 获取最弱项
     */
    public String getWeakestDimension() {
        int min = Math.min(Math.min(codeQuality, innovation),
                          Math.min(completeness, documentation));
        if (min == codeQuality) return "代码质量";
        if (min == innovation) return "创新性";
        if (min == completeness) return "完成度";
        return "文档质量";
    }

    /**
     * 获取分数详情描述（动态权重版）
     */
    public String getScoreDetails() {
        return String.format(
            "总分: %d (%s)\n" +
            "  代码质量: %d (%.0f%%)\n" +
            "  创新性:   %d (%.0f%%)\n" +
            "  完成度:   %d (%.0f%%)\n" +
            "  文档质量: %d (%.0f%%)",
            calculateTotalScore(), getGrade(),
            codeQuality, getDimensionWeight("code_quality") * 100,
            innovation, getDimensionWeight("innovation") * 100,
            completeness, getDimensionWeight("completeness") * 100,
            documentation, getDimensionWeight("documentation") * 100
        );
    }

    // Getters
    public int getCodeQuality() {
        return codeQuality;
    }

    public int getInnovation() {
        return innovation;
    }

    public int getCompleteness() {
        return completeness;
    }

    public int getDocumentation() {
        return documentation;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int codeQuality;
        private int innovation;
        private int completeness;
        private int documentation;
        private Map<String, Double> customWeights;

        public Builder codeQuality(int codeQuality) {
            this.codeQuality = codeQuality;
            return this;
        }

        public Builder innovation(int innovation) {
            this.innovation = innovation;
            return this;
        }

        public Builder completeness(int completeness) {
            this.completeness = completeness;
            return this;
        }

        public Builder documentation(int documentation) {
            this.documentation = documentation;
            return this;
        }

        /**
         * 设置自定义权重
         */
        public Builder customWeights(Map<String, Double> weights) {
            this.customWeights = weights;
            return this;
        }

        /**
         * 从配置设置权重
         */
        public Builder weightsFromConfig(HackathonScoringConfig config) {
            if (config != null) {
                this.customWeights = new HashMap<>();
                this.customWeights.put("code_quality", config.getDimensionWeight("code_quality"));
                this.customWeights.put("innovation", config.getDimensionWeight("innovation"));
                this.customWeights.put("completeness", config.getDimensionWeight("completeness"));
                this.customWeights.put("documentation", config.getDimensionWeight("documentation"));
            }
            return this;
        }

        public HackathonScore build() {
            return new HackathonScore(this);
        }
    }

    @Override
    public String toString() {
        return String.format(
            "HackathonScore{total=%d, grade=%s, " +
            "codeQuality=%d, innovation=%d, completeness=%d, documentation=%d}",
            calculateTotalScore(), getGrade(),
            codeQuality, innovation, completeness, documentation
        );
    }
}


