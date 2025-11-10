package top.yumbo.ai.reviewer.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.yumbo.ai.reviewer.analyzer.AIAnalyzer;
import top.yumbo.ai.reviewer.config.Config;
import top.yumbo.ai.reviewer.entity.AnalysisResult;
import top.yumbo.ai.reviewer.entity.SourceFile;
import top.yumbo.ai.reviewer.exception.AnalysisException;
import top.yumbo.ai.reviewer.report.ReportBuilder;
import top.yumbo.ai.reviewer.scanner.FileScanner;
import top.yumbo.ai.reviewer.service.AIService;
import top.yumbo.ai.reviewer.service.DeepseekAIService;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * AI Reviewer v2.0 - 主入口类（简化版）
 *
 * 核心特性：
 * - 3层架构：API → Core → Foundation
 * - 流式 API：链式调用，易于使用
 * - 自动资源管理：实现 AutoCloseable
 * - 统一异常处理：AnalysisException
 *
 * 使用示例：
 * <pre>
 * try (AIReviewer reviewer = AIReviewer.create("path/to/project")) {
 *     AnalysisResult result = reviewer
 *         .configure(config -> config
 *             .aiPlatform("deepseek")
 *             .concurrency(5))
 *         .analyze();
 *     System.out.println(result.getSummary());
 * }
 * </pre>
 */
public class AIReviewer implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(AIReviewer.class);

    private final Config config;
    private AIService aiService;
    private AIAnalyzer aiAnalyzer;

    private AIReviewer(Config config) {
        this.config = config;
    }

    /**
     * 创建 AIReviewer 实例（使用默认配置）
     */
    public static AIReviewer create(String projectPath) {
        return create(Path.of(projectPath));
    }

    /**
     * 创建 AIReviewer 实例（自动加载 v1.0 配置文件）
     *
     * 会自动加载以下配置文件（如果存在）：
     * - classpath:/reviewer/reviewer.yml
     * - classpath:/reviewer/ai/{platform}.yml
     * - {projectPath}/reviewer.yml (可选覆盖)
     */
    public static AIReviewer create(Path projectPath) {
        try {
            // 优先尝试从 v1.0 配置文件加载
            Config config = Config.fromV1ConfigFiles(projectPath);
            log.info("已从 v1.0 配置文件加载配置");
            return new AIReviewer(config);
        } catch (Exception e) {
            // 如果加载失败，使用默认配置
            log.warn("加载 v1.0 配置文件失败，使用默认配置: {}", e.getMessage());
            Config config = Config.defaultConfig(projectPath);
            return new AIReviewer(config);
        }
    }

    /**
     * 创建 AIReviewer 实例（使用自定义配置）
     */
    public static AIReviewer create(Config config) {
        return new AIReviewer(config);
    }

    /**
     * 配置 AIReviewer（流式 API）
     */
    public AIReviewer configure(Consumer<Config.Builder> configurer) {
        Config.Builder builder = Config.builder()
            .projectPath(config.getProjectPath())
            .apiKey(config.getApiKey());

        configurer.accept(builder);

        Config newConfig = builder.build();
        return new AIReviewer(newConfig);
    }

    /**
     * 执行分析
     */
    public AnalysisResult analyze() throws AnalysisException {
        long startTime = System.currentTimeMillis();
        log.info("========== AI Reviewer v2.0 开始分析 ==========");
        log.info("项目路径: {}", config.getProjectPath());
        log.info("配置: {}", config);

        try {
            // 1. 初始化组件
            initializeComponents();

            // 2. 扫描文件
            log.info(">>> 阶段 1: 文件扫描");
            FileScanner scanner = new FileScanner(config);
            List<SourceFile> files = scanner.scan();
            log.info("扫描完成: {}", scanner.getScanSummary(files));

            if (files.isEmpty()) {
                log.warn("没有文件需要分析");
                return AnalysisResult.empty(config.getProjectPath().toString());
            }

            // 3. AI 分析
            log.info(">>> 阶段 2: AI 分析");
            AnalysisResult result = aiAnalyzer.analyze(files);

            // 4. 生成报告
            log.info(">>> 阶段 3: 生成报告");
            ReportBuilder reportBuilder = new ReportBuilder(config);
            reportBuilder.buildReports(result);

            // 5. 完成
            long duration = System.currentTimeMillis() - startTime;
            result = AnalysisResult.builder()
                .projectPath(result.getProjectPath())
                .totalFiles(result.getTotalFiles())
                .analyzedFiles(result.getAnalyzedFiles())
                .skippedFiles(result.getSkippedFiles())
                .totalChunks(result.getTotalChunks())
                .successfulChunks(result.getSuccessfulChunks())
                .failedChunks(result.getFailedChunks())
                .detailReports(result.getDetailReports())
                .summaryReport(result.getSummaryReport())
                .duration(duration)
                .build();

            log.info("========== 分析完成 (耗时: {}ms) ==========", duration);
            log.info("结果: {}", result.getSummary());

            return result;

        } catch (AnalysisException e) {
            log.error("分析失败: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("未预期的错误", e);
            throw new AnalysisException("分析过程中发生未预期的错误: " + e.getMessage(), e);
        }
    }

    /**
     * 初始化核心组件
     */
    private void initializeComponents() {
        if (aiService == null) {
            log.debug("初始化 AI 服务: {}", config.getAiPlatform());
            // 根据配置创建对应的 AI 服务
            switch (config.getAiPlatform().toLowerCase()) {
                case "deepseek" -> aiService = new DeepseekAIService(config);
                // TODO: 支持更多 AI 平台
                // case "openai" -> aiService = new OpenAIService(config);
                // case "qwen" -> aiService = new QwenAIService(config);
                default -> throw new IllegalArgumentException("不支持的 AI 平台: " + config.getAiPlatform());
            }
        }

        if (aiAnalyzer == null) {
            log.debug("初始化 AI 分析器");
            aiAnalyzer = new AIAnalyzer(config, aiService);
        }
    }

    /**
     * 关闭资源
     */
    @Override
    public void close() {
        log.debug("关闭 AIReviewer 资源");
        if (aiAnalyzer != null) {
            aiAnalyzer.close();
        }
    }

    /**
     * 获取配置
     */
    public Config getConfig() {
        return config;
    }
}

