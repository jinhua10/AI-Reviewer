package top.yumbo.ai.reviewer.application.hackathon.service;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.yumbo.ai.reviewer.application.port.output.CloneRequest;
import top.yumbo.ai.reviewer.application.port.output.RepositoryPort;
import top.yumbo.ai.reviewer.domain.hackathon.model.HackathonProject;
import top.yumbo.ai.reviewer.domain.hackathon.model.HackathonScore;
import top.yumbo.ai.reviewer.adapter.storage.local.LocalFileSystemAdapter;
import top.yumbo.ai.reviewer.application.service.ProjectAnalysisService;
import top.yumbo.ai.reviewer.domain.hackathon.model.Participant;
import top.yumbo.ai.reviewer.domain.hackathon.model.Submission;
import top.yumbo.ai.reviewer.domain.model.Project;
import top.yumbo.ai.reviewer.domain.model.ProjectType;
import top.yumbo.ai.reviewer.domain.model.ReviewReport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * 黑客松集成服务
 * <p>
 * 整合完整的工作流程：GitHub/Gitee URL → 克隆 → 扫描 → 分析 → 评分 → 排行榜
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-12
 */
public class HackathonIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(HackathonIntegrationService.class);

    private final TeamManagementService teamManagement;
    private final RepositoryPort repositoryPort;
    private final LocalFileSystemAdapter fileSystemAdapter;
    private final ProjectAnalysisService coreAnalysisService;
    private final HackathonScoringService scoringService;
    private final LeaderboardService leaderboardService;

    /**
     * 构造函数
     */
    public HackathonIntegrationService(
            TeamManagementService teamManagement,
            RepositoryPort repositoryPort,
            LocalFileSystemAdapter fileSystemAdapter,
            ProjectAnalysisService coreAnalysisService,
            HackathonScoringService scoringService,
            LeaderboardService leaderboardService) {
        this.teamManagement = teamManagement;
        this.repositoryPort = repositoryPort;
        this.fileSystemAdapter = fileSystemAdapter;
        this.coreAnalysisService = coreAnalysisService;
        this.scoringService = scoringService;
        this.leaderboardService = leaderboardService;
    }

    /**
     * 提交并分析项目（同步）
     *
     * 完整流程：提交 URL → 克隆 → 扫描 → 分析 → 评分 → 排行榜
     *
     * @param projectId 项目ID
     * @param githubUrl GitHub 仓库 URL
     * @param branch 分支名称
     * @param submitter 提交者
     * @return 完成评审的项目
     * @throws HackathonIntegrationException 如果流程失败
     */
    public HackathonProject submitAndAnalyze(
            String projectId,
            String githubUrl,
            String branch,
            Participant submitter) throws HackathonIntegrationException {

        log.info("开始黑客松项目提交和分析流程: projectId={}, url={}", projectId, githubUrl);

        Path localPath = null;

        try {
            // 1. 提交代码
            log.info("步骤 1/7: 提交 GitHub URL");
            HackathonProject project = teamManagement.submitCode(
                projectId, githubUrl, branch, submitter
            );

            Submission submission = project.getLatestSubmission();
            submission.startReview();

            // 2. 验证仓库 URL
            log.info("步骤 2/7: 验证仓库 URL 可访问性");
            if (!repositoryPort.isAccessible(githubUrl)) {
                throw new HackathonIntegrationException("仓库不可访问: " + githubUrl);
            }

            // 3. 克隆仓库
            log.info("步骤 3/7: 克隆仓库");
            localPath = repositoryPort.cloneRepository(
                CloneRequest.builder()
                    .url(githubUrl)
                    .branch(branch)
                    .timeoutSeconds(300)
                    .build()
            );
            log.info("仓库已克隆到: {}", localPath);

            // 4. 扫描项目文件
            log.info("步骤 4/7: 扫描项目文件");
            java.util.List<top.yumbo.ai.reviewer.domain.model.SourceFile> sourceFiles =
                fileSystemAdapter.scanProjectFiles(localPath);
            log.info("扫描到 {} 个文件", sourceFiles.size());

            // 构建 Project 对象
            Project coreProject = Project.builder()
                .name(project.getName())
                .rootPath(localPath)
                .type(ProjectType.JAVA) // 默认类型，可根据实际情况推断
                .sourceFiles(sourceFiles)
                .build();

            // 5. 分析项目（调用核心服务）
            log.info("步骤 5/7: 分析项目代码");
            top.yumbo.ai.reviewer.domain.model.AnalysisTask analysisTask =
                coreAnalysisService.analyzeProject(coreProject);

            // 创建评审报告（简化实现）
            ReviewReport reviewReport = ReviewReport.builder()
                .reportId(analysisTask.getTaskId())
                .projectName(project.getName())
                .overallScore(75) // 默认分数，实际应从分析结果获取
                .build();

            // 6. 计算黑客松评分
            log.info("步骤 6/7: 计算黑客松评分");
            HackathonScore hackathonScore = scoringService.calculateScore(
                reviewReport, coreProject
            );
            log.info("评分完成: {}", hackathonScore);

            // 7. 完成评审
            submission.completeReview(reviewReport, hackathonScore);
            project.markAsReviewed();

            // 8. 更新排行榜
            log.info("步骤 7/7: 更新排行榜");
            leaderboardService.updateLeaderboard(
                teamManagement.getAllProjects()
            );

            log.info("黑客松项目分析完成: projectId={}, score={}",
                    projectId, hackathonScore.calculateTotalScore());

            return project;


        } catch (Exception e) {
            log.error("项目分析失败: projectId={}", projectId, e);
            failSubmission(projectId, "分析失败: " + e.getMessage());
            throw new HackathonIntegrationException("项目分析失败", e);

        } finally {
            // 清理临时文件
            if (localPath != null) {
                cleanupLocalPath(localPath);
            }
        }
    }

    /**
     * 提交并分析项目（异步）
     *
     * @param projectId 项目ID
     * @param githubUrl GitHub 仓库 URL
     * @param branch 分支名称
     * @param submitter 提交者
     * @return 异步结果
     */
    public CompletableFuture<HackathonProject> submitAndAnalyzeAsync(
            String projectId,
            String githubUrl,
            String branch,
            Participant submitter) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                return submitAndAnalyze(projectId, githubUrl, branch, submitter);
            } catch (HackathonIntegrationException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 获取仓库指标
     *
     * @param repositoryUrl 仓库 URL
     * @return 仓库指标
     * @throws HackathonIntegrationException 如果获取失败
     */
    public top.yumbo.ai.reviewer.application.port.output.RepositoryMetrics getRepositoryMetrics(String repositoryUrl)
            throws HackathonIntegrationException {
        try {
            log.info("获取仓库指标: {}", repositoryUrl);
            return repositoryPort.getMetrics(repositoryUrl);
        } catch (Exception e) {
            throw new HackathonIntegrationException("获取仓库指标失败", e);
        }
    }

    /**
     * 验证仓库 URL
     *
     * @param repositoryUrl 仓库 URL
     * @return 验证结果
     */
    public ValidationResult validateRepositoryUrl(String repositoryUrl) {
        ValidationResult result = new ValidationResult();

        // 1. 格式验证
        if (!isValidRepositoryUrlFormat(repositoryUrl)) {
            result.addError("URL 格式无效");
            return result;
        }

        // 2. 可访问性验证
        if (!repositoryPort.isAccessible(repositoryUrl)) {
            result.addError("仓库不可访问或不存在");
            return result;
        }

        // 3. 必需文件检查
        try {
            boolean hasReadme = repositoryPort.hasFile(repositoryUrl, "README.md") ||
                              repositoryPort.hasFile(repositoryUrl, "readme.md");
            if (!hasReadme) {
                result.addWarning("缺少 README.md 文件");
            }
        } catch (Exception e) {
            log.warn("文件检查失败: {}", e.getMessage());
        }

        result.setValid(result.getErrors().isEmpty());
        return result;
    }

    /**
     * 获取项目分析进度
     *
     * @param projectId 项目ID
     * @return 进度信息
     */
    public AnalysisProgressInfo getAnalysisProgress(String projectId) {
        HackathonProject project = teamManagement.getProjectById(projectId);
        if (project == null) {
            return null;
        }

        Submission latestSubmission = project.getLatestSubmission();
        if (latestSubmission == null) {
            return new AnalysisProgressInfo(projectId, 0, "未提交", null);
        }

        int progress = 0;
        String phase = "";

        switch (latestSubmission.getStatus()) {
            case PENDING -> {
                phase = "等待分析";
            }
            case REVIEWING -> {
                progress = 50;
                phase = "分析中";
            }
            case COMPLETED -> {
                progress = 100;
                phase = "已完成";
            }
            case FAILED -> {
                progress = -1;
                phase = "失败";
            }
        }

        return new AnalysisProgressInfo(
            projectId,
            progress,
            phase,
            latestSubmission.getErrorMessage()
        );
    }

    /**
     * 标记提交失败
     */
    private void failSubmission(String projectId, String errorMessage) {
        try {
            HackathonProject project = teamManagement.getProjectById(projectId);
            if (project != null) {
                Submission submission = project.getLatestSubmission();
                if (submission != null) {
                    submission.fail(errorMessage);
                }
            }
        } catch (Exception e) {
            log.error("标记提交失败时出错", e);
        }
    }

    /**
     * 清理本地路径
     */
    private void cleanupLocalPath(Path localPath) {
        try {
            if (Files.exists(localPath)) {
                Files.walk(localPath)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (Exception e) {
                            // ignore
                        }
                    });
                log.info("已清理临时目录: {}", localPath);
            }
        } catch (Exception e) {
            log.warn("清理临时目录失败: {}", localPath, e);
        }
    }

    /**
     * 验证仓库 URL 格式（支持 GitHub 和 Gitee）
     */
    private boolean isValidRepositoryUrlFormat(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        // 支持 GitHub 和 Gitee
        return url.matches("^https?://(github\\.com|gitee\\.com)/[\\w-]+/[\\w.-]+.*$");
    }

    /**
     * 黑客松集成异常
     */
    public static class HackathonIntegrationException extends Exception {
        public HackathonIntegrationException(String message) {
            super(message);
        }

        public HackathonIntegrationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 验证结果
     */
    @Getter
    public static class ValidationResult {
        @Setter
        private boolean valid = true;
        private final java.util.List<String> errors = new java.util.ArrayList<>();
        private final java.util.List<String> warnings = new java.util.ArrayList<>();

        public void addError(String error) {
            errors.add(error);
            valid = false;
        }

        public void addWarning(String warning) {
            warnings.add(warning);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("ValidationResult{valid=").append(valid);
            if (!errors.isEmpty()) {
                sb.append(", errors=").append(errors);
            }
            if (!warnings.isEmpty()) {
                sb.append(", warnings=").append(warnings);
            }
            sb.append("}");
            return sb.toString();
        }
    }

    /**
     * 分析进度信息
     */
    @Getter
    public static class AnalysisProgressInfo {
        private final String projectId;
        private final int progress;  // 0-100, -1 表示失败
        private final String phase;
        private final String errorMessage;

        public AnalysisProgressInfo(String projectId, int progress, String phase, String errorMessage) {
            this.projectId = projectId;
            this.progress = progress;
            this.phase = phase;
            this.errorMessage = errorMessage;
        }

        @Override
        public String toString() {
            return String.format("AnalysisProgressInfo{projectId='%s', progress=%d%%, phase='%s'}",
                               projectId, progress, phase);
        }
    }
}


