package top.yumbo.ai.application.hackathon.core;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.api.model.AIConfig;
import top.yumbo.ai.api.model.ProcessResult;
import top.yumbo.ai.api.model.ProcessorConfig;
import top.yumbo.ai.application.hackathon.util.ScoreExtractor;
import top.yumbo.ai.application.hackathon.util.ZipUtil;
import top.yumbo.ai.core.context.ExecutionContext;
import top.yumbo.ai.starter.config.AIReviewerProperties;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Enhanced AI Engine for batch processing multiple projects from ZIP files
 */
@Slf4j
public class HackathonAIEngineV2 {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final HackathonAIEngine baseEngine;
    private final AIReviewerProperties properties;
    private final int batchThreadPoolSize;
    private final Path tempExtractDir;

    public HackathonAIEngineV2(HackathonAIEngine baseEngine, AIReviewerProperties properties) {
        this.baseEngine = baseEngine;
        this.properties = properties;
        this.batchThreadPoolSize = getBatchThreadPoolSize(properties);
        this.tempExtractDir = getTempExtractDir(properties);
    }

    private int getBatchThreadPoolSize(AIReviewerProperties properties) {
        if (properties.getBatch() != null && properties.getBatch().getThreadPoolSize() != null) {
            return properties.getBatch().getThreadPoolSize();
        }
        return 4; // default
    }

    private Path getTempExtractDir(AIReviewerProperties properties) {
        if (properties.getBatch() != null && properties.getBatch().getTempExtractDir() != null) {
            return Paths.get(properties.getBatch().getTempExtractDir());
        }
        return Paths.get("./temp/extracted-projects");
    }

    /**
     * Review a single project (original behavior)
     */
    public ProcessResult reviewSingleProject(String targetPath) {
        log.info("Reviewing single project: {}", targetPath);

        AIConfig aiConfig = properties.getAi();
        ProcessorConfig processorConfig = ProcessorConfig.builder()
                .processorType(properties.getProcessor().getType())
                .outputFormat(properties.getProcessor().getOutputFormat())
                .outputPath(Paths.get(properties.getProcessor().getOutputPath(),
                    new File(targetPath).getName() + "-review-report.md"))
                .build();

        ExecutionContext context = ExecutionContext.builder()
                .targetDirectory(Paths.get(targetPath))
                .includePatterns(properties.getScanner().getIncludePatterns())
                .excludePatterns(properties.getScanner().getExcludePatterns())
                .aiConfig(aiConfig)
                .processorConfig(processorConfig)
                .threadPoolSize(properties.getExecutor().getThreadPoolSize())
                .build();

        return baseEngine.execute(context);
    }

    /**
     * Review all projects from ZIP files in a directory
     */
    public BatchResult reviewAllProjects(String zipDirectory) {
        log.info("Starting batch review for all projects in: {}", zipDirectory);
        long startTime = System.currentTimeMillis();

        BatchResult batchResult = new BatchResult();
        batchResult.setStartTime(LocalDateTime.now());

        try {
            // Find all ZIP files
            Path zipDir = Paths.get(zipDirectory);
            if (!Files.exists(zipDir) || !Files.isDirectory(zipDir)) {
                String error = "Directory does not exist or is not a directory: " + zipDirectory;
                log.error(error);
                batchResult.setSuccess(false);
                batchResult.setErrorMessage(error);
                return batchResult;
            }

            List<Path> zipFiles = Files.list(zipDir)
                    .filter(path -> path.toString().toLowerCase().endsWith(".zip"))
                    .collect(Collectors.toList());

            log.info("Found {} ZIP files to process", zipFiles.size());
            batchResult.setTotalProjects(zipFiles.size());

            if (zipFiles.isEmpty()) {
                batchResult.setSuccess(true);
                batchResult.setEndTime(LocalDateTime.now());
                return batchResult;
            }

            // Create temp extraction directory
            Files.createDirectories(tempExtractDir);

            // Get list of already reviewed projects to skip
            Set<String> completedProjects = getCompletedProjects();
            log.info("Found {} already completed projects, will skip them", completedProjects.size());

            // Create tasks for projects that haven't been reviewed yet
            List<ProjectReviewTask> tasks = new ArrayList<>();
            for (Path zipFile : zipFiles) {
                String projectName = ZipUtil.getProjectNameFromZip(zipFile);
                if (completedProjects.contains(projectName)) {
                    log.info("Skipping already reviewed project: {}", projectName);
                    batchResult.incrementSkipped();
                    continue;
                }
                tasks.add(new ProjectReviewTask(projectName, zipFile));
            }

            log.info("Will process {} new projects with {} threads", tasks.size(), batchThreadPoolSize);

            // Process projects in parallel using thread pool
            ExecutorService executorService = Executors.newFixedThreadPool(batchThreadPoolSize);
            List<Future<ProjectReviewResult>> futures = new ArrayList<>();

            for (ProjectReviewTask task : tasks) {
                Future<ProjectReviewResult> future = executorService.submit(() -> processProject(task));
                futures.add(future);
            }

            // Wait for all tasks to complete and collect results
            for (Future<ProjectReviewResult> future : futures) {
                try {
                    ProjectReviewResult result = future.get();
                    batchResult.addProjectResult(result);

                    if (result.isSuccess()) {
                        log.info("Successfully reviewed project: {} (Score: {})",
                            result.getProjectName(), result.getScore());
                    } else {
                        log.error("Failed to review project: {} - {}",
                            result.getProjectName(), result.getErrorMessage());
                    }
                } catch (ExecutionException e) {
                    log.error("Task execution failed", e);
                    ProjectReviewResult errorResult = new ProjectReviewResult();
                    errorResult.setSuccess(false);
                    errorResult.setErrorMessage("Execution exception: " + e.getMessage());
                    batchResult.addProjectResult(errorResult);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Task interrupted", e);
                    break;
                }
            }

            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }

            batchResult.setSuccess(true);
            batchResult.setEndTime(LocalDateTime.now());

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("Batch review completed: {} successful, {} failed, {} skipped in {} ms",
                batchResult.getSuccessCount(), batchResult.getFailedCount(),
                batchResult.getSkippedCount(), totalTime);

            // Generate summary report
            generateBatchSummaryReport(batchResult);

        } catch (Exception e) {
            log.error("Batch review failed", e);
            batchResult.setSuccess(false);
            batchResult.setErrorMessage(e.getMessage());
            batchResult.setEndTime(LocalDateTime.now());
        }

