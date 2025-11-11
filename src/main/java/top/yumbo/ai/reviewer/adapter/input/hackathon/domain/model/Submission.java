package top.yumbo.ai.reviewer.adapter.input.hackathon.domain.model;

import top.yumbo.ai.reviewer.domain.model.ReviewReport;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 提交记录领域模型
 *
 * 表示一次代码提交，包含GitHub仓库地址和评审结果
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-12
 */
public class Submission {

    private final String id;
    private final String githubUrl;
    private final String gitBranch;
    private final String commitHash;
    private final LocalDateTime submittedAt;
    private final Participant submitter;

    // 评审结果
    private ReviewReport reviewReport;
    private HackathonScore score;
    private SubmissionStatus status;
    private LocalDateTime reviewedAt;
    private String errorMessage;

    // 私有构造函数，强制使用Builder
    private Submission(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.githubUrl = builder.githubUrl;
        this.gitBranch = builder.gitBranch != null ? builder.gitBranch : "main";
        this.commitHash = builder.commitHash;
        this.submittedAt = builder.submittedAt != null ? builder.submittedAt : LocalDateTime.now();
        this.submitter = builder.submitter;
        this.reviewReport = builder.reviewReport;
        this.score = builder.score;
        this.status = builder.status != null ? builder.status : SubmissionStatus.PENDING;
        this.reviewedAt = builder.reviewedAt;
        this.errorMessage = builder.errorMessage;
    }

    /**
     * 开始评审
     */
    public void startReview() {
        if (this.status != SubmissionStatus.PENDING) {
            throw new IllegalStateException("只有待评审的提交才能开始评审");
        }
        this.status = SubmissionStatus.REVIEWING;
    }

    /**
     * 完成评审
     */
    public void completeReview(ReviewReport reviewReport, HackathonScore score) {
        if (this.status != SubmissionStatus.REVIEWING) {
            throw new IllegalStateException("只有评审中的提交才能完成评审");
        }
        if (reviewReport == null || score == null) {
            throw new IllegalArgumentException("评审报告和分数不能为空");
        }

        this.reviewReport = reviewReport;
        this.score = score;
        this.status = SubmissionStatus.COMPLETED;
        this.reviewedAt = LocalDateTime.now();
    }

    /**
     * 标记评审失败
     */
    public void fail(String errorMessage) {
        if (this.status == SubmissionStatus.COMPLETED) {
            throw new IllegalStateException("已完成的评审不能标记为失败");
        }

        this.status = SubmissionStatus.FAILED;
        this.errorMessage = errorMessage;
        this.reviewedAt = LocalDateTime.now();
    }

    /**
     * 检查提交是否有效
     */
    public boolean isValid() {
        return githubUrl != null && !githubUrl.trim().isEmpty()
            && isValidGitHubUrl(githubUrl)
            && submitter != null && submitter.isValid();
    }

    /**
     * 验证GitHub URL格式
     */
    private boolean isValidGitHubUrl(String url) {
        return url != null && url.matches("^https?://github\\.com/[\\w-]+/[\\w.-]+.*$");
    }

    /**
     * 检查评审是否完成
     */
    public boolean isReviewed() {
        return status == SubmissionStatus.COMPLETED;
    }

    /**
     * 检查评审是否失败
     */
    public boolean isFailed() {
        return status == SubmissionStatus.FAILED;
    }

    /**
     * 获取总分
     */
    public Integer getTotalScore() {
        return score != null ? score.calculateTotalScore() : null;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public String getGitBranch() {
        return gitBranch;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public Participant getSubmitter() {
        return submitter;
    }

    public ReviewReport getReviewReport() {
        return reviewReport;
    }

    public HackathonScore getScore() {
        return score;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String githubUrl;
        private String gitBranch;
        private String commitHash;
        private LocalDateTime submittedAt;
        private Participant submitter;
        private ReviewReport reviewReport;
        private HackathonScore score;
        private SubmissionStatus status;
        private LocalDateTime reviewedAt;
        private String errorMessage;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder githubUrl(String githubUrl) {
            this.githubUrl = githubUrl;
            return this;
        }

        public Builder gitBranch(String gitBranch) {
            this.gitBranch = gitBranch;
            return this;
        }

        public Builder commitHash(String commitHash) {
            this.commitHash = commitHash;
            return this;
        }

        public Builder submittedAt(LocalDateTime submittedAt) {
            this.submittedAt = submittedAt;
            return this;
        }

        public Builder submitter(Participant submitter) {
            this.submitter = submitter;
            return this;
        }

        public Builder reviewReport(ReviewReport reviewReport) {
            this.reviewReport = reviewReport;
            return this;
        }

        public Builder score(HackathonScore score) {
            this.score = score;
            return this;
        }

        public Builder status(SubmissionStatus status) {
            this.status = status;
            return this;
        }

        public Builder reviewedAt(LocalDateTime reviewedAt) {
            this.reviewedAt = reviewedAt;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Submission build() {
            // 验证必填字段
            if (githubUrl == null || githubUrl.trim().isEmpty()) {
                throw new IllegalArgumentException("GitHub URL 不能为空");
            }
            if (submitter == null) {
                throw new IllegalArgumentException("提交者信息不能为空");
            }

            return new Submission(this);
        }
    }

    @Override
    public String toString() {
        return "Submission{" +
                "id='" + id + '\'' +
                ", githubUrl='" + githubUrl + '\'' +
                ", branch='" + gitBranch + '\'' +
                ", status=" + status +
                ", totalScore=" + getTotalScore() +
                ", submittedAt=" + submittedAt +
                '}';
    }
}

