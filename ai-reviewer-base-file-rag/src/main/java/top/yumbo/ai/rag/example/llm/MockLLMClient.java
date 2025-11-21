package top.yumbo.ai.rag.example.llm;

/**
 * 模拟LLM客户端
 * 用于演示和测试，实际使用时应替换为真实的LLM客户端
 *
 * 真实实现示例：
 * - OpenAIClient: 调用OpenAI GPT-4 API
 * - ClaudeClient: 调用Anthropic Claude API
 * - LocalLlamaClient: 调用本地Llama模型
 */
public class MockLLMClient implements LLMClient {

    @Override
    public String generate(String prompt) {
        // 实际使用时，这里应该调用真实的LLM API
        // 例如：
        // - OpenAI GPT-4: openai.chat.completions.create(...)
        // - 本地Llama: llama_cpp.generate(...)
        // - Claude: anthropic.messages.create(...)

        return """
            基于提供的文档内容，我为您总结如下：
            
            [这是一个模拟回答，实际使用时会调用真实的LLM]
            
            1. 文档中包含了详细的数据信息
            2. 数据已被成功解析并存储在知识库中
            3. 您可以通过关键词搜索快速找到相关内容
            
            💡 提示：请替换MockLLMClient为真实的LLM客户端以获得实际的AI回答。
            
            可选的LLM客户端：
            - OpenAI GPT-4 / GPT-3.5
            - Anthropic Claude
            - 本地Llama模型
            - 阿里通义千问
            - 百度文心一言
            """;
    }
}

