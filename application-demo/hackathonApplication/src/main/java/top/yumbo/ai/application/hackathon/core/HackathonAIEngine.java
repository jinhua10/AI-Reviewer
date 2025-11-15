package top.yumbo.ai.application.hackathon.core;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.api.model.AIResponse;
import top.yumbo.ai.api.model.FileMetadata;
import top.yumbo.ai.api.model.PreProcessedData;
import top.yumbo.ai.api.model.ProcessResult;
import top.yumbo.ai.core.AIEngine;
import top.yumbo.ai.core.context.ExecutionContext;
import top.yumbo.ai.core.registry.AdapterRegistry;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * Main AI Engine for orchestrating the entire processing pipeline
 */
@Slf4j
public class HackathonAIEngine extends AIEngine {


    public HackathonAIEngine(AdapterRegistry registry) {
        super(registry);
    }

    public String getFileContent(PreProcessedData preProcessedData) {
        String content = preProcessedData.getContent();
        FileMetadata metadata = preProcessedData.getMetadata();
        String fileName = metadata.getFileName();
        return String.format("""
                file name: %s
                file content:
                ```
                %s
                ```
                
                """, fileName, content);
    }

    /**
     * Execute the AI review process
     */
    @Override
    public ProcessResult execute(ExecutionContext context) {
        context.setExecutionId(UUID.randomUUID().toString());
        context.setStartTime(LocalDateTime.now());
        long startTimeMs = System.currentTimeMillis();

        log.info("Starting AI Engine execution: {}", context.getExecutionId());

        try {
            // Initialize thread pool
            this.executorService = Executors.newFixedThreadPool(context.getThreadPoolSize());

            // Step 1: Scan files
            long scanStartMs = System.currentTimeMillis();
            List<Path> files = fileScanner.scan(context.getTargetDirectory());
            long scanTimeMs = System.currentTimeMillis() - scanStartMs;
            log.debug("File scanning took {} ms", scanTimeMs);

            // Step 2: Filter files
            long filterStartMs = System.currentTimeMillis();
            List<Path> filteredFiles = fileFilter.filter(files,
                    context.getIncludePatterns(),
                    context.getExcludePatterns());
            long filterTimeMs = System.currentTimeMillis() - filterStartMs;
            log.debug("File filtering took {} ms", filterTimeMs);

            // Step 3: Parse files
            long parseStartMs = System.currentTimeMillis();
            List<PreProcessedData> preprocessedDataList = parseFiles(filteredFiles);
            long parseTimeMs = System.currentTimeMillis() - parseStartMs;
            context.setParsingTimeMs(parseTimeMs);
            log.info("File parsing took {} ms", parseTimeMs);
            StringBuilder sb = new StringBuilder();
            for (PreProcessedData preProcessedData : preprocessedDataList) {
                sb.append(getFileContent(preProcessedData));
            }
            PreProcessedData oneContent = PreProcessedData.builder()
                    .metadata(FileMetadata.builder().build())
                    .content(sb.toString())
                    .build();
            // Step 4: Invoke AI service
            long aiStartMs = System.currentTimeMillis();
            List<AIResponse> aiResponses = invokeAI(Collections.singletonList(oneContent), context);
            long aiTimeMs = System.currentTimeMillis() - aiStartMs;
            context.setAiInvocationTimeMs(aiTimeMs);
            log.info("AI invocation took {} ms", aiTimeMs);

            // Prepare timing information for processor
            if (context.getProcessorConfig().getCustomParams() == null) {
                context.getProcessorConfig().setCustomParams(new HashMap<>());
            }
            context.getProcessorConfig().getCustomParams().put("executionId", context.getExecutionId());
            context.getProcessorConfig().getCustomParams().put("scanTimeMs", scanTimeMs);
            context.getProcessorConfig().getCustomParams().put("filterTimeMs", filterTimeMs);
            context.getProcessorConfig().getCustomParams().put("parsingTimeMs", parseTimeMs);
            context.getProcessorConfig().getCustomParams().put("aiInvocationTimeMs", aiTimeMs);

            // Step 5: Process results
            long processStartMs = System.currentTimeMillis();
            ProcessResult result = processResults(aiResponses, context);
            long processTimeMs = System.currentTimeMillis() - processStartMs;
            context.setResultProcessingTimeMs(processTimeMs);
            log.info("Result processing took {} ms", processTimeMs);

            // Calculate total time
            context.setEndTime(LocalDateTime.now());


            // Update processor config with final timing
            context.getProcessorConfig().getCustomParams().put("resultProcessingTimeMs", processTimeMs);

            // Add timing information to result metadata
            if (result.getMetadata() == null) {
                result.setMetadata(new HashMap<>());
            }
            result.getMetadata().put("executionId", context.getExecutionId());
            result.getMetadata().put("parsingTimeMs", parseTimeMs);
            result.getMetadata().put("aiInvocationTimeMs", aiTimeMs);
            result.getMetadata().put("resultProcessingTimeMs", processTimeMs);
            result.getMetadata().put("scanTimeMs", scanTimeMs);
            result.getMetadata().put("filterTimeMs", filterTimeMs);

            log.info("AI Engine execution completed: {} ( parsing: {} ms, AI: {} ms, processing: {} ms)",
                    context.getExecutionId(), parseTimeMs, aiTimeMs, processTimeMs);
            return result;

        } catch (Exception e) {
            log.error("AI Engine execution failed: {}", context.getExecutionId(), e);
            context.setEndTime(LocalDateTime.now());

            return ProcessResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .metadata(Map.of(
                            "executionId", context.getExecutionId()
                    ))
                    .build();
        } finally {
            if (executorService != null) {
                executorService.shutdown();
            }
        }
    }

}
