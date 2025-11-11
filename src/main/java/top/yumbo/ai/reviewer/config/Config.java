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
        private boolean includeTests; // 是否包含测试文件
        private boolean includeDependencies; // 是否包含依赖目录
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnalysisConfig {
        private List<String> analysisDimensions; // 分析维度: architecture, code_quality, technical_debt, functionality, business_value, test_coverage
        private Map<String, Double> dimensionWeights; // 各维度的权重配置
        private int batchSize; // 批处理大小
        private int maxContextLength; // 最大上下文长度
        private boolean enableIncrementalAnalysis; // 是否启用增量分析
        private Map<String, Object> domainKnowledge; // 领域知识补充
    }
}
