package top.yumbo.ai.application.hackathon.processor;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.api.model.AIResponse;
import top.yumbo.ai.api.model.ProcessResult;
import top.yumbo.ai.api.model.ProcessorConfig;
import top.yumbo.ai.api.processor.IResultProcessor;
import top.yumbo.ai.common.exception.ProcessorException;
import top.yumbo.ai.common.util.FileUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Code review processor that generates review reports
 */
@Slf4j
public class HackathonCodeReviewProcessor implements IResultProcessor {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private long getValue(Long value) {
        return value == null ? 0 : value;
    }

    @Override
    public ProcessResult process(List<AIResponse> responses, ProcessorConfig config) throws Exception {
        log.info("Processing {} AI responses for code review", responses.size());

        try {
            // Generate markdown report
            StringBuilder report = new StringBuilder();
            report.append("# Code Review Report\n\n");
            report.append("**Generated at:** ").append(LocalDateTime.now().format(DATE_FORMATTER)).append("\n\n");
            report.append("**Total Files Reviewed:** ").append(responses.size()).append("\n\n");
            report.append("---\n\n");

            // Process each response
            int fileIndex = 1;
            for (AIResponse response : responses) {
                report.append("## File ").append(fileIndex++).append("\n\n");
                report.append("**Model:** ").append(response.getModel()).append("\n");
                report.append("**Provider:** ").append(response.getProvider()).append("\n");

                if (response.getTokenUsage() != null) {
                    report.append("**Tokens Used:** ").append(response.getTokenUsage().getTotalTokens()).append("\n");
                }

                if (response.getProcessingTimeMs() != null) {
                    report.append("**Processing Time:** ").append(response.getProcessingTimeMs()).append(" ms\n");
                }

                report.append("\n### Review Comments\n\n");
                report.append(response.getContent()).append("\n\n");
                report.append("---\n\n");
            }

            // Add summary
            report.append("## Summary\n\n");
            report.append("- Total files reviewed: ").append(responses.size()).append("\n");

            long totalTokens = responses.stream()
                    .filter(r -> r.getTokenUsage() != null)
                    .mapToLong(r -> r.getTokenUsage().getTotalTokens())
                    .sum();
            report.append("- Total tokens used: ").append(totalTokens).append("\n");

            long aiProcessingTime = responses.stream()
                    .filter(r -> r.getProcessingTimeMs() != null)
                    .mapToLong(AIResponse::getProcessingTimeMs)
                    .sum();
            report.append("- AI processing time: ").append(aiProcessingTime).append(" ms\n");

            // Add timing breakdown from config metadata if available
            if (config.getCustomParams() != null) {
                Long parseTime = getValue((Long) config.getCustomParams().get("parsingTimeMs"));
                Long aiTime = getValue((Long) config.getCustomParams().get("aiInvocationTimeMs"));
                Long processTime = getValue((Long) config.getCustomParams().get("resultProcessingTimeMs"));
                Long scanTime = getValue((Long) config.getCustomParams().get("scanTimeMs"));
                Long filterTime = getValue((Long) config.getCustomParams().get("filterTimeMs"));

                report.append("\n### Execution Time Breakdown\n\n");
                report.append("- **File scanning:** ").append(getValue(scanTime)).append(" ms\n");
                report.append("- **File filtering:** ").append(filterTime).append(" ms\n");
                report.append("- **File parsing:** ").append(parseTime).append(" ms\n");
                report.append("- **Result processing:** ").append(processTime).append(" ms\n");
                report.append("- **AI invocation:** ").append(aiTime).append(" ms\n");
                report.append("- **total time:** ").append((scanTime + filterTime + parseTime + aiTime + processTime)).append(" ms\n");

            }

            String content = report.toString();

            // Write to file if output path is specified
            if (config.getOutputPath() != null) {
                FileUtil.writeStringToFile(config.getOutputPath(), content);
                log.info("Code review report written to: {}", config.getOutputPath());
            }

            // Build metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("filesReviewed", responses.size());
            metadata.put("totalTokens", totalTokens);
            metadata.put("aiProcessingTime", aiProcessingTime);

            // Copy timing information from config if available
            if (config.getCustomParams() != null) {
                config.getCustomParams().forEach((key, value) -> {
                    if (key.endsWith("TimeMs") || key.equals("executionId")) {
                        metadata.put(key, value);
                    }
                });
            }

            return ProcessResult.builder()
                    .resultType(getProcessorType())
                    .content(content)
                    .format(config.getOutputFormat())
                    .timestamp(LocalDateTime.now())
                    .metadata(metadata)
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Error processing code review", e);
            throw new ProcessorException("Code review processing failed", e);
        }
    }

    @Override
    public String getProcessorType() {
        return "code-review";
    }

    @Override
    public String[] getSupportedFormats() {
        return new String[]{"markdown", "html", "json"};
    }
}
