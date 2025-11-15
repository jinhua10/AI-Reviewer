package top.yumbo.ai.application.hackathon.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.yumbo.ai.application.hackathon.core.HackathonAIEngine;
import top.yumbo.ai.application.hackathon.parser.HackathonFileParser;
import top.yumbo.ai.core.registry.AdapterRegistry;
import top.yumbo.ai.starter.config.AIReviewerProperties;

/**
 * Auto-configuration for Hackathon
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(AIReviewerProperties.class)
public class HackathonAutoConfiguration {

    @Autowired
    AdapterRegistry registry;

    @Bean
    public AdapterRegistry adapterRegistry() {
        log.info("Initializing AdapterRegistry");
        // remove default parser for hackathon
        registry.getAllParsers().clear();
        registry.registerParser(new HackathonFileParser());
        registry.loadAdaptersFromSPI();
        return registry;
    }

    @Bean
    public HackathonAIEngine hackathonAIEngine(AdapterRegistry registry) {
        log.info("Initializing AIEngine");
        return new HackathonAIEngine(registry);
    }

}
