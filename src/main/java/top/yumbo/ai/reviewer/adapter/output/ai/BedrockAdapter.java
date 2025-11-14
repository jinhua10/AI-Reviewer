package top.yumbo.ai.reviewer.adapter.output.ai;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import top.yumbo.ai.reviewer.application.port.output.AIServicePort;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AWS Bedrock AI服务适配器
 * 支持 Claude、Titan、Llama 等 Bedrock 模型
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-12
 */
@Slf4j
public class BedrockAdapter implements AIServicePort {

    private final BedrockRuntimeClient bedrockClient;
    private final String modelId;
    private final int maxTokens;
    private final double temperature;
    private final String region;

    private final ExecutorService executorService;
    private final Semaphore concurrencyLimiter;
    private final AtomicInteger activeRequests = new AtomicInteger(0);

    private final int maxRetries;
    private final int retryDelayMillis;

    /**
     * 构造函数
     *
     * @param config AI服务配置
     */
    public BedrockAdapter(AIServiceConfig config) {
        this.modelId = config.model() != null ? config.model() : "anthropic.claude-v2";
        this.maxTokens = config.maxTokens() > 0 ? config.maxTokens() : 4000;
        this.temperature = config.temperature() >= 0 ? config.temperature() : 0.3;
        this.region = config.region() != null ? config.region() : "us-east-1";
        this.maxRetries = config.maxRetries() >= 0 ? config.maxRetries() : 3;
        this.retryDelayMillis = config.retryDelayMillis() > 0 ? config.retryDelayMillis() : 1000;

        int maxConcurrency = config.maxConcurrency() > 0 ? config.maxConcurrency() : 3;

        // 初始化 Bedrock 客户端
        var clientBuilder = BedrockRuntimeClient.builder()
                .region(Region.of(this.region));

        // 如果提供了 Access Key 和 Secret Key，使用静态凭证
        if (config.apiKey() != null && !config.apiKey().isEmpty()) {
            String[] credentials = config.apiKey().split(":");
            if (credentials.length == 2) {
                AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(
                        credentials[0], // Access Key ID
                        credentials[1]  // Secret Access Key
                );
                clientBuilder.credentialsProvider(StaticCredentialsProvider.create(awsCredentials));
                log.info("使用提供的 AWS 凭证");
            } else {
                log.info("使用默认 AWS 凭证链");
            }
        } else {
            log.info("使用默认 AWS 凭证链");
        }

        this.bedrockClient = clientBuilder.build();

        // 配置线程池
        this.executorService = Executors.newFixedThreadPool(
                maxConcurrency,
                new ThreadFactory() {
                    private final AtomicInteger threadNumber = new AtomicInteger(1);

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "bedrock-analyzer-" + threadNumber.getAndIncrement());
                        t.setDaemon(true);
                        return t;
                    }
                }
        );

        this.concurrencyLimiter = new Semaphore(maxConcurrency);

        log.info("AWS Bedrock 适配器初始化完成 - 模型: {}, 区域: {}, 最大并发: {}",
                modelId, region, maxConcurrency);
    }

    @Override
    public String analyze(String prompt) {
        try {
            return analyzeWithRetry(prompt, 0);
        } catch (Exception e) {
            log.error("Bedrock 分析失败: {}", e.getMessage(), e);
            throw new RuntimeException("AWS Bedrock 分析失败: " + e.getMessage(), e);
        }
    }

    @Override
    public CompletableFuture<String> analyzeAsync(String prompt) {
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
                throw new RuntimeException("分析被中断", e);
            } catch (Exception e) {
                log.error("Bedrock 异步分析失败: {}", e.getMessage(), e);
                throw new RuntimeException("AWS Bedrock 异步分析失败: " + e.getMessage(), e);
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

    /**
     * 带重试的分析
     */
    private String analyzeWithRetry(String prompt, int retryCount) {
        try {
            return invokeModel(prompt);
        } catch (Exception e) {
            if (retryCount < maxRetries) {
                log.warn("分析失败，第 {} 次重试 - 错误: {}", retryCount + 1, e.getMessage());
                try {
                    Thread.sleep(retryDelayMillis * (retryCount + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("重试等待被中断", ie);
                }
                return analyzeWithRetry(prompt, retryCount + 1);
            } else {
                log.error("达到最大重试次数 ({}), 分析失败", maxRetries);
                throw new RuntimeException("AWS Bedrock 分析失败，已重试 " + maxRetries + " 次", e);
            }
        }
    }

    /**
     * 调用 Bedrock 模型
     */
    private String invokeModel(String prompt) {
        try {
            // 构建请求体（根据不同模型格式会有所不同）
            String requestBody = buildRequestBody(prompt);

            log.debug("调用 Bedrock 模型 - Model ID: {}, Region: {}", modelId, region);
            log.debug("请求体: {}", requestBody);

            // 调用模型
            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(modelId)
                    .body(SdkBytes.fromString(requestBody, StandardCharsets.UTF_8))
                    .build();

            InvokeModelResponse response = bedrockClient.invokeModel(request);

            // 解析响应
            String responseBody = response.body().asUtf8String();
            log.debug("响应体: {}", responseBody);
            return parseResponse(responseBody);

        } catch (Exception e) {
            log.error("调用 Bedrock 模型失败: {}", e.getMessage(), e);
            throw new RuntimeException("Bedrock 模型调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建请求体（根据模型类型）
     */
    private String buildRequestBody(String prompt) {
        JSONObject requestBody = new JSONObject();

        // 支持 ARN 格式的 model ID（例如：arn:aws:bedrock:us-east-1:xxx:inference-profile/us.anthropic.claude-xxx）
        if (modelId.contains("anthropic.claude") || modelId.startsWith("anthropic.claude") ||
            modelId.contains("claude-3") || modelId.contains("claude-sonnet") || modelId.contains("claude-haiku")) {

            // 检测是否为 Claude 3+ 模型（需要使用 Messages API）
            boolean isClaude3Plus = modelId.contains("claude-3") ||
                                   modelId.contains("claude-sonnet") ||
                                   modelId.contains("claude-haiku") ||
                                   modelId.contains("claude-opus");

            if (isClaude3Plus) {
                // Claude 3+ Messages API 格式
                JSONObject message = new JSONObject();
                message.put("role", "user");
                message.put("content", prompt);

                requestBody.put("anthropic_version", "bedrock-2023-05-31");
                requestBody.put("max_tokens", maxTokens);
                requestBody.put("messages", new Object[]{message});
                // Claude 3+ 只能使用 temperature 或 top_p，不能同时使用
                requestBody.put("temperature", temperature);
                // 不添加 top_p

            } else {
                // Claude 2 及以下版本（旧的文本补全格式）
                requestBody.put("prompt", "\n\nHuman: " + prompt + "\n\nAssistant:");
                requestBody.put("max_tokens_to_sample", maxTokens);
                requestBody.put("temperature", temperature);
                requestBody.put("top_p", 0.9);
                requestBody.put("stop_sequences", new String[]{"\n\nHuman:"});
            }

        } else if (modelId.contains("amazon.titan") || modelId.startsWith("amazon.titan")) {
            // Titan 模型格式
            JSONObject textGenerationConfig = new JSONObject();
            textGenerationConfig.put("maxTokenCount", maxTokens);
            textGenerationConfig.put("temperature", temperature);
            textGenerationConfig.put("topP", 0.9);

            requestBody.put("inputText", prompt);
            requestBody.put("textGenerationConfig", textGenerationConfig);

        } else if (modelId.contains("meta.llama") || modelId.startsWith("meta.llama")) {
            // Llama 模型格式
            requestBody.put("prompt", prompt);
            requestBody.put("max_gen_len", maxTokens);
            requestBody.put("temperature", temperature);
            requestBody.put("top_p", 0.9);

        } else if (modelId.contains("cohere.command") || modelId.startsWith("cohere.command")) {
            // Cohere 模型格式
            requestBody.put("prompt", prompt);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", temperature);
            requestBody.put("p", 0.9);

        } else if (modelId.contains("ai21.j2") || modelId.startsWith("ai21.j2")) {
            // AI21 Jurassic 模型格式
            requestBody.put("prompt", prompt);
            requestBody.put("maxTokens", maxTokens);
            requestBody.put("temperature", temperature);
            requestBody.put("topP", 0.9);

        } else {
            // 默认格式（通用）
            requestBody.put("prompt", prompt);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", temperature);
        }

        return requestBody.toJSONString();
    }

    /**
     * 解析响应（根据模型类型）
     */
    private String parseResponse(String responseBody) {
        try {
            JSONObject response = JSON.parseObject(responseBody);

            // 支持 ARN 格式的 model ID
            if (modelId.contains("anthropic.claude") || modelId.startsWith("anthropic.claude") ||
                modelId.contains("claude-3") || modelId.contains("claude-sonnet") || modelId.contains("claude-haiku")) {

                // 检测是否为 Claude 3+ 模型
                boolean isClaude3Plus = modelId.contains("claude-3") ||
                                       modelId.contains("claude-sonnet") ||
                                       modelId.contains("claude-haiku") ||
                                       modelId.contains("claude-opus");

                if (isClaude3Plus) {
                    // Claude 3+ Messages API 响应格式
                    if (response.containsKey("content")) {
                        var content = response.getJSONArray("content");
                        if (content != null && content.size() > 0) {
                            return content.getJSONObject(0).getString("text");
                        }
                    }
                    // 降级处理 - 如果没有 content 字段，尝试 completion
                    if (response.containsKey("completion")) {
                        return response.getString("completion");
                    }
                    // 如果都没有，返回原始响应
                    log.warn("Claude 3+ 响应格式无法识别，返回原始响应");
                    return responseBody;
                } else {
                    // Claude 2 响应格式
                    return response.getString("completion");
                }

            } else if (modelId.contains("amazon.titan") || modelId.startsWith("amazon.titan")) {
                // Titan 响应格式
                return response.getJSONArray("results")
                        .getJSONObject(0)
                        .getString("outputText");

            } else if (modelId.contains("meta.llama") || modelId.startsWith("meta.llama")) {
                // Llama 响应格式
                return response.getString("generation");

            } else if (modelId.contains("cohere.command") || modelId.startsWith("cohere.command")) {
                // Cohere 响应格式
                return response.getJSONArray("generations")
                        .getJSONObject(0)
                        .getString("text");

            } else if (modelId.contains("ai21.j2") || modelId.startsWith("ai21.j2")) {
                // AI21 响应格式
                return response.getJSONArray("completions")
                        .getJSONObject(0)
                        .getJSONObject("data")
                        .getString("text");

            } else {
                // 尝试通用字段
                if (response.containsKey("completion")) {
                    return response.getString("completion");
                } else if (response.containsKey("text")) {
                    return response.getString("text");
                } else if (response.containsKey("output")) {
                    return response.getString("output");
                } else {
                    log.warn("无法识别响应格式，返回原始响应");
                    return responseBody;
                }
            }

        } catch (Exception e) {
            log.error("解析响应失败: {}", e.getMessage(), e);
            return responseBody; // 返回原始响应
        }
    }

    @Override
    public String getProviderName() {
        return "AWS Bedrock (" + modelId + ")";
    }

    @Override
    public boolean isAvailable() {
        try {
            // 简单的可用性检查
            return bedrockClient != null;
        } catch (Exception e) {
            log.error("Bedrock 服务不可用: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public int getMaxConcurrency() {
        return concurrencyLimiter.availablePermits();
    }

    @Override
    public void shutdown() {
        log.info("关闭 AWS Bedrock 适配器");

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.error("线程池未能正常关闭");
                }
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        if (bedrockClient != null) {
            bedrockClient.close();
        }

        log.info("AWS Bedrock 适配器已关闭");
    }

    /**
     * 获取活跃请求数
     */
    public int getActiveRequests() {
        return activeRequests.get();
    }

    /**
     * 获取模型 ID
     */
    public String getModelId() {
        return modelId;
    }

    /**
     * 获取区域
     */
    public String getRegion() {
        return region;
    }
}

