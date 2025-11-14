package top.yumbo.ai.reviewer.adapter.output.ai;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.reviewer.application.port.output.AIServicePort;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

/**
 * 基于 HTTP 的通用 AI 服务适配器
 * 支持 OpenAI、Claude、Gemini、DeepSeek 等兼容 OpenAI API 格式的服务
 *
 * @author AI-Reviewer Team
 * @version 2.0
 * @since 2025-11-14
 */
@Slf4j
public class HttpBasedAIAdapter implements AIServicePort {

    private final String providerName;
    private final String apiKey;
    private final String apiUrl;
    private final String model;
    private final int maxTokens;
    private final double temperature;
    private final HttpClient httpClient;
    private final ExecutorService executorService;
    private final Semaphore concurrencyLimiter;
    private final AtomicInteger activeRequests = new AtomicInteger(0);
    private final int maxRetries;
    private final int retryDelayMillis;

    // 不同提供商的请求/响应处理策略
    private final BiFunction<String, JSONObject, HttpRequest.Builder> requestBuilder;
    private final java.util.function.Function<JSONObject, String> responseParser;

    /**
     * 构造函数
     *
     * @param providerName 提供商名称（如 "OpenAI", "Claude", "Gemini", "DeepSeek"）
     * @param config AI服务配置
     * @param requestBuilder 请求构建器（处理不同API的请求格式差异）
     * @param responseParser 响应解析器（处理不同API的响应格式差异）
     */
    public HttpBasedAIAdapter(
            String providerName,
            AIServiceConfig config,
            BiFunction<String, JSONObject, HttpRequest.Builder> requestBuilder,
            java.util.function.Function<JSONObject, String> responseParser) {

        this.providerName = providerName;
        this.apiKey = config.apiKey();
        this.apiUrl = config.baseUrl();
        this.model = config.model();
        this.maxTokens = config.maxTokens() > 0 ? config.maxTokens() : 4000;
        this.temperature = config.temperature() >= 0 ? config.temperature() : 0.3;
        this.maxRetries = config.maxRetries() >= 0 ? config.maxRetries() : 3;
        this.retryDelayMillis = config.retryDelayMillis() > 0 ? config.retryDelayMillis() : 1000;

        int maxConcurrency = config.maxConcurrency() > 0 ? config.maxConcurrency() : 5;
        int connectTimeout = config.connectTimeoutMillis() > 0 ? config.connectTimeoutMillis() : 30000;
        int readTimeout = config.readTimeoutMillis() > 0 ? config.readTimeoutMillis() : 60000;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeout))
                .build();

        this.executorService = Executors.newFixedThreadPool(
                maxConcurrency,
                new ThreadFactory() {
                    private final AtomicInteger threadNumber = new AtomicInteger(1);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, providerName.toLowerCase() + "-analyzer-" + threadNumber.getAndIncrement());
                        t.setDaemon(true);
                        return t;
                    }
                }
        );

        this.concurrencyLimiter = new Semaphore(maxConcurrency);
        this.requestBuilder = requestBuilder;
        this.responseParser = responseParser;

        log.info("{} 适配器初始化完成 - 模型: {}, URL: {}, 最大并发: {}",
                providerName, model, apiUrl, maxConcurrency);
    }

    @Override
    public String analyze(String prompt) {
        try {
            return analyzeAsync(prompt).get();
        } catch (ExecutionException e) {
            throw new RuntimeException(providerName + " 分析失败: " + e.getCause().getMessage(), e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(providerName + " 分析被中断", e);
        }
    }

    @Override
    public CompletableFuture<String> analyzeAsync(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("提示词不能为空"));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                concurrencyLimiter.acquire();
                activeRequests.incrementAndGet();
                try {
                    return analyzeWithRetry(prompt, 0);
                } finally {
                    activeRequests.decrementAndGet();
                    concurrencyLimiter.release();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CompletionException("分析被中断", e);
            }
        }, executorService);
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<String[]> analyzeBatchAsync(String[] prompts) {
        CompletableFuture<String>[] futures = new CompletableFuture[prompts.length];
        for (int i = 0; i < prompts.length; i++) {
            futures[i] = analyzeAsync(prompts[i]);
        }

        return CompletableFuture.allOf(futures)
                .thenApply(v -> {
                    String[] results = new String[prompts.length];
                    for (int i = 0; i < prompts.length; i++) {
                        try {
                            results[i] = futures[i].join();
                        } catch (Exception e) {
                            log.error("批量分析第 {} 个任务失败", i, e);
                            results[i] = "分析失败: " + e.getMessage();
                        }
                    }
                    return results;
                });
    }

    @Override
    public String getProviderName() {
        return providerName + " " + model;
    }

    @Override
    public boolean isAvailable() {
        try {
            analyze("test");
            return true;
        } catch (Exception e) {
            log.debug("{} 服务不可用: {}", providerName, e.getMessage());
            return false;
        }
    }

    @Override
    public int getMaxConcurrency() {
        return concurrencyLimiter.availablePermits() + activeRequests.get();
    }

    @Override
    public void shutdown() {
        log.info("关闭 {} 适配器", providerName);
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 带重试的分析
     */
    private String analyzeWithRetry(String prompt, int retryCount) {
        try {
            return callAPI(prompt);
        } catch (Exception e) {
            if (retryCount < maxRetries) {
                log.warn("{} 调用失败，第 {} 次重试 - 错误: {}",
                        providerName, retryCount + 1, e.getMessage());
                try {
                    Thread.sleep((long) retryDelayMillis * (retryCount + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("重试等待被中断", ie);
                }
                return analyzeWithRetry(prompt, retryCount + 1);
            } else {
                log.error("{} 达到最大重试次数 ({}), 分析失败", providerName, maxRetries);
                throw new RuntimeException(providerName + " 分析失败，已重试 " + maxRetries + " 次", e);
            }
        }
    }

    /**
     * 调用 AI API
     */
    private String callAPI(String prompt) throws IOException, InterruptedException {
        // 构建请求体
        JSONObject requestBody = buildRequestBody(prompt);

        // 构建 HTTP 请求（使用提供商特定的构建器）
        HttpRequest.Builder builder = requestBuilder.apply(apiUrl, requestBody);
        HttpRequest request = builder
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toJSONString()))
                .build();

        // 发送请求
        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        // 检查响应状态
        validateResponse(response);

        // 解析响应
        JSONObject responseJson = JSON.parseObject(response.body());
        logTokenUsage(responseJson);

        // 使用提供商特定的解析器
        return responseParser.apply(responseJson);
    }

    /**
     * 构建请求体
     */
    private JSONObject buildRequestBody(String prompt) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model);
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);

        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", prompt);
        requestBody.put("messages", new Object[]{message});

        return requestBody;
    }

    /**
     * 验证响应状态
     */
    private void validateResponse(HttpResponse<String> response) {
        int statusCode = response.statusCode();
        if (statusCode == 429) {
            throw new RuntimeException(providerName + " 速率限制: 请求过于频繁");
        } else if (statusCode == 401 || statusCode == 403) {
            throw new RuntimeException(providerName + " 认证失败: API Key 无效");
        } else if (statusCode != 200) {
            throw new RuntimeException(providerName + " 请求失败: " + statusCode +
                    ", " + response.body());
        }
    }

    /**
     * 记录 Token 使用情况（仅 DEBUG 级别）
     */
    private void logTokenUsage(JSONObject responseJson) {
        if (!log.isDebugEnabled()) {
            return;
        }

        if (responseJson.containsKey("usage")) {
            JSONObject usage = responseJson.getJSONObject("usage");
            log.debug("{} Token 使用: {}", providerName, usage.toJSONString());
        }
    }
}

