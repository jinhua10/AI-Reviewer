package top.yumbo.ai.reviewer.adapter.output.ai;

import com.alibaba.fastjson2.JSONObject;

import java.net.URI;
import java.net.http.HttpRequest;

/**
 * AI 适配器工厂 - 创建不同提供商的适配器实例
 *
 * @author AI-Reviewer Team
 * @version 2.0
 * @since 2025-11-14
 */
public class AIAdapterFactory {

    /**
     * 创建 OpenAI 适配器
     */
    public static HttpBasedAIAdapter createOpenAI(AIServiceConfig config) {
        return new HttpBasedAIAdapter(
                "OpenAI",
                config,
                (url, requestBody) -> HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + config.apiKey()),
                responseJson -> responseJson.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
        );
    }

    /**
     * 创建 Claude 适配器
     */
    public static HttpBasedAIAdapter createClaude(AIServiceConfig config) {
        return new HttpBasedAIAdapter(
                "Claude",
                config,
                (url, requestBody) -> {
                    // Claude 使用 Messages API，需要调整请求体
                    requestBody.remove("model"); // 暂时移除
                    requestBody.put("model", config.model());

                    return HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header("Content-Type", "application/json")
                            .header("x-api-key", config.apiKey())
                            .header("anthropic-version", "2023-06-01");
                },
                responseJson -> responseJson.getJSONArray("content")
                        .getJSONObject(0)
                        .getString("text")
        );
    }

    /**
     * 创建 Gemini 适配器
     */
    public static HttpBasedAIAdapter createGemini(AIServiceConfig config) {
        return new HttpBasedAIAdapter(
                "Gemini",
                config,
                (url, requestBody) -> {
                    // Gemini API 格式不同，需要重构请求体
                    JSONObject content = new JSONObject();
                    JSONObject part = new JSONObject();
                    Object[] messages = (Object[]) requestBody.get("messages");
                    if (messages != null && messages.length > 0) {
                        JSONObject msg = (JSONObject) messages[0];
                        part.put("text", msg.getString("content"));
                    }
                    content.put("parts", new Object[]{part});

                    // 重构请求体以符合 Gemini API 格式
                    requestBody.clear();
                    requestBody.put("contents", new Object[]{content});

                    JSONObject generationConfig = new JSONObject();
                    generationConfig.put("temperature", config.temperature());
                    generationConfig.put("maxOutputTokens", config.maxTokens());
                    requestBody.put("generationConfig", generationConfig);

                    // Gemini 使用 URL 参数传递 API Key
                    String urlWithKey = url + "?key=" + config.apiKey();

                    return HttpRequest.newBuilder()
                            .uri(URI.create(urlWithKey))
                            .header("Content-Type", "application/json");
                },
                responseJson -> {
                    try {
                        return responseJson.getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text");
                    } catch (Exception e) {
                        throw new RuntimeException("Gemini 响应解析失败: " + e.getMessage(), e);
                    }
                }
        );
    }

    /**
     * 创建 DeepSeek 适配器（兼容 OpenAI 格式）
     */
    public static HttpBasedAIAdapter createDeepSeek(AIServiceConfig config) {
        return new HttpBasedAIAdapter(
                "DeepSeek",
                config,
                (url, requestBody) -> HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + config.apiKey()),
                responseJson -> responseJson.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
        );
    }
}

