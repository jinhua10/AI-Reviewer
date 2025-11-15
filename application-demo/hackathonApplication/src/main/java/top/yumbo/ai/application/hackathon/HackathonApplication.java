package top.yumbo.ai.application.hackathon;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import top.yumbo.ai.api.model.AIConfig;
import top.yumbo.ai.api.model.ProcessResult;
import top.yumbo.ai.api.model.ProcessorConfig;
import top.yumbo.ai.application.hackathon.core.HackathonAIEngine;
import top.yumbo.ai.core.context.ExecutionContext;
import top.yumbo.ai.starter.config.AIReviewerProperties;

import java.io.File;
import java.nio.file.Paths;

/**
 * Main application class for AI Reviewer
 */
@Slf4j
@SpringBootApplication
@EnableAutoConfiguration
public class HackathonApplication {
    public static void main(String[] args) {
        SpringApplication.run(HackathonApplication.class, args);
    }

    @Bean
    public CommandLineRunner runner(HackathonAIEngine hackathonAIEngine, AIReviewerProperties properties) {
        return args -> {
            log.info("AI Reviewer Started - Hackathon AIEngine bean found: {}", hackathonAIEngine != null);
            log.info("Configuration: {}", properties);
            // Example usage - can be triggered via REST API or CLI
            if (hackathonAIEngine != null && args.length > 0 && args[0].equals("--review")) {
                String targetPath = args.length > 1 ? args[1] : "./src";
                runReview(hackathonAIEngine, properties, targetPath);
            }
        };
    }

    private void runReview(HackathonAIEngine hackathonAIEngine, AIReviewerProperties properties, String targetPath) {
        log.info("Starting code review for: {}", targetPath);
        // Build execution context
        AIConfig aiConfig = AIConfig.builder()
                .provider(properties.getAi().getProvider())
                .region(properties.getAi().getRegion())
                .model(properties.getAi().getModel())
                .apiKey(properties.getAi().getApiKey())
                .sysPrompt(properties.getAi().getSysPrompt())
                .userPrompt(properties.getAi().getUserPrompt())
                .endpoint(properties.getAi().getEndpoint())
                .temperature(properties.getAi().getTemperature())
                .maxTokens(properties.getAi().getMaxTokens())
                .timeoutSeconds(properties.getAi().getTimeoutSeconds())
                .maxRetries(properties.getAi().getMaxRetries())
                .build();

        ProcessorConfig processorConfig = ProcessorConfig.builder()
                .processorType(properties.getProcessor().getType())
                .outputFormat(properties.getProcessor().getOutputFormat())
                .outputPath(Paths.get(properties.getProcessor().getOutputPath(), new File(targetPath).getName() + "-review-report.md"))
                .build();
        ExecutionContext context = ExecutionContext.builder()
                .targetDirectory(Paths.get(targetPath))
                .includePatterns(properties.getScanner().getIncludePatterns())
                .excludePatterns(properties.getScanner().getExcludePatterns())
                .aiConfig(aiConfig)
                .processorConfig(processorConfig)
                .threadPoolSize(properties.getExecutor().getThreadPoolSize())
                .build();
        // Execute
        ProcessResult result = hackathonAIEngine.execute(context);
        if (result.isSuccess()) {
            log.info("Code review completed successfully!");
            log.info("Report saved to: {}", processorConfig.getOutputPath());
        } else {
            log.error("Code review failed: {}", result.getErrorMessage());
        }
    }
}
