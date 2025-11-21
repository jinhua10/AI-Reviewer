package top.yumbo.ai.rag.example.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * DeepSeek LLM客户端
 * 使用OpenAI兼容的API接口调用DeepSeek
 *
 * API Key从环境变量 AI_API_KEY 获取
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
public class MockLLMClient implements LLMClient {

    private static final String DEEPSEEK_API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final String DEFAULT_MODEL = "deepseek-chat";
    private static final int DEFAULT_MAX_TOKENS = 2000;
    private static final double DEFAULT_TEMPERATURE = 0.7;

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String model;

    /**
     * 默认构造函数，从环境变量读取API Key
     */
    public MockLLMClient() {
        this(System.getenv("AI_API_KEY"), DEFAULT_MODEL);
    }

    /**
     * 自定义构造函数
     *
     * @param apiKey DeepSeek API Key
     * @param model 模型名称
     */
    public MockLLMClient(String apiKey, String model) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("⚠️ AI_API_KEY environment variable not set. Using mock mode.");
            this.apiKey = null;
        } else {
            this.apiKey = apiKey;
            log.info("✅ DeepSeek API Key loaded from environment variable");
        }

        this.model = model;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        this.objectMapper = new ObjectMapper();

        log.info("DeepSeek LLM Client initialized with model: {}", model);
    }

    @Override
    public String generate(String prompt) {
        // 如果没有API Key，返回模拟回答
        if (apiKey == null || apiKey.isEmpty()) {
            return generateMockResponse(prompt);
        }

        try {
            return callDeepSeekAPI(prompt);
        } catch (Exception e) {
            log.error("Failed to call DeepSeek API: {}", e.getMessage(), e);
            log.warn("Falling back to mock response");
            return generateMockResponse(prompt);
        }
    }

    /**
     * 调用DeepSeek API
     */
    private String callDeepSeekAPI(String prompt) throws IOException, InterruptedException {
        log.debug("Calling DeepSeek API with prompt length: {}", prompt.length());

        // 构建请求体
        String requestBody = buildRequestBody(prompt);

        // 创建HTTP请求
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(DEEPSEEK_API_URL))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .timeout(Duration.ofSeconds(60))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        // 发送请求
        long startTime = System.currentTimeMillis();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        long elapsedTime = System.currentTimeMillis() - startTime;

        log.debug("DeepSeek API response received in {}ms, status: {}", elapsedTime, response.statusCode());

        // 检查响应状态
        if (response.statusCode() != 200) {
            log.error("DeepSeek API error: Status {}, Body: {}", response.statusCode(), response.body());
            throw new IOException("DeepSeek API returned status: " + response.statusCode());
        }

        // 解析响应
        return parseResponse(response.body());
    }

    /**
     * 构建请求体
     */
    private String buildRequestBody(String prompt) {
        try {
            var requestNode = objectMapper.createObjectNode();
            requestNode.put("model", model);
            requestNode.put("max_tokens", DEFAULT_MAX_TOKENS);
            requestNode.put("temperature", DEFAULT_TEMPERATURE);

            // 构建messages数组
            var messagesArray = requestNode.putArray("messages");
            var userMessage = messagesArray.addObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);

            return objectMapper.writeValueAsString(requestNode);
        } catch (Exception e) {
            log.error("Failed to build request body", e);
            throw new RuntimeException("Failed to build request body", e);
        }
    }

    /**
     * 解析API响应
     */
    private String parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            // 提取内容: response.choices[0].message.content
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode firstChoice = choices.get(0);
                JsonNode message = firstChoice.path("message");
                String content = message.path("content").asText();

                // 记录token使用情况
                JsonNode usage = root.path("usage");
                if (usage != null && !usage.isMissingNode()) {
                    int promptTokens = usage.path("prompt_tokens").asInt(0);
                    int completionTokens = usage.path("completion_tokens").asInt(0);
                    int totalTokens = usage.path("total_tokens").asInt(0);

                    log.info("Token usage - Prompt: {}, Completion: {}, Total: {}",
                        promptTokens, completionTokens, totalTokens);
                }

                return content;
            } else {
                log.error("Unexpected response format: {}", responseBody);
                throw new IOException("Invalid response format");
            }
        } catch (Exception e) {
            log.error("Failed to parse DeepSeek response: {}", responseBody, e);
            throw new RuntimeException("Failed to parse response", e);
        }
    }

    /**
     * 生成模拟响应（当API Key不可用时）
     */
    private String generateMockResponse(String prompt) {
        log.debug("Using mock response (no API key)");

        return """
            基于提供的文档内容，我为您总结如下：
            
            [这是一个模拟回答 - DeepSeek API Key未配置]
            
            1. 文档中包含了详细的数据信息
            2. 数据已被成功解析并存储在知识库中
            3. 您可以通过关键词搜索快速找到相关内容
            
            💡 提示：请设置环境变量 AI_API_KEY 以启用真实的DeepSeek AI回答。
            
            设置方法：
            - Windows: set AI_API_KEY=your-deepseek-api-key
            - Linux/Mac: export AI_API_KEY=your-deepseek-api-key
            
            获取API Key: https://platform.deepseek.com/
            """;
    }

    /**
     * 检查API是否可用
     */
    public boolean isApiAvailable() {
        return apiKey != null && !apiKey.isEmpty();
    }
}

