package top.yumbo.ai.reviewer;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.reviewer.config.Config;
import top.yumbo.ai.reviewer.entity.AnalysisResult;
import top.yumbo.ai.reviewer.exception.AnalysisException;
import top.yumbo.ai.reviewer.report.ReportBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Hackathon AI 源码评分工具 - 专业版
 * 专为黑客松比赛设计的AI驱动智能评分系统
 *
 * 功能特性:
 * - 多模式评审: 快速评审、详细评审、专家评审
 * - 智能评分算法: 基于AI的客观量化评估
 * - 批量处理: 支持大规模项目同时评审
 * - 排行榜系统: 实时排名和统计分析
 * - 专业报告: 结构化评审报告生成
 * - 评审历史: 完整的评审记录追踪
 */
@Slf4j
public class HackathonReviewer {

    // 评审模式枚举
    public enum ReviewMode {
        QUICK("快速评审", "10秒内完成基础评分"),
        DETAILED("详细评审", "30秒内完成全面分析"),
        EXPERT("专家评审", "60秒内深度技术评估");

        private final String displayName;
        private final String description;

        ReviewMode(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    // 评审状态枚举
    public enum ReviewStatus {
        PENDING("待评审"),
        IN_PROGRESS("评审中"),
        COMPLETED("已完成"),
        FAILED("评审失败");

        private final String displayName;

        ReviewStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() { return displayName; }
    }

    private final AIReviewer reviewer;
    private final ReportBuilder reportBuilder;
    private final ExecutorService executorService;
    private final Map<String, ReviewRecord> reviewHistory;
    private final Config config;

    public HackathonReviewer() throws IOException {
        this("hackathon-config.yaml");
    }

    public HackathonReviewer(String configPath) throws IOException {
        // 加载Hackathon专用配置，支持从类路径加载作为fallback
        Config config = null;
        try {
            // 首先尝试从文件系统加载
            config = Config.loadFromFile(configPath);
            log.info("✅ 从文件加载配置成功: {}", configPath);
        } catch (IOException e) {
            log.warn("⚠️ 配置文件不存在，尝试从类路径加载: {}", configPath);
            try {
                // 文件不存在时，从类路径加载
                config = Config.loadFromFile("hackathon-config.yaml");
                log.info("✅ 从类路径加载配置成功: hackathon-config.yaml");
            } catch (IOException classPathException) {
                log.warn("⚠️ 类路径配置也不存在，尝试加载默认配置");
                try {
                    // 类路径也不存在时，加载默认配置
                    config = Config.loadDefault();
                    log.info("✅ 加载默认配置成功");
                } catch (IOException defaultException) {
                    log.error("❌ 所有配置加载方式都失败", defaultException);
                    throw new IOException("无法加载任何配置文件", defaultException);
                }
            }
        }

        this.config = config;
        this.reviewer = new AIReviewer(config);
        this.reportBuilder = new ReportBuilder();

        // 创建线程池用于并发评审
        int threadPoolSize = Math.max(2, Runtime.getRuntime().availableProcessors());
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);

        // 初始化评审历史记录
        this.reviewHistory = new HashMap<>();

        log.info("🏆 Hackathon AI 评审工具初始化完成");
        log.info("📊 支持评审模式: {}", Arrays.toString(ReviewMode.values()));
        log.info("⚡ 并发处理能力: {} 线程", threadPoolSize);
    }

    /**
     * 智能评审 - 根据项目复杂度自动选择评审模式
     */
    public HackathonScore smartReview(String projectPath) throws AnalysisException {
        Path projectRoot = Paths.get(projectPath);
        if (!Files.exists(projectRoot)) {
            throw new AnalysisException("项目路径不存在: " + projectPath);
        }

        // 根据项目大小和复杂度选择评审模式
        ReviewMode mode = determineReviewMode(projectRoot);
        log.info("🎯 智能选择评审模式: {} for 项目: {}", mode.getDisplayName(), projectPath);

        return review(projectPath, mode);
    }

