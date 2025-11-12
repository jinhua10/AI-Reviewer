package top.yumbo.ai.reviewer.domain.hackathon.model;

import java.util.UUID;

/**
 * 参与者领域模型
 *
 * 表示一个黑客松参与者
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-12
 */
public class Participant {

    private final String id;
    private final String name;
    private final String email;
    private final String githubUsername;
    private final String organization;
    private final ParticipantRole role;

    // 私有构造函数，强制使用Builder
    private Participant(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.name = builder.name;
        this.email = builder.email;
        this.githubUsername = builder.githubUsername;
        this.organization = builder.organization;
        this.role = builder.role != null ? builder.role : ParticipantRole.MEMBER;
    }

    /**
     * 检查参与者信息是否有效
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty()
            && email != null && isValidEmail(email);
    }

    /**
     * 简单的邮箱格式验证
     */
    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    /**
     * 检查是否是队长
     */
    public boolean isLeader() {
        return role == ParticipantRole.LEADER;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getGithubUsername() {
        return githubUsername;
    }

    public String getOrganization() {
        return organization;
    }

    public ParticipantRole getRole() {
        return role;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String name;
        private String email;
        private String githubUsername;
        private String organization;
        private ParticipantRole role;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder githubUsername(String githubUsername) {
            this.githubUsername = githubUsername;
            return this;
        }

        public Builder organization(String organization) {
            this.organization = organization;
            return this;
        }

        public Builder role(ParticipantRole role) {
            this.role = role;
            return this;
        }

        public Participant build() {
            // 验证必填字段
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("参与者姓名不能为空");
            }
            if (email == null || email.trim().isEmpty()) {
                throw new IllegalArgumentException("参与者邮箱不能为空");
            }

            return new Participant(this);
        }
    }

    @Override
    public String toString() {
        return "Participant{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", githubUsername='" + githubUsername + '\'' +
                ", role=" + role +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Participant that = (Participant) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}


