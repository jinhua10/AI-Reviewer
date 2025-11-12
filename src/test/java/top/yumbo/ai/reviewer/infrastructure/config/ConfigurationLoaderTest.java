package top.yumbo.ai.reviewer.infrastructure.config;

import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ConfigurationLoader 测试类
 *
 * @author AI-Reviewer Team
 * @version 2.0
 * @since 2025-11-13
 */
@DisplayName("ConfigurationLoader 测试")
class ConfigurationLoaderTest {

    @BeforeEach
    void setUp() {
        // 清除环境变量影响
        System.clearProperty("ai.provider");
        System.clearProperty("ai.apiKey");
        System.clearProperty("ai.model");
    }

    @Test
    @DisplayName("应该从环境变量加载配置")
    void shouldLoadFromEnvironmentVariables() {
        // 设置环境变量（通过系统属性模拟）
        System.setProperty("ai.provider", "openai");
        System.setProperty("ai.apiKey", "test-key-from-env");
        System.setProperty("ai.model", "gpt-4");

        Configuration config = ConfigurationLoader.load();

        assertThat(config.getAiProvider()).isEqualTo("openai");
        assertThat(config.getAiApiKey()).isEqualTo("test-key-from-env");
        assertThat(config.getAiModel()).isEqualTo("gpt-4");
    }

    @Test
    @DisplayName("应该使用默认值当配置未提供")
    void shouldUseDefaultValues() {
        // 提供最小配置（只有 API Key）
        System.setProperty("ai.apiKey", "test-key");

        Configuration config = ConfigurationLoader.load();

        assertThat(config.getAiProvider()).isEqualTo("deepseek"); // 默认值
        assertThat(config.getAiApiKey()).isEqualTo("test-key");
        assertThat(config.getAiMaxTokens()).isEqualTo(4000); // 默认值
        assertThat(config.getAiTemperature()).isEqualTo(0.3); // 默认值
    }

    @Test
    @DisplayName("应该在缺少 API Key 时抛出异常")
    void shouldThrowExceptionWhenApiKeyMissing() {
        // 不设置 API Key
        assertThatThrownBy(() -> ConfigurationLoader.load())
                .isInstanceOf(Configuration.ConfigurationException.class)
                .hasMessageContaining("API Key");
    }

    @Test
    @DisplayName("应该验证配置的完整性")
    void shouldValidateConfiguration() {
        System.setProperty("ai.apiKey", "test-key");
        System.setProperty("ai.provider", "deepseek");

        Configuration config = ConfigurationLoader.load();

        // 配置应该通过验证
        assertThat(config.getAiApiKey()).isNotNull();
        assertThat(config.getAiProvider()).isNotNull();
    }

    @Test
    @DisplayName("应该正确处理系统属性优先级")
    void shouldRespectSystemPropertyPriority() {
        // 系统属性应该优先于其他配置源
        System.setProperty("ai.provider", "claude");
        System.setProperty("ai.apiKey", "system-property-key");

        Configuration config = ConfigurationLoader.load();

        assertThat(config.getAiProvider()).isEqualTo("claude");
        assertThat(config.getAiApiKey()).isEqualTo("system-property-key");
    }

    @Test
    @DisplayName("应该创建有效的 AIServiceConfig")
    void shouldCreateValidAIServiceConfig() {
        System.setProperty("ai.apiKey", "test-key");
        System.setProperty("ai.provider", "openai");
        System.setProperty("ai.model", "gpt-4");

        Configuration config = ConfigurationLoader.load();
        Configuration.AIServiceConfig aiConfig = config.getAIServiceConfig();

        assertThat(aiConfig).isNotNull();
        assertThat(aiConfig.provider()).isEqualTo("openai");
        assertThat(aiConfig.apiKey()).isEqualTo("test-key");
        assertThat(aiConfig.model()).isEqualTo("gpt-4");
        assertThat(aiConfig.maxTokens()).isNotNull();
        assertThat(aiConfig.temperature()).isNotNull();
    }

    @Test
    @DisplayName("应该支持 AWS Bedrock 配置")
    void shouldSupportBedrockConfiguration() {
        System.setProperty("ai.apiKey", "test-key");
        System.setProperty("ai.provider", "bedrock");

        Configuration config = ConfigurationLoader.load();

        assertThat(config.getAiProvider()).isEqualTo("bedrock");
        assertThat(config.getAwsRegion()).isNotNull(); // 应有默认值
    }
}

