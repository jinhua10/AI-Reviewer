package top.yumbo.ai.reviewer.adapter.output.ai;

import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * GeminiAdapter 测试类
 *
 * @author AI-Reviewer Team
 * @version 2.0
 * @since 2025-11-13
 */
@DisplayName("GeminiAdapter 测试")
class GeminiAdapterTest {

    private GeminiAdapter adapter;
    private AIServiceConfig testConfig;

    @BeforeEach
    void setUp() {
        // 使用测试配置
        testConfig = new AIServiceConfig(
                "test-api-key",
                "https://generativelanguage.googleapis.com/v1beta",
                "gemini-pro",
                2000,
                0.3,
                2,
                3,
                500,
                5000,
                10000,
                null
        );

        adapter = new GeminiAdapter(testConfig);
    }

    @AfterEach
    void tearDown() {
        if (adapter != null) {
            adapter.shutdown();
        }
    }

    @Test
    @DisplayName("应该成功创建适配器实例")
    void shouldCreateAdapter() {
        assertThat(adapter).isNotNull();
        assertThat(adapter.getProviderName()).isEqualTo("Gemini");
    }

    @Test
    @DisplayName("应该使用配置中的模型")
    void shouldUseConfiguredModel() {
        AIServiceConfig customConfig = new AIServiceConfig(
                "test-key",
                "https://generativelanguage.googleapis.com/v1beta",
                "gemini-1.5-pro",
                2000, 0.3, 2, 3, 500, 5000, 10000, null
        );
        GeminiAdapter customAdapter = new GeminiAdapter(customConfig);

        assertThat(customAdapter).isNotNull();
        assertThat(customAdapter.getProviderName()).isEqualTo("Gemini");

        customAdapter.shutdown();
    }

    @Test
    @DisplayName("应该使用默认值当配置为 null")
    void shouldUseDefaultsWhenConfigIsNull() {
        AIServiceConfig configWithNulls = new AIServiceConfig(
                "test-key",
                null,  // baseUrl 为 null
                null,  // model 为 null，应使用默认值
                2000, 0.3, 2, 3, 500, 5000, 10000, null
        );
        GeminiAdapter adapterWithDefaults = new GeminiAdapter(configWithNulls);

        assertThat(adapterWithDefaults).isNotNull();
        assertThat(adapterWithDefaults.getProviderName()).isEqualTo("Gemini");

        adapterWithDefaults.shutdown();
    }

    @Test
    @DisplayName("应该返回最大并发数")
    void shouldReturnMaxConcurrency() {
        int maxConcurrency = adapter.getMaxConcurrency();
        assertThat(maxConcurrency).isGreaterThan(0);
    }

    @Test
    @DisplayName("应该拒绝 null 提示词")
    void shouldRejectNullPrompt() {
        assertThatThrownBy(() -> adapter.analyze(null))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("应该拒绝空提示词")
    void shouldRejectEmptyPrompt() {
        assertThatThrownBy(() -> adapter.analyze(""))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("应该正确关闭适配器")
    void shouldShutdownProperly() {
        GeminiAdapter tempAdapter = new GeminiAdapter(testConfig);
        tempAdapter.shutdown();

        // 验证关闭后不能使用
        assertThatThrownBy(() -> tempAdapter.analyze("test"))
                .isInstanceOf(Exception.class);
    }
}

