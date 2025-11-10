package top.yumbo.ai.reviewer.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 统一配置类（简化版）
 * 使用 Builder 模式，所有配置集中管理
 */
public class Config {
    // 基本配置
    private final Path projectPath;
    private final Path outputDir;

    // AI 配置
    private final String aiPlatform;      // deepseek / openai / qwen
    private final String apiKey;
    private final String apiUrl;
    private final String model;
    private final int maxTokens;

    // 分析配置
    private final int concurrency;
    private final int retryCount;
    private final int chunkSize;          // 单个块的最大 Token 数
    private final List<String> includePatterns;
    private final List<String> excludePatterns;

    // 功能开关
    private final boolean enableCache;
    private final boolean enableCheckpoint;
    private final boolean generateFileDetails;  // 是否生成文件级详细报告
    private final List<String> reportFormats;  // markdown / json / pdf

    private Config(Builder builder) {
        this.projectPath = builder.projectPath;
        this.outputDir = builder.outputDir;
        this.aiPlatform = builder.aiPlatform;
        this.apiKey = builder.apiKey;
        this.apiUrl = builder.apiUrl;
        this.model = builder.model;
        this.maxTokens = builder.maxTokens;
        this.concurrency = builder.concurrency;
        this.retryCount = builder.retryCount;
        this.chunkSize = builder.chunkSize;
        this.includePatterns = builder.includePatterns;
        this.excludePatterns = builder.excludePatterns;
        this.enableCache = builder.enableCache;
        this.enableCheckpoint = builder.enableCheckpoint;
        this.generateFileDetails = builder.generateFileDetails;
        this.reportFormats = builder.reportFormats;
    }

    // Getters
    public Path getProjectPath() { return projectPath; }
    public Path getOutputDir() { return outputDir; }
    public String getAiPlatform() { return aiPlatform; }
    public String getApiKey() { return apiKey; }
    public String getApiUrl() { return apiUrl; }
    public String getModel() { return model; }
    public int getMaxTokens() { return maxTokens; }
    public int getConcurrency() { return concurrency; }
    public int getRetryCount() { return retryCount; }
    public int getChunkSize() { return chunkSize; }
    public List<String> getIncludePatterns() { return includePatterns; }
    public List<String> getExcludePatterns() { return excludePatterns; }
    public boolean isEnableCache() { return enableCache; }
    public boolean isEnableCheckpoint() { return enableCheckpoint; }
    public boolean isGenerateFileDetails() { return generateFileDetails; }
    public List<String> getReportFormats() { return reportFormats; }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 默认配置
     */
    public static Config defaultConfig(Path projectPath) {
        return builder()
            .projectPath(projectPath)
            .build();
    }

    /**
     * 从 v1.0 配置文件加载（兼容模式）
     *
     * 支持的配置文件：
     * - classpath:/reviewer/reviewer.yml
     * - classpath:/reviewer/ai/{platform}.yml
     * - classpath:/reviewer/analyzer/analyzer.yml
     * - {projectPath}/reviewer.yml (可选，覆盖默认配置)
     */
    public static Config fromV1ConfigFiles(Path projectPath) {
        return ConfigLoader.loadConfig(projectPath);
    }

    /**
     * Builder 类
     */
    public static class Builder {
        private Path projectPath;
        private Path outputDir;
        private String aiPlatform = "deepseek";
        private String apiKey = System.getenv("AI_API_KEY");
        private String apiUrl = "https://api.deepseek.com/v1/chat/completions";
        private String model = "deepseek-chat";
        private int maxTokens = 4096;
        private int concurrency = 3;
        private int retryCount = 3;
        private int chunkSize = 8000;
        private List<String> includePatterns = new ArrayList<>();
        private List<String> excludePatterns = new ArrayList<>();
        private boolean enableCache = true;
        private boolean enableCheckpoint = true;
        private boolean generateFileDetails = true;
        private List<String> reportFormats = Arrays.asList("markdown", "json");

        public Builder projectPath(Path projectPath) {
            this.projectPath = projectPath;
            // 默认输出目录为项目根目录的 .ai-review
            if (this.outputDir == null) {
                this.outputDir = projectPath.resolve(".ai-review");
            }
            return this;
        }

        public Builder projectPath(String projectPath) {
            return projectPath(Paths.get(projectPath));
        }

        public Builder outputDir(Path outputDir) {
            this.outputDir = outputDir;
            return this;
        }

        public Builder outputDir(String outputDir) {
            return outputDir(Paths.get(outputDir));
        }

        public Builder aiPlatform(String aiPlatform) {
            this.aiPlatform = aiPlatform;
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder apiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder concurrency(int concurrency) {
            this.concurrency = concurrency;
            return this;
        }

        public Builder retryCount(int retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        public Builder chunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
            return this;
        }

        public Builder includePatterns(String... patterns) {
            this.includePatterns = Arrays.asList(patterns);
            return this;
        }

        public Builder excludePatterns(String... patterns) {
            this.excludePatterns = Arrays.asList(patterns);
            return this;
        }

        public Builder enableCache(boolean enableCache) {
            this.enableCache = enableCache;
            return this;
        }

        public Builder enableCheckpoint(boolean enableCheckpoint) {
            this.enableCheckpoint = enableCheckpoint;
            return this;
        }

        public Builder generateFileDetails(boolean generateFileDetails) {
            this.generateFileDetails = generateFileDetails;
            return this;
        }

        public Builder reportFormats(String... formats) {
            this.reportFormats = Arrays.asList(formats);
            return this;
        }

        public Config build() {
            if (projectPath == null) {
                throw new IllegalArgumentException("projectPath is required");
            }
            if (apiKey == null || apiKey.isEmpty()) {
                throw new IllegalArgumentException("apiKey is required (set AI_API_KEY env var or use builder.apiKey())");
            }
            return new Config(this);
        }
    }

    @Override
    public String toString() {
        return String.format("Config{platform=%s, model=%s, concurrency=%d, chunkSize=%d}",
            aiPlatform, model, concurrency, chunkSize);
    }
}

