package top.yumbo.ai.reviewer.adapter.input.hackathon.domain.model;

/**
 * 参与者角色枚举
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-12
 */
public enum ParticipantRole {

    /**
     * 队长
     */
    LEADER("队长", "团队负责人，拥有所有权限"),

    /**
     * 成员
     */
    MEMBER("成员", "团队普通成员"),

    /**
     * 导师
     */
    MENTOR("导师", "团队指导老师，不参与编码");

    private final String displayName;
    private final String description;

    ParticipantRole(String displayName, String description) {
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
     * 检查是否有提交权限
     */
    public boolean canSubmit() {
        return this == LEADER || this == MEMBER;
    }

    /**
     * 检查是否有管理权限
     */
    public boolean canManage() {
        return this == LEADER;
    }
}

