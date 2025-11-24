package top.yumbo.ai.application.hackathon.core;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.api.model.AIResponse;
import top.yumbo.ai.api.model.FileMetadata;
import top.yumbo.ai.api.model.PreProcessedData;
import top.yumbo.ai.api.model.ProcessResult;
import top.yumbo.ai.application.hackathon.util.AntiCheatFilter;
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
        Path filePath = metadata.getFilePath();

        // Apply anti-cheat filter to remove suspicious comments
        String filteredContent = AntiCheatFilter.filterSuspiciousContent(
                content,
                filePath != null ? filePath.toString() : fileName
        );

        return String.format("""
                file path: %s
                file content:
                ```
                %s
                ```
                
                """, filePath, filteredContent);
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

            // Separate README.md files from other files
            List<PreProcessedData> readmeFiles = new ArrayList<>();
            List<PreProcessedData> otherFiles = new ArrayList<>();

            for (PreProcessedData data : preprocessedDataList) {
                String fileName = data.getMetadata().getFileName();
                if (fileName != null && fileName.equalsIgnoreCase("README.md")) {
                    readmeFiles.add(data);
                    log.debug("Found README.md file: {}", data.getMetadata().getFilePath());
                } else {
                    otherFiles.add(data);
                }
            }

            // Build content with README.md first, then other source files
            StringBuilder sb = new StringBuilder();
            int filesWithSuspiciousContent = 0;

            // Add README.md files first (with anti-cheat filtering)
            for (PreProcessedData readmeData : readmeFiles) {
                AntiCheatFilter.FilterStatistics stats = AntiCheatFilter.analyzeContent(
                        readmeData.getContent());
                if (stats.hasSuspiciousContent()) {
                    filesWithSuspiciousContent++;
                }
                sb.append(getFileContent(readmeData));
            }

            // Then add other source files (with anti-cheat filtering)
            for (PreProcessedData otherData : otherFiles) {
                AntiCheatFilter.FilterStatistics stats = AntiCheatFilter.analyzeContent(
                        otherData.getContent());
                if (stats.hasSuspiciousContent()) {
                    filesWithSuspiciousContent++;
                }
                sb.append(getFileContent(otherData));
            }

            log.info("Built prompt with {} README.md file(s) at the beginning, followed by {} source file(s)",
                    readmeFiles.size(), otherFiles.size());

            if (filesWithSuspiciousContent > 0) {
                log.warn("Anti-cheat filter detected suspicious content in {} file(s)",
                        filesWithSuspiciousContent);
            }

            // Add anti-cheat notice if needed
            String finalContent = sb.toString();
            if (filesWithSuspiciousContent > 0) {
                finalContent = AntiCheatFilter.addAntiCheatNotice(finalContent, filesWithSuspiciousContent);
            }

            PreProcessedData oneContent = PreProcessedData.builder()
                    .metadata(FileMetadata.builder().build())
                    .content(finalContent)
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
