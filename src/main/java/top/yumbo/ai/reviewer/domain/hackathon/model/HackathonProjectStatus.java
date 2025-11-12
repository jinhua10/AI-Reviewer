package top.yumbo.ai.reviewer.domain.hackathon.model;

/**
 * 黑客松项目状态枚举
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-12
 */
public enum HackathonProjectStatus {

    /**
     * 已创建，等待提交
     */
    CREATED("已创建", "项目已创建，等待提交代码"),

    /**
     * 已提交，等待评审
     */
    SUBMITTED("已提交", "代码已提交，等待自动评审"),

    /**
     * 评审中
     */
    REVIEWING("评审中", "正在进行自动代码评审"),

    /**
     * 已评审
     */
    REVIEWED("已评审", "评审完成，分数已生成"),

    /**
     * 已关闭
     */
    CLOSED("已关闭", "项目已关闭，不再接受提交");

    private final String displayName;
    private final String description;

    HackathonProjectStatus(String displayName, String description) {
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
     * 检查是否可以添加提交
     */
    public boolean canSubmit() {
        return this == CREATED || this == SUBMITTED || this == REVIEWED;
    }

    /**
     * 检查是否可以评审
     */
    public boolean canReview() {
        return this == SUBMITTED || this == REVIEWING;
    }

    /**
     * 检查是否已完成
     */
    public boolean isCompleted() {
        return this == REVIEWED || this == CLOSED;
    }
}


