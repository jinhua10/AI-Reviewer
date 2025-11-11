package top.yumbo.ai.reviewer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 配置类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Config {

    // AI服务配置
    private AIServiceConfig aiService;

    // 文件扫描配置
    private FileScanConfig fileScan;

    // 分析配置
    private AnalysisConfig analysis;

    // 缓存配置
    private CacheConfig cache;

    // 评分规则配置
    private ScoringConfig scoring;

    // 报告配置
    private ReportConfig report;

    // 日志配置
    private LoggingConfig logging;

    // 监控配置
    private MetricsConfig metrics;

    // 高级配置
    private AdvancedConfig advanced;

    /**
     * 从YAML文件加载配置
     */
    public static Config loadFromFile(String configPath) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try (InputStream input = new FileInputStream(configPath)) {
            return mapper.readValue(input, Config.class);
        }
    }

    /**
     * 从classpath加载默认配置
     */
    public static Config loadDefault() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try (InputStream input = Config.class.getClassLoader().getResourceAsStream("config.yaml")) {
            if (input == null) {
                throw new IOException("默认配置文件不存在");
            }
            return mapper.readValue(input, Config.class);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AIServiceConfig {
        private String provider; // "deepseek", "openai", etc.
        private String apiKey;
        private String baseUrl;
        private String model;
        private int maxTokens;
        private double temperature;
        private int timeout; // 请求超时时间(毫秒)
        private int maxRetries; // 最大重试次数
        private int retryDelay; // 重试延迟(毫秒)
        private int maxConcurrency; // 最大并发请求数
        private Map<String, Object> additionalParams;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FileScanConfig {
        private List<String> includePatterns; // 包含的文件模式
        private List<String> excludePatterns; // 排除的文件模式
        private List<String> coreFilePatterns; // 核心文件模式
        private int maxFileSize; // 最大文件大小 (KB)
        private int maxFilesCount; // 最大文件数量
        private int minFileSize; // 最小文件大小 (KB)
        private boolean includeTests; // 是否包含测试文件
        private boolean includeDependencies; // 是否包含依赖目录
        private boolean includeGenerated; // 是否包含生成的文件
        private boolean includeDocumentation; // 是否包含文档文件
        private boolean followSymlinks; // 是否跟随符号链接
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnalysisConfig {
        private List<String> analysisDimensions; // 分析维度: architecture, code_quality, technical_debt, functionality, business_value, test_coverage
        private Map<String, Double> dimensionWeights; // 各维度的权重配置
        private int batchSize; // 批处理大小
        private int maxConcurrentBatches; // 最大并发批次数
        private int batchTimeout; // 批处理超时(毫秒)
        private int maxContextLength; // 最大上下文长度
        private int contextOverlap; // 上下文重叠长度
        private boolean enableIncrementalAnalysis; // 是否启用增量分析
        private boolean enableParallelAnalysis; // 是否启用并行分析
        private boolean enableCaching; // 是否启用缓存
        private boolean enableMetrics; // 是否启用指标收集
        private Map<String, Object> domainKnowledge; // 领域知识补充
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CacheConfig {
        private boolean enabled; // 是否启用缓存
        private String type; // 缓存类型
        private int ttlHours; // 缓存过期时间(小时)
        private int maxSize; // 最大缓存条目数
        private int cleanupIntervalMinutes; // 清理间隔(分钟)
        private Map<String, Object> fileCache; // 文件缓存配置
        private Map<String, Object> redisCache; // Redis缓存配置
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ScoringConfig {
        private List<Map<String, Object>> rules; // 评分规则列表
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReportConfig {
        private String defaultFormat; // 默认报告格式
        private Map<String, Object> templates; // 报告模板配置
        private boolean includeCharts; // 是否包含图表
        private boolean includeMetrics; // 是否包含指标
        private boolean includeRecommendations; // 是否包含建议
        private boolean includeCodeExamples; // 是否包含代码示例
        private int maxRecommendations; // 最大建议数量
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LoggingConfig {
        private String level; // 日志级别
        private Map<String, Object> file; // 文件日志配置
        private Map<String, Object> console; // 控制台日志配置
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MetricsConfig {
        private boolean enabled; // 是否启用监控
        private Map<String, Object> export; // 导出配置
        private Map<String, Object> custom; // 自定义指标配置
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AdvancedConfig {
        private Map<String, Object> performance; // 性能配置
        private Map<String, Object> security; // 安全配置
        private Map<String, Object> experimental; // 实验性功能
        private Map<String, Object> debug; // 调试选项
    }
}
