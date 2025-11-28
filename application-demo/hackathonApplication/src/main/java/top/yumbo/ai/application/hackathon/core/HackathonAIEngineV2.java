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
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;
import java.util.Comparator;

/**
 * Enhanced AI Engine for batch processing multiple projects from ZIP files
 * Supports hierarchical directory structure: FolderA -> FolderB (with done.txt) -> ZipC
 *
 * Features:
 * - Batch processing with parallel execution
 * - README.md priority sorting in prompts
 * - Anti-cheat filtering (automatically applied via HackathonAIEngine)
 * - CSV-based progress tracking and resume capability
 * - Smart report naming: FolderBName-Score-ZipFileName.md
 */
@Slf4j
public class HackathonAIEngineV2 {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String CSV_FILENAME = "completed-reviews.csv";
    private static final String CSV_HEADER = "FolderB,ZipFileName,Score,ReportFileName,CompletedTime,OverallComment,RetryCount\n";
    private static final String DONE_MARKER_FILE = "done.txt";
    private static final long DEFAULT_SCAN_INTERVAL_MS = 2 * 60 * 1000; // 2 minutes
    private static final double MIN_VALID_SCORE = 30.0; // Minimum valid score threshold
    private static final int MAX_RETRY_ATTEMPTS = 3; // Maximum retry attempts for low scores

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
     * Execute download script to sync files from remote
     */
    private boolean executeDownloadScript() {
        String scriptPath = properties.getBatch() != null && properties.getBatch().getDownloadScriptPath() != null
                ? properties.getBatch().getDownloadScriptPath()
                : "/home/jinhua/AI-Reviewer/download";

        log.info("Executing download script: {}", scriptPath);

        try {
            // Check if script file exists
            File scriptFile = new File(scriptPath);
            if (!scriptFile.exists()) {
                log.error("Download script not found: {}", scriptPath);
                return false;
            }

            // Use shell to execute the script on Unix-like systems
            ProcessBuilder processBuilder;
            String osName = System.getProperty("os.name").toLowerCase();

            if (osName.contains("win")) {
                // Windows
                processBuilder = new ProcessBuilder("cmd.exe", "/c", scriptPath);
            } else {
                // Unix-like systems (Linux, Mac)
                processBuilder = new ProcessBuilder("/bin/bash", scriptPath);
            }

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Capture and log output
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("Script output: {}", line);
                }
            }

