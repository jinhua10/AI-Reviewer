package top.yumbo.ai.reviewer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 配置加载器 - 兼容 v1.0 的配置文件
 *
 * 支持的配置文件：
 * - reviewer/reviewer.yml (主配置)
 * - reviewer/ai/deepseek.yml (AI 平台配置)
 * - reviewer/analyzer/analyzer.yml (分析器配置)
 *
 * 配置加载顺序（优先级从低到高）：
 * 1. classpath:/reviewer/*.yml (默认配置)
 * 2. {projectPath}/reviewer.yml (项目配置)
 * 3. 环境变量覆盖
 * 4. 代码配置覆盖
 */
public class ConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(ConfigLoader.class);
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    /**
     * 从 classpath 和项目路径加载配置
     */
    public static Config loadConfig(Path projectPath) {
        log.info("开始加载配置，项目路径: {}", projectPath);

        // 1. 加载默认配置（classpath）
        ReviewerConfig reviewerConfig = loadReviewerConfig();
        AIConfig aiConfig = loadAIConfig(reviewerConfig.getPlatform());
        AnalyzerConfig analyzerConfig = loadAnalyzerConfig();

        // 2. 尝试加载项目配置（覆盖默认配置）
        Path projectConfigFile = projectPath.resolve("reviewer.yml");
        if (Files.exists(projectConfigFile)) {
            log.info("发现项目配置文件: {}", projectConfigFile);
            ReviewerConfig projectReviewerConfig = loadYamlFile(projectConfigFile, ReviewerConfig.class);
            if (projectReviewerConfig != null) {
                reviewerConfig = mergeReviewerConfig(reviewerConfig, projectReviewerConfig);
            }
        }

        // 3. 环境变量覆盖
        String apiKeyFromEnv = System.getenv("AI_API_KEY");
        if (apiKeyFromEnv == null) {
            apiKeyFromEnv = System.getenv("DEEPSEEK_API_KEY");
        }

        // 4. 获取 report 配置
        ReportConfig summaryReportConfig = null;
        ReportConfig chunkReportConfig = null;
        if (reviewerConfig.getSummary() != null && reviewerConfig.getSummary().getReport() != null) {
            summaryReportConfig = reviewerConfig.getSummary().getReport();
        }
        if (reviewerConfig.getChunk() != null && reviewerConfig.getChunk().getReport() != null) {
            chunkReportConfig = reviewerConfig.getChunk().getReport();
        }

        // 使用 summary 的 report 配置作为主配置，chunk 作为详细报告配置
        List<String> reportFormats = summaryReportConfig != null
            ? summaryReportConfig.getFormats()
            : List.of("json", "markdown");

        // 输出目录处理
        Path outputDirPath;
        String configOutputDir = summaryReportConfig != null ? summaryReportConfig.getOutputDir() : null;
        if (configOutputDir != null && !configOutputDir.isEmpty() && !configOutputDir.equals("reports")) {
            // 使用配置的输出目录（可以是绝对路径或相对路径）
            Path configPath = Path.of(configOutputDir);
            if (configPath.isAbsolute()) {
                outputDirPath = configPath;
            } else {
                outputDirPath = projectPath.resolve(configOutputDir);
            }
        } else {
            // 使用默认的 {projectPath}-AI 目录
            outputDirPath = projectPath.getParent()
                .resolve(projectPath.getFileName() + "-AI");
        }

        boolean generateFileDetails = chunkReportConfig != null
            ? chunkReportConfig.isGenerateFileDetails()
            : (summaryReportConfig != null ? summaryReportConfig.isGenerateFileDetails() : true);

        // 5. 构建 Config 对象
        Config.Builder builder = Config.builder()
            .projectPath(projectPath)
            .outputDir(outputDirPath)
            .aiPlatform(reviewerConfig.getPlatform())
            .apiKey(apiKeyFromEnv != null ? apiKeyFromEnv : "")
            .apiUrl(aiConfig.getApiUrl())
            .model(aiConfig.getModel())
            .maxTokens(aiConfig.getTotalMaxTokens())
            .concurrency(aiConfig.getMaxConcurrency())
            .retryCount(aiConfig.getMaxRetries())
            .chunkSize(reviewerConfig.getMaxTokensPerChunk())
            .enableCache(true)
            .enableCheckpoint(true)
            .generateFileDetails(generateFileDetails)
            .reportFormats(reportFormats.toArray(new String[0]));

        // 6. 设置 include/exclude patterns
        if (analyzerConfig != null) {
            if (analyzerConfig.getIncludePatterns() != null) {
                builder.includePatterns(analyzerConfig.getIncludePatterns().toArray(new String[0]));
            }
            if (analyzerConfig.getExcludePatterns() != null) {
                builder.excludePatterns(analyzerConfig.getExcludePatterns().toArray(new String[0]));
            }
        }

        log.info("配置加载完成: platform={}, model={}, concurrency={}",
            reviewerConfig.getPlatform(), aiConfig.getModel(), aiConfig.getMaxConcurrency());

        return builder.build();
    }

    /**
     * 加载主配置文件 reviewer.yml
     */
    private static ReviewerConfig loadReviewerConfig() {
        ReviewerConfig config = loadClasspathYaml("reviewer/reviewer.yml", ReviewerConfig.class);
        if (config == null) {
            log.warn("未找到 reviewer.yml，使用默认配置");
            config = new ReviewerConfig();
            config.setPlatform("deepseek");
            config.setMaxTokensPerChunk(8000);
        }
        return config;
    }

    /**
     * 加载 AI 平台配置
     */
    private static AIConfig loadAIConfig(String platform) {
        String configPath = String.format("reviewer/ai/%s.yml", platform);
        AIConfig config = loadClasspathYaml(configPath, AIConfig.class);

        if (config == null) {
            log.warn("未找到 {} 配置，使用默认配置", platform);
            config = createDefaultAIConfig(platform);
        }

        return config;
    }

    /**
     * 加载分析器配置
     */
    private static AnalyzerConfig loadAnalyzerConfig() {
        AnalyzerConfig config = loadClasspathYaml("reviewer/analyzer/analyzer.yml", AnalyzerConfig.class);
        if (config == null) {
            log.warn("未找到 analyzer.yml，使用默认配置");
            config = new AnalyzerConfig();
        }
        return config;
    }

    /**
     * 从 classpath 加载 YAML 文件
     */
    private static <T> T loadClasspathYaml(String resourcePath, Class<T> clazz) {
        try (InputStream is = ConfigLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                return null;
            }
            return YAML_MAPPER.readValue(is, clazz);
        } catch (IOException e) {
            log.error("加载配置文件失败: {}", resourcePath, e);
            return null;
        }
    }

    /**
     * 从文件系统加载 YAML 文件
     */
    private static <T> T loadYamlFile(Path filePath, Class<T> clazz) {
        try {
            return YAML_MAPPER.readValue(filePath.toFile(), clazz);
        } catch (IOException e) {
            log.error("加载配置文件失败: {}", filePath, e);
            return null;
        }
    }

    /**
     * 合并配置（项目配置覆盖默认配置）
     */
    private static ReviewerConfig mergeReviewerConfig(ReviewerConfig defaults, ReviewerConfig override) {
        if (override.getPlatform() != null) {
            defaults.setPlatform(override.getPlatform());
        }
        if (override.getMaxTokensPerChunk() > 0) {
            defaults.setMaxTokensPerChunk(override.getMaxTokensPerChunk());
        }
        return defaults;
    }

    /**
     * 创建默认 AI 配置
     */
    private static AIConfig createDefaultAIConfig(String platform) {
        AIConfig config = new AIConfig();

        switch (platform.toLowerCase()) {
            case "deepseek" -> {
                config.setApiUrl("https://api.deepseek.com/v1/chat/completions");
                config.setModel("deepseek-chat");
                config.setTotalMaxTokens(8190);
            }
            case "openai" -> {
                config.setApiUrl("https://api.openai.com/v1/chat/completions");
                config.setModel("gpt-4o-mini");
                config.setTotalMaxTokens(8190);
            }
            case "qwen" -> {
                config.setApiUrl("https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation");
                config.setModel("qwen-turbo");
                config.setTotalMaxTokens(8000);
            }
            default -> {
                log.warn("未知平台: {}，使用 deepseek 默认配置", platform);
                config.setApiUrl("https://api.deepseek.com/v1/chat/completions");
                config.setModel("deepseek-chat");
                config.setTotalMaxTokens(8190);
            }
        }

        config.setTemperature(0.3);
        config.setConnectTimeoutSeconds(60);
        config.setReadTimeoutSeconds(120);
        config.setMaxConcurrency(3);
        config.setMaxRetries(2);
        config.setInitialRetryDelaySeconds(3);
        config.setMaxRetryDelaySeconds(20);

        return config;
    }

    /**
     * 主配置模型
     */
    public static class ReviewerConfig {
        private String platform = "deepseek";
        private int maxTokensPerChunk = 8000;
        private List<AIConfigEntry> ai;
        private ChunkConfig chunk;
        private SummaryConfig summary;

        // Getters and Setters
        public String getPlatform() { return platform; }
        public void setPlatform(String platform) { this.platform = platform; }

        public int getMaxTokensPerChunk() { return maxTokensPerChunk; }
        public void setMaxTokensPerChunk(int maxTokensPerChunk) { this.maxTokensPerChunk = maxTokensPerChunk; }

        public List<AIConfigEntry> getAi() { return ai; }
        public void setAi(List<AIConfigEntry> ai) { this.ai = ai; }

        public ChunkConfig getChunk() { return chunk; }
        public void setChunk(ChunkConfig chunk) { this.chunk = chunk; }

        public SummaryConfig getSummary() { return summary; }
        public void setSummary(SummaryConfig summary) { this.summary = summary; }
    }

    public static class AIConfigEntry {
        private String platform;
        private String apiUrl;
        private String model;
        private double temperature;
        private int connectTimeoutSeconds;
        private int readTimeoutSeconds;
        private String systemPrompt;
        private int totalMaxTokens;
        private int maxConcurrency;
        private int maxRetries;
        private int initialRetryDelaySeconds;
        private int maxRetryDelaySeconds;

        // Getters and Setters
        public String getPlatform() { return platform; }
        public void setPlatform(String platform) { this.platform = platform; }

        public String getApiUrl() { return apiUrl; }
        public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }

        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }

        public int getConnectTimeoutSeconds() { return connectTimeoutSeconds; }
        public void setConnectTimeoutSeconds(int connectTimeoutSeconds) { this.connectTimeoutSeconds = connectTimeoutSeconds; }

        public int getReadTimeoutSeconds() { return readTimeoutSeconds; }
        public void setReadTimeoutSeconds(int readTimeoutSeconds) { this.readTimeoutSeconds = readTimeoutSeconds; }

        public String getSystemPrompt() { return systemPrompt; }
        public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }

        public int getTotalMaxTokens() { return totalMaxTokens; }
        public void setTotalMaxTokens(int totalMaxTokens) { this.totalMaxTokens = totalMaxTokens; }

        public int getMaxConcurrency() { return maxConcurrency; }
        public void setMaxConcurrency(int maxConcurrency) { this.maxConcurrency = maxConcurrency; }

        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

        public int getInitialRetryDelaySeconds() { return initialRetryDelaySeconds; }
        public void setInitialRetryDelaySeconds(int initialRetryDelaySeconds) { this.initialRetryDelaySeconds = initialRetryDelaySeconds; }

        public int getMaxRetryDelaySeconds() { return maxRetryDelaySeconds; }
        public void setMaxRetryDelaySeconds(int maxRetryDelaySeconds) { this.maxRetryDelaySeconds = maxRetryDelaySeconds; }
    }

    public static class ChunkConfig {
        private String platform;
        private String feature;
        private String chunkPromptTemplate;
        private ReportConfig report;

        public String getPlatform() { return platform; }
        public void setPlatform(String platform) { this.platform = platform; }

        public String getFeature() { return feature; }
        public void setFeature(String feature) { this.feature = feature; }

        public String getChunkPromptTemplate() { return chunkPromptTemplate; }
        public void setChunkPromptTemplate(String chunkPromptTemplate) { this.chunkPromptTemplate = chunkPromptTemplate; }

        public ReportConfig getReport() { return report; }
        public void setReport(ReportConfig report) { this.report = report; }
    }

    public static class SummaryConfig {
        private String platform;
        private String feature;
        private String projectPromptTemplate;
        private ReportConfig report;

        public String getPlatform() { return platform; }
        public void setPlatform(String platform) { this.platform = platform; }

        public String getFeature() { return feature; }
        public void setFeature(String feature) { this.feature = feature; }

        public String getProjectPromptTemplate() { return projectPromptTemplate; }
        public void setProjectPromptTemplate(String projectPromptTemplate) { this.projectPromptTemplate = projectPromptTemplate; }

        public ReportConfig getReport() { return report; }
        public void setReport(ReportConfig report) { this.report = report; }
    }

    /**
     * 报告配置模型
     */
    public static class ReportConfig {
        private List<String> formats;
        private String outputDir;
        private Boolean generateFileDetails;

        public List<String> getFormats() {
            return formats != null ? formats : List.of("json", "markdown");
        }
        public void setFormats(List<String> formats) { this.formats = formats; }

        public String getOutputDir() {
            return outputDir != null ? outputDir : "reports";
        }
        public void setOutputDir(String outputDir) { this.outputDir = outputDir; }

        public boolean isGenerateFileDetails() {
            return generateFileDetails != null ? generateFileDetails : true;
        }
        public void setGenerateFileDetails(Boolean generateFileDetails) { this.generateFileDetails = generateFileDetails; }
    }

    /**
     * AI 配置模型
     */
    public static class AIConfig {
        private String apiUrl;
        private String model;
        private double temperature;
        private int connectTimeoutSeconds;
        private int readTimeoutSeconds;
        private String systemPrompt;
        private int totalMaxTokens;
        private int maxConcurrency;
        private int maxRetries;
        private int initialRetryDelaySeconds;
        private int maxRetryDelaySeconds;

        // Getters and Setters
        public String getApiUrl() { return apiUrl; }
        public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }

        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }

        public int getConnectTimeoutSeconds() { return connectTimeoutSeconds; }
        public void setConnectTimeoutSeconds(int connectTimeoutSeconds) { this.connectTimeoutSeconds = connectTimeoutSeconds; }

        public int getReadTimeoutSeconds() { return readTimeoutSeconds; }
        public void setReadTimeoutSeconds(int readTimeoutSeconds) { this.readTimeoutSeconds = readTimeoutSeconds; }

        public String getSystemPrompt() { return systemPrompt; }
        public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }

        public int getTotalMaxTokens() { return totalMaxTokens; }
        public void setTotalMaxTokens(int totalMaxTokens) { this.totalMaxTokens = totalMaxTokens; }

        public int getMaxConcurrency() { return maxConcurrency; }
        public void setMaxConcurrency(int maxConcurrency) { this.maxConcurrency = maxConcurrency; }

        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

        public int getInitialRetryDelaySeconds() { return initialRetryDelaySeconds; }
        public void setInitialRetryDelaySeconds(int initialRetryDelaySeconds) { this.initialRetryDelaySeconds = initialRetryDelaySeconds; }

        public int getMaxRetryDelaySeconds() { return maxRetryDelaySeconds; }
        public void setMaxRetryDelaySeconds(int maxRetryDelaySeconds) { this.maxRetryDelaySeconds = maxRetryDelaySeconds; }
    }

    /**
     * 分析器配置模型
     */
    public static class AnalyzerConfig {
        private List<String> includePatterns;
        private List<String> excludePatterns;

        public List<String> getIncludePatterns() { return includePatterns; }
        public void setIncludePatterns(List<String> includePatterns) { this.includePatterns = includePatterns; }

        public List<String> getExcludePatterns() { return excludePatterns; }
        public void setExcludePatterns(List<String> excludePatterns) { this.excludePatterns = excludePatterns; }
    }
}

