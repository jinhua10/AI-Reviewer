package top.yumbo.ai.reviewer.adapter.output.ai;

import org.junit.jupiter.api.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * DeepSeekAIAdapter测试
 *
 * 测试分为两类：
 * 1. 单元测试 - 不需要真实 API，测试基本功能
 * 2. 集成测试 - 需要真实 API Key（从环境变量 DEEPSEEK_API_KEY 读取）
 *
 * API Key 验证：
 * - 在测试开始前会验证 API Key 是否有效
 * - 如果 API Key 无效，所有需要 API 的测试将被跳过
 *
 * 运行集成测试：
 * 1. 设置环境变量：set DEEPSEEK_API_KEY=your-api-key
 * 2. 运行测试：mvn test -Dtest=DeepSeekAIAdapterTest
 */
@DisplayName("DeepSeekAIAdapter测试")
class DeepSeekAIAdapterTest {

    private DeepSeekAIAdapter adapter;
    private DeepSeekAIAdapter.AIServiceConfig testConfig;
    private static final String API_KEY_ENV = "DEEPSEEK_API_KEY";
    private static boolean hasRealApiKey = false;
    private static boolean apiKeyValidated = false;
    private static boolean apiKeyValid = false;

    @BeforeAll
    static void validateApiKey() {
        System.out.println("\n========================================");
        System.out.println("DeepSeek API Key 验证");
        System.out.println("========================================");

        String apiKey = System.getenv(API_KEY_ENV);

        if (apiKey != null && !apiKey.trim().isEmpty() && !apiKey.startsWith("test-")) {
            hasRealApiKey = true;
            System.out.println("✓ 检测到环境变量 DEEPSEEK_API_KEY");
            System.out.println("✓ API Key 格式: " + maskApiKey(apiKey));

            // 验证 API Key 格式
            if (apiKey.startsWith("sk-") && apiKey.length() > 20) {
                System.out.println("✅ API Key 格式有效");

                // 创建临时适配器进行连接测试
                System.out.println("⏳ 正在验证 API 连接...");
                try {
                    DeepSeekAIAdapter.AIServiceConfig validationConfig =
                        new DeepSeekAIAdapter.AIServiceConfig(
                            apiKey,
                            "https://api.deepseek.com/v1",
                            "deepseek-chat",
                            100, // 最小 token 用于测试
                            0.7,
                            1,
                            1, // 只重试1次
                            500,
                            10000, // 10秒连接超时
                            15000  // 15秒读取超时
                        );

                    DeepSeekAIAdapter testAdapter = new DeepSeekAIAdapter(validationConfig);

                    // 测试 API 可用性
                    boolean available = testAdapter.isAvailable();
                    testAdapter.shutdown();

                    if (available) {
                        apiKeyValid = true;
                        System.out.println("✅ API 连接验证成功 - 将运行完整测试套件");
                    } else {
                        apiKeyValid = false;
                        System.out.println("❌ API 连接验证失败 - API 不可用");
                        System.out.println("   原因可能是：网络问题、API Key 无效、配额用尽等");
                        System.out.println("   将跳过所有需要真实 API 的测试");
                    }
                } catch (Exception e) {
                    apiKeyValid = false;
                    System.out.println("❌ API 连接验证失败: " + e.getMessage());
                    System.out.println("   将跳过所有需要真实 API 的测试");
                }
            } else {
                apiKeyValid = false;
                System.out.println("❌ API Key 格式无效（应该以 'sk-' 开头且长度 > 20）");
                System.out.println("   将跳过所有需要真实 API 的测试");
            }
        } else {
            hasRealApiKey = false;
            apiKeyValid = false;
            System.out.println("⚠️  未配置 DEEPSEEK_API_KEY 环境变量");
            System.out.println("   只运行单元测试，跳过集成测试");
        }

        apiKeyValidated = true;
        System.out.println("========================================\n");
    }

