package top.yumbo.ai.application.hackathon.ai;


import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import top.yumbo.ai.api.ai.IAIService;
import top.yumbo.ai.api.model.AIConfig;
import top.yumbo.ai.api.model.AIResponse;
import top.yumbo.ai.api.model.PreProcessedData;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * AWS Bedrock AI服务适配器
 * 支持 Claude、Titan、Llama 等 Bedrock 模型
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-12
 */
@Slf4j
public class BedrockAdapter implements IAIService {

    private final BedrockRuntimeClient bedrockClient;
    private AIConfig config;
    private String modelId;
    private Integer maxTokens;
    private String region;
    private double temperature;

    /**
     * 构造函数
     *
     * @param config AI服务配置
     */
    public BedrockAdapter(AIConfig config) {
        this.config = config;
        this.modelId = config.getModel();
        this.maxTokens = config.getMaxTokens();
        this.temperature = config.getTemperature();
        this.region = "us-east-1";
        // 初始化 Bedrock 客户端
        var clientBuilder = BedrockRuntimeClient.builder()
                .region(Region.of(this.region));


        // 配置超时时间（解决 Read timeout 问题）
        clientBuilder.overrideConfiguration(builder -> builder
                .apiCallTimeout(java.time.Duration.ofMillis(config.getTimeoutSeconds()))
                .apiCallAttemptTimeout(java.time.Duration.ofMillis(config.getTimeoutSeconds()))
                .retryPolicy(retry -> retry
                        .numRetries(config.getMaxRetries())
                )
        );

        this.bedrockClient = clientBuilder.build();
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
     * 构建请求体（根据模型类型）
     * 支持 AWS Bedrock 平台上的所有主流模型
     */
    private String buildRequestBody(String prompt) {
        JSONObject requestBody = new JSONObject();

        // 提取实际的模型名称（处理 ARN 格式）
        String actualModelId = extractModelId(config.getModel());

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
            } else {
                // Llama 2 使用旧格式
                requestBody.put("prompt", prompt);
                requestBody.put("max_gen_len", maxTokens);
            }
            requestBody.put("temperature", temperature);
            requestBody.put("top_p", 0.9);

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

    @Override
    public String[] getSupportedModels() {
        return IAIService.super.getSupportedModels();
    }

    @Override
    public AIResponse invoke(PreProcessedData data, AIConfig config) throws Exception {
        try {
            // 构建请求体（根据不同模型格式会有所不同）
            String requestBody = buildRequestBody(data.getContent());

            log.debug("调用 Bedrock 模型 - Model ID: {}, Region: {}", modelId, region);
            log.debug("请求体: {}", requestBody);

            // 调用模型
            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(modelId)
                    .body(SdkBytes.fromString(requestBody, StandardCharsets.UTF_8))
                    .build();

            InvokeModelResponse response = bedrockClient.invokeModel(request);

            // 解析响应
            JSONObject responseBody = JSONObject.parseObject(response.body().asUtf8String());
            log.debug("响应体: {}", responseBody);
            // Parse response
            String content = responseBody.getJSONArray("choices").getJSONObject(0)
                    .getJSONObject("message").getString("content");
            JSONObject usage = responseBody.getJSONObject("usage");
            AIResponse.TokenUsage tokenUsage = AIResponse.TokenUsage.builder()
                    .promptTokens(usage.getInteger("prompt_tokens"))
                    .completionTokens(usage.getInteger("completion_tokens"))
                    .totalTokens(usage.getInteger("total_tokens"))
                    .build();
            return AIResponse.builder()
                    .content(content)
                    .model(config.getModel())
                    .provider(getProviderName())
                    .processingTimeMs(0L)
                    .timestamp(LocalDateTime.now())
                    .tokenUsage(tokenUsage)
                    .build();

        } catch (Exception e) {
            log.error("调用 Bedrock 模型失败: {}", e.getMessage(), e);
            throw new RuntimeException("Bedrock 模型调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getProviderName() {
        return config.getProvider();
    }


}
