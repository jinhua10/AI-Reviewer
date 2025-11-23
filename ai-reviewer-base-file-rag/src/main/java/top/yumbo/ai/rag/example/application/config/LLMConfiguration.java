package top.yumbo.ai.rag.example.application.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.yumbo.ai.rag.example.llm.DeepSeekLLMClient;
import top.yumbo.ai.rag.example.llm.LLMClient;
import top.yumbo.ai.rag.example.llm.MockLLMClient;
import top.yumbo.ai.rag.example.llm.OpenAILLMClient;

/**
 * LLM 客户端配置
 *
 * 支持多种 LLM 提供商：
 * - deepseek: DeepSeek（默认，从环境变量 AI_API_KEY 读取）
 * - openai: OpenAI（从环境变量 OPENAI_API_KEY 读取）
 * - mock: Mock 模式（测试用，返回固定回答）
 *
 * @author AI Reviewer Team
 * @since 2025-11-23
 */
@Slf4j
@Configuration
public class LLMConfiguration {

    private final KnowledgeQAProperties properties;

    public LLMConfiguration(KnowledgeQAProperties properties) {
        this.properties = properties;
    }

    /**
     * DeepSeek LLM 客户端
     */
    @Bean
    @ConditionalOnProperty(
        prefix = "knowledge.qa.llm",
        name = "provider",
        havingValue = "deepseek",
        matchIfMissing = true  // 默认使用 DeepSeek
    )
    @ConditionalOnMissingBean
    public LLMClient deepSeekLLMClient() {
        String apiKey = resolveEnvVariable(properties.getLlm().getApiKey());

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("⚠️  未设置环境变量 AI_API_KEY");
            log.warn("💡 提示：如需使用 DeepSeek API，请配置:");
            log.warn("      export AI_API_KEY=your-deepseek-key");
            log.warn("💡 将降级使用 Mock 模式");
            return new MockLLMClient();
        }

        String model = properties.getLlm().getModel();
        return new DeepSeekLLMClient(apiKey, model);
    }

    /**
     * OpenAI LLM 客户端
     */
    @Bean
    @ConditionalOnProperty(
        prefix = "knowledge.qa.llm",
        name = "provider",
        havingValue = "openai"
    )
    @ConditionalOnMissingBean
    public LLMClient openAILLMClient() {
        String apiKey = resolveEnvVariable(properties.getLlm().getApiKey());
        String model = properties.getLlm().getModel();
        String apiUrl = properties.getLlm().getApiUrl();

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("⚠️  未配置 OpenAI API Key");
            log.warn("💡 提示: 设置环境变量 OPENAI_API_KEY 或 AI_API_KEY");
            log.warn("💡 将降级使用 Mock 模式");
            return new MockLLMClient();
        }

        return new OpenAILLMClient(apiKey, model, apiUrl);
    }

    /**
     * Mock LLM 客户端（测试用）
     */
    @Bean
    @ConditionalOnProperty(
        prefix = "knowledge.qa.llm",
        name = "provider",
        havingValue = "mock"
    )
    @ConditionalOnMissingBean
    public LLMClient mockLLMClient() {
        log.info("🤖 创建 Mock LLM 客户端（仅用于测试）");
        log.info("   ⚠️  Mock 模式将返回固定的模拟回答");
        log.info("   💡 如需使用真实 LLM，请配置:");
        log.info("      - DeepSeek: export AI_API_KEY=your-deepseek-key");
        log.info("      - OpenAI: knowledge.qa.llm.provider=openai 并 export OPENAI_API_KEY=your-key");
        return new MockLLMClient();
    }

    /**
     * 解析环境变量占位符
     */
    private String resolveEnvVariable(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        // 处理 ${VAR:default} 格式
        if (value.startsWith("${") && value.endsWith("}")) {
            String content = value.substring(2, value.length() - 1);
            String[] parts = content.split(":", 2);
            String envVar = parts[0];
            String defaultValue = parts.length > 1 ? parts[1] : "";

            String envValue = System.getenv(envVar);
            return envValue != null && !envValue.isEmpty() ? envValue : defaultValue;
        }

        return value;
    }
}

