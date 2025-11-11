package top.yumbo.ai.reviewer.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import top.yumbo.ai.reviewer.config.Config;
import top.yumbo.ai.reviewer.exception.AnalysisException;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Deepseek AI服务实现
 * 
 * 负责与Deepseek API交互，提供代码分析能力
 */
@Slf4j
public class DeepseekAIService implements AIService {

    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    private final Config config;
    private final OkHttpClient httpClient;
    private boolean closed = false;

    public DeepseekAIService(Config config) {
        this.config = config;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String analyze(String prompt, int maxTokens) {
        if (closed) {
            throw new AnalysisException("DeepseekAIService has been closed");
        }

        try {
            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", config.getModel());
            requestBody.put("messages", Collections.singletonList(
                    new JSONObject() {{
                        put("role", "user");
                        put("content", prompt);
                    }}
            ));
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", 0.3);
            requestBody.put("stream", false);

            // 创建请求
            RequestBody body = RequestBody.create(requestBody.toString(), JSON_MEDIA_TYPE);
            Request request = new Request.Builder()
                    .url(API_URL)
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .header("Content-Type", "application/json")
                    .post(body)
                    .build();

            // 发送请求
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "未知错误";
                    throw new AnalysisException("Deepseek API请求失败: " + response.code() + ", " + errorBody);
                }

                // 解析响应
                String responseBody = response.body() != null ? response.body().string() : "";
                JSONObject jsonResponse = JSON.parseObject(responseBody);

                if (jsonResponse.containsKey("choices") && 
                    jsonResponse.getJSONArray("choices").size() > 0) {

                    JSONObject choice = jsonResponse.getJSONArray("choices").getJSONObject(0);
                    JSONObject message = choice.getJSONObject("message");

                    if (message != null && message.containsKey("content")) {
                        return message.getString("content");
                    }
                }

                throw new AnalysisException("无法解析Deepseek API响应: " + responseBody);
            }

        } catch (IOException e) {
            throw new AnalysisException("调用Deepseek API时发生IO异常", e);
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

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            // 关闭HTTP客户端
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();

            try {
                if (!httpClient.dispatcher().executorService().awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("HTTP客户端未能正常关闭");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("关闭HTTP客户端时被中断", e);
            }
        }
    }
}
