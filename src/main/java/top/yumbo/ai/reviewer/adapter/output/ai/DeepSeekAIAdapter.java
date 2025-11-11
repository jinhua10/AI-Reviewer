package top.yumbo.ai.reviewer.adapter.output.ai;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import top.yumbo.ai.reviewer.application.port.output.AIServicePort;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DeepSeek AI服务适配器
 * 实现AIServicePort接口，适配DeepSeek API
 */
@Slf4j
public class DeepSeekAIAdapter implements AIServicePort {

    private final OkHttpClient httpClient;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final int maxTokens;
    private final double temperature;

    private final ExecutorService executorService;
    private final Semaphore concurrencyLimiter;
    private final AtomicInteger activeRequests = new AtomicInteger(0);

    private final int maxRetries;
    private final int retryDelayMillis;

    public DeepSeekAIAdapter(AIServiceConfig config) {
        this.apiKey = config.apiKey();
        this.baseUrl = config.baseUrl() != null ? config.baseUrl() : "https://api.deepseek.com/v1";
        this.model = config.model() != null ? config.model() : "deepseek-chat";
        this.maxTokens = config.maxTokens() > 0 ? config.maxTokens() : 4000;
        this.temperature = config.temperature() >= 0 ? config.temperature() : 0.3;
        this.maxRetries = config.maxRetries() >= 0 ? config.maxRetries() : 3;
        this.retryDelayMillis = config.retryDelayMillis() > 0 ? config.retryDelayMillis() : 1000;

        int maxConcurrency = config.maxConcurrency() > 0 ? config.maxConcurrency() : 3;
        int connectTimeout = config.connectTimeoutMillis() > 0 ? config.connectTimeoutMillis() : 30000;
        int readTimeout = config.readTimeoutMillis() > 0 ? config.readTimeoutMillis() : 60000;

        // 配置HTTP客户端
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
                .writeTimeout(15000, TimeUnit.MILLISECONDS)
                .build();

        // 创建线程池
        int threadPoolSize = Math.max(2, Runtime.getRuntime().availableProcessors());
        this.executorService = Executors.newFixedThreadPool(threadPoolSize,
                new ThreadFactory() {
                    private final AtomicInteger counter = new AtomicInteger(0);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "deepseek-ai-" + counter.incrementAndGet());
                        t.setDaemon(true);
                        return t;
                    }
                });

        // 并发限制器
        this.concurrencyLimiter = new Semaphore(maxConcurrency);

        log.info("初始化DeepSeek AI适配器: model={}, baseUrl={}, maxConcurrency={}",
                model, baseUrl, maxConcurrency);
    }

    @Override
    public String analyze(String prompt) {
        try {
            return analyzeAsync(prompt).get(60, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("同步分析失败", e);
            throw new RuntimeException("AI分析失败: " + e.getMessage(), e);
        }
    }

    @Override
    public CompletableFuture<String> analyzeAsync(String prompt) {
        // 验证输入
        if (prompt == null) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("提示词不能为空(null)"));
        }
        if (prompt.trim().isEmpty()) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("提示词不能为空字符串"));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                concurrencyLimiter.acquire();
                activeRequests.incrementAndGet();

                log.debug("开始AI分析，提示词长度: {}, 活跃请求: {}",
                        prompt.length(), activeRequests.get());

                String result = doAnalyzeWithRetry(prompt);
                log.debug("AI分析完成");
                return result;

            } catch (Exception e) {
                log.error("异步AI分析失败", e);
                throw new CompletionException(e);
            } finally {
                activeRequests.decrementAndGet();
                concurrencyLimiter.release();
            }
        }, executorService);
    }

    @Override
    public CompletableFuture<String[]> analyzeBatchAsync(String[] prompts) {
        CompletableFuture<String>[] futures = new CompletableFuture[prompts.length];
        for (int i = 0; i < prompts.length; i++) {
            futures[i] = analyzeAsync(prompts[i]);
        }

        return CompletableFuture.allOf(futures)
                .thenApply(v -> {
                    String[] results = new String[prompts.length];
                    for (int i = 0; i < futures.length; i++) {
                        try {
                            results[i] = futures[i].join();
                        } catch (CompletionException e) {
                            log.error("批量分析第{}个请求失败", i, e.getCause());
                            results[i] = "分析失败: " + e.getCause().getMessage();
                        }
                    }
                    return results;
                });
    }

    @Override
    public String getProviderName() {
        return "DeepSeek";
    }

    @Override
    public boolean isAvailable() {
        try {
            // 简单的健康检查
            String testResult = analyze("test");
            return testResult != null && !testResult.isEmpty();
        } catch (Exception e) {
            log.warn("AI服务不可用: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public int getMaxConcurrency() {
        return concurrencyLimiter.availablePermits() + activeRequests.get();
    }

    @Override
    public void shutdown() {
        log.info("关闭DeepSeek AI适配器");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // 关闭HTTP客户端连接池
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }

    /**
     * 带重试的分析
     */
    private String doAnalyzeWithRetry(String prompt) {
        Exception lastException = null;

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                return doAnalyze(prompt);
            } catch (Exception e) {
                lastException = e;
                log.warn("AI调用失败，尝试次数: {}/{}", attempt + 1, maxRetries, e);

                if (attempt < maxRetries - 1) {
                    try {
                        Thread.sleep(retryDelayMillis * (attempt + 1)); // 指数退避
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        throw new RuntimeException("AI调用失败，已重试" + maxRetries + "次", lastException);
    }

    /**
     * 执行实际的AI分析
     */
    private String doAnalyze(String prompt) throws IOException {
        String requestBody = buildRequestBody(prompt);
        String response = makeApiCall(requestBody);
        return parseResponse(response);
    }

    /**
     * 构建请求体
     */
    private String buildRequestBody(String prompt) {
        JSONObject json = new JSONObject();
        json.put("model", model);
        json.put("messages", new Object[]{
                new JSONObject()
                        .fluentPut("role", "user")
                        .fluentPut("content", prompt)
        });
        json.put("max_tokens", maxTokens);
        json.put("temperature", temperature);
        json.put("stream", false);

        return json.toJSONString();
    }

    /**
     * 调用API
     */
    private String makeApiCall(String requestBody) throws IOException {
        RequestBody body = RequestBody.create(
                requestBody,
                MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API调用失败: " + response.code() + " " + response.message());
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new IOException("响应体为空");
            }

            return responseBody.string();
        }
    }

    /**
     * 解析响应
     */
    private String parseResponse(String response) {
        try {
            JSONObject json = JSON.parseObject(response);
            return json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
        } catch (Exception e) {
            log.error("解析AI响应失败: {}", response, e);
            throw new RuntimeException("解析AI响应失败", e);
        }
    }

    /**
     * AI服务配置
     */
    public record AIServiceConfig(
            String apiKey,
            String baseUrl,
            String model,
            int maxTokens,
            double temperature,
            int maxConcurrency,
            int maxRetries,
            int retryDelayMillis,
            int connectTimeoutMillis,
            int readTimeoutMillis
    ) {}
}

