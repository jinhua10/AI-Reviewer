package top.yumbo.ai.reviewer.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import top.yumbo.ai.reviewer.config.Config;
import top.yumbo.ai.reviewer.exception.AnalysisException;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 异步DeepSeek AI服务实现
 */
@Slf4j
public class AsyncDeepseekAIService implements AsyncAIService {

    private static final String DEFAULT_BASE_URL = "https://api.deepseek.com/v1";
    private static final String DEFAULT_MODEL = "deepseek-chat";
    private static final int DEFAULT_MAX_TOKENS = 4000;
    private static final double DEFAULT_TEMPERATURE = 0.3;
    private static final int DEFAULT_MAX_CONCURRENCY = 3;

    private final OkHttpClient httpClient;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final int maxTokens;
    private final double temperature;

    // 异步处理相关
    private final ExecutorService executorService;
    private final Semaphore concurrencyLimiter;
    private final AtomicInteger activeRequests = new AtomicInteger(0);

    public AsyncDeepseekAIService(Config.AIServiceConfig config) {
        this.apiKey = config.getApiKey();
        this.baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : DEFAULT_BASE_URL;
        this.model = config.getModel() != null ? config.getModel() : DEFAULT_MODEL;
        this.maxTokens = config.getMaxTokens() > 0 ? config.getMaxTokens() : DEFAULT_MAX_TOKENS;
        this.temperature = config.getTemperature() >= 0 ? config.getTemperature() : DEFAULT_TEMPERATURE;

        // 配置HTTP客户端
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)  // 增加读取超时
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        // 创建线程池
        int threadPoolSize = Math.max(2, Runtime.getRuntime().availableProcessors());
        this.executorService = Executors.newFixedThreadPool(threadPoolSize,
                new ThreadFactory() {
                    private final AtomicInteger counter = new AtomicInteger(0);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "ai-analyzer-" + counter.incrementAndGet());
                        t.setDaemon(true);
                        return t;
                    }
                });

        // 并发限制器
        this.concurrencyLimiter = new Semaphore(DEFAULT_MAX_CONCURRENCY);

        log.info("初始化异步DeepSeek AI服务: model={}, baseUrl={}, maxConcurrency={}",
                model, baseUrl, DEFAULT_MAX_CONCURRENCY);
    }

    @Override
    public String analyze(String prompt) throws AnalysisException {
        try {
            return analyzeAsync(prompt).get(60, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new AnalysisException("AI分析超时或失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String[] analyzeBatch(String[] prompts) throws AnalysisException {
        try {
            return analyzeBatchAsync(prompts).get(120, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new AnalysisException("批量AI分析超时或失败: " + e.getMessage(), e);
        }
    }

    @Override
    public CompletableFuture<String> analyzeAsync(String prompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                concurrencyLimiter.acquire();
                activeRequests.incrementAndGet();

                log.debug("开始异步AI分析，提示词长度: {}, 活跃请求: {}",
                         prompt.length(), activeRequests.get());

                String result = doAnalyze(prompt);
                log.debug("异步AI分析完成");
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
        // 创建所有异步任务
        CompletableFuture<String>[] futures = new CompletableFuture[prompts.length];
        for (int i = 0; i < prompts.length; i++) {
            futures[i] = analyzeAsync(prompts[i]);
        }

        // 合并所有结果
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
    public int getMaxConcurrency() {
        return concurrencyLimiter.availablePermits() + activeRequests.get();
    }

    @Override
    public void setMaxConcurrency(int maxConcurrency) {
        // 注意：这里是简化实现，实际应该重新创建Semaphore
        log.warn("动态调整并发限制需要在服务重启后生效");
    }

    @Override
    public String getProviderName() {
        return "AsyncDeepSeek";
    }

    @Override
    public boolean isAvailable() {
        try {
            analyze("Hello").length();
            return true;
        } catch (Exception e) {
            log.warn("AI服务不可用: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 执行实际的AI分析
     */
    private String doAnalyze(String prompt) throws AnalysisException {
        try {
            String requestBody = buildRequestBody(prompt);
            String response = makeApiCall(requestBody);
            return parseResponse(response);

        } catch (IOException e) {
            throw new AnalysisException(AnalysisException.ErrorType.AI_SERVICE_ERROR,
                    "AI服务调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建请求体
     */
    private String buildRequestBody(String prompt) {
        JSONObject request = new JSONObject();
        request.put("model", model);
        request.put("messages", JSON.toJSON(buildMessages(prompt)));
        request.put("max_tokens", maxTokens);
        request.put("temperature", temperature);
        request.put("stream", false);

        return request.toJSONString();
    }

    /**
     * 构建消息列表
     */
    private Object[] buildMessages(String prompt) {
        return new Object[] {
            JSON.toJSON(new JSONObject()
                .fluentPut("role", "user")
                .fluentPut("content", prompt))
        };
    }

    /**
     * 调用API
     */
    private String makeApiCall(String requestBody) throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API调用失败: " + response.code() + " " + response.message());
            }

            ResponseBody body = response.body();
            return body != null ? body.string() : "";
        }
    }

    /**
     * 解析响应
     */
    private String parseResponse(String response) throws AnalysisException {
        try {
            JSONObject jsonResponse = JSON.parseObject(response);

            // 检查错误
            if (jsonResponse.containsKey("error")) {
                JSONObject error = jsonResponse.getJSONObject("error");
                throw new AnalysisException("AI服务返回错误: " + error.getString("message"));
            }

            // 提取内容
            if (jsonResponse.containsKey("choices")) {
                var choices = jsonResponse.getJSONArray("choices");
                if (!choices.isEmpty()) {
                    JSONObject choice = choices.getJSONObject(0);
                    JSONObject message = choice.getJSONObject("message");
                    return message.getString("content");
                }
            }

            throw new AnalysisException("AI服务返回格式错误");

        } catch (Exception e) {
            log.error("解析AI响应失败: {}", response, e);
            throw new AnalysisException("解析AI响应失败: " + e.getMessage(), e);
        }
    }

    /**
     * 关闭资源
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