    /**
     * 指定模式评审项目
     */
    public HackathonScore review(String projectPath, ReviewMode mode) throws AnalysisException {
        String reviewId = generateReviewId(projectPath);
        ReviewRecord record = new ReviewRecord(reviewId, projectPath, mode);
        reviewHistory.put(reviewId, record);

        try {
            record.setStatus(ReviewStatus.IN_PROGRESS);
            log.info("🚀 开始{} - 项目: {}", mode.getDisplayName(), projectPath);

            long startTime = System.currentTimeMillis();

            // 执行评审
            HackathonScore score = performReview(projectPath, mode);

            long duration = System.currentTimeMillis() - startTime;
            record.setDuration(duration);
            record.setScore(score);
            record.setStatus(ReviewStatus.COMPLETED);

            log.info("✅ {}完成 - 耗时: {}ms, 评分: {:.1f}",
                    mode.getDisplayName(), duration, score.getTotalScore());

            return score;

        } catch (Exception e) {
            record.setStatus(ReviewStatus.FAILED);
            record.setErrorMessage(e.getMessage());
            log.error("❌ {}失败 - 项目: {}, 错误: {}", mode.getDisplayName(), projectPath, e.getMessage());
            throw new AnalysisException("评审失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量评审多个项目
     */
    public List<HackathonScore> batchReview(List<String> projectPaths, ReviewMode mode) throws AnalysisException {
        log.info("📦 开始批量评审 {} 个项目 (模式: {})", projectPaths.size(), mode.getDisplayName());

        List<CompletableFuture<HackathonScore>> futures = projectPaths.stream()
                .map(path -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return review(path, mode);
                    } catch (AnalysisException e) {
                        log.error("批量评审项目失败: {}", path, e);
                        return createErrorScore(path, e.getMessage());
                    }
                }, executorService))
                .collect(Collectors.toList());

        // 等待所有评审完成
        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    /**
     * 生成排行榜
     */
    public Leaderboard generateLeaderboard(List<HackathonScore> scores) {
        Leaderboard leaderboard = new Leaderboard();

        // 按总分排序
        List<HackathonScore> sortedScores = scores.stream()
                .sorted((a, b) -> Double.compare(b.getTotalScore(), a.getTotalScore()))
                .collect(Collectors.toList());

        leaderboard.setScores(sortedScores);
        leaderboard.setGeneratedTime(LocalDateTime.now());

        // 计算统计信息
        leaderboard.setTotalProjects(scores.size());
        leaderboard.setAverageScore(scores.stream().mapToDouble(HackathonScore::getTotalScore).average().orElse(0.0));
        leaderboard.setHighestScore(scores.stream().mapToDouble(HackathonScore::getTotalScore).max().orElse(0.0));
        leaderboard.setLowestScore(scores.stream().mapToDouble(HackathonScore::getTotalScore).min().orElse(0.0));

        // 分类统计
        Map<String, Long> statusStats = scores.stream()
                .collect(Collectors.groupingBy(HackathonScore::getJudgeStatus, Collectors.counting()));
        leaderboard.setStatusStatistics(statusStats);

        return leaderboard;
    }

    /**
     * 生成评审报告
     */
    public void generateReviewReport(HackathonScore score, String outputPath, ReviewMode mode) throws AnalysisException {
        try {
            // 根据评审模式选择模板
            String templateType = getTemplateTypeForMode(mode);

            // 生成报告
            reportBuilder.saveReport(score.getOriginalResult(), outputPath, templateType);

            // 添加评审模式信息
            appendReviewModeInfo(outputPath, mode, score);

            log.info("📄 评审报告已生成: {} (模式: {})", outputPath, mode.getDisplayName());

        } catch (Exception e) {
            throw new AnalysisException("生成评审报告失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取评审历史
     */
    public List<ReviewRecord> getReviewHistory() {
        return new ArrayList<>(reviewHistory.values());
    }

    /**
     * 获取评审统计信息
     */
    public ReviewStatistics getReviewStatistics() {
        ReviewStatistics stats = new ReviewStatistics();

        List<ReviewRecord> records = getReviewHistory();
        stats.setTotalReviews(records.size());
        stats.setCompletedReviews(records.stream().mapToInt(r -> r.getStatus() == ReviewStatus.COMPLETED ? 1 : 0).sum());
        stats.setFailedReviews(records.stream().mapToInt(r -> r.getStatus() == ReviewStatus.FAILED ? 1 : 0).sum());

        if (!records.isEmpty()) {
            stats.setAverageDuration(records.stream().mapToLong(ReviewRecord::getDuration).average().orElse(0.0));
        }

        return stats;
    }

    /**
     * 关闭评审工具
     */
    public void shutdown() {
        executorService.shutdown();
        log.info("🏆 Hackathon AI 评审工具已关闭");
    }

    // 私有方法实现

    private ReviewMode determineReviewMode(Path projectRoot) {
        try {
            // 根据项目大小和复杂度选择评审模式
            long fileCount = Files.walk(projectRoot)
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String fileName = path.getFileName().toString().toLowerCase();
                        return fileName.endsWith(".java") || fileName.endsWith(".py") ||
                               fileName.endsWith(".js") || fileName.endsWith(".ts");
                    })
                    .count();

            long totalLines = Files.walk(projectRoot)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java") || path.toString().endsWith(".py"))
                    .mapToLong(path -> {
                        try {
                            return Files.lines(path).count();
                        } catch (IOException e) {
                            return 0L;
                        }
                    })
                    .sum();

            // 根据文件数量和代码行数判断复杂度
            if (fileCount > 20 || totalLines > 2000) {
                return ReviewMode.EXPERT;  // 大型复杂项目
            } else if (fileCount > 10 || totalLines > 1000) {
                return ReviewMode.DETAILED; // 中等复杂度项目
            } else {
                return ReviewMode.QUICK;   // 小型简单项目
            }

        } catch (IOException e) {
            log.warn("无法确定项目复杂度，使用默认评审模式", e);
            return ReviewMode.QUICK;
        }
    }

    private HackathonScore performReview(String projectPath, ReviewMode mode) throws AnalysisException {
        // 执行基础分析
        AnalysisResult result = reviewer.analyzeProject(projectPath);

        // 根据评审模式调整评分权重
        HackathonScore score = convertToHackathonScore(result, mode);

        // 添加评审模式信息
        score.setReviewMode(mode);
        score.setReviewTime(LocalDateTime.now());

        return score;
    }

    private HackathonScore convertToHackathonScore(AnalysisResult result, ReviewMode mode) {
        HackathonScore score = new HackathonScore();
        score.setProjectName(result.getProjectName());
        score.setOriginalResult(result);

        // 根据评审模式调整权重
        double[] weights = getWeightsForMode(mode);

        // 计算各维度评分
        double architecture = result.getArchitectureScore() * weights[0];
        double codeQuality = result.getCodeQualityScore() * weights[1];
        double technicalDebt = Math.max(0, 100 - result.getTechnicalDebtScore()) * weights[2];
        double functionality = result.getFunctionalityScore() * weights[3];
        double businessValue = result.getBusinessValueScore() * weights[4];
        double testCoverage = result.getTestCoverageScore() * weights[5];

        // 创新性评分 (基于技术栈和实现复杂度)
        double innovation = calculateInnovationScore(result) * weights[6];

        double totalScore = architecture + codeQuality + technicalDebt +
                           functionality + businessValue + testCoverage + innovation;

        // 设置详细评分
        score.setTotalScore(Math.min(100.0, totalScore)); // 确保不超过100分
        score.setArchitectureScore(result.getArchitectureScore());
        score.setCodeQualityScore(result.getCodeQualityScore());
        score.setFunctionalityScore(result.getFunctionalityScore());
        score.setBusinessValueScore(result.getBusinessValueScore());
        score.setTestCoverageScore(result.getTestCoverageScore());
        score.setInnovationScore(innovation / weights[6]); // 还原到0-100分制

        // 根据评审模式和分数确定评审状态
        score.setJudgeStatus(determineJudgeStatus(totalScore, mode));

        return score;
    }

    private double[] getWeightsForMode(ReviewMode mode) {
        switch (mode) {
            case QUICK:
                return new double[]{0.15, 0.20, 0.10, 0.25, 0.20, 0.10, 0.00}; // 无创新性评分
            case DETAILED:
                return new double[]{0.15, 0.20, 0.10, 0.20, 0.15, 0.10, 0.10}; // 标准权重
            case EXPERT:
                return new double[]{0.20, 0.20, 0.15, 0.15, 0.10, 0.10, 0.10}; // 更注重技术深度
            default:
                return new double[]{0.15, 0.20, 0.10, 0.25, 0.20, 0.10, 0.00};
        }
    }

    private double calculateInnovationScore(AnalysisResult result) {
        // 基于项目特点计算创新性评分
        double baseScore = 70.0; // 基础分

        // 技术栈创新性 (使用较新的技术栈加分)
        if (result.getProjectName().toLowerCase().contains("ai") ||
            result.getProjectName().toLowerCase().contains("ml") ||
            result.getProjectName().toLowerCase().contains("blockchain")) {
            baseScore += 15;
        }

        // 架构创新性 (高分项目加分)
        if (result.getArchitectureScore() > 85) {
            baseScore += 10;
        }

        // 功能完整性创新性 (高分项目加分)
        if (result.getFunctionalityScore() > 90) {
            baseScore += 5;
        }

        return Math.min(100.0, baseScore);
    }

    private String determineJudgeStatus(double totalScore, ReviewMode mode) {
        // 根据评审模式调整阈值
        double excellentThreshold = mode == ReviewMode.EXPERT ? 90 : 85;
        double goodThreshold = mode == ReviewMode.EXPERT ? 80 : 75;
        double fairThreshold = mode == ReviewMode.EXPERT ? 70 : 65;

        if (totalScore >= excellentThreshold) {
            return "🏆 优秀 - 进入决赛";
        } else if (totalScore >= goodThreshold) {
            return "🥈 良好 - 晋级复赛";
        } else if (totalScore >= fairThreshold) {
            return "🥉 及格 - 基础奖项";
        } else {
            return "📜 参与奖";
        }
    }

    private String getTemplateTypeForMode(ReviewMode mode) {
        switch (mode) {
            case QUICK:
                return "hackathon-quick";
            case DETAILED:
                return "hackathon-detailed";
            case EXPERT:
                return "hackathon-expert";
            default:
                return "hackathon";
        }
    }

    private void appendReviewModeInfo(String outputPath, ReviewMode mode, HackathonScore score) {
        // 这里可以添加评审模式特定的信息到报告中
        // 暂时留空，后续可以扩展
    }

    private HackathonScore createErrorScore(String projectPath, String errorMessage) {
        HackathonScore score = new HackathonScore();
        score.setProjectName(Paths.get(projectPath).getFileName().toString());
        score.setTotalScore(0.0);
        score.setJudgeStatus("❌ 评审失败: " + errorMessage);
        score.setReviewTime(LocalDateTime.now());
        return score;
    }

    private String generateReviewId(String projectPath) {
        return projectPath + "_" + System.currentTimeMillis();
    }

    // 内部类定义

    /**
     * 评审记录
     */
    public static class ReviewRecord {
        private final String reviewId;
        private final String projectPath;
        private final ReviewMode mode;
        private ReviewStatus status;
        private HackathonScore score;
        private long duration;
        private String errorMessage;
        private final LocalDateTime startTime;

        public ReviewRecord(String reviewId, String projectPath, ReviewMode mode) {
            this.reviewId = reviewId;
            this.projectPath = projectPath;
            this.mode = mode;
            this.status = ReviewStatus.PENDING;
            this.startTime = LocalDateTime.now();
        }

        // Getters and setters
        public String getReviewId() { return reviewId; }
        public String getProjectPath() { return projectPath; }
        public ReviewMode getMode() { return mode; }
        public ReviewStatus getStatus() { return status; }
        public void setStatus(ReviewStatus status) { this.status = status; }
        public HackathonScore getScore() { return score; }
        public void setScore(HackathonScore score) { this.score = score; }
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public LocalDateTime getStartTime() { return startTime; }
    }

    /**
     * 排行榜
     */
    public static class Leaderboard {
        private List<HackathonScore> scores;
        private LocalDateTime generatedTime;
        private int totalProjects;
        private double averageScore;
        private double highestScore;
        private double lowestScore;
        private Map<String, Long> statusStatistics;

        // Getters and setters
        public List<HackathonScore> getScores() { return scores; }
        public void setScores(List<HackathonScore> scores) { this.scores = scores; }
        public LocalDateTime getGeneratedTime() { return generatedTime; }
        public void setGeneratedTime(LocalDateTime generatedTime) { this.generatedTime = generatedTime; }
        public int getTotalProjects() { return totalProjects; }
        public void setTotalProjects(int totalProjects) { this.totalProjects = totalProjects; }
        public double getAverageScore() { return averageScore; }
        public void setAverageScore(double averageScore) { this.averageScore = averageScore; }
        public double getHighestScore() { return highestScore; }
        public void setHighestScore(double highestScore) { this.highestScore = highestScore; }
        public double getLowestScore() { return lowestScore; }
        public void setLowestScore(double lowestScore) { this.lowestScore = lowestScore; }
        public Map<String, Long> getStatusStatistics() { return statusStatistics; }
        public void setStatusStatistics(Map<String, Long> statusStatistics) { this.statusStatistics = statusStatistics; }
    }

    /**
     * 评审统计信息
     */
    public static class ReviewStatistics {
        private int totalReviews;
        private int completedReviews;
        private int failedReviews;
        private double averageDuration;

        // Getters and setters
        public int getTotalReviews() { return totalReviews; }
        public void setTotalReviews(int totalReviews) { this.totalReviews = totalReviews; }
        public int getCompletedReviews() { return completedReviews; }
        public void setCompletedReviews(int completedReviews) { this.completedReviews = completedReviews; }
        public int getFailedReviews() { return failedReviews; }
        public void setFailedReviews(int failedReviews) { this.failedReviews = failedReviews; }
        public double getAverageDuration() { return averageDuration; }
        public void setAverageDuration(double averageDuration) { this.averageDuration = averageDuration; }
    }

    /**
     * Hackathon评分结果类
     */
    public static class HackathonScore {
        private String projectName;
        private double totalScore;
        private double architectureScore;
        private double codeQualityScore;
        private double functionalityScore;
        private double businessValueScore;
        private double testCoverageScore;
        private double innovationScore;
        private String judgeStatus;
        private AnalysisResult originalResult;
        private ReviewMode reviewMode;
        private LocalDateTime reviewTime;

        // Getters and setters
        public String getProjectName() { return projectName; }
        public void setProjectName(String projectName) { this.projectName = projectName; }

        public double getTotalScore() { return totalScore; }
        public void setTotalScore(double totalScore) { this.totalScore = totalScore; }

        public double getArchitectureScore() { return architectureScore; }
        public void setArchitectureScore(double architectureScore) { this.architectureScore = architectureScore; }

        public double getCodeQualityScore() { return codeQualityScore; }
        public void setCodeQualityScore(double codeQualityScore) { this.codeQualityScore = codeQualityScore; }

        public double getFunctionalityScore() { return functionalityScore; }
        public void setFunctionalityScore(double functionalityScore) { this.functionalityScore = functionalityScore; }

        public double getBusinessValueScore() { return businessValueScore; }
        public void setBusinessValueScore(double businessValueScore) { this.businessValueScore = businessValueScore; }

        public double getTestCoverageScore() { return testCoverageScore; }
        public void setTestCoverageScore(double testCoverageScore) { this.testCoverageScore = testCoverageScore; }

        public double getInnovationScore() { return innovationScore; }
        public void setInnovationScore(double innovationScore) { this.innovationScore = innovationScore; }

        public String getJudgeStatus() { return judgeStatus; }
        public void setJudgeStatus(String judgeStatus) { this.judgeStatus = judgeStatus; }

        public AnalysisResult getOriginalResult() { return originalResult; }
        public void setOriginalResult(AnalysisResult originalResult) { this.originalResult = originalResult; }

        public ReviewMode getReviewMode() { return reviewMode; }
        public void setReviewMode(ReviewMode reviewMode) { this.reviewMode = reviewMode; }

        public LocalDateTime getReviewTime() { return reviewTime; }
        public void setReviewTime(LocalDateTime reviewTime) { this.reviewTime = reviewTime; }

        @Override
        public String toString() {
            return String.format("%s: %.1f分 (%s) [%s]",
                    projectName, totalScore, judgeStatus,
                    reviewMode != null ? reviewMode.getDisplayName() : "未知模式");
        }
    }
}
