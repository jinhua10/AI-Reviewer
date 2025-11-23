package top.yumbo.ai.rag.example.llm;

import lombok.extern.slf4j.Slf4j;

/**
 * DeepSeek LLM 客户端
 * 使用 OpenAI 兼容的 API 接口调用 DeepSeek
 *
 * DeepSeek API 完全兼容 OpenAI API 格式，因此直接复用 OpenAILLMClient
 *
 * @author AI Reviewer Team
 * @since 2025-11-23
 */
@Slf4j
public class DeepSeekLLMClient extends OpenAILLMClient {

    private static final String DEEPSEEK_API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final String DEFAULT_MODEL = "deepseek-chat";

    /**
     * 默认构造函数，从环境变量读取 API Key
     */
    public DeepSeekLLMClient() {
        this(System.getenv("AI_API_KEY"));
    }

    /**
     * 自定义构造函数
     *
     * @param apiKey DeepSeek API Key
     */
    public DeepSeekLLMClient(String apiKey) {
        this(apiKey, DEFAULT_MODEL);
    }

    /**
     * 完整构造函数
     *
     * @param apiKey DeepSeek API Key
     * @param model 模型名称
     */
    public DeepSeekLLMClient(String apiKey, String model) {
        super(apiKey, model != null ? model : DEFAULT_MODEL, DEEPSEEK_API_URL);

        log.info("✅ DeepSeek LLM 客户端初始化完成");
        log.info("   - 模型: {}", model != null ? model : DEFAULT_MODEL);
        log.info("   - API: {}", DEEPSEEK_API_URL);
    }

    /**
     * 从环境变量创建
     */
    public static DeepSeekLLMClient fromEnv() {
        String apiKey = System.getenv("AI_API_KEY");
        String model = System.getenv("DEEPSEEK_MODEL");

        if (model == null || model.isEmpty()) {
            model = DEFAULT_MODEL;
        }

        return new DeepSeekLLMClient(apiKey, model);
    }
}

