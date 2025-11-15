package top.yumbo.ai.application.hackathon.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.yumbo.ai.api.model.AIConfig;
import top.yumbo.ai.api.model.ProcessResult;
import top.yumbo.ai.api.model.ProcessorConfig;
import top.yumbo.ai.application.hackathon.ai.BedrockAdapter;
import top.yumbo.ai.application.hackathon.core.HackathonAIEngine;
import top.yumbo.ai.application.hackathon.parser.HackathonFileParser;
import top.yumbo.ai.core.context.ExecutionContext;
import top.yumbo.ai.core.registry.AdapterRegistry;
import top.yumbo.ai.starter.config.AIReviewerProperties;
import jakarta.annotation.PostConstruct;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Auto-configuration for Hackathon
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(AIReviewerProperties.class)
public class HackathonAutoConfiguration {

    @Autowired
    AdapterRegistry registry;
    @Autowired
    AIReviewerProperties properties;

    private AIConfig aiConfig;

    @PostConstruct
    public void customizeAdapterRegistry() {
        log.info("Customizing AdapterRegistry for Hackathon");
        // Build execution context
        this.aiConfig = AIConfig.builder().provider(properties.getAi().getProvider())
                .region(properties.getAi().getRegion())
                .model(properties.getAi().getModel()).apiKey(properties.getAi().getApiKey())
                .sysPrompt(properties.getAi().getSysPrompt())
                .userPrompt(properties.getAi().getUserPrompt())
                .endpoint(properties.getAi().getEndpoint())
                .temperature(properties.getAi().getTemperature())
                .maxTokens(properties.getAi().getMaxTokens())
                .timeoutSeconds(properties.getAi().getTimeoutSeconds())
                .maxRetries(properties.getAi().getMaxRetries()).build();
        // remove default parser for hackathon
        registry.clearParsers();
        registry.clearAIServices();
        registry.registerParser(new HackathonFileParser());
        registry.registerAIService(new BedrockAdapter(aiConfig));
        registry.loadAdaptersFromSPI();
    }


    @Bean
    public HackathonAIEngine hackathonAIEngine(AdapterRegistry registry) {
        log.info("Initializing AIEngine");
        return new HackathonAIEngine(registry);
    }

    @Bean
    public CommandLineRunner runner(HackathonAIEngine hackathonAIEngine, AIReviewerProperties properties) {
        return args -> {
            log.info("AI Reviewer Started - Hackathon AIEngine bean found: {}", hackathonAIEngine != null);
            log.info("Configuration: {}", properties);

            // Merge JVM property -Dspring-boot.run.arguments if provided (used by some wrappers)
            List<String> mergedArgs = new ArrayList<>();
            if (args != null) {
                mergedArgs.addAll(Arrays.asList(args));
            }
            String prop = System.getProperty("spring-boot.run.arguments");
            if (prop != null && !prop.isBlank()) {
                // parse into tokens, supporting quoted values
                mergedArgs.addAll(parseArgsString(prop));
                log.debug("Merged args from spring-boot.run.arguments: {}", mergedArgs);
            }

            // Also handle the case where -Dspring-boot.run.arguments is passed as a normal program arg
            // (for example when the caller places -D after -jar):
            // Replace any -Dspring-boot.run.arguments=... token in-place with its parsed tokens
            List<String> effectiveArgs = new ArrayList<>(mergedArgs);
            for (int i = 0; i < effectiveArgs.size(); i++) {
                String a = effectiveArgs.get(i);
                if (a.startsWith("-Dspring-boot.run.arguments=")) {
                    String val = a.substring("-Dspring-boot.run.arguments=".length());
                    List<String> parsed = parseArgsString(val);
                    // replace the original token with parsed tokens to preserve ordering
                    effectiveArgs.remove(i);
                    if (!parsed.isEmpty()) {
                        effectiveArgs.addAll(i, parsed);
                        i += parsed.size() - 1; // advance index
                    } else {
                        i--; // adjust for removed element
                    }
                }
            }
            log.debug("Effective args to inspect: {}", effectiveArgs);

            // Example usage - can be triggered via REST API or CLI
            if (hackathonAIEngine != null) {
                String targetPath = null;
                // look for --review [value] or --review=value
                for (int i = 0; i < effectiveArgs.size(); i++) {
                    String a = effectiveArgs.get(i);
                    if (a.startsWith("--review=")) {
                        targetPath = a.substring("--review=".length());
                        break;
                    }
                    if ("--review".equals(a)) {
                        if (i + 1 < effectiveArgs.size()) {
                            targetPath = effectiveArgs.get(i + 1);
                        } else {
                            targetPath = ".";
                        }
                        break;
                    }
                }

                if (targetPath != null) {
                    runReview(hackathonAIEngine, properties, targetPath);
                } else {
                    log.info("No --review argument provided; application started without running a review.");
                }
            }
        };
    }

    // Simple tokenizer: splits on whitespace but respects single or double quoted substrings
    private static List<String> parseArgsString(String input) {
        List<String> tokens = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\s*('([^']*)'|\"([^\"]*)\"|([^\s]+))\\s*");
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            String token = matcher.group(2);
            if (token == null) token = matcher.group(3);
            if (token == null) token = matcher.group(4);
            if (token != null && !token.isEmpty()) tokens.add(token);
        }
        return tokens;
    }

    private void runReview(HackathonAIEngine hackathonAIEngine, AIReviewerProperties properties, String targetPath) {
        log.info("Starting code review for: {}", targetPath);

        ProcessorConfig processorConfig = ProcessorConfig.builder().processorType(properties.getProcessor().getType()).outputFormat(properties.getProcessor().getOutputFormat()).outputPath(Paths.get(properties.getProcessor().getOutputPath(), new File(targetPath).getName() + "-review-report.md")).build();
        ExecutionContext context = ExecutionContext.builder().targetDirectory(Paths.get(targetPath)).includePatterns(properties.getScanner().getIncludePatterns()).excludePatterns(properties.getScanner().getExcludePatterns()).aiConfig(aiConfig).processorConfig(processorConfig).threadPoolSize(properties.getExecutor().getThreadPoolSize()).build();
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
