package top.yumbo.ai.starter.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import top.yumbo.ai.adaptor.ai.HttpBasedAIAdapter;
import top.yumbo.ai.adaptor.parser.JavaFileParser;
import top.yumbo.ai.adaptor.parser.JavaScriptFileParser;
import top.yumbo.ai.adaptor.parser.PlainTextFileParser;
import top.yumbo.ai.adaptor.parser.PythonFileParser;
import top.yumbo.ai.adaptor.processor.CodeReviewProcessor;
import top.yumbo.ai.api.model.AIConfig;
import top.yumbo.ai.core.AIEngine;
import top.yumbo.ai.core.registry.AdapterRegistry;

/**
 * Auto-configuration for AI Reviewer
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(AIReviewerProperties.class)
public class AIReviewerAutoConfiguration {

    @Autowired
    AIReviewerProperties properties;

    @Bean
    @Primary
    public AdapterRegistry adapterRegistry() {
        // 直接使用从配置文件绑定的 AIConfig
        AIConfig aiConfig = properties.getAi();
        log.info("Initializing AdapterRegistry");
        AdapterRegistry registry = new AdapterRegistry();
        registry.registerParser(new JavaFileParser());
        registry.registerParser(new PythonFileParser());
        registry.registerParser(new JavaScriptFileParser());
        registry.registerParser(new PlainTextFileParser());
        registry.registerAIService(new HttpBasedAIAdapter(aiConfig));
        registry.registerProcessor(new CodeReviewProcessor());
        registry.loadAdaptersFromSPI();
        return registry;
    }

    @Bean
    public AIEngine aiEngine(AdapterRegistry registry) {
        log.info("Initializing AIEngine");
        return new AIEngine(registry);
    }
}
