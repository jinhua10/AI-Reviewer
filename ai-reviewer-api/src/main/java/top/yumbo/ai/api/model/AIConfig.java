package top.yumbo.ai.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * AI service configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    /**
     * System prompt
     */
    private String sysPrompt = "You are a code review assistant. Analyze the provided code and give constructive feedback.";
    /**
     * User prompt template
     */
    private String userPrompt = "Please review this code:\n\n%s";
    /**
     * Temperature (0.0 to 2.0)
     */
    private Double temperature = 0.7;
    /**
     * Max tokens to generate
     */
    private Integer maxTokens = 2000;
    /**
     * Timeout in seconds
     */
    private Integer timeoutSeconds = 30;
    /**
     * Max retry attempts
     */
    private Integer maxRetries = 3;
    /**
     * Custom parameters
     */
    private Map<String, Object> customParams;
}
