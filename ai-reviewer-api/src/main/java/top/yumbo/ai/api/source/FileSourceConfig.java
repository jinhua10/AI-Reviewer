package top.yumbo.ai.api.source;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for file sources
 * <p>
 * This class contains all necessary configuration for different types of file sources.
 * Each source type uses a subset of these fields.
 *
 * @author AI-Reviewer Team
 * @since 1.1.0
 */
@Data
@Builder
public class FileSourceConfig {

    // ========== Common Configuration ==========

    /**
     * Type of file source: "local", "sftp", "git", "s3", etc.
     */
    private String sourceType;

    /**
     * Base path within the source (e.g., directory path, S3 prefix)
     */
    private String basePath;

    // ========== Network Configuration ==========

    /**
     * Host address (for SFTP, FTP, etc.)
     */
    private String host;

    /**
     * Port number (for SFTP, FTP, etc.)
     */
    private Integer port;

    /**
     * Username for authentication
     */
    private String username;

    /**
     * Password for authentication
     */
    private String password;

    /**
     * Path to private key file (for SSH authentication)
     */
    private String privateKeyPath;

    /**
     * Passphrase for private key
     */
    private String privateKeyPassphrase;

    // ========== Git Configuration ==========

    /**
     * Git repository URL (HTTPS or SSH)
     */
    private String repositoryUrl;

    /**
     * Git branch name
     */
    private String branch;

    /**
     * Specific commit ID to checkout
     */
    private String commitId;

    /**
     * Git access token (for HTTPS authentication)
     */
    private String accessToken;

    // ========== AWS S3 Configuration ==========

    /**
     * AWS access key ID
     */
    private String accessKey;

    /**
     * AWS secret access key
     */
    private String secretKey;

    /**
     * AWS region
     */
    private String region;

    /**
     * S3 bucket name
     */
    private String bucket;

    /**
     * S3 endpoint URL (for S3-compatible services)
     */
    private String endpoint;

    // ========== Timeout Configuration ==========

    /**
     * Connection timeout in milliseconds
     */
    @Builder.Default
    private int connectionTimeout = 30000;

    /**
     * Read timeout in milliseconds
     */
    @Builder.Default
    private int readTimeout = 60000;

    // ========== Custom Parameters ==========

    /**
     * Custom parameters for specific source types
     */
    @Builder.Default
    private Map<String, Object> customParams = new HashMap<>();

    /**
     * Put a custom parameter
     *
     * @param key parameter key
     * @param value parameter value
     */
    public void putCustomParam(String key, Object value) {
        if (customParams == null) {
            customParams = new HashMap<>();
        }
        customParams.put(key, value);
    }

    /**
     * Get a custom parameter
     *
     * @param key parameter key
     * @return parameter value, or null if not found
     */
    public Object getCustomParam(String key) {
        return customParams != null ? customParams.get(key) : null;
    }
}

