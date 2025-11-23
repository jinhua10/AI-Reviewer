package top.yumbo.ai.rag.example.application.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.yumbo.ai.rag.example.llm.LLMClient;
import top.yumbo.ai.rag.example.llm.MockLLMClient;

/**
 * LLM 客户端配置
 *
 * 提供 LLMClient Bean，使用依赖注入而不是直接 new 对象
 *
 * 扩展方式：
 * 1. 创建自定义 LLMClient 实现类（如 DeepSeekLLMClient）
 * 2. 在配置类中添加对应的 @Bean 方法
 * 3. 使用 @ConditionalOnProperty 根据配置选择实现
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
@Configuration
public class LLMConfiguration {

    /**
     * Mock LLM 客户端（默认实现）
     *
     * 用于测试和演示，返回模拟的回答
     * 如需使用真实的 LLM API，请创建自定义实现并覆盖此 Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public LLMClient llmClient() {
        log.info("🤖 创建 Mock LLM 客户端");
        log.info("   💡 提示：这是模拟客户端，返回固定回答");
        log.info("   💡 如需使用真实 LLM，请实现 LLMClient 接口并注册为 Bean");
        return new MockLLMClient();
    }
}

