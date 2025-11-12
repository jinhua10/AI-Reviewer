package top.yumbo.ai.reviewer.domain.hackathon.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 团队领域模型
 *
 * 表示一个参赛团队，包含成员信息和联系方式
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-12
 */
public class Team {

    private final String id;
    private final String name;
    private final String description;
    private final List<Participant> members;
    private final Participant leader;
    private final String contactEmail;

    // 私有构造函数，强制使用Builder
    private Team(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.name = builder.name;
        this.description = builder.description;
        this.members = new ArrayList<>(builder.members);
        this.leader = builder.leader;
        this.contactEmail = builder.contactEmail;
    }

    /**
     * 检查团队是否有效
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty()
            && leader != null
            && !members.isEmpty()
            && members.contains(leader)
            && contactEmail != null && isValidEmail(contactEmail);
    }

    /**
     * 获取团队人数
     */
    public int getMemberCount() {
        return members.size();
    }

    /**
     * 检查是否是团队成员
     */
    public boolean isMember(Participant participant) {
        return members.contains(participant);
    }

    /**
     * 检查是否是队长
     */
    public boolean isLeader(Participant participant) {
        return leader != null && leader.equals(participant);
    }

    /**
     * 简单的邮箱格式验证
     */
    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<Participant> getMembers() {
        return new ArrayList<>(members);
    }

    public Participant getLeader() {
        return leader;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String name;
        private String description;
        private List<Participant> members = new ArrayList<>();
        private Participant leader;
        private String contactEmail;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder members(List<Participant> members) {
            this.members = new ArrayList<>(members);
            return this;
        }

        public Builder addMember(Participant member) {
            this.members.add(member);
            return this;
        }

        public Builder leader(Participant leader) {
            this.leader = leader;
            // 确保队长也在成员列表中
            if (leader != null && !this.members.contains(leader)) {
                this.members.add(leader);
            }
            return this;
        }

        public Builder contactEmail(String contactEmail) {
            this.contactEmail = contactEmail;
            return this;
        }

        public Team build() {
            // 验证必填字段
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("团队名称不能为空");
            }
            if (leader == null) {
                throw new IllegalArgumentException("团队必须有队长");
            }
            if (members.isEmpty()) {
                throw new IllegalArgumentException("团队必须至少有一名成员");
            }
            if (!members.contains(leader)) {
                throw new IllegalArgumentException("队长必须是团队成员");
            }
            if (contactEmail == null || contactEmail.trim().isEmpty()) {
                throw new IllegalArgumentException("联系邮箱不能为空");
            }

            return new Team(this);
        }
    }

    @Override
    public String toString() {
        return "Team{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", leader=" + leader.getName() +
                ", memberCount=" + members.size() +
                ", contactEmail='" + contactEmail + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Team team = (Team) o;
        return id.equals(team.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}