            // Wait for script to complete
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                log.info("Download script completed successfully");
                return true;
            } else {
                log.error("Download script failed with exit code: {}", exitCode);
                return false;
            }
        } catch (IOException e) {
            log.error("Failed to execute download script: {}", e.getMessage(), e);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Download script execution interrupted", e);
            return false;
        }
    }

    /**
     * Get total count of completed reviews from CSV
     */
    private int getCompletedReviewsCount() {
        try {
            Path csvPath = Paths.get(properties.getProcessor().getOutputPath(), CSV_FILENAME);
            if (!Files.exists(csvPath)) {
                return 0;
            }

            List<String> lines = Files.readAllLines(csvPath, StandardCharsets.UTF_8);
            // Subtract 1 for header line
            return Math.max(0, lines.size() - 1);
        } catch (IOException e) {
            log.warn("Failed to count completed reviews", e);
            return 0;
        }
    }

    /**
     * Review all projects with continuous monitoring
     * Executes download script every 2 minutes and rescans for new done.txt files
     * Structure: FolderA (root) -> FolderB (subfolders) -> ZipC (zip files)
     * Only processes FolderB that contains done.txt file
     */
    public void reviewAllProjectsContinuous(String rootDirectory) {
        boolean enableDownloadScript = properties.getBatch() != null 
                && properties.getBatch().getEnableDownloadScript() != null
                && properties.getBatch().getEnableDownloadScript();
        
        log.info("Starting continuous batch review for all projects in: {}", rootDirectory);
        log.info("Download script mode: {}", enableDownloadScript ? "ENABLED" : "DISABLED (Web upload only)");
        log.info("Will rescan every {} minutes",
                properties.getBatch() != null && properties.getBatch().getScanIntervalMinutes() != null
                        ? properties.getBatch().getScanIntervalMinutes() : 2);

        long scanIntervalMs = properties.getBatch() != null && properties.getBatch().getScanIntervalMinutes() != null
                ? properties.getBatch().getScanIntervalMinutes() * 60 * 1000L
                : DEFAULT_SCAN_INTERVAL_MS;

        while (true) {
            try {
                // Execute download script only if enabled
                if (enableDownloadScript) {
                    log.info("Executing download script...");
                    executeDownloadScript();
                    // Wait a bit for file system to sync
                    Thread.sleep(2000);
                } else {
                    log.debug("Download script disabled, skipping...");
                }

                // Review all available projects
                reviewAllProjects(rootDirectory);

                // Print total completed count from CSV
                int totalCompleted = getCompletedReviewsCount();
                log.info("========================================");
                log.info("CSV总记录数 (Total completed reviews): {}", totalCompleted);
                log.info("========================================");

                // Wait for next scan interval
                log.info("Waiting {} minutes before next scan...",
                        scanIntervalMs / 60000);
                Thread.sleep(scanIntervalMs);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("Continuous review interrupted, shutting down...");
                break;
            } catch (Exception e) {
                log.error("Error in continuous review loop", e);
                // Continue even if there's an error
                try {
                    Thread.sleep(scanIntervalMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * Review all projects from ZIP files in a hierarchical directory structure
     * Structure: FolderA (root) -> FolderB (subfolders) -> ZipC (zip files)
     * Only processes FolderB that contains done.txt file
     */
    public BatchResult reviewAllProjects(String rootDirectory) {
        log.info("Starting batch review for all projects in: {}", rootDirectory);
        long startTime = System.currentTimeMillis();

        BatchResult batchResult = new BatchResult();
        batchResult.setStartTime(LocalDateTime.now());

        try {
            // Validate root directory (FolderA)
            Path rootDir = Paths.get(rootDirectory);
            if (!Files.exists(rootDir) || !Files.isDirectory(rootDir)) {
                String error = "Directory does not exist or is not a directory: " + rootDirectory;
                log.error(error);
                batchResult.setSuccess(false);
                batchResult.setErrorMessage(error);
                return batchResult;
            }

            // Find all subfolders (FolderB) that contain done.txt
            List<Path> eligibleFolders = findEligibleFolders(rootDir);
            log.info("Found {} eligible folders (with done.txt) to process", eligibleFolders.size());

            if (eligibleFolders.isEmpty()) {
                log.info("No eligible folders found. Make sure subfolders contain 'done.txt' file.");
                batchResult.setSuccess(true);
                batchResult.setEndTime(LocalDateTime.now());
                return batchResult;
            }

            // Create temp extraction directory
            Files.createDirectories(tempExtractDir);

            // Load completed reviews from CSV
            Map<String, CompletedReview> completedReviews = loadCompletedReviews();
            log.info("Found {} already completed reviews in CSV", completedReviews.size());

            // Create tasks for each eligible folder
            List<ProjectReviewTask> tasks = new ArrayList<>();
            for (Path folderB : eligibleFolders) {
                String folderBName = folderB.getFileName().toString();

                // Find the latest ZIP file in this folder
                Path latestZip = findLatestZipFile(folderB);
                if (latestZip == null) {
                    log.warn("No ZIP files found in folder: {}", folderBName);
                    continue;
                }

                String zipFileName = latestZip.getFileName().toString();
                String taskKey = folderBName + "|" + zipFileName;

                // Check if already completed
                if (completedReviews.containsKey(taskKey)) {
                    log.info("Skipping already reviewed: {} - {}", folderBName, zipFileName);
                    batchResult.incrementSkipped();
                    continue;
                }

                tasks.add(new ProjectReviewTask(folderBName, latestZip));
            }

            batchResult.setTotalProjects(tasks.size() + batchResult.getSkippedCount());
            log.info("Will process {} new projects with {} threads", tasks.size(), batchThreadPoolSize);

            if (tasks.isEmpty()) {
                batchResult.setSuccess(true);
                batchResult.setEndTime(LocalDateTime.now());
                return batchResult;
            }

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
                        log.info("Successfully reviewed: {} - {} (Score: {})",
                            result.getFolderBName(), result.getZipFileName(), result.getScore());

                        // Append to CSV
                        appendToCompletedReviewsCsv(result);
                    } else {
                        log.error("Failed to review: {} - {} - {}",
                            result.getFolderBName(), result.getZipFileName(), result.getErrorMessage());
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
     * Find all subfolders that contain done.txt file
     */
    private List<Path> findEligibleFolders(Path rootDir) throws IOException {
        List<Path> eligibleFolders = new ArrayList<>();

        try (Stream<Path> folders = Files.list(rootDir)) {
            folders.filter(Files::isDirectory).forEach(folderB -> {
                Path doneFile = folderB.resolve(DONE_MARKER_FILE);
                if (Files.exists(doneFile)) {
                    eligibleFolders.add(folderB);
                    log.debug("Found eligible folder: {}", folderB.getFileName());
                }
            });
        }

        return eligibleFolders;
    }

    /**
     * Find the latest (most recently modified) ZIP file in a folder
     */
    private Path findLatestZipFile(Path folder) throws IOException {
        try (Stream<Path> files = Files.list(folder)) {
            return files
                .filter(path -> path.toString().toLowerCase().endsWith(".zip"))
                .max(Comparator.comparingLong(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toMillis();
                    } catch (IOException e) {
                        log.warn("Failed to get last modified time for: {}", path, e);
                        return 0L;
                    }
                }))
                .orElse(null);
        }
    }

    /**
     * Load completed reviews from CSV file
     */
    private Map<String, CompletedReview> loadCompletedReviews() {
        Map<String, CompletedReview> completed = new HashMap<>();

        try {
            Path csvPath = Paths.get(properties.getProcessor().getOutputPath(), CSV_FILENAME);
            if (!Files.exists(csvPath)) {
                // Create CSV file with header
                Files.createDirectories(csvPath.getParent());
                Files.writeString(csvPath, CSV_HEADER, StandardCharsets.UTF_8);
                return completed;
            }

            List<String> lines = Files.readAllLines(csvPath, StandardCharsets.UTF_8);
            for (int i = 1; i < lines.size(); i++) { // Skip header
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(",", 7);
                if (parts.length >= 4) {
                    String folderB = parts[0];
                    String zipFileName = parts[1];
                    String key = folderB + "|" + zipFileName;

                    CompletedReview review = new CompletedReview();
                    review.folderBName = folderB;
                    review.zipFileName = zipFileName;
                    review.score = parts[2];
                    review.reportFileName = parts[3];
                    if (parts.length >= 5) {
                        review.completedTime = parts[4];
                    }
                    if (parts.length >= 6) {
                        review.overallComment = parts[5];
                    }
                    if (parts.length >= 7) {
                        try {
                            review.retryCount = Integer.parseInt(parts[6]);
                        } catch (NumberFormatException e) {
                            review.retryCount = 0;
                        }
                    }

                    completed.put(key, review);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to load completed reviews CSV", e);
        }

        return completed;
    }

    /**
     * Append a completed review to the CSV file
     */
    private synchronized void appendToCompletedReviewsCsv(ProjectReviewResult result) {
        try {
            Path csvPath = Paths.get(properties.getProcessor().getOutputPath(), CSV_FILENAME);

            String scoreStr = result.getScore() != null ?
                String.format("%.1f", result.getScore()) : "N/A";
            String reportFileName = result.getReportPath() != null ?
                result.getReportPath().getFileName().toString() : "";
            String completedTime = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String overallComment = result.getOverallComment() != null ?
                result.getOverallComment() : "";

            // Wrap overall comment in quotes for CSV if it contains commas or quotes
            String csvLine = String.format("%s,%s,%s,%s,%s,\"%s\",%d\n",
                result.getFolderBName(),
                result.getZipFileName(),
                scoreStr,
                reportFileName,
                completedTime,
                overallComment,
                result.getRetryCount());

            Files.writeString(csvPath, csvLine, StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND);

            log.debug("Appended to CSV: {}", csvLine.trim());

            // Print total completed count
            int totalCompleted = getCompletedReviewsCount();
            log.info("✅ Review completed and recorded. CSV总记录数: {}", totalCompleted);
        } catch (IOException e) {
            log.error("Failed to append to completed reviews CSV", e);
        }
    }

    /**
     * Process a single project (extract, review, cleanup)
     * Report naming: FolderBName-Score-ZipFileName.md
     * Retry logic: If score is 0 or < 30, retry up to 3 times
     */
    private ProjectReviewResult processProject(ProjectReviewTask task) {
        ProjectReviewResult result = new ProjectReviewResult();
        result.setFolderBName(task.getFolderBName());
        result.setZipFileName(task.getZipFilePath().getFileName().toString());
        result.setStartTime(LocalDateTime.now());
        result.setRetryCount(0);

        Path extractedPath = null;

        try {
            // Extract ZIP once
            log.info("Extracting project from folder {}: {}", task.getFolderBName(), result.getZipFileName());
            extractedPath = ZipUtil.extractZip(task.getZipFilePath(), tempExtractDir);

            // Retry loop: up to MAX_RETRY_ATTEMPTS times
            for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
                log.info("Reviewing project: {}/{} (Attempt {}/{})",
                    task.getFolderBName(), result.getZipFileName(), attempt, MAX_RETRY_ATTEMPTS);

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

                // Execute review with automatic anti-cheat filtering and README priority sorting
                ProcessResult processResult = baseEngine.execute(context);

                if (processResult.isSuccess()) {
                    // Extract score from content
                    Double score = ScoreExtractor.extractScore(processResult.getContent());
                    result.setScore(score);

                    // Extract overall comment from content
                    String overallComment = ScoreExtractor.extractOverallComment(processResult.getContent());
                    result.setOverallComment(overallComment);

                    // Check if score is valid (not 0 and >= 30)
                    boolean isValidScore = score != null && score > 0 && score >= MIN_VALID_SCORE;

                    if (!isValidScore && attempt < MAX_RETRY_ATTEMPTS) {
                        // Score is too low, retry
                        log.warn("⚠️ Project {}/{} received low score: {} (Attempt {}/{}). Retrying...",
                            task.getFolderBName(), result.getZipFileName(), score, attempt, MAX_RETRY_ATTEMPTS);
                        result.setRetryCount(attempt);
                        continue; // Retry
                    }

                    // Either score is valid, or we've exhausted retries
                    result.setRetryCount(attempt - 1); // Record actual retry count (0 if first attempt succeeded)

                    if (!isValidScore) {
                        // Final attempt still has low score
                        log.error("❌ Project {}/{} still has low score after {} attempts: {}. Recording final result.",
                            task.getFolderBName(), result.getZipFileName(), MAX_RETRY_ATTEMPTS, score);
                    }

                    // Get ZIP file name without extension for report
                    String zipNameWithoutExt = ZipUtil.getProjectNameFromZip(task.getZipFilePath());

                    // Generate report filename: FolderBName-Score-ZipFileName.md
                    String reportFileName = task.getFolderBName() + "-" +
                        ScoreExtractor.formatScoreForFilename(score) + "-" +
                        zipNameWithoutExt + ".md";
                    Path reportPath = Paths.get(properties.getProcessor().getOutputPath(), reportFileName);

                    // Write report with score in filename
                    Files.createDirectories(reportPath.getParent());
                    Files.writeString(reportPath, processResult.getContent());

                    result.setReportPath(reportPath);
                    result.setSuccess(true);

                    log.info("✅ Project {}/{} reviewed successfully with score: {} (Retry count: {})",
                        task.getFolderBName(), result.getZipFileName(), score, result.getRetryCount());

                    break; // Success, exit retry loop

                } else {
                    result.setSuccess(false);
                    result.setErrorMessage(processResult.getErrorMessage());
                    log.error("Project {}/{} review failed: {}",
                        task.getFolderBName(), result.getZipFileName(), processResult.getErrorMessage());
                    break; // API call failed, don't retry
                }
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            log.error("Failed to process project {}/{}: {}",
                task.getFolderBName(), result.getZipFileName(), e.getMessage(), e);
        } finally {
            // Cleanup extracted directory
            if (extractedPath != null) {
                try {
                    ZipUtil.cleanupExtractedDir(extractedPath);
                } catch (Exception e) {
                    log.warn("Failed to cleanup extracted directory for {}/{}",
                        task.getFolderBName(), result.getZipFileName(), e);
                }
            }
        }

        result.setEndTime(LocalDateTime.now());
        return result;
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
        private String folderBName;
        private String zipFileName;
        private boolean success;
        private Double score;
        private String errorMessage;
        private Path reportPath;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String overallComment;
        private int retryCount;

        // For backward compatibility with summary report
        public String getProjectName() {
            return folderBName + "/" + zipFileName;
        }
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
        private final String folderBName;
        private final Path zipFilePath;

        public ProjectReviewTask(String folderBName, Path zipFilePath) {
            this.folderBName = folderBName;
            this.zipFilePath = zipFilePath;
        }
    }

    /**
     * Inner class for completed review record from CSV
     */
    @Data
    static class CompletedReview {
        String folderBName;
        String zipFileName;
        String score;
        String reportFileName;
        String completedTime;
        String overallComment;
        int retryCount;
    }
}

