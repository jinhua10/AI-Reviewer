package top.yumbo.ai.reviewer.infrastructure.factory;

import org.junit.jupiter.api.*;
import top.yumbo.ai.reviewer.adapter.ai.decorator.LoggingAIServiceDecorator;
import top.yumbo.ai.reviewer.application.port.output.AIServicePort;
import top.yumbo.ai.reviewer.infrastructure.config.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AIServiceFactory 测试类
 *
 * @author AI-Reviewer Team
 * @version 2.0
 * @since 2025-11-13
 */
@DisplayName("AIServiceFactory 测试")
class AIServiceFactoryTest {

    @Test
    @DisplayName("应该创建 DeepSeek 适配器")
    void shouldCreateDeepSeekAdapter() {
        Configuration.AIServiceConfig deepseekConfig = new Configuration.AIServiceConfig(
                "deepseek", "test-key", null, null,
                2000, 0.3, 3, 500, 5000, 10000, 3,
                null, null, null
        );

        AIServicePort adapter = AIServiceFactory.create(deepseekConfig);

        assertThat(adapter).isNotNull();
        assertThat(adapter).isInstanceOf(LoggingAIServiceDecorator.class);
        assertThat(adapter.getProviderName()).contains("DeepSeek");

        adapter.shutdown();
    }

    @Test
    @DisplayName("应该创建 OpenAI 适配器")
    void shouldCreateOpenAIAdapter() {
        Configuration.AIServiceConfig openaiConfig = new Configuration.AIServiceConfig(
                "openai", "test-key", null, null,
                2000, 0.3, 3, 500, 5000, 10000, 3,
                null, null, null
        );

        AIServicePort adapter = AIServiceFactory.create(openaiConfig);

        assertThat(adapter).isNotNull();
        assertThat(adapter).isInstanceOf(LoggingAIServiceDecorator.class);
        assertThat(adapter.getProviderName()).contains("OpenAI");

        adapter.shutdown();
    }

    @Test
    @DisplayName("应该创建 Claude 适配器")
    void shouldCreateClaudeAdapter() {
        Configuration.AIServiceConfig claudeConfig = new Configuration.AIServiceConfig(
                "claude", "test-key", null, null,
                2000, 0.3, 3, 500, 5000, 10000, 3,
                null, null, null
        );

        AIServicePort adapter = AIServiceFactory.create(claudeConfig);

        assertThat(adapter).isNotNull();
        assertThat(adapter).isInstanceOf(LoggingAIServiceDecorator.class);
        assertThat(adapter.getProviderName()).contains("Claude");

        adapter.shutdown();
    }

    @Test
    @DisplayName("应该创建 Gemini 适配器")
    void shouldCreateGeminiAdapter() {
        Configuration.AIServiceConfig geminiConfig = new Configuration.AIServiceConfig(
                "gemini", "test-key", null, null,
                2000, 0.3, 3, 500, 5000, 10000, 3,
                null, null, null
        );

        AIServicePort adapter = AIServiceFactory.create(geminiConfig);

        assertThat(adapter).isNotNull();
        assertThat(adapter).isInstanceOf(LoggingAIServiceDecorator.class);
        assertThat(adapter.getProviderName()).contains("Gemini");

        adapter.shutdown();
    }

    @Test
    @DisplayName("应该创建 Bedrock 适配器")
    void shouldCreateBedrockAdapter() {
        Configuration.AIServiceConfig bedrockConfig = new Configuration.AIServiceConfig(
                "bedrock", "test-key", null, null,
                2000, 0.3, 3, 500, 5000, 10000, 3,
                "us-east-1", null, null
        );

        AIServicePort adapter = AIServiceFactory.create(bedrockConfig);

        assertThat(adapter).isNotNull();
        assertThat(adapter).isInstanceOf(LoggingAIServiceDecorator.class);
        assertThat(adapter.getProviderName()).contains("Bedrock");

        adapter.shutdown();
    }

