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
     * 支持 AWS Bedrock 平台上的所有主流模型
     */
    private String buildRequestBody(String prompt) {
        JSONObject requestBody = new JSONObject();

        // 提取实际的模型名称（处理 ARN 格式）
        String actualModelId = extractModelId(modelId);

        // Anthropic Claude 模型系列
        if (actualModelId.contains("anthropic.claude") || actualModelId.contains("claude-")) {

            // 检测是否为 Claude 3+ 模型（需要使用 Messages API）
            // 包括 Claude 3, Claude 4 及以上版本
            boolean isClaude3Plus = actualModelId.contains("claude-3") ||
                                   actualModelId.contains("claude-4") ||
                                   actualModelId.contains("claude-sonnet") ||
                                   actualModelId.contains("claude-haiku") ||
                                   actualModelId.contains("claude-opus");

            if (isClaude3Plus) {
                // Claude 3+ Messages API 格式
                JSONObject message = new JSONObject();
                message.put("role", "user");
                message.put("content", prompt);

                requestBody.put("anthropic_version", "bedrock-2023-05-31");
                requestBody.put("max_tokens", maxTokens);
                requestBody.put("messages", new Object[]{message});
                requestBody.put("temperature", temperature);

            } else {
                // Claude 2 及以下版本（旧的文本补全格式）
                requestBody.put("prompt", "\n\nHuman: " + prompt + "\n\nAssistant:");
                requestBody.put("max_tokens_to_sample", maxTokens);
                requestBody.put("temperature", temperature);
                requestBody.put("top_p", 0.9);
                requestBody.put("stop_sequences", new String[]{"\n\nHuman:"});
            }

        // Amazon Titan 模型系列
        } else if (actualModelId.contains("amazon.titan")) {
            JSONObject textGenerationConfig = new JSONObject();
            textGenerationConfig.put("maxTokenCount", maxTokens);
            textGenerationConfig.put("temperature", temperature);
            textGenerationConfig.put("topP", 0.9);

            requestBody.put("inputText", prompt);
            requestBody.put("textGenerationConfig", textGenerationConfig);

        // Amazon Nova 模型系列（新增）
        } else if (actualModelId.contains("amazon.nova")) {
            // Nova 使用 Messages API 格式（类似 Claude 3+）
            JSONObject message = new JSONObject();
            message.put("role", "user");

            // Nova 支持多模态，这里简化为文本
            JSONObject contentItem = new JSONObject();
            contentItem.put("text", prompt);
            message.put("content", new Object[]{contentItem});

            requestBody.put("messages", new Object[]{message});
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", temperature);

            // Nova 特定配置
            JSONObject inferenceConfig = new JSONObject();
            inferenceConfig.put("max_new_tokens", maxTokens);
            inferenceConfig.put("temperature", temperature);
            inferenceConfig.put("top_p", 0.9);
            requestBody.put("inferenceConfig", inferenceConfig);

        // Meta Llama 模型系列（包括 Llama 2, Llama 3, Llama 3.1, Llama 3.2）
        } else if (actualModelId.contains("meta.llama")) {
            // Llama 3+ 使用新的 Converse API 格式
            if (actualModelId.contains("llama3") || actualModelId.contains("llama-3")) {
                // Llama 3 系列使用 Messages 格式
                JSONObject message = new JSONObject();
                message.put("role", "user");
                message.put("content", prompt);

                requestBody.put("messages", new Object[]{message});
                requestBody.put("max_tokens", maxTokens);
                requestBody.put("temperature", temperature);
                requestBody.put("top_p", 0.9);
            } else {
                // Llama 2 使用旧格式
                requestBody.put("prompt", prompt);
                requestBody.put("max_gen_len", maxTokens);
                requestBody.put("temperature", temperature);
                requestBody.put("top_p", 0.9);
            }

        // Mistral AI 模型系列（新增）
        } else if (actualModelId.contains("mistral")) {
            // Mistral 使用标准的对话格式
            requestBody.put("prompt", "<s>[INST] " + prompt + " [/INST]");
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", temperature);
            requestBody.put("top_p", 0.9);
            requestBody.put("top_k", 50);

        // Cohere Command 模型系列
        } else if (actualModelId.contains("cohere.command")) {
            requestBody.put("prompt", prompt);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", temperature);
            requestBody.put("p", 0.9);
            requestBody.put("k", 0);
            requestBody.put("return_likelihoods", "NONE");

        // AI21 Jurassic 模型系列
        } else if (actualModelId.contains("ai21.j2") || actualModelId.contains("ai21.jamba")) {
            requestBody.put("prompt", prompt);
            requestBody.put("maxTokens", maxTokens);
            requestBody.put("temperature", temperature);
            requestBody.put("topP", 0.9);
            requestBody.put("stopSequences", new String[]{});
            requestBody.put("countPenalty", new JSONObject().fluentPut("scale", 0));
            requestBody.put("presencePenalty", new JSONObject().fluentPut("scale", 0));
            requestBody.put("frequencyPenalty", new JSONObject().fluentPut("scale", 0));

        // Stability AI 模型系列（新增 - 图像生成，但也支持文本）
        } else if (actualModelId.contains("stability")) {
            // 如果是文本到图像模型，需要特殊处理
            // 这里假设是文本生成场景
            requestBody.put("text_prompts", new Object[]{
                new JSONObject().fluentPut("text", prompt).fluentPut("weight", 1)
            });
            requestBody.put("cfg_scale", 7);
            requestBody.put("steps", 30);
            requestBody.put("seed", 0);

        } else {
            // 默认格式（通用，适用于未知模型）
            log.warn("使用默认请求格式，模型ID: {}", actualModelId);
            requestBody.put("prompt", prompt);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", temperature);
            requestBody.put("top_p", 0.9);
        }

        return requestBody.toJSONString();
    }

    /**
     * 从模型 ID 中提取实际的模型名称
     * 处理 ARN 格式：arn:aws:bedrock:region:account:inference-profile/model-id
     */
    private String extractModelId(String modelId) {
        if (modelId.contains("inference-profile/")) {
            // 提取 ARN 中的实际模型 ID
            return modelId.substring(modelId.indexOf("inference-profile/") + 18);
        } else if (modelId.contains("foundation-model/")) {
            // 提取基础模型 ID
            return modelId.substring(modelId.indexOf("foundation-model/") + 17);
        }
        return modelId;
    }

    /**
     * 解析响应（根据模型类型）
     * 支持 AWS Bedrock 平台上的所有主流模型
     */
    private String parseResponse(String responseBody) {
        try {
            JSONObject response = JSON.parseObject(responseBody);

            // 提取实际的模型名称（处理 ARN 格式）
            String actualModelId = extractModelId(modelId);

            // Anthropic Claude 模型系列
            if (actualModelId.contains("anthropic.claude") || actualModelId.contains("claude-")) {

                // 检测是否为 Claude 3+ 模型（包括 Claude 3, Claude 4 及以上版本）
                boolean isClaude3Plus = actualModelId.contains("claude-3") ||
                                       actualModelId.contains("claude-4") ||
                                       actualModelId.contains("claude-sonnet") ||
                                       actualModelId.contains("claude-haiku") ||
                                       actualModelId.contains("claude-opus");

                if (isClaude3Plus) {
                    // Claude 3+ Messages API 响应格式
                    if (response.containsKey("content")) {
                        var content = response.getJSONArray("content");
                        if (content != null && !content.isEmpty()) {
                            return content.getJSONObject(0).getString("text");
                        }
                    }
                    // 降级处理
                    if (response.containsKey("completion")) {
                        return response.getString("completion");
                    }
                    log.warn("Claude 3+ 响应格式无法识别，返回原始响应");
                    return responseBody;
                } else {
                    // Claude 2 响应格式
                    return response.getString("completion");
                }

            // Amazon Titan 模型系列
            } else if (actualModelId.contains("amazon.titan")) {
                return response.getJSONArray("results")
                        .getJSONObject(0)
                        .getString("outputText");

            // Amazon Nova 模型系列（新增）
            } else if (actualModelId.contains("amazon.nova")) {
                // Nova 使用 Messages 格式响应
                if (response.containsKey("output")) {
                    var output = response.getJSONObject("output");
                    if (output.containsKey("message")) {
                        var message = output.getJSONObject("message");
                        if (message.containsKey("content")) {
                            var content = message.getJSONArray("content");
                            if (content != null && !content.isEmpty()) {
                                var firstContent = content.getJSONObject(0);
                                if (firstContent.containsKey("text")) {
                                    return firstContent.getString("text");
                                }
                            }
                        }
                    }
                }
                // 降级处理
                if (response.containsKey("generation")) {
                    return response.getString("generation");
                }
                log.warn("Nova 响应格式无法识别，返回原始响应");
                return responseBody;

            // Meta Llama 模型系列
            } else if (actualModelId.contains("meta.llama")) {
                // Llama 3+ 使用新格式
                if (actualModelId.contains("llama3") || actualModelId.contains("llama-3")) {
                    // 尝试 Messages 格式响应
                    if (response.containsKey("generation")) {
                        return response.getString("generation");
                    }
                    if (response.containsKey("output")) {
                        return response.getString("output");
                    }
                }
                // Llama 2 使用旧格式
                if (response.containsKey("generation")) {
                    return response.getString("generation");
                }
                log.warn("Llama 响应格式无法识别，返回原始响应");
                return responseBody;

            // Mistral AI 模型系列（新增）
            } else if (actualModelId.contains("mistral")) {
                // Mistral 响应格式
                if (response.containsKey("outputs")) {
                    var outputs = response.getJSONArray("outputs");
                    if (outputs != null && !outputs.isEmpty()) {
                        var firstOutput = outputs.getJSONObject(0);
                        if (firstOutput.containsKey("text")) {
                            return firstOutput.getString("text");
                        }
                    }
                }
                // 备选格式
                if (response.containsKey("completion")) {
                    return response.getString("completion");
                }
                log.warn("Mistral 响应格式无法识别，返回原始响应");
                return responseBody;

            // Cohere Command 模型系列
            } else if (actualModelId.contains("cohere.command")) {
                return response.getJSONArray("generations")
                        .getJSONObject(0)
                        .getString("text");

            // AI21 Jurassic/Jamba 模型系列
            } else if (actualModelId.contains("ai21.j2") || actualModelId.contains("ai21.jamba")) {
                // Jamba 使用新格式
                if (actualModelId.contains("jamba")) {
                    if (response.containsKey("outputs")) {
                        var outputs = response.getJSONArray("outputs");
                        if (outputs != null && !outputs.isEmpty()) {
                            return outputs.getJSONObject(0).getString("text");
                        }
                    }
                }
                // J2 使用旧格式
                return response.getJSONArray("completions")
                        .getJSONObject(0)
                        .getJSONObject("data")
                        .getString("text");

            // Stability AI 模型系列（新增）
            } else if (actualModelId.contains("stability")) {
                // Stability 主要用于图像生成，但如果返回文本
                if (response.containsKey("artifacts")) {
                    var artifacts = response.getJSONArray("artifacts");
                    if (artifacts != null && !artifacts.isEmpty()) {
                        var firstArtifact = artifacts.getJSONObject(0);
                        if (firstArtifact.containsKey("base64")) {
                            return "[图像生成完成，Base64数据已返回]";
                        }
                    }
                }
                if (response.containsKey("text")) {
                    return response.getString("text");
                }
                log.warn("Stability 响应格式无法识别，返回原始响应");
                return responseBody;

            } else {
                // 通用响应解析（尝试多个常见字段）
                log.debug("使用通用响应解析，模型ID: {}", actualModelId);

                // 尝试常见的响应字段
                if (response.containsKey("completion")) {
                    return response.getString("completion");
                } else if (response.containsKey("generation")) {
                    return response.getString("generation");
                } else if (response.containsKey("text")) {
                    return response.getString("text");
                } else if (response.containsKey("output")) {
                    var output = response.get("output");
                    if (output instanceof String) {
                        return (String) output;
                    } else if (output instanceof JSONObject) {
                        return ((JSONObject) output).toJSONString();
                    }
                } else if (response.containsKey("content")) {
                    var content = response.get("content");
                    if (content instanceof String) {
                        return (String) content;
                    }
                }

                log.warn("无法识别响应格式，返回原始响应。模型ID: {}", actualModelId);
                return responseBody;
            }

        } catch (Exception e) {
            log.error("解析响应失败: {}", e.getMessage(), e);
            log.debug("原始响应体: {}", responseBody);
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

