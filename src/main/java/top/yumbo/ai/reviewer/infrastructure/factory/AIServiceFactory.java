package top.yumbo.ai.reviewer.infrastructure.factory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.yumbo.ai.reviewer.adapter.output.ai.*;
import top.yumbo.ai.reviewer.application.port.output.AIServicePort;
import top.yumbo.ai.reviewer.infrastructure.config.Configuration;

/**
 * AI 服务工厂
 * <p>
 * 根据配置创建对应的 AI 服务适配器
 *
 * @author AI-Reviewer Team
 * @version 2.0
 * @since 2025-11-13
 */
public class AIServiceFactory {

    private static final Logger log = LoggerFactory.getLogger(AIServiceFactory.class);

    /**
     * 根据配置创建 AI 服务（带日志装饰器）
     */
    public static AIServicePort create(Configuration.AIServiceConfig config) {
        return create(config, true);
    }

    /**
     * 根据配置创建 AI 服务
     *
     * @param config AI 服务配置
     * @param enableLogging 是否启用日志装饰器
     * @return AI 服务端口实例
     */
    public static AIServicePort create(Configuration.AIServiceConfig config, boolean enableLogging) {
        String provider = config.provider().toLowerCase();

        log.info("创建 AI 服务: provider={}, model={}, enableLogging={}",
                provider, config.model(), enableLogging);

        top.yumbo.ai.reviewer.adapter.output.ai.AIServiceConfig adapterConfig = mapToAdapterConfig(config);

        AIServicePort service = switch (provider) {
            case "deepseek" -> createDeepSeek(adapterConfig);
            case "openai" -> createOpenAI(adapterConfig);
            case "claude", "anthropic" -> createClaude(adapterConfig);
            case "gemini", "google" -> createGemini(adapterConfig);
            case "bedrock", "aws" -> createBedrock(adapterConfig);
            default -> throw new IllegalArgumentException(
                    "不支持的 AI 服务提供商: " + provider +
                    "。支持的提供商: deepseek, openai, claude, gemini, bedrock"
            );
        };

        // 使用装饰器模式添加日志功能
        if (enableLogging) {
            log.debug("为 AI 服务添加日志装饰器");
            service = new LoggingAIServiceDecorator(service);
        }

        return service;
    }

    /**
     * 创建 DeepSeek AI 服务
     */
    private static AIServicePort createDeepSeek(top.yumbo.ai.reviewer.adapter.output.ai.AIServiceConfig config) {
        log.debug("初始化 DeepSeek AI 适配器");
        return AIAdapterFactory.createDeepSeek(config);
    }

    /**
     * 创建 OpenAI 服务
     */
    private static AIServicePort createOpenAI(top.yumbo.ai.reviewer.adapter.output.ai.AIServiceConfig config) {
        log.debug("初始化 OpenAI 适配器");
        return AIAdapterFactory.createOpenAI(config);
    }

    /**
     * 创建 Claude 服务
     */
    private static AIServicePort createClaude(top.yumbo.ai.reviewer.adapter.output.ai.AIServiceConfig config) {
        log.debug("初始化 Claude 适配器");
        return AIAdapterFactory.createClaude(config);
    }

    /**
     * 创建 Gemini 服务
     */
    private static AIServicePort createGemini(top.yumbo.ai.reviewer.adapter.output.ai.AIServiceConfig config) {
        log.debug("初始化 Gemini 适配器");
        return AIAdapterFactory.createGemini(config);
    }

    /**
     * 创建 AWS Bedrock 服务
     */
    private static AIServicePort createBedrock(top.yumbo.ai.reviewer.adapter.output.ai.AIServiceConfig config) {
        log.debug("初始化 AWS Bedrock 适配器: region={}", config.region());
        return new BedrockAdapter(config);
    }

    /**
     * 将 Configuration.AIServiceConfig 映射到 AIServiceConfig
     */
    private static top.yumbo.ai.reviewer.adapter.output.ai.AIServiceConfig mapToAdapterConfig(Configuration.AIServiceConfig config) {
        // 确定 API Key（处理 Bedrock 的特殊情况）
        String apiKey = config.apiKey();
        if ("bedrock".equalsIgnoreCase(config.provider()) || "aws".equalsIgnoreCase(config.provider())) {
            // Bedrock 使用 AWS 凭证
            if (config.awsAccessKeyId() != null && config.awsSecretAccessKey() != null) {
                apiKey = config.awsAccessKeyId() + ":" + config.awsSecretAccessKey();
            }
        }

        // 确定 Base URL
        String baseUrl = config.baseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = getDefaultBaseUrl(config.provider());
        }

        // 确定 Model
        String model = config.model();
        if (model == null || model.isBlank()) {
            model = getDefaultModel(config.provider());
        }

        return new top.yumbo.ai.reviewer.adapter.output.ai.AIServiceConfig(
                apiKey,
                baseUrl,
                model,
                config.maxTokens() != null ? config.maxTokens() : 4000,
                config.temperature() != null ? config.temperature() : 0.3,
                config.maxConcurrency() != null ? config.maxConcurrency() : 3,
                config.maxRetries() != null ? config.maxRetries() : 3,
                config.retryDelayMillis() != null ? config.retryDelayMillis() : 1000,
                config.connectTimeoutMillis() != null ? config.connectTimeoutMillis() : 30000,
                config.readTimeoutMillis() != null ? config.readTimeoutMillis() : 60000,
                config.awsRegion()
        );
    }

    /**
     * 获取默认 Base URL
     */
    private static String getDefaultBaseUrl(String provider) {
        return switch (provider.toLowerCase()) {
            case "deepseek" -> "https://api.deepseek.com/v1/chat/completions";
            case "openai" -> "https://api.openai.com/v1/chat/completions";
            case "claude", "anthropic" -> "https://api.anthropic.com/v1/messages";
            case "gemini", "google" -> "https://generativelanguage.googleapis.com/v1beta";
            case "bedrock", "aws" -> null;  // Bedrock 不使用 Base URL
            default -> null;
        };
    }

    /**
     * 获取默认 Model
     */
    private static String getDefaultModel(String provider) {
        return switch (provider.toLowerCase()) {
            case "deepseek" -> "deepseek-chat";
            case "openai" -> "gpt-4";
            case "claude", "anthropic" -> "claude-3-sonnet-20240229";
            case "gemini", "google" -> "gemini-pro";
            case "bedrock", "aws" -> "anthropic.claude-3-sonnet-20240229-v1:0";
            default -> null;
        };
    }
}

