package top.yumbo.ai.reviewer.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * 配置加载器
 *
 * 支持多种配置来源，优先级：
 * 1. 系统属性（命令行 -D 参数）
 * 2. 环境变量
 * 3. config.yaml 文件
 * 4. 默认值
 *
 * @author AI-Reviewer Team
 * @version 2.0
 * @since 2025-11-13
 */
public class ConfigurationLoader {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationLoader.class);
    private static final String DEFAULT_CONFIG_FILE = "config.yaml";
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    /**
     * 加载配置
     */
    public static Configuration load() {
        Configuration config = new Configuration();

        // 1. 从 YAML 文件加载
        loadFromYaml(config);

        // 2. 从环境变量覆盖
        overrideFromEnvironment(config);

        // 3. 从系统属性覆盖
        overrideFromSystemProperties(config);

        // 4. 验证配置
        try {
            config.validate();
            log.info("配置加载成功: provider={}, model={}", config.getAiProvider(), config.getAiModel());
        } catch (Configuration.ConfigurationException e) {
            log.error("配置验证失败: {}", e.getMessage());
            throw e;
        }

        return config;
    }

    /**
     * 从 YAML 文件加载
     */
    private static void loadFromYaml(Configuration config) {
        try {
            // 优先从 classpath 加载
            InputStream inputStream = ConfigurationLoader.class.getClassLoader()
                    .getResourceAsStream(DEFAULT_CONFIG_FILE);

            if (inputStream != null) {
                ConfigYaml yaml = YAML_MAPPER.readValue(inputStream, ConfigYaml.class);
                applyYamlConfig(config, yaml);
                log.info("配置已从 classpath:{} 加载", DEFAULT_CONFIG_FILE);
                return;
            }

            // 然后尝试从文件系统加载
            Path configPath = Paths.get(DEFAULT_CONFIG_FILE);
            if (Files.exists(configPath)) {
                ConfigYaml yaml = YAML_MAPPER.readValue(configPath.toFile(), ConfigYaml.class);
                applyYamlConfig(config, yaml);
                log.info("配置已从文件 {} 加载", configPath);
                return;
            }

            log.warn("未找到配置文件 {}，使用默认配置", DEFAULT_CONFIG_FILE);

        } catch (Exception e) {
            log.warn("加载配置文件失败，使用默认配置: {}", e.getMessage());
        }
    }

    /**
     * 应用 YAML 配置到 Configuration
     */
    private static void applyYamlConfig(Configuration config, ConfigYaml yaml) {
        if (yaml == null) {
            return;
        }

        // AI 服务配置
        if (yaml.aiService != null) {
            if (yaml.aiService.provider != null) {
                config.setAiProvider(yaml.aiService.provider);
            }
            if (yaml.aiService.apiKey != null) {
                config.setAiApiKey(yaml.aiService.apiKey);
            }
            if (yaml.aiService.baseUrl != null) {
                config.setAiBaseUrl(yaml.aiService.baseUrl);
            }
            if (yaml.aiService.model != null) {
                config.setAiModel(yaml.aiService.model);
            }
            if (yaml.aiService.maxTokens != null) {
                config.setAiMaxTokens(yaml.aiService.maxTokens);
            }
            if (yaml.aiService.temperature != null) {
                config.setAiTemperature(yaml.aiService.temperature);
            }
            if (yaml.aiService.maxRetries != null) {
                config.setAiMaxRetries(yaml.aiService.maxRetries);
            }
            if (yaml.aiService.retryDelay != null) {
                config.setAiRetryDelayMillis(yaml.aiService.retryDelay);
            }
            if (yaml.aiService.region != null) {
                config.setAwsRegion(yaml.aiService.region);
            }

            // 将 YAML 的毫秒级超时映射到 Configuration 中的字段（如果存在）
            if (yaml.aiService.connectTimeout != null) {
                config.setAiConnectTimeoutMillis(yaml.aiService.connectTimeout);
            }
            if (yaml.aiService.readTimeout != null) {
                config.setAiReadTimeoutMillis(yaml.aiService.readTimeout);
            }
            // 其余高级超时字段（writeTimeout/analyzeTimeout/batchAnalyzeTimeout）暂时不映射
        }

        // 文件系统配置
        if (yaml.fileSystem != null) {
            if (yaml.fileSystem.includePatterns != null) {
                config.setFileSystemIncludePatterns(yaml.fileSystem.includePatterns);
            }
            if (yaml.fileSystem.excludePatterns != null) {
                config.setFileSystemExcludePatterns(yaml.fileSystem.excludePatterns);
            }
            if (yaml.fileSystem.maxFileSizeKB != null) {
                config.setMaxFileSizeKB(yaml.fileSystem.maxFileSizeKB);
            }
        }

        // 缓存配置
        if (yaml.cache != null) {
            if (yaml.cache.enabled != null) {
                config.setCacheEnabled(yaml.cache.enabled);
            }
            if (yaml.cache.type != null) {
                config.setCacheType(yaml.cache.type);
            }
            if (yaml.cache.ttlHours != null) {
                config.setCacheTtlHours(yaml.cache.ttlHours);
            }
        }

        // S3 存储配置
        if (yaml.s3Storage != null) {
            if (yaml.s3Storage.region != null) {
                config.setS3Region(yaml.s3Storage.region);
            }
            if (yaml.s3Storage.bucketName != null) {
                config.setS3BucketName(yaml.s3Storage.bucketName);
            }
            if (yaml.s3Storage.accessKeyId != null) {
                config.setS3AccessKeyId(yaml.s3Storage.accessKeyId);
            }
            if (yaml.s3Storage.secretAccessKey != null) {
                config.setS3SecretAccessKey(yaml.s3Storage.secretAccessKey);
            }
            if (yaml.s3Storage.maxConcurrency != null) {
                config.setS3MaxConcurrency(yaml.s3Storage.maxConcurrency);
            }
            if (yaml.s3Storage.connectTimeout != null) {
                config.setS3ConnectTimeout(yaml.s3Storage.connectTimeout);
            }
            if (yaml.s3Storage.readTimeout != null) {
                config.setS3ReadTimeout(yaml.s3Storage.readTimeout);
            }
            if (yaml.s3Storage.maxRetries != null) {
                config.setS3MaxRetries(yaml.s3Storage.maxRetries);
            }
            if (yaml.s3Storage.retryDelay != null) {
                config.setS3RetryDelay(yaml.s3Storage.retryDelay);
            }
            if (yaml.s3Storage.useAccelerateEndpoint != null) {
                config.setS3UseAccelerateEndpoint(yaml.s3Storage.useAccelerateEndpoint);
            }
            if (yaml.s3Storage.usePathStyleAccess != null) {
                config.setS3UsePathStyleAccess(yaml.s3Storage.usePathStyleAccess);
            }
            if (yaml.s3Storage.endpoint != null) {
                config.setS3Endpoint(yaml.s3Storage.endpoint);
            }
        }
    }

    /**
     * 从环境变量覆盖
     */
    private static void overrideFromEnvironment(Configuration config) {
        // AI 服务配置
        String aiProvider = System.getenv("AI_PROVIDER");
        if (aiProvider != null && !aiProvider.isBlank()) {
            config.setAiProvider(aiProvider);
            log.debug("AI Provider 从环境变量覆盖: {}", aiProvider);
        }

        String aiApiKey = System.getenv("AI_API_KEY");
        if (aiApiKey != null && !aiApiKey.isBlank()) {
            config.setAiApiKey(aiApiKey);
            log.debug("AI API Key 从环境变量覆盖");
        }

        String aiModel = System.getenv("AI_MODEL");
        if (aiModel != null && !aiModel.isBlank()) {
            config.setAiModel(aiModel);
            log.debug("AI Model 从环境变量覆盖: {}", aiModel);
        }

        // AWS 配置
        String awsRegion = System.getenv("AWS_REGION");
        if (awsRegion != null && !awsRegion.isBlank()) config.setAwsRegion(awsRegion);
        String awsAccessKeyId = System.getenv("AWS_ACCESS_KEY_ID");
        if (awsAccessKeyId != null && !awsAccessKeyId.isBlank()) config.setAwsAccessKeyId(awsAccessKeyId);
        String awsSecretAccessKey = System.getenv("AWS_SECRET_ACCESS_KEY");
        if (awsSecretAccessKey != null && !awsSecretAccessKey.isBlank()) config.setAwsSecretAccessKey(awsSecretAccessKey);
    }

    /**
     * 从系统属性覆盖（最高优先级）
     */
    private static void overrideFromSystemProperties(Configuration config) {
        String aiProvider = System.getProperty("ai.provider");
        if (aiProvider != null && !aiProvider.isBlank()) {
            config.setAiProvider(aiProvider);
            log.debug("AI Provider 从系统属性覆盖: {}", aiProvider);
        }

        String aiApiKey = System.getProperty("ai.apiKey");
        if (aiApiKey != null && !aiApiKey.isBlank()) {
            config.setAiApiKey(aiApiKey);
            log.debug("AI API Key 从系统属性覆盖");
        }

        String aiModel = System.getProperty("ai.model");
        if (aiModel != null && !aiModel.isBlank()) {
            config.setAiModel(aiModel);
            log.debug("AI Model 从系统属性覆盖: {}", aiModel);
        }
    }

    /**
     * YAML 配置映射类
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ConfigYaml {
        private AIServiceYaml aiService;
        private FileSystemYaml fileSystem;
        private CacheYaml cache;
        private S3StorageYaml s3Storage;
        private Map<String, Object> hackathon;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class AIServiceYaml {
            private String provider;
            private String apiKey;
            private String baseUrl;
            private String model;
            private Integer maxTokens;
            private Double temperature;
            private Integer maxRetries;
            private Integer retryDelay;
            private Integer maxConcurrency;
            private String region;  // AWS region

            // 新增：超时配置（毫秒）
            private Integer connectTimeout;
            private Integer readTimeout;
            private Integer writeTimeout;
            private Integer analyzeTimeout;
            private Integer batchAnalyzeTimeout;

            // 额外参数
            private Map<String, Object> additionalParams;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class FileSystemYaml {
            private List<String> includePatterns;
            private List<String> excludePatterns;
            private Integer maxFileSizeKB;
            private Integer scanDepth;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class CacheYaml {
            private Boolean enabled;
            private String type;
            private Integer ttlHours;
            private Integer maxSize;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class S3StorageYaml {
            private String region;
            private String bucketName;
            private String accessKeyId;
            private String secretAccessKey;
            private Integer maxConcurrency;
            private Integer connectTimeout;
            private Integer readTimeout;
            private Integer maxRetries;
            private Integer retryDelay;
            private Boolean useAccelerateEndpoint;
            private Boolean usePathStyleAccess;
            private String endpoint;
        }
    }
}