        return batchResult;
    }

    /**
     * Process a single project (extract, review, cleanup)
     */
    private ProjectReviewResult processProject(ProjectReviewTask task) {
        ProjectReviewResult result = new ProjectReviewResult();
        result.setProjectName(task.getProjectName());
        result.setStartTime(LocalDateTime.now());

        Path extractedPath = null;

        try {
            // Extract ZIP
            log.info("Extracting project: {}", task.getProjectName());
            extractedPath = ZipUtil.extractZip(task.getZipFilePath(), tempExtractDir);

            // Review project
            log.info("Reviewing project: {}", task.getProjectName());
            AIConfig aiConfig = properties.getAi();

            // Create processor config with custom output path
            ProcessorConfig processorConfig = ProcessorConfig.builder()
                    .processorType(properties.getProcessor().getType())
                    .outputFormat(properties.getProcessor().getOutputFormat())
                    .outputPath(null) // Will be set after getting score
                    .build();

            ExecutionContext context = ExecutionContext.builder()
                    .targetDirectory(extractedPath)
                    .includePatterns(properties.getScanner().getIncludePatterns())
                    .excludePatterns(properties.getScanner().getExcludePatterns())
                    .aiConfig(aiConfig)
                    .processorConfig(processorConfig)
                    .threadPoolSize(properties.getExecutor().getThreadPoolSize())
                    .build();

            ProcessResult processResult = baseEngine.execute(context);

            if (processResult.isSuccess()) {
                // Extract score from content
                Double score = ScoreExtractor.extractScore(processResult.getContent());
                result.setScore(score);

                // Generate report filename with score
                String reportFileName = task.getProjectName() + "_" +
                    ScoreExtractor.formatScoreForFilename(score) + "_review-report.md";
                Path reportPath = Paths.get(properties.getProcessor().getOutputPath(), reportFileName);

                // Write report with score in filename
                Files.createDirectories(reportPath.getParent());
                Files.writeString(reportPath, processResult.getContent());

                result.setReportPath(reportPath);
                result.setSuccess(true);

                log.info("Project {} reviewed successfully with score: {}", task.getProjectName(), score);
            } else {
                result.setSuccess(false);
                result.setErrorMessage(processResult.getErrorMessage());
                log.error("Project {} review failed: {}", task.getProjectName(), processResult.getErrorMessage());
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            log.error("Failed to process project: {}", task.getProjectName(), e);
        } finally {
            // Cleanup extracted directory
            if (extractedPath != null) {
                try {
                    ZipUtil.cleanupExtractedDir(extractedPath);
                } catch (Exception e) {
                    log.warn("Failed to cleanup extracted directory for {}", task.getProjectName(), e);
                }
            }
        }

        result.setEndTime(LocalDateTime.now());
        return result;
    }

    /**
     * Get list of already completed projects by checking existing reports
     */
    private Set<String> getCompletedProjects() {
        Set<String> completed = new HashSet<>();

        try {
            Path reportDir = Paths.get(properties.getProcessor().getOutputPath());
            if (!Files.exists(reportDir)) {
                return completed;
            }

            Files.list(reportDir)
                .filter(path -> path.toString().endsWith("-review-report.md"))
                .forEach(path -> {
                    String fileName = path.getFileName().toString();
                    // Extract project name from filename like "projectName_85_5_review-report.md"
                    int lastUnderscore = fileName.lastIndexOf("_review-report.md");
                    if (lastUnderscore > 0) {
                        String fullName = fileName.substring(0, lastUnderscore);
                        // Find the last score pattern (X_Y_review-report.md)
                        int scoreStart = fullName.lastIndexOf("_");
                        if (scoreStart > 0) {
                            int prevUnderscore = fullName.lastIndexOf("_", scoreStart - 1);
                            if (prevUnderscore > 0) {
                                String projectName = fullName.substring(0, prevUnderscore);
                                completed.add(projectName);
                            }
                        }
                    }
                });
        } catch (IOException e) {
            log.warn("Failed to scan for completed projects", e);
        }

        return completed;
    }

    /**
     * Generate a summary report for the batch review
     */
    private void generateBatchSummaryReport(BatchResult batchResult) {
        try {
            Path reportDir = Paths.get(properties.getProcessor().getOutputPath());
            Files.createDirectories(reportDir);

            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
            Path summaryPath = reportDir.resolve("batch-summary-" + timestamp + ".md");

            StringBuilder summary = new StringBuilder();
            summary.append("# Batch Review Summary\n\n");
            summary.append("**Generated at:** ").append(LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
            summary.append("## Overall Statistics\n\n");
            summary.append("- **Total Projects:** ").append(batchResult.getTotalProjects()).append("\n");
            summary.append("- **Successful:** ").append(batchResult.getSuccessCount()).append("\n");
            summary.append("- **Failed:** ").append(batchResult.getFailedCount()).append("\n");
            summary.append("- **Skipped:** ").append(batchResult.getSkippedCount()).append("\n\n");

            summary.append("## Project Results\n\n");
            summary.append("| Project Name | Score | Status | Report |\n");
            summary.append("|--------------|-------|--------|--------|\n");

            for (ProjectReviewResult result : batchResult.getProjectResults()) {
                summary.append("| ").append(result.getProjectName()).append(" | ");
                summary.append(result.getScore() != null ? String.format("%.1f", result.getScore()) : "N/A").append(" | ");
                summary.append(result.isSuccess() ? "✅ Success" : "❌ Failed").append(" | ");
                if (result.getReportPath() != null) {
                    summary.append("[Report](").append(result.getReportPath().getFileName()).append(")");
                } else {
                    summary.append("-");
                }
                summary.append(" |\n");
            }

            if (batchResult.getFailedCount() > 0) {
                summary.append("\n## Failed Projects\n\n");
                for (ProjectReviewResult result : batchResult.getProjectResults()) {
                    if (!result.isSuccess()) {
                        summary.append("- **").append(result.getProjectName()).append("**: ");
                        summary.append(result.getErrorMessage()).append("\n");
                    }
                }
            }

            Files.writeString(summaryPath, summary.toString());
            log.info("Batch summary report written to: {}", summaryPath);

        } catch (IOException e) {
            log.error("Failed to generate batch summary report", e);
        }
    }

    /**
     * Inner class for project review result
     */
    @Data
    public static class ProjectReviewResult {
        private String projectName;
        private boolean success;
        private Double score;
        private String errorMessage;
        private Path reportPath;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
    }

    /**
     * Inner class for batch review result
     */
    @Data
    public static class BatchResult {
        private boolean success;
        private String errorMessage;
        private int totalProjects;
        private List<ProjectReviewResult> projectResults = new ArrayList<>();
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private int skippedCount = 0;

        public void addProjectResult(ProjectReviewResult result) {
            projectResults.add(result);
        }

        public int getSuccessCount() {
            return (int) projectResults.stream().filter(ProjectReviewResult::isSuccess).count();
        }

        public int getFailedCount() {
            return (int) projectResults.stream().filter(r -> !r.isSuccess()).count();
        }

        public void incrementSkipped() {
            skippedCount++;
        }
    }

    /**
     * Inner class for project review task
     */
    @Data
    public static class ProjectReviewTask {
        private final String projectName;
        private final Path zipFilePath;

        public ProjectReviewTask(String projectName, Path zipFilePath) {
            this.projectName = projectName;
            this.zipFilePath = zipFilePath;
        }
    }
}

