package top.yumbo.ai.reviewer.adapter.input.hackathon.domain.model;

/**
 * 提交状态枚举
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-12
 */
public enum SubmissionStatus {

    /**
     * 待评审
     */
    PENDING("待评审", "等待自动评审"),

    /**
     * 评审中
     */
    REVIEWING("评审中", "正在进行代码分析"),

    /**
     * 已完成
     */
    COMPLETED("已完成", "评审完成，分数已生成"),

    /**
     * 失败
     */
    FAILED("失败", "评审失败，请检查代码或重新提交");

    private final String displayName;
    private final String description;

    SubmissionStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 检查是否可以开始评审
     */
    public boolean canStartReview() {
        return this == PENDING;
    }

    /**
     * 检查是否正在评审
     */
    public boolean isReviewing() {
        return this == REVIEWING;
    }

    /**
     * 检查是否已完成
     */
    public boolean isCompleted() {
        return this == COMPLETED;
    }

    /**
     * 检查是否失败
     */
    public boolean isFailed() {
        return this == FAILED;
    }

    /**
     * 检查是否可以重新提交
     */
    public boolean canResubmit() {
        return this == FAILED;
    }
}

