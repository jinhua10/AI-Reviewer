package top.yumbo.ai.reviewer.infrastructure.factory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.yumbo.ai.reviewer.adapter.ai.AIAdapterFactory;
import top.yumbo.ai.reviewer.adapter.ai.bedrock.BedrockAdapter;
import top.yumbo.ai.reviewer.adapter.ai.config.AIServiceConfig;
import top.yumbo.ai.reviewer.adapter.ai.decorator.LoggingAIServiceDecorator;
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
     * 根据Configuration创建 AI 服务（带日志装饰器）
     */
    public static AIServicePort create(Configuration configuration) {
        return create(configuration, true);
    }

    /**
     * 根据Configuration创建 AI 服务
     *
     * @param configuration 应用配置
     * @param enableLogging 是否启用日志装饰器
     * @return AI 服务端口实例
     */
    public static AIServicePort create(Configuration configuration, boolean enableLogging) {
        String provider = configuration.getAiProvider().toLowerCase();

        log.info("创建 AI 服务: provider={}, model={}, enableLogging={}",
                provider, configuration.getAiModel(), enableLogging);

        AIServiceConfig config = configuration.getAIServiceConfig();

        AIServicePort service = switch (provider) {
            case "deepseek" -> createDeepSeek(config);
            case "openai" -> createOpenAI(config);
            case "claude", "anthropic" -> createClaude(config);
            case "gemini", "google" -> createGemini(config);
            case "bedrock", "aws" -> createBedrock(config);
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
    private static AIServicePort createDeepSeek(AIServiceConfig config) {
        log.debug("初始化 DeepSeek AI 适配器");
        return AIAdapterFactory.createDeepSeek(config);
    }

    /**
     * 创建 OpenAI 服务
     */
    private static AIServicePort createOpenAI(AIServiceConfig config) {
        log.debug("初始化 OpenAI 适配器");
        return AIAdapterFactory.createOpenAI(config);
    }

    /**
     * 创建 Claude 服务
     */
    private static AIServicePort createClaude(AIServiceConfig config) {
        log.debug("初始化 Claude 适配器");
        return AIAdapterFactory.createClaude(config);
    }

    /**
     * 创建 Gemini 服务
     */
    private static AIServicePort createGemini(AIServiceConfig config) {
        log.debug("初始化 Gemini 适配器");
        return AIAdapterFactory.createGemini(config);
    }

    /**
     * 创建 AWS Bedrock 服务
     */
    private static AIServicePort createBedrock(AIServiceConfig config) {
        log.debug("初始化 AWS Bedrock 适配器: region={}", config.region());
        return new BedrockAdapter(config);
    }
}

