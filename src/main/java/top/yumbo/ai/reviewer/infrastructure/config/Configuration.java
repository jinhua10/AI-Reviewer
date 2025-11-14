package top.yumbo.ai.reviewer.infrastructure.config;

import lombok.Data;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 应用配置类
 *
 * @author AI-Reviewer Team
 * @version 2.0
 * @since 2025-11-13
 */
@Data
public class Configuration {

    // ========== AI 服务配置 ==========
    private String aiProvider = "deepseek";
    private String aiApiKey;
    private String aiBaseUrl;
    private String aiModel;
    private Integer aiMaxTokens = 4000;
    private Double aiTemperature = 0.3;
    private Integer aiMaxRetries = 3;
    private Integer aiRetryDelayMillis = 1000;
    private Integer aiConnectTimeoutMillis = 30000;
    private Integer aiReadTimeoutMillis = 60000;
    private Integer aiMaxConcurrency = 3;

    // AWS Bedrock 特定配置
    private String awsRegion = "us-east-1";
    private String awsAccessKeyId;
    private String awsSecretAccessKey;

    // ========== 文件系统配置 ==========
    private List<String> fileSystemIncludePatterns = List.of(
            "*.java", "*.py", "*.js", "*.ts", "*.go", "*.rs",
            "*.c", "*.cpp", "*.h", "*.hpp",
            "*.xml", "*.yaml", "*.yml", "*.json",
            "*.md", "*.txt", "README*", "pom.xml", "package.json"
    );

    private List<String> fileSystemExcludePatterns = List.of(
            "*test*", "*.class", "*.jar", "*.war",
            "node_modules", "target", "build", "dist",
            ".git", ".idea", ".vscode"
    );

    private Integer maxFileSizeKB = 1024;
    private Integer scanDepth = 10;

    // ========== 缓存配置 ==========
    private Boolean cacheEnabled = true;
    private String cacheType = "file";
    private Integer cacheTtlHours = 24;
    private Integer cacheMaxSize = 1000;
    private String cacheDirectory = ".ai-reviewer/cache";

    // ========== 工作目录配置 ==========
    private Path workingDirectory = Paths.get(System.getProperty("java.io.tmpdir"), "ai-reviewer");

    // ========== S3 存储配置 ==========
    private String s3Region = "us-east-1";
    private String s3BucketName;
    private String s3AccessKeyId;
    private String s3SecretAccessKey;
    private Integer s3MaxConcurrency = 10;
    private Integer s3ConnectTimeout = 30000;
    private Integer s3ReadTimeout = 60000;
    private Integer s3MaxRetries = 3;
    private Integer s3RetryDelay = 1000;
    private Boolean s3UseAccelerateEndpoint = false;
    private Boolean s3UsePathStyleAccess = false;
    private String s3Endpoint;

    // ========== 黑客松配置 ==========
    private Integer hackathonCloneTimeout = 300;
    private Integer hackathonAnalysisTimeout = 600;

    /**
     * 验证配置
     */
    public void validate() {
        if (aiProvider == null || aiProvider.isBlank()) {
            throw new ConfigurationException("AI Provider 未配置");
        }

        // API Key 验证：Bedrock 使用 IAM 角色，不需要 API Key
        if (!"bedrock".equalsIgnoreCase(aiProvider)) {
            if (aiApiKey == null || aiApiKey.isBlank()) {
                throw new ConfigurationException("AI API Key 未配置。请设置环境变量 AI_API_KEY 或在 config.yaml 中配置");
            }
        }

        if (aiMaxTokens != null && aiMaxTokens <= 0) {
            throw new ConfigurationException("AI Max Tokens 必须大于 0");
        }

        if (aiTemperature != null && (aiTemperature < 0 || aiTemperature > 2)) {
            throw new ConfigurationException("AI Temperature 必须在 0-2 之间");
        }

        // Bedrock 特定验证
        if ("bedrock".equalsIgnoreCase(aiProvider)) {
            if (awsRegion == null || awsRegion.isBlank()) {
                throw new ConfigurationException("AWS Region 未配置（Bedrock 必需）");
            }
            // Bedrock 使用 IAM 角色认证，不需要 API Key
            // 但需要确保 model 已配置
            if (aiModel == null || aiModel.isBlank()) {
                throw new ConfigurationException("AI Model 未配置（Bedrock 必需）");
            }
        }
    }

    /**
     * 获取 AI 服务配置
     */
    public AIServiceConfig getAIServiceConfig() {
        return new AIServiceConfig(
                aiProvider,
                aiApiKey,
                aiBaseUrl,
                aiModel,
                aiMaxTokens,
                aiTemperature,
                aiMaxRetries,
                aiRetryDelayMillis,
                aiConnectTimeoutMillis,
                aiReadTimeoutMillis,
                aiMaxConcurrency,
                awsRegion,
                awsAccessKeyId,
                awsSecretAccessKey
        );
    }

    /**
     * AI 服务配置记录
     */
    public record AIServiceConfig(
            String provider,
            String apiKey,
            String baseUrl,
            String model,
            Integer maxTokens,
            Double temperature,
            Integer maxRetries,
            Integer retryDelayMillis,
            Integer connectTimeoutMillis,
            Integer readTimeoutMillis,
            Integer maxConcurrency,
            String awsRegion,
            String awsAccessKeyId,
            String awsSecretAccessKey
    ) {}

    /**
     * 配置异常
     */
    public static class ConfigurationException extends RuntimeException {
        public ConfigurationException(String message) {
            super(message);
        }

        public ConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

