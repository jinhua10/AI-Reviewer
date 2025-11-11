package top.yumbo.ai.reviewer;

import top.yumbo.ai.reviewer.analyzer.AIAnalyzer;
import top.yumbo.ai.reviewer.analyzer.ChunkSplitter;
import top.yumbo.ai.reviewer.config.Config;
import top.yumbo.ai.reviewer.entity.AnalysisResult;
import top.yumbo.ai.reviewer.entity.FileChunk;
import top.yumbo.ai.reviewer.entity.SourceFile;
import top.yumbo.ai.reviewer.exception.AnalysisException;
import top.yumbo.ai.reviewer.report.ReportBuilder;
import top.yumbo.ai.reviewer.scanner.FileScanner;
import top.yumbo.ai.reviewer.service.AIService;
import top.yumbo.ai.reviewer.service.DeepseekAIService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI Reviewer 主入口类
 * 
 * 负责协调整个代码分析流程，包括文件扫描、代码分块、AI分析和报告生成
 */
public class AIReviewer implements AutoCloseable {

    private final Config config;
    private final AIService aiService;
    private final ExecutorService executorService;
    private boolean closed = false;

    private AIReviewer(Config config) {
        this.config = config;
        this.aiService = createAIService(config);
        this.executorService = Executors.newFixedThreadPool(config.getConcurrency());
    }

    /**
     * 使用项目路径创建 AIReviewer 实例
     * 
     * @param projectPath 项目路径
     * @return AIReviewer 实例
     */
    public static AIReviewer create(String projectPath) {
        Config config = Config.builder()
                .projectPath(projectPath)
                .build();
        return new AIReviewer(config);
    }

    /**
     * 使用配置对象创建 AIReviewer 实例
     * 
     * @param config 配置对象
     * @return AIReviewer 实例
     */
    public static AIReviewer create(Config config) {
        return new AIReviewer(config);
    }

    /**
     * 配置分析参数
     * 
     * @param configurer 配置函数
     * @return 当前 AIReviewer 实例，支持链式调用
     */
    public AIReviewer configure(Configurer configurer) {
        configurer.configure(config);
        return this;
    }

    /**
     * 执行代码分析
     * 
     * @return 分析结果
     * @throws AnalysisException 如果分析过程中发生错误
     */
    public AnalysisResult analyze() {
        if (closed) {
            throw new AnalysisException("AIReviewer has been closed");
        }

        try {
            // 1. 扫描项目文件
            FileScanner scanner = new FileScanner(config);
            List<SourceFile> sourceFiles = scanner.scan();

            // 2. 智能分块
            ChunkSplitter splitter = new ChunkSplitter(config);
            List<FileChunk> chunks = splitter.split(sourceFiles);

            // 3. AI 分析
            AIAnalyzer analyzer = new AIAnalyzer(config, aiService, executorService);
            AnalysisResult result = analyzer.analyze(chunks);

            // 4. 生成报告
            ReportBuilder reportBuilder = new ReportBuilder(config);
            reportBuilder.build(result);

            return result;
        } catch (Exception e) {
            throw new AnalysisException("分析过程中发生异常", e);
        }
    }

    /**
     * 异步执行代码分析
     * 
     * @return 包含分析结果的 CompletableFuture
     */
    public CompletableFuture<AnalysisResult> analyzeAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return analyze();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executorService);
    }

    /**
     * 创建 AI 服务实例
     * 
     * @param config 配置对象
     * @return AI 服务实例
     */
    private AIService createAIService(Config config) {
        switch (config.getAiPlatform().toLowerCase()) {
            case "deepseek":
                return new DeepseekAIService(config);
            default:
                throw new AnalysisException("不支持的AI平台: " + config.getAiPlatform());
        }
    }

    @Override
    public void close() throws Exception {
        if (!closed) {
            closed = true;
            executorService.shutdown();
            aiService.close();
        }
    }

    /**
     * 配置函数接口
     */
    @FunctionalInterface
    public interface Configurer {
        void configure(Config config);
    }
}
