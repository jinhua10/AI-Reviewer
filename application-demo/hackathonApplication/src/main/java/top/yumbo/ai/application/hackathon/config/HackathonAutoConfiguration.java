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
import top.yumbo.ai.application.hackathon.core.HackathonAIEngineV2;
import top.yumbo.ai.application.hackathon.parser.HackathonFileParser;
import top.yumbo.ai.application.hackathon.processor.HackathonCodeReviewProcessor;
import top.yumbo.ai.application.hackathon.web.service.AccountService;
import top.yumbo.ai.application.hackathon.web.service.FileUploadService;
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
    AIReviewerProperties aiReviewerProperties;

    private AIConfig aiConfig;

    @PostConstruct
    public void customizeAdapterRegistry() {
        log.info("Customizing AdapterRegistry for Hackathon");

        // 直接使用配置文件中的 AIConfig
        this.aiConfig = aiReviewerProperties.getAi();

        // Debug: 打印配置信息
        log.info("========== 配置加载调试信息 ==========");
        log.info("Provider: {}", aiConfig.getProvider());
        log.info("Region: {}", aiConfig.getRegion());
        log.info("Model: {}", aiConfig.getModel());
        log.info("Temperature: {}", aiConfig.getTemperature());
        log.info("TopP: {}", aiConfig.getTopP());
        log.info("MaxTokens: {}", aiConfig.getMaxTokens());
        log.info("TimeoutSeconds: {}", aiConfig.getTimeoutSeconds());
        log.info("MaxRetries: {}", aiConfig.getMaxRetries());
        log.info("SysPrompt: {}", aiConfig.getSysPrompt() != null ?
                aiConfig.getSysPrompt().substring(0, Math.min(50, aiConfig.getSysPrompt().length())) : "null");
        log.info("UserPrompt: {}", aiConfig.getUserPrompt() != null ?
                aiConfig.getUserPrompt().substring(0, Math.min(50, aiConfig.getUserPrompt().length())) : "null");
        log.info("=====================================");

        // remove default parser for hackathon
        registry.clearParsers();
        registry.clearAIServices();
        registry.clearProcessors();
        registry.registerParser(new HackathonFileParser());
        registry.registerAIService(new BedrockAdapter(aiConfig));
        registry.registerProcessor(new HackathonCodeReviewProcessor());
        registry.loadAdaptersFromSPI();
    }



    @Bean
    public HackathonAIEngine hackathonAIEngine(AdapterRegistry registry) {
        log.info("Initializing AIEngine");
        return new HackathonAIEngine(registry);
    }

    @Bean
    public HackathonAIEngineV2 hackathonAIEngineV2(HackathonAIEngine hackathonAIEngine) {
        log.info("Initializing HackathonAIEngineV2 for batch processing");
        return new HackathonAIEngineV2(
                hackathonAIEngine, aiReviewerProperties);
    }

    @Bean
    public CommandLineRunner runner(HackathonAIEngine hackathonAIEngine,
                                     HackathonAIEngineV2 hackathonAIEngineV2,
                                     AccountService accountService,
                                     FileUploadService fileUploadService) {
        return args -> {
            log.info("AI Reviewer Started - Hackathon AIEngine bean found: {}", hackathonAIEngine != null);
            log.info("Configuration: {}", aiReviewerProperties);

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

            // Check for --reviewAll mode (batch processing)
            String reviewAllPath = null;
            String reviewPath = null;

            for (int i = 0; i < effectiveArgs.size(); i++) {
                String a = effectiveArgs.get(i);

                // Check for --reviewAll
                if (a.startsWith("--reviewAll=")) {
                    reviewAllPath = a.substring("--reviewAll=".length());
                    break;
                }
                if ("--reviewAll".equals(a)) {
                    if (i + 1 < effectiveArgs.size()) {
                        reviewAllPath = effectiveArgs.get(i + 1);
                    }
                    break;
                }

                // Check for --review (single project mode)
                if (a.startsWith("--review=")) {
                    reviewPath = a.substring("--review=".length());
                }
                if ("--review".equals(a) && reviewAllPath == null) {
                    if (i + 1 < effectiveArgs.size()) {
                        reviewPath = effectiveArgs.get(i + 1);
                    } else {
                        reviewPath = ".";
                    }
                }
            }

            // Execute based on mode
            if (reviewAllPath != null) {
                // Initialize web services for upload functionality
                final String finalReviewAllPath = reviewAllPath;
                log.info("Initializing web services for project path: {}", finalReviewAllPath);
                accountService.loadAccounts(finalReviewAllPath);
                fileUploadService.setProjectRootPath(finalReviewAllPath);

                // Batch review mode with continuous monitoring
                runBatchReviewContinuous(hackathonAIEngineV2, finalReviewAllPath);
            } else if (reviewPath != null) {
                // Single review mode
                runReview(hackathonAIEngine, aiReviewerProperties, reviewPath);
            } else {
                log.info("No --review or --reviewAll argument provided; web interface is available for uploads.");
                log.info("Please use --reviewAll=<path> to enable batch review mode with web uploads.");
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

    private void runBatchReview(HackathonAIEngineV2 engineV2, String zipDirectory) {
        log.info("Starting batch code review for all projects in: {}", zipDirectory);

        HackathonAIEngineV2.BatchResult result = engineV2.reviewAllProjects(zipDirectory);

        if (result.isSuccess()) {
            log.info("Batch review completed successfully!");
            log.info("Total: {}, Successful: {}, Failed: {}, Skipped: {}",
                    result.getTotalProjects(),
                    result.getSuccessCount(),
                    result.getFailedCount(),
                    result.getSkippedCount());
        } else {
            log.error("Batch review failed: {}", result.getErrorMessage());
        }
    }

    private void runBatchReviewContinuous(HackathonAIEngineV2 engineV2, String zipDirectory) {
        log.info("Starting continuous batch code review with periodic sync for: {}", zipDirectory);

        // This method will run indefinitely, rescanning every 2 minutes
        engineV2.reviewAllProjectsContinuous(zipDirectory);
    }

}
