package top.yumbo.ai.reviewer.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.yumbo.ai.reviewer.config.Config;
import top.yumbo.ai.reviewer.exception.AnalysisException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Deepseek AI 客户端实现（简化版）
 */
public class DeepseekAIService implements AIService {

    private static final Logger log = LoggerFactory.getLogger(DeepseekAIService.class);

    private final Config config;
    private final OkHttpClient httpClient;

    // 共享的 HTTP 客户端，复用连接池
    private static final OkHttpClient SHARED_CLIENT = new OkHttpClient.Builder()
        .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES))
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build();

    public DeepseekAIService(Config config) {
        this.config = config;
        this.httpClient = SHARED_CLIENT;
    }

    @Override
    public String analyze(String prompt, int maxTokens) throws AnalysisException {
        // 构建请求体
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", config.getModel());
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("temperature", 0.7);

        // 构建消息
        JSONArray messages = new JSONArray();
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", getSystemPrompt());
        messages.add(systemMessage);

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);

        requestBody.put("messages", messages);

        // 构建 HTTP 请求
        RequestBody body = RequestBody.create(
            requestBody.toJSONString(),
            MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
            .url(config.getApiUrl())
            .addHeader("Authorization", "Bearer " + config.getApiKey())
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build();

        // 执行请求
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw AnalysisException.aiError(
                    String.format("AI API call failed: HTTP %d - %s", response.code(), errorBody),
                    null
                );
            }

            if (response.body() == null) {
                throw AnalysisException.aiError("AI API returned empty response", null);
            }

            // 解析响应
            String responseBody = response.body().string();
            JSONObject jsonResponse = JSONObject.parseObject(responseBody);

            JSONArray choices = jsonResponse.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw AnalysisException.aiError("AI API returned no choices", null);
            }

            JSONObject firstChoice = choices.getJSONObject(0);
            JSONObject message = firstChoice.getJSONObject("message");
            String content = message.getString("content");

            if (content == null || content.isEmpty()) {
                throw AnalysisException.aiError("AI API returned empty content", null);
            }

            log.debug("AI analysis completed, response length: {}", content.length());
            return content;

        } catch (IOException e) {
            throw AnalysisException.networkError("Network error during AI API call", e);
        } catch (Exception e) {
            throw AnalysisException.aiError("Unexpected error during AI API call: " + e.getMessage(), e);
        }
    }

    @Override
    public int getMaxTokens() {
        return config.getMaxTokens();
    }

    @Override
    public String getModelName() {
        return config.getModel();
    }

    /**
     * 获取系统提示词
     */
    private String getSystemPrompt() {
        return """
            你是一个专业的代码审查专家。请对提供的代码进行全面分析，包括：
            1. 代码质量评估（可读性、可维护性、性能）
            2. 潜在问题识别（Bug、安全漏洞、逻辑错误）
            3. 最佳实践建议（设计模式、代码规范）
            4. 改进建议（重构建议、优化方向）
            
            请以结构化的方式返回分析结果。
            """;
    }
}

