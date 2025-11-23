package top.yumbo.ai.rag.example.application.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.yumbo.ai.rag.example.llm.LLMClient;
import top.yumbo.ai.rag.example.llm.MockLLMClient;
import top.yumbo.ai.rag.example.llm.OpenAILLMClient;

/**
 * LLM 客户端配置
 *
 * 支持多种 LLM 提供商：
 * - mock: 模拟客户端（默认，用于测试）
 * - openai: OpenAI (GPT-4o, GPT-4 Turbo, GPT-3.5 等)
 * - deepseek: DeepSeek
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
@Configuration
public class LLMConfiguration {

    private final KnowledgeQAProperties properties;

    public LLMConfiguration(KnowledgeQAProperties properties) {
        this.properties = properties;
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
        log.info("🤖 创建 OpenAI LLM 客户端");

        String apiKey = resolveEnvVariable(properties.getLlm().getApiKey());
        String model = properties.getLlm().getModel();
        String apiUrl = properties.getLlm().getApiUrl();

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("⚠️  未配置 OpenAI API Key");
            log.warn("💡 提示: 设置环境变量 OPENAI_API_KEY 或 AI_API_KEY");
            log.warn("💡 将使用 Mock 客户端");
            return new MockLLMClient();
        }

        log.info("   - 模型: {}", model);

        return new OpenAILLMClient(apiKey, model, apiUrl);
    }

    /**
     * Mock LLM 客户端（默认实现）
     */
    @Bean
    @ConditionalOnMissingBean
    public LLMClient llmClient() {
        log.info("🤖 创建 Mock LLM 客户端");
        log.info("   💡 提示：这是模拟客户端，返回固定回答");
        log.info("   💡 如需使用 OpenAI，请配置:");
        log.info("      knowledge.qa.llm.provider=openai");
        log.info("      export OPENAI_API_KEY=your-key");
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

