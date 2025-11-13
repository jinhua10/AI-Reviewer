package top.yumbo.ai.reviewer.adapter.input.api;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.reviewer.adapter.output.ai.AIServiceConfig;
import top.yumbo.ai.reviewer.adapter.output.ai.DeepSeekAIAdapter;
import top.yumbo.ai.reviewer.adapter.output.cache.FileCacheAdapter;
import top.yumbo.ai.reviewer.adapter.output.filesystem.LocalFileSystemAdapter;
import top.yumbo.ai.reviewer.application.port.input.ProjectAnalysisUseCase;
import top.yumbo.ai.reviewer.application.port.input.ReportGenerationUseCase;
import top.yumbo.ai.reviewer.application.service.ProjectAnalysisService;
import top.yumbo.ai.reviewer.application.service.ReportGenerationService;
import top.yumbo.ai.reviewer.domain.model.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * API适配器（RESTful API接口）
 * 提供编程式API，供其他系统集成使用
 */
@Slf4j
public class APIAdapter {

    private final ProjectAnalysisUseCase analysisUseCase;
    private final ReportGenerationUseCase reportUseCase;

    public APIAdapter(
            ProjectAnalysisUseCase analysisUseCase,
            ReportGenerationUseCase reportUseCase) {
        this.analysisUseCase = analysisUseCase;
        this.reportUseCase = reportUseCase;
    }

    /**
     * 默认构造函数，使用标准适配器
     */
    public APIAdapter() {
        DeepSeekAIAdapter aiAdapter = createDefaultAIAdapter();
        FileCacheAdapter cacheAdapter = new FileCacheAdapter();
        LocalFileSystemAdapter fileSystemAdapter = createDefaultFileSystemAdapter();

        this.analysisUseCase = new ProjectAnalysisService(
                aiAdapter, cacheAdapter, fileSystemAdapter);
        this.reportUseCase = new ReportGenerationService(fileSystemAdapter);
    }

    /**
     * 分析项目（同步）
     */
    public AnalysisResponse analyzeProject(AnalysisRequest request) {
        log.info("API: 分析项目 - {}", request.projectPath());

        try {
            // 构建项目对象
            Project project = buildProjectFromRequest(request);

            // 执行分析
            AnalysisTask task = analysisUseCase.analyzeProject(project);

            // 获取报告
            ReviewReport report = analysisUseCase.getAnalysisResult(task.getTaskId());

            return new AnalysisResponse(
                    true,
                    task.getTaskId(),
                    report.getOverallScore(),
                    report.getGrade(),
                    report.getDimensionScores(),
                    task.getDurationMillis(),
                    null
            );

        } catch (Exception e) {
            log.error("API: 分析失败", e);
            return new AnalysisResponse(
                    false,
                    null,
                    0,
                    "F",
                    null,
                    0,
                    e.getMessage()
            );
        }
    }

    /**
     * 分析项目（异步）
     */
    public AsyncAnalysisResponse analyzeProjectAsync(AnalysisRequest request) {
        log.info("API: 异步分析项目 - {}", request.projectPath());

        try {
            Project project = buildProjectFromRequest(request);
            String taskId = analysisUseCase.analyzeProjectAsync(project);

            return new AsyncAnalysisResponse(true, taskId, "任务已启动", null);

        } catch (Exception e) {
            log.error("API: 异步分析启动失败", e);
            return new AsyncAnalysisResponse(false, null, null, e.getMessage());
        }
    }

    /**
     * 获取任务状态
     */
    public TaskStatusResponse getTaskStatus(String taskId) {
        log.info("API: 获取任务状态 - {}", taskId);

        try {
            AnalysisTask task = analysisUseCase.getTaskStatus(taskId);

            return new TaskStatusResponse(
                    true,
                    taskId,
                    task.getStatus().name(),
                    task.getProgress() != null ? task.getProgress().getPercentage() : 0.0,
                    task.getProgress() != null ? task.getProgress().getCurrentPhase() : "",
                    task.isCompleted(),
                    null
            );

        } catch (Exception e) {
            log.error("API: 获取任务状态失败", e);
            return new TaskStatusResponse(
                    false, taskId, "UNKNOWN", 0.0, "", false, e.getMessage()
            );
        }
    }