    @Test
    @DisplayName("应该支持 provider 别名")
    void shouldSupportProviderAliases() {
        // 测试 anthropic 别名
        Configuration.AIServiceConfig anthropicConfig = new Configuration.AIServiceConfig(
                "anthropic", "test-key", null, null,
                2000, 0.3, 3, 500, 5000, 10000, 3,
                null, null, null
        );
        AIServicePort anthropicAdapter = AIServiceFactory.create(anthropicConfig);
        assertThat(anthropicAdapter).isInstanceOf(LoggingAIServiceDecorator.class);
        assertThat(anthropicAdapter.getProviderName()).contains("Claude");
        anthropicAdapter.shutdown();

        // 测试 google 别名
        Configuration.AIServiceConfig googleConfig = new Configuration.AIServiceConfig(
                "google", "test-key", null, null,
                2000, 0.3, 3, 500, 5000, 10000, 3,
                null, null, null
        );
        AIServicePort googleAdapter = AIServiceFactory.create(googleConfig);
        assertThat(googleAdapter).isInstanceOf(LoggingAIServiceDecorator.class);
        assertThat(googleAdapter.getProviderName()).contains("Gemini");
        googleAdapter.shutdown();

        // 测试 aws 别名
        Configuration.AIServiceConfig awsConfig = new Configuration.AIServiceConfig(
                "aws", "test-key", null, null,
                2000, 0.3, 3, 500, 5000, 10000, 3,
                "us-east-1", null, null
        );
        AIServicePort awsAdapter = AIServiceFactory.create(awsConfig);
        assertThat(awsAdapter).isInstanceOf(LoggingAIServiceDecorator.class);
        assertThat(awsAdapter.getProviderName()).contains("Bedrock");
        awsAdapter.shutdown();
    }

    @Test
    @DisplayName("应该在不支持的 provider 时抛出异常")
    void shouldThrowExceptionForUnsupportedProvider() {
        Configuration.AIServiceConfig invalidConfig = new Configuration.AIServiceConfig(
                "invalid-provider", "test-key", null, null,
                2000, 0.3, 3, 500, 5000, 10000, 3,
                null, null, null
        );

        assertThatThrownBy(() -> AIServiceFactory.create(invalidConfig))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不支持的 AI 服务提供商");
    }

    @Test
    @DisplayName("应该正确映射配置到适配器配置")
    void shouldMapConfigurationCorrectly() {
        Configuration.AIServiceConfig customConfig = new Configuration.AIServiceConfig(
                "openai",
                "custom-api-key",
                "https://custom.openai.com",
                "gpt-4-turbo",
                8000,
                0.7,
                5,
                1000,
                10000,
                20000,
                10,
                null,
                null,
                null
        );

        AIServicePort adapter = AIServiceFactory.create(customConfig);

        assertThat(adapter).isNotNull();
        assertThat(adapter).isInstanceOf(LoggingAIServiceDecorator.class);
        assertThat(adapter.getProviderName()).contains("OpenAI");

        adapter.shutdown();
    }

    @Test
    @DisplayName("应该使用默认 Base URL")
    void shouldUseDefaultBaseUrl() {
        Configuration.AIServiceConfig configWithoutUrl = new Configuration.AIServiceConfig(
                "openai",
                "test-key",
                null,  // 没有提供 baseUrl
                null,
                2000, 0.3, 3, 500, 5000, 10000, 3,
                null, null, null
        );

        AIServicePort adapter = AIServiceFactory.create(configWithoutUrl);

        assertThat(adapter).isNotNull();
        assertThat(adapter).isInstanceOf(LoggingAIServiceDecorator.class);
        assertThat(adapter.getProviderName()).contains("OpenAI");

        adapter.shutdown();
    }

    @Test
    @DisplayName("应该使用默认 Model")
    void shouldUseDefaultModel() {
        Configuration.AIServiceConfig configWithoutModel = new Configuration.AIServiceConfig(
                "deepseek",
                "test-key",
                null,
                null,  // 没有提供 model
                2000, 0.3, 3, 500, 5000, 10000, 3,
                null, null, null
        );

        AIServicePort adapter = AIServiceFactory.create(configWithoutModel);

        assertThat(adapter).isNotNull();
        assertThat(adapter).isInstanceOf(LoggingAIServiceDecorator.class);
        assertThat(adapter.getProviderName()).contains("DeepSeek");

        adapter.shutdown();
    }
}

