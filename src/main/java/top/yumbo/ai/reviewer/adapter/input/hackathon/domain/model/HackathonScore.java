package top.yumbo.ai.reviewer.adapter.input.hackathon.domain.model;

/**
 * 黑客松评分领域模型
 *
 * 包含四个维度的评分和综合得分计算
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-12
 */
public class HackathonScore {

    // 代码质量分数 (0-100)，权重 40%
    private final int codeQuality;

    // 创新性分数 (0-100)，权重 30%
    private final int innovation;

    // 完成度分数 (0-100)，权重 20%
    private final int completeness;

    // 文档质量分数 (0-100)，权重 10%
    private final int documentation;

    // 权重常量
    private static final double WEIGHT_CODE_QUALITY = 0.40;
    private static final double WEIGHT_INNOVATION = 0.30;
    private static final double WEIGHT_COMPLETENESS = 0.20;
    private static final double WEIGHT_DOCUMENTATION = 0.10;

    // 私有构造函数，强制使用Builder
    private HackathonScore(Builder builder) {
        this.codeQuality = validateScore(builder.codeQuality, "代码质量");
        this.innovation = validateScore(builder.innovation, "创新性");
        this.completeness = validateScore(builder.completeness, "完成度");
        this.documentation = validateScore(builder.documentation, "文档质量");
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
     * 计算综合得分
     */
    public int calculateTotalScore() {
        double total = codeQuality * WEIGHT_CODE_QUALITY
                     + innovation * WEIGHT_INNOVATION
                     + completeness * WEIGHT_COMPLETENESS
                     + documentation * WEIGHT_DOCUMENTATION;
        return (int) Math.round(total);
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
     * 获取分数详情描述
     */
    public String getScoreDetails() {
        return String.format(
            "总分: %d (%s)\n" +
            "  代码质量: %d (40%%)\n" +
            "  创新性:   %d (30%%)\n" +
            "  完成度:   %d (20%%)\n" +
            "  文档质量: %d (10%%)",
            calculateTotalScore(), getGrade(),
            codeQuality, innovation, completeness, documentation
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