    /**
     * 获取分析结果
     */
    public ReportResponse getAnalysisResult(String taskId, String format) {
        log.info("API: 获取分析结果 - taskId={}, format={}", taskId, format);

        try {
            ReviewReport report = analysisUseCase.getAnalysisResult(taskId);

            String content = switch (format.toLowerCase()) {
                case "html" -> reportUseCase.generateHtmlReport(report);
                case "json" -> reportUseCase.generateJsonReport(report);
                default -> reportUseCase.generateMarkdownReport(report);
            };

            return new ReportResponse(
                    true,
                    report.getReportId(),
                    format,
                    content,
                    report.getOverallScore(),
                    null
            );

        } catch (Exception e) {
            log.error("API: 获取分析结果失败", e);
            return new ReportResponse(false, null, format, null, 0, e.getMessage());
        }
    }

    /**
     * 取消任务
     */
    public CancelResponse cancelTask(String taskId) {
        log.info("API: 取消任务 - {}", taskId);

        try {
            analysisUseCase.cancelTask(taskId);
            return new CancelResponse(true, taskId, "任务已取消", null);
        } catch (Exception e) {
            log.error("API: 取消任务失败", e);
            return new CancelResponse(false, taskId, null, e.getMessage());
        }
    }

    /**
     * 构建项目对象
     */
    private Project buildProjectFromRequest(AnalysisRequest request) {
        LocalFileSystemAdapter fsAdapter = createDefaultFileSystemAdapter();
        Path projectRoot = Paths.get(request.projectPath());

        List<SourceFile> sourceFiles = fsAdapter.scanProjectFiles(projectRoot);
        String structureTree = fsAdapter.generateProjectStructure(projectRoot);

        return Project.builder()
                .name(projectRoot.getFileName().toString())
                .rootPath(projectRoot)
                .type(ProjectType.JAVA) // 简化实现
                .sourceFiles(sourceFiles)
                .structureTree(structureTree)
                .build();
    }

    private DeepSeekAIAdapter createDefaultAIAdapter() {
        log.info("将使用Deepseek作为默认AI服务");
        String apiKey = System.getenv("AI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("AI_API_KEY环境变量未设置，无法使用Deepseek AI服务");
        }
        return new DeepSeekAIAdapter(new AIServiceConfig(
                apiKey, null, null, 4000, 0.3, 3, 3, 1000, 30000, 60000, null
        ));
    }

    private LocalFileSystemAdapter createDefaultFileSystemAdapter() {
        return new LocalFileSystemAdapter(
                new LocalFileSystemAdapter.FileSystemConfig(
                        List.of("*.java", "*.py", "*.js", "*.ts"),
                        List.of("*test*", "*.class", "*.jar"),
                        1024, 4
                )
        );
    }

    // ==================== API请求/响应对象 ====================

    public record AnalysisRequest(String projectPath, String configuration) {
    }

    public record AnalysisResponse(
            boolean success,
            String taskId,
            int overallScore,
            String grade,
            java.util.Map<String, Integer> dimensionScores,
            long durationMillis,
            String error
    ) {
    }

    public record AsyncAnalysisResponse(
            boolean success,
            String taskId,
            String message,
            String error
    ) {
    }

    public record TaskStatusResponse(
            boolean success,
            String taskId,
            String status,
            double progress,
            String currentPhase,
            boolean completed,
            String error
    ) {
    }

    public record ReportResponse(
            boolean success,
            String reportId,
            String format,
            String content,
            int overallScore,
            String error
    ) {
    }

    public record CancelResponse(
            boolean success,
            String taskId,
            String message,
            String error
    ) {
    }
}

