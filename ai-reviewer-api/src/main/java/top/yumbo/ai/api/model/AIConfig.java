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
    private String sysPrompt;
    /**
     * User prompt template
     */
    private String userPrompt;
    /**
     * Temperature (0.0 to 2.0)
     */
    private Double temperature;
    /**
     * Max tokens to generate
     */
    private Integer maxTokens;
    /**
     * Timeout in seconds
     */
    private Integer timeoutSeconds;
    /**
     * Max retry attempts
     */
    private Integer maxRetries;
    /**
     * Custom parameters
     */
    private Map<String, Object> customParams;
}