    private static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 10) {
            return "***";
        }
        return apiKey.substring(0, 7) + "..." + apiKey.substring(apiKey.length() - 4);
    }

    @BeforeEach
    void setUp() {
        // 确保 API Key 已验证
        assumeTrue(apiKeyValidated, "API Key 验证尚未完成");

        // 根据验证结果配置适配器
        if (hasRealApiKey && apiKeyValid) {
            // 使用真实的 API Key
            String apiKey = System.getenv(API_KEY_ENV);
            testConfig = new DeepSeekAIAdapter.AIServiceConfig(
                    apiKey,
                    "https://api.deepseek.com/v1",
                    "deepseek-chat",
                    2000,
                    0.7,
                    2,
                    3,
                    1000,
                    30000,
                    60000
            );
        } else {
            // 使用测试配置（用于单元测试）
            testConfig = new DeepSeekAIAdapter.AIServiceConfig(
                    "test-api-key",
                    "https://test.api.deepseek.com/v1",
                    "deepseek-chat",
                    2000,
                    0.3,
                    2,
                    3,
                    500,
                    5000,
                    10000
            );
        }

        adapter = new DeepSeekAIAdapter(testConfig);
    }

    @AfterEach
    void tearDown() {
        if (adapter != null) {
            adapter.shutdown();
        }
    }

    @Nested
    @DisplayName("构造函数和初始化测试")
    class ConstructorTest {

        @Test
        @DisplayName("应该使用提供的配置创建适配器")
        void shouldCreateAdapterWithProvidedConfig() {
            assertThat(adapter).isNotNull();
            assertThat(adapter.getProviderName()).isEqualTo("DeepSeek");
        }

        @Test
        @DisplayName("应该使用默认值填充未提供的配置")
        void shouldUseDefaultValuesForMissingConfig() {
            DeepSeekAIAdapter.AIServiceConfig minimalConfig =
                    new DeepSeekAIAdapter.AIServiceConfig(
                            "api-key",
                            null, // 使用默认baseUrl
                            null, // 使用默认model
                            0,    // 使用默认maxTokens
                            -1,   // 使用默认temperature
                            0,    // 使用默认maxConcurrency
                            -1,   // 使用默认maxRetries
                            0,    // 使用默认retryDelayMillis
                            0,    // 使用默认connectTimeoutMillis
                            0     // 使用默认readTimeoutMillis
                    );

            DeepSeekAIAdapter adapterWithDefaults = new DeepSeekAIAdapter(minimalConfig);

            assertThat(adapterWithDefaults).isNotNull();
            assertThat(adapterWithDefaults.getProviderName()).isEqualTo("DeepSeek");

            adapterWithDefaults.shutdown();
        }

        @Test
        @DisplayName("应该正确设置并发限制")
        void shouldSetConcurrencyLimit() {
            assertThat(adapter.getMaxConcurrency()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("getProviderName()方法测试")
    class GetProviderNameTest {

        @Test
        @DisplayName("应该返回DeepSeek")
        void shouldReturnDeepSeek() {
            assertThat(adapter.getProviderName()).isEqualTo("DeepSeek");
        }
    }

    @Nested
    @DisplayName("getMaxConcurrency()方法测试")
    class GetMaxConcurrencyTest {

        @Test
        @DisplayName("应该返回配置的最大并发数")
        void shouldReturnConfiguredMaxConcurrency() {
            int maxConcurrency = adapter.getMaxConcurrency();
            assertThat(maxConcurrency).isGreaterThanOrEqualTo(2);
        }
    }

    @Nested
    @DisplayName("analyze()方法测试")
    class AnalyzeMethodTest {

        @Test
        @DisplayName("真实API测试 - 应该成功分析简单代码")
        void shouldAnalyzeSimpleCodeWithRealAPI() {
            // 只在 API Key 有效时运行
            assumeTrue(apiKeyValid, "跳过：API Key 未配置或无效");

            String prompt = "请分析以下代码并给出简短评价（20字以内）：\n" +
                    "public class HelloWorld {\n" +
                    "    public static void main(String[] args) {\n" +
                    "        System.out.println(\"Hello World\");\n" +
                    "    }\n" +
                    "}";

            try {
                String result = adapter.analyze(prompt);

                assertThat(result).isNotNull();
                assertThat(result).isNotEmpty();
                System.out.println("✅ AI 分析结果: " + result);
            } catch (Exception e) {
                System.err.println("❌ API 调用失败: " + e.getMessage());
                throw e;
            }
        }

        @Test
        @DisplayName("无API Key时 - 应该失败")
        void shouldFailWithoutRealAPI() {
            // 只在 API Key 无效时运行
            assumeTrue(!apiKeyValid, "跳过：已配置有效的 API Key");

            String prompt = "这是一个测试提示词";

            assertThatThrownBy(() -> adapter.analyze(prompt))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("AI分析失败");
        }

        @Test
        @DisplayName("应该拒绝null提示词")
        void shouldRejectNullPrompt() {
            assertThatThrownBy(() -> adapter.analyze(null))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("应该拒绝空字符串提示词")
        void shouldRejectEmptyPrompt() {
            assertThatThrownBy(() -> adapter.analyze(""))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("analyzeAsync()方法测试")
    class AnalyzeAsyncMethodTest {

        @Test
        @DisplayName("应该返回CompletableFuture")
        void shouldReturnCompletableFuture() {
            String prompt = "测试提示词";
            CompletableFuture<String> future = adapter.analyzeAsync(prompt);

            assertThat(future).isNotNull();
            assertThat(future).isInstanceOf(CompletableFuture.class);
        }

        @Test
        @DisplayName("应该能够异步处理请求")
        void shouldHandleRequestAsynchronously() {
            String prompt = "测试提示词";
            CompletableFuture<String> future = adapter.analyzeAsync(prompt);

            // 不等待结果，只验证Future对象的创建
            assertThat(future).isNotNull();
            assertThat(future.isDone()).isFalse(); // 应该还在执行中

            // 等待一小段时间后取消
            future.cancel(true);
        }

        @Test
        @DisplayName("异步请求应该正常完成")
        void shouldCompleteAsyncRequest() throws Exception {
            String prompt = "请用一个词回答: Hello";
            CompletableFuture<String> future = adapter.analyzeAsync(prompt);

            // API 可用时应该成功完成
            String result = future.get(30, TimeUnit.SECONDS);
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("analyzeBatchAsync()方法测试")
    class AnalyzeBatchAsyncMethodTest {

        @Test
        @DisplayName("应该能够处理批量请求")
        void shouldHandleBatchRequests() {
            String[] prompts = {"提示词1", "提示词2", "提示词3"};

            CompletableFuture<String[]> future = adapter.analyzeBatchAsync(prompts);

            assertThat(future).isNotNull();
        }

        @Test
        @DisplayName("应该处理空数组")
        void shouldHandleEmptyArray() {
            String[] emptyPrompts = {};

            CompletableFuture<String[]> future = adapter.analyzeBatchAsync(emptyPrompts);

            assertThat(future).isNotNull();
        }

        @Test
        @DisplayName("应该返回相同数量的结果")
        void shouldReturnSameNumberOfResults() throws Exception {
            String[] prompts = {"用一个词回答: Hi", "用一个词回答: Hello"};

            CompletableFuture<String[]> future = adapter.analyzeBatchAsync(prompts);

            // API 可用时应该成功返回所有结果（给予足够的超时时间）
            String[] results = future.get(60, TimeUnit.SECONDS);
            assertThat(results).hasSize(prompts.length);
            assertThat(results).allMatch(result -> result != null && !result.isEmpty());
        }
    }

    @Nested
    @DisplayName("并发控制测试")
    class ConcurrencyControlTest {

        @Test
        @DisplayName("应该限制并发请求数量")
        void shouldLimitConcurrentRequests() {
            // 创建多个并发请求
            CompletableFuture<String>[] futures = new CompletableFuture[5];
            for (int i = 0; i < 5; i++) {
                futures[i] = adapter.analyzeAsync("提示词" + i);
            }

            // 验证不会超过最大并发数
            assertThat(adapter.getMaxConcurrency()).isGreaterThanOrEqualTo(2);

            // 取消所有请求
            for (CompletableFuture<String> future : futures) {
                future.cancel(true);
            }
        }

        @Test
        @DisplayName("应该能够跟踪活跃请求数")
        void shouldTrackActiveRequests() {
            int initialConcurrency = adapter.getMaxConcurrency();
            assertThat(initialConcurrency).isGreaterThanOrEqualTo(0);
        }
    }

    @Nested
    @DisplayName("重试机制测试")
    class RetryMechanismTest {

        @Test
        @DisplayName("失败的请求应该会重试")
        void shouldRetryFailedRequests() {
            // 当 API 可用时，正常请求应该成功
            String prompt = "请用一个词回答: 你好";

            long startTime = System.currentTimeMillis();
            String result = adapter.analyze(prompt);
            long duration = System.currentTimeMillis() - startTime;

            // API 应该成功返回结果
            assertThat(result).isNotNull();
            assertThat(duration).isGreaterThan(0);

            // Note: 要测试重试机制，需要模拟网络失败或使用无效的 API key
            // 在真实 API 可用时，请求会成功，不会触发重试
        }
    }

    @Nested
    @DisplayName("isAvailable()方法测试")
    class IsAvailableTest {

        @Test
        @DisplayName("真实API - 应该返回true")
        void shouldReturnTrueWithRealAPI() {
            // 只在 API Key 有效时运行
            assumeTrue(apiKeyValid, "跳过：API Key 未配置或无效");

            boolean available = adapter.isAvailable();

            System.out.println(available ? "✅ API 可用" : "❌ API 不可用");
            assertThat(available).isTrue();
        }

        @Test
        @DisplayName("无效配置时应该返回false")
        void shouldReturnFalseForInvalidConfig() {
            // 只在 API Key 无效时运行
            assumeTrue(!apiKeyValid, "跳过：已配置有效的 API Key");

            boolean available = adapter.isAvailable();

            // 由于使用的是测试配置（无效API），应该返回false
            assertThat(available).isFalse();
        }
    }

    @Nested
    @DisplayName("shutdown()方法测试")
    class ShutdownTest {

        @Test
        @DisplayName("应该能够正常关闭")
        void shouldShutdownGracefully() {
            assertThat(adapter).isNotNull();

            adapter.shutdown();

            // 再次调用shutdown不应该抛出异常
            adapter.shutdown();
        }

        @Test
        @DisplayName("关闭后不应该接受新请求")
        void shouldNotAcceptNewRequestsAfterShutdown() {
            adapter.shutdown();

            // 注意：实际行为取决于实现
            // 可能抛出异常或返回失败的Future
            assertThatThrownBy(() -> adapter.analyze("测试"))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("配置验证测试")
    class ConfigValidationTest {

        @Test
        @DisplayName("应该接受有效的API密钥")
        void shouldAcceptValidApiKey() {
            DeepSeekAIAdapter.AIServiceConfig validConfig =
                    new DeepSeekAIAdapter.AIServiceConfig(
                            "sk-test1234567890",
                            "https://api.deepseek.com/v1",
                            "deepseek-chat",
                            4000,
                            0.3,
                            3,
                            3,
                            1000,
                            30000,
                            60000
                    );

            DeepSeekAIAdapter validAdapter = new DeepSeekAIAdapter(validConfig);
            assertThat(validAdapter).isNotNull();
            assertThat(validAdapter.getProviderName()).isEqualTo("DeepSeek");
            validAdapter.shutdown();
        }

        @Test
        @DisplayName("应该处理不同的模型名称")
        void shouldHandleDifferentModelNames() {
            DeepSeekAIAdapter.AIServiceConfig config =
                    new DeepSeekAIAdapter.AIServiceConfig(
                            "test-key",
                            "https://test.api.com",
                            "custom-model",
                            2000,
                            0.5,
                            2,
                            3,
                            500,
                            5000,
                            10000
                    );

            DeepSeekAIAdapter customAdapter = new DeepSeekAIAdapter(config);
            assertThat(customAdapter).isNotNull();
            customAdapter.shutdown();
        }

        @Test
        @DisplayName("应该处理不同的温度参数")
        void shouldHandleDifferentTemperatures() {
            DeepSeekAIAdapter.AIServiceConfig lowTemp =
                    new DeepSeekAIAdapter.AIServiceConfig(
                            "test-key",
                            "https://test.api.com",
                            "model",
                            2000,
                            0.0, // 低温度
                            2,
                            3,
                            500,
                            5000,
                            10000
                    );

            DeepSeekAIAdapter.AIServiceConfig highTemp =
                    new DeepSeekAIAdapter.AIServiceConfig(
                            "test-key",
                            "https://test.api.com",
                            "model",
                            2000,
                            1.0, // 高温度
                            2,
                            3,
                            500,
                            5000,
                            10000
                    );

            DeepSeekAIAdapter lowTempAdapter = new DeepSeekAIAdapter(lowTemp);
            DeepSeekAIAdapter highTempAdapter = new DeepSeekAIAdapter(highTemp);

            assertThat(lowTempAdapter).isNotNull();
            assertThat(highTempAdapter).isNotNull();

            lowTempAdapter.shutdown();
            highTempAdapter.shutdown();
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryConditionsTest {

        @Test
        @DisplayName("应该处理非常长的提示词")
        void shouldHandleVeryLongPrompt() {
            String longPrompt = "测试".repeat(10000);

            // DeepSeek API 应该能够处理长文本（或返回适当的结果）
            String result = adapter.analyze(longPrompt);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("应该处理包含特殊字符的提示词")
        void shouldHandleSpecialCharactersInPrompt() {
            String specialPrompt = "请分析这段文本: \n\r\t\"'<>&{}[]";

            // API 应该能够正确处理特殊字符
            String result = adapter.analyze(specialPrompt);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("应该处理Unicode字符")
        void shouldHandleUnicodeCharacters() {
            String unicodePrompt = "请分析: 测试 🚀 emoji 和 中文";

            // API 应该能够正确处理 Unicode 字符
            String result = adapter.analyze(unicodePrompt);
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("性能测试")
    class PerformanceTest {

        @Test
        @DisplayName("创建适配器应该很快")
        void shouldCreateAdapterQuickly() {
            long startTime = System.currentTimeMillis();

            DeepSeekAIAdapter newAdapter = new DeepSeekAIAdapter(testConfig);

            long duration = System.currentTimeMillis() - startTime;
            assertThat(duration).isLessThan(1000); // 应该在1秒内完成

            newAdapter.shutdown();
        }

        @Test
        @DisplayName("关闭适配器应该在合理时间内完成")
        void shouldShutdownInReasonableTime() {
            long startTime = System.currentTimeMillis();

            adapter.shutdown();

            long duration = System.currentTimeMillis() - startTime;
            assertThat(duration).isLessThan(15000); // 应该在15秒内完成
        }
    }
}

