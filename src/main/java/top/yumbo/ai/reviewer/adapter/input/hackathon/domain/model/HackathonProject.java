package top.yumbo.ai.reviewer.adapter.input.hackathon.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 黑客松项目领域模型
 *
 * 表示一个黑客松项目，包含团队、提交记录和评分信息
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-12
 */
public class HackathonProject {

    private final String id;
    private final String name;
    private final String description;
    private final Team team;
    private final List<Submission> submissions;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private HackathonProjectStatus status;

    // 私有构造函数，强制使用Builder
    private HackathonProject(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.name = builder.name;
        this.description = builder.description;
        this.team = builder.team;
        this.submissions = new ArrayList<>(builder.submissions);
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = builder.status != null ? builder.status : HackathonProjectStatus.CREATED;
    }

    /**
     * 添加新的提交记录
     */
    public void addSubmission(Submission submission) {
        if (submission == null) {
            throw new IllegalArgumentException("提交记录不能为空");
        }

        if (this.status == HackathonProjectStatus.CLOSED) {
            throw new IllegalStateException("项目已关闭，无法添加提交");
        }

        this.submissions.add(submission);
        this.updatedAt = LocalDateTime.now();
        this.status = HackathonProjectStatus.SUBMITTED;
    }

    /**
     * 获取最新的提交记录
     */
    public Submission getLatestSubmission() {
        if (submissions.isEmpty()) {
            return null;
        }
        return submissions.get(submissions.size() - 1);
    }

    /**
     * 获取最高分数的提交记录
     */
    public Submission getBestSubmission() {
        return submissions.stream()
            .filter(s -> s.getScore() != null)
            .max((s1, s2) -> Integer.compare(
                s1.getScore().calculateTotalScore(),
                s2.getScore().calculateTotalScore()
            ))
            .orElse(null);
    }

    /**
     * 检查项目是否有效（是否有提交记录）
     */
    public boolean isValid() {
        return !submissions.isEmpty() && team != null && team.isValid();
    }

    /**
     * 获取项目的最高分数
     */
    public Integer getBestScore() {
        Submission best = getBestSubmission();
        return best != null && best.getScore() != null
            ? best.getScore().calculateTotalScore()
            : null;
    }

    /**
     * 标记项目为已评审
     */
    public void markAsReviewed() {
        if (this.status == HackathonProjectStatus.SUBMITTED) {
            this.status = HackathonProjectStatus.REVIEWED;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 关闭项目
     */
    public void close() {
        this.status = HackathonProjectStatus.CLOSED;
        this.updatedAt = LocalDateTime.now();
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

    public Team getTeam() {
        return team;
    }

    public List<Submission> getSubmissions() {
        return new ArrayList<>(submissions);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public HackathonProjectStatus getStatus() {
        return status;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String name;
        private String description;
        private Team team;
        private List<Submission> submissions = new ArrayList<>();
        private LocalDateTime createdAt;
        private HackathonProjectStatus status;

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

        public Builder team(Team team) {
            this.team = team;
            return this;
        }

        public Builder submissions(List<Submission> submissions) {
            this.submissions = new ArrayList<>(submissions);
            return this;
        }

        public Builder addSubmission(Submission submission) {
            this.submissions.add(submission);
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder status(HackathonProjectStatus status) {
            this.status = status;
            return this;
        }

        public HackathonProject build() {
            // 验证必填字段
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("项目名称不能为空");
            }
            if (team == null) {
                throw new IllegalArgumentException("团队信息不能为空");
            }

            return new HackathonProject(this);
        }
    }

    @Override
    public String toString() {
        return "HackathonProject{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", team=" + team.getName() +
                ", submissions=" + submissions.size() +
                ", status=" + status +
                ", bestScore=" + getBestScore() +
                '}';
    }
}

