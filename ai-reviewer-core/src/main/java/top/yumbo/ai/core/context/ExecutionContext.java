package top.yumbo.ai.core.context;
import lombok.Builder;
import lombok.Data;
import top.yumbo.ai.api.model.AIConfig;
import top.yumbo.ai.api.model.ProcessorConfig;
import top.yumbo.ai.api.source.FileSourceConfig;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/**
 * Execution context for tracking state during processing
 */
@Data
@Builder
public class ExecutionContext {
    /**
     * Execution ID
     */
    private String executionId;
    /**
     * Start time
     */
    @Builder.Default
    private LocalDateTime startTime = LocalDateTime.now();
    /**
     * End time
     */
    private LocalDateTime endTime;
    /**
     * File parsing time in milliseconds
     */
    private Long parsingTimeMs;
    /**
     * AI invocation time in milliseconds
     */
    private Long aiInvocationTimeMs;
    /**
     * Result processing time in milliseconds
     */
    private Long resultProcessingTimeMs;
    /**
     * File source configuration (new approach)
     * @since 1.1.0
     */
    private FileSourceConfig fileSourceConfig;
    /**
     * Target directory to process (deprecated, use fileSourceConfig instead)
     * @deprecated Use {@link #fileSourceConfig} instead for more flexible file source support
     */
    @Deprecated
    private Path targetDirectory;
    /**
     * Include patterns
     */
    private List<String> includePatterns;
    /**
     * Exclude patterns
     */
    private List<String> excludePatterns;

    /**
     * Maximum file size in bytes (files larger than this will be skipped)
     */
    private Long maxFileSize;

    /**
     * AI configuration
     */
    private AIConfig aiConfig;
    /**
     * Processor configuration
     */
    private ProcessorConfig processorConfig;
    /**
     * Custom context data
     */
    @Builder.Default
    private Map<String, Object> contextData = new ConcurrentHashMap<>();
    /**
     * Thread pool size
     */
    @Builder.Default
    private int threadPoolSize = 10;
    /**
     * Put value in context
     */
    public void put(String key, Object value) {
        contextData.put(key, value);
    }
    /**
     * Get value from context
     */
    public Object get(String key) {
        return contextData.get(key);
    }
}
