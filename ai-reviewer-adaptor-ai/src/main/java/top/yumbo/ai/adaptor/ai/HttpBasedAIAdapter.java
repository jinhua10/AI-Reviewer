package top.yumbo.ai.adaptor.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import top.yumbo.ai.api.ai.IAIService;
import top.yumbo.ai.api.model.AIConfig;
import top.yumbo.ai.api.model.AIResponse;
import top.yumbo.ai.api.model.PreProcessedData;
import top.yumbo.ai.common.exception.AIServiceException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * HttpBasedAI service adapter
 */
@Slf4j
public class HttpBasedAIAdapter implements IAIService {
    private static final String DEFAULT_ENDPOINT = "https://api.openai.com/v1/chat/completions";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AIConfig config;

    public HttpBasedAIAdapter(AIConfig config) {
        this.config = config;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public AIResponse invoke(PreProcessedData data, AIConfig config) throws Exception {
        log.info("Invoking HttpBasedAI with model: {}", config.getModel());
        long startTime = System.currentTimeMillis();
        try {
            // Build request payload
            Map<String, Object> requestBody = buildRequestBody(data, config);
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            // Build HTTP request
            Request request = new Request.Builder()
                    .url(config.getEndpoint() != null ? config.getEndpoint() : DEFAULT_ENDPOINT)
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, JSON))
                    .build();
            // Execute request
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new AIServiceException("AI API call failed: " + response.code() + " - " + response.message());
                }
                String responseBody = response.body().string();
                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                // Parse response
                String content = jsonResponse.get("choices").get(0).get("message").get("content").asText();
                JsonNode usage = jsonResponse.get("usage");
                AIResponse.TokenUsage tokenUsage = AIResponse.TokenUsage.builder()
                        .promptTokens(usage.get("prompt_tokens").asInt())
                        .completionTokens(usage.get("completion_tokens").asInt())
                        .totalTokens(usage.get("total_tokens").asInt())
                        .build();
                long processingTime = System.currentTimeMillis() - startTime;
                return AIResponse.builder()
                        .content(content)
                        .model(config.getModel())
                        .provider(getProviderName())
                        .processingTimeMs(processingTime)
                        .timestamp(LocalDateTime.now())
                        .tokenUsage(tokenUsage)
                        .build();
            }
        } catch (IOException e) {
            log.error("HttpBasedAI API call failed", e);
            throw new AIServiceException("HttpBasedAI invocation error", e);
        }
    }

    @Override
    public boolean isAvailable() {
        // Could implement a health check here
        return true;
    }

    @Override
    public String getProviderName() {
        return config.getProvider();
    }

    @Override
    public String[] getSupportedModels() {
        return new String[]{"please refer " + config.getProvider() + " official documentation for supported models"};
    }

    private Map<String, Object> buildRequestBody(PreProcessedData data, AIConfig config) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", config.getModel());
        requestBody.put("temperature", config.getTemperature());
        requestBody.put("max_tokens", config.getMaxTokens());
        // Build messages
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", config.getSysPrompt());
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        System.out.println(data.getMetadata());
        System.out.println(config.getUserPrompt());
        System.out.println(data.getContent());
        // 使用 replace 而不是 String.format 避免提示词中的特殊字符（如 '+', '%'）被误认为格式化标志
        String formattedPrompt = config.getUserPrompt().replace("%s", data.getContent());
        userMessage.put("content", formattedPrompt);
        requestBody.put("messages", new Map[]{systemMessage, userMessage});
        return requestBody;
    }
}
