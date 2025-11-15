package top.yumbo.ai.api.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * AI service configuration
 */
@Data
@Builder
public class AIConfig {
    /**
     * AI provider name
     */
    private String provider;
    private String region;
    /**
     * Model identifier
     */
    private String model;
    /**
     * API key
     */
    private String apiKey;
    /**
     * API endpoint URL
     */
    private String endpoint;
    @Builder.Default
    private String sysPrompt = "You are a code review assistant. Analyze the provided code and give constructive feedback.";    @Builder.Default
    private String userPrompt = "Please review this code:\n\n%s";
    /**
     * Temperature (0.0 to 2.0)
     */
    @Builder.Default
    private Double temperature = 0.7;
    /**
     * Max tokens to generate
     */
    @Builder.Default
    private Integer maxTokens = 2000;
    /**
     * Timeout in seconds
     */
    @Builder.Default
    private Integer timeoutSeconds = 30;
    /**
     * Max retry attempts
     */
    @Builder.Default
    private Integer maxRetries = 3;
    /**
     * Custom parameters
     */
    private Map<String, Object> customParams;
}
