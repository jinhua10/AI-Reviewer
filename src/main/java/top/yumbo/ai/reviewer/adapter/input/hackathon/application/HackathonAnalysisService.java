package top.yumbo.ai.reviewer.adapter.input.hackathon.application;

import top.yumbo.ai.reviewer.adapter.input.hackathon.domain.model.*;
import top.yumbo.ai.reviewer.application.service.ProjectAnalysisService;
import top.yumbo.ai.reviewer.domain.model.AnalysisTask;
import top.yumbo.ai.reviewer.domain.model.Project;
import top.yumbo.ai.reviewer.domain.model.ReviewReport;

import java.util.concurrent.CompletableFuture;

/**
 * 黑客松分析服务
 *
 * 负责将GitHub项目转换为代码分析任务，并生成黑客松专属评分
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-12
 */
public class HackathonAnalysisService {

    private final ProjectAnalysisService coreAnalysisService;
    private final HackathonScoringService scoringService;

    /**
     * 构造函数
     *
     * @param coreAnalysisService 核心分析服务
     * @param scoringService 黑客松评分服务
     */
    public HackathonAnalysisService(
            ProjectAnalysisService coreAnalysisService,
            HackathonScoringService scoringService) {
        this.coreAnalysisService = coreAnalysisService;
        this.scoringService = scoringService;
    }

    /**
     * 分析黑客松项目（同步）
     *
     * @param hackathonProject 黑客松项目
     * @param coreProject 核心项目对象（已扫描文件）
     * @return 更新后的黑客松项目（包含分析结果）
     * @throws RuntimeException 如果分析失败
     */
    public HackathonProject analyzeProject(HackathonProject hackathonProject, Project coreProject) {
        Submission latestSubmission = hackathonProject.getLatestSubmission();
        if (latestSubmission == null) {
            throw new IllegalStateException("项目没有提交记录");
        }

        try {
            // 1. 标记开始评审
            latestSubmission.startReview();

            // 2. 调用核心分析服务
            AnalysisTask analysisTask = coreAnalysisService.analyzeProject(coreProject);

            // 3. 创建一个基本的评审报告
            // 注意：由于 AnalysisTask 没有直接的 getResult() 方法
            // 我们需要根据任务状态创建评审报告
            ReviewReport reviewReport = createReportFromTask(analysisTask, coreProject);

            // 4. 转换为黑客松评分
            HackathonScore hackathonScore = scoringService.calculateScore(
                reviewReport,
                coreProject
            );

            // 5. 完成评审
            latestSubmission.completeReview(reviewReport, hackathonScore);
            hackathonProject.markAsReviewed();

            return hackathonProject;

        } catch (Exception e) {
            // 标记评审失败
            latestSubmission.fail("分析失败: " + e.getMessage());
            throw new RuntimeException("黑客松项目分析失败", e);
        }
    }

    /**
     * 从分析任务创建评审报告
     */
    private ReviewReport createReportFromTask(AnalysisTask task, Project project) {
        // 创建基本的评审报告
        // 实际环境中应该从 ProjectAnalysisService 的 reports Map 获取
        return ReviewReport.builder()
            .reportId(task.getTaskId())
            .projectName(project.getName())
            .overallScore(75) // 默认分数，实际应从 AI 分析结果获取
            .build();
    }

    /**
     * 异步分析黑客松项目
     *
     * @param hackathonProject 黑客松项目
     * @param coreProject 核心项目对象
     * @return 异步结果
     */
    public CompletableFuture<HackathonProject> analyzeProjectAsync(
            HackathonProject hackathonProject,
            Project coreProject) {

        return CompletableFuture.supplyAsync(() ->
            analyzeProject(hackathonProject, coreProject)
        );
    }

    /**
     * 重新分析项目（用于重新提交的情况）
     *
     * @param hackathonProject 黑客松项目
     * @param coreProject 核心项目对象
     * @return 更新后的项目
     */
    public HackathonProject reanalyzeProject(HackathonProject hackathonProject, Project coreProject) {
        if (hackathonProject.getStatus() == HackathonProjectStatus.CLOSED) {
            throw new IllegalStateException("项目已关闭，无法重新分析");
        }

        return analyzeProject(hackathonProject, coreProject);
    }

    /**
     * 获取分析进度
     *
     * @param hackathonProject 黑客松项目
     * @return 进度百分比 (0-100)
     */
    public int getAnalysisProgress(HackathonProject hackathonProject) {
        Submission latestSubmission = hackathonProject.getLatestSubmission();
        if (latestSubmission == null) {
            return 0;
        }

        return switch (latestSubmission.getStatus()) {
            case PENDING -> 0;
            case REVIEWING -> 50;
            case COMPLETED -> 100;
            case FAILED -> -1;
        };
    }

    /**
     * 检查项目是否可以分析
     *
     * @param hackathonProject 黑客松项目
     * @return true 如果可以分析
     */
    public boolean canAnalyze(HackathonProject hackathonProject) {
        if (hackathonProject == null || !hackathonProject.isValid()) {
            return false;
        }

        if (hackathonProject.getStatus() == HackathonProjectStatus.CLOSED) {
            return false;
        }

        Submission latestSubmission = hackathonProject.getLatestSubmission();
        return latestSubmission != null && latestSubmission.getStatus().canStartReview();
    }
}

