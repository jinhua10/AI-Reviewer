package top.yumbo.ai.reviewer.analyzer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.yumbo.ai.reviewer.concurrent.*;
import top.yumbo.ai.reviewer.config.Config;
import top.yumbo.ai.reviewer.entity.*;
import top.yumbo.ai.reviewer.exception.AnalysisException;
import top.yumbo.ai.reviewer.service.AIService;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AI 分析器（增强版 v2.1）
 *
 * 核心改进：
 * 1. ✅ 断路器模式 (Circuit Breaker) - 防止级联失败
 * 2. ✅ 智能重试 (Retry Policy) - 指数退避 + 随机抖动
 * 3. ✅ 速率限制 (Rate Limiter) - 控制 API 调用频率
 * 4. ✅ 断点续传 (Checkpoint) - 支持中断后继续
 * 5. ✅ 性能监控 (Metrics) - 实时统计分析指标
 * 6. ✅ 自定义线程池 - 更好的资源控制
 *
 * 职责：
 * 1. 代码分块
 * 2. 并发调用 AI 进行分析
 * 3. 聚合分析结果
 * 4. 生成报告
 */
public class AIAnalyzer implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(AIAnalyzer.class);

    private final Config config;
    private final AIService aiService;
    private final ChunkSplitter chunkSplitter;
    private final ExecutorService executorService;

    // 并发控制组件
    private final CircuitBreaker circuitBreaker;
    private final RetryPolicy retryPolicy;
    private final RateLimiter rateLimiter;
    private final CheckpointStore checkpointStore;
    private final PerformanceMetrics metrics;

    public AIAnalyzer(Config config, AIService aiService) {
        this.config = config;
        this.aiService = aiService;
        this.chunkSplitter = new ChunkSplitter(config.getChunkSize());

        // 创建自定义线程池
        int coreSize = Math.max(2, config.getConcurrency());
        int maxSize = Math.max(coreSize, config.getConcurrency() * 2);
        this.executorService = new ThreadPoolExecutor(
            coreSize,
            maxSize,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "ai-analyzer-" + counter.getAndIncrement());
                    t.setDaemon(false);
                    t.setUncaughtExceptionHandler((thread, throwable) ->
                        log.error("线程 {} 发生未捕获异常", thread.getName(), throwable)
                    );
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );

        // 初始化并发控制组件
        this.circuitBreaker = new CircuitBreaker(5, 30000);  // 5 次失败，30 秒恢复
        this.retryPolicy = new RetryPolicy();  // 默认重试策略
        this.rateLimiter = new RateLimiter(config.getConcurrency() * 2);  // 每秒最多 2*并发数
        // 缓存存储在 {projectPath}-AI/cache 目录
        Path cacheDir = config.getProjectPath().getParent()
            .resolve(config.getProjectPath().getFileName() + "-AI")
            .resolve("cache");
        this.checkpointStore = new CheckpointStore(cacheDir, config.isEnableCheckpoint());
        this.metrics = new PerformanceMetrics();

        log.info("AIAnalyzer 初始化完成: concurrency={}, maxRetries={}, checkpointEnabled={}",
            config.getConcurrency(), config.getRetryCount(), config.isEnableCheckpoint());
    }

    /**
     * 分析文件列表
     */
    public AnalysisResult analyze(List<SourceFile> files) throws AnalysisException {
        log.info("开始 AI 分析，共 {} 个文件", files.size());

        // 1. 分块
        List<FileChunk> chunks = chunkSplitter.split(files);
        log.info("分块完成，共 {} 个块", chunks.size());

        // 2. 并发分析
        List<ChunkAnalysisResult> chunkResults = analyzeChunks(chunks);
        log.info("AI 分析完成，成功 {} 个块", chunkResults.size());

        // 3. 聚合结果
        return aggregateResults(files, chunks, chunkResults);
    }

    /**
     * 并发分析所有代码块（增强版）
     */
    private List<ChunkAnalysisResult> analyzeChunks(List<FileChunk> chunks) {
        log.info("开始并发分析 {} 个代码块", chunks.size());
        List<Future<ChunkAnalysisResult>> futures = new ArrayList<>();

        // 提交所有任务
        for (int i = 0; i < chunks.size(); i++) {
            final int index = i;
            final FileChunk chunk = chunks.get(i);

            Future<ChunkAnalysisResult> future = executorService.submit(() -> {
                metrics.recordRequestStart();
                return analyzeChunkWithEnhancements(index, chunk);
            });
            futures.add(future);
        }

        // 收集结果
        List<ChunkAnalysisResult> results = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (int i = 0; i < futures.size(); i++) {
            try {
                ChunkAnalysisResult result = futures.get(i).get();
                if (result != null) {
                    results.add(result);
                    successCount++;
                } else {
                    failureCount++;
                }
            } catch (ExecutionException e) {
                log.error("分析块 {} 执行失败: {}", i, e.getCause().getMessage());
                metrics.recordFailure("OTHER");
                failureCount++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("分析块 {} 被中断", i);
                failureCount++;
            }
        }

        log.info("并发分析完成: 成功 {}, 失败 {}", successCount, failureCount);
        log.info("\n{}", metrics.generateReport());

        return results;
    }

    /**
     * 分析单个代码块（增强版：集成所有并发控制机制）
     */
    private ChunkAnalysisResult analyzeChunkWithEnhancements(int index, FileChunk chunk) {
        long startTime = System.currentTimeMillis();
        String chunkId = computeChunkId(chunk);

        // 1. 检查断点续传
        if (config.isEnableCheckpoint()) {
            CheckpointStore.CheckpointData checkpoint = checkpointStore.load(chunkId);
            if (checkpoint != null) {
                log.info("从断点加载块 {} (id={})", index + 1, chunkId);
                long latency = System.currentTimeMillis() - startTime;
                metrics.recordSuccess(latency);
                return new ChunkAnalysisResult(
                    chunk.getChunkIndex(),
                    chunk.getFiles(),
                    checkpoint.getAnalysis()
                );
            }
        }

        // 2. 检查断路器
        if (!circuitBreaker.allowRequest()) {
            log.warn("断路器打开，跳过块 {} (id={})", index + 1, chunkId);
            metrics.recordCircuitBreakerOpen();
            return null;
        }

        // 3. 带重试的分析
        AnalysisException lastException = null;
        for (int attempt = 0; attempt <= retryPolicy.getMaxRetries(); attempt++) {
            try {
                // 3.1 速率限制
                rateLimiter.acquire();

                // 3.2 执行分析
                String prompt = buildPrompt(chunk);
                String response = aiService.analyze(prompt, config.getMaxTokens());

                // 3.3 记录成功
                long latency = System.currentTimeMillis() - startTime;
                metrics.recordSuccess(latency);
                circuitBreaker.recordSuccess();

                ChunkAnalysisResult result = new ChunkAnalysisResult(
                    chunk.getChunkIndex(),
                    chunk.getFiles(),
                    response
                );

                // 3.4 保存断点
                if (config.isEnableCheckpoint()) {
                    checkpointStore.save(chunkId, response, System.currentTimeMillis());
                }

                if (attempt > 0) {
                    log.info("块 {} 分析成功 (尝试 {}/{})", index + 1, attempt + 1, retryPolicy.getMaxRetries() + 1);
                }

                return result;

            } catch (AnalysisException e) {
                lastException = e;
                circuitBreaker.recordFailure();
                metrics.recordFailure(e.getErrorType().name());

                // 判断是否应该重试
                if (attempt < retryPolicy.getMaxRetries() &&
                    retryPolicy.shouldRetry(e.getErrorType(), attempt)) {

                    metrics.recordRetry();
                    int delayMs = retryPolicy.calculateDelayWithJitter(attempt);
                    log.warn("块 {} 分析失败 ({}), {}ms 后重试 (尝试 {}/{})",
                        index + 1, e.getErrorType(), delayMs, attempt + 1, retryPolicy.getMaxRetries());

                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("重试等待被中断");
                        break;
                    }
                } else {
                    log.error("块 {} 分析永久失败: {}", index + 1, e.getMessage());
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("块 {} 分析被中断", index + 1);
                break;
            }
        }

        return null;  // 所有重试都失败
    }

    /**
     * 构建 AI 提示词
     */
    private String buildPrompt(FileChunk chunk) {
        String prompt = """
            请对以下代码进行全面分析：
            
            %s
            
            请提供：
            1. 代码质量评估（可读性、可维护性、性能）
            2. 潜在问题列表（按严重程度：HIGH/MEDIUM/LOW，包括类型和描述）
            3. 最佳实践建议
            4. 改进建议（重构方向、优化思路）
            
            请以结构化的方式返回分析结果。
            """;
        return String.format(prompt, chunk.getContent());
    }

    /**
     * 计算代码块 ID（用于断点续传）
     */
    private String computeChunkId(FileChunk chunk) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String content = chunk.getContent();
            byte[] hash = md.digest(content.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().substring(0, 16);  // 取前 16 位
        } catch (Exception e) {
            // 降级方案：使用 hashCode
            return String.valueOf(Math.abs(chunk.getContent().hashCode()));
        }
    }

    /**
     * 聚合分析结果
     */
    private AnalysisResult aggregateResults(List<SourceFile> files,
                                           List<FileChunk> chunks,
                                           List<ChunkAnalysisResult> chunkResults) {

        // 生成详细报告（每个文件一个）
        List<DetailReport> detailReports = generateDetailReports(chunkResults);

        // 生成摘要报告
        SummaryReport summaryReport = generateSummaryReport(files, detailReports);

        return AnalysisResult.builder()
            .projectPath(config.getProjectPath().toString())
            .totalFiles(files.size())
            .analyzedFiles(files.size())
            .skippedFiles(0)
            .totalChunks(chunks.size())
            .successfulChunks(chunkResults.size())
            .failedChunks(chunks.size() - chunkResults.size())
            .detailReports(detailReports)
            .summaryReport(summaryReport)
            .build();
    }

    /**
     * 生成详细报告
     */
    private List<DetailReport> generateDetailReports(List<ChunkAnalysisResult> chunkResults) {
        List<DetailReport> reports = new ArrayList<>();

        for (ChunkAnalysisResult chunkResult : chunkResults) {
            for (SourceFile file : chunkResult.getFiles()) {
                DetailReport report = new DetailReport(
                    file.getPath().toString(),
                    file.getFileName(),
                    file.getFileType().name(),
                    file.getSize(),
                    chunkResult.getAnalysis(),
                    new ArrayList<>(), // TODO: 解析 issues
                    new HashMap<>()    // TODO: 计算 metrics
                );
                reports.add(report);
            }
        }

        return reports;
    }

    /**
     * 生成摘要报告
     */
    private SummaryReport generateSummaryReport(List<SourceFile> files, List<DetailReport> detailReports) {
        String projectName = config.getProjectPath().getFileName().toString();

        return new SummaryReport(
            projectName,
            "项目整体代码质量良好，建议关注以下问题...",
            75.0, // TODO: 计算真实评分
            Map.of("HIGH", 0, "MEDIUM", 0, "LOW", 0),
            Map.of()
        );
    }

    /**
     * 关闭资源（增强版）
     */
    @Override
    public void close() {
        log.info("开始关闭 AIAnalyzer...");

        // 1. 打印最终指标报告
        log.info("\n最终性能指标:\n{}", metrics.generateReport());

        // 2. 重置断路器
        circuitBreaker.reset();

        // 3. 关闭线程池
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                log.warn("线程池未能在 60 秒内正常关闭，强制关闭");
                List<Runnable> pending = executorService.shutdownNow();
                log.warn("丢弃了 {} 个待执行任务", pending.size());
            }
        } catch (InterruptedException e) {
            log.error("等待线程池关闭被中断");
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("AIAnalyzer 已关闭");
    }

    /**
     * 获取性能指标
     */
    public PerformanceMetrics getMetrics() {
        return metrics;
    }

    /**
     * 获取断路器状态
     */
    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }

    /**
     * 清除断点数据
     */
    public void clearCheckpoints() {
        checkpointStore.clear();
        log.info("断点数据已清除");
    }

    /**
     * 代码块分析结果
     */
    private static class ChunkAnalysisResult {
        private final int chunkIndex;
        private final List<SourceFile> files;
        private final String analysis;

        public ChunkAnalysisResult(int chunkIndex, List<SourceFile> files, String analysis) {
            this.chunkIndex = chunkIndex;
            this.files = files;
            this.analysis = analysis;
        }

        public int getChunkIndex() { return chunkIndex; }
        public List<SourceFile> getFiles() { return files; }
        public String getAnalysis() { return analysis; }
    }
}

