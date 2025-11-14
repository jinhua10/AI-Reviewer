package top.yumbo.ai.reviewer.adapter.storage.s3;

import lombok.Builder;

/**
 * S3 存储配置
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-14
 */
@Builder
public record S3StorageConfig(
        String region,
        String bucketName,
        String accessKeyId,
        String secretAccessKey,
        Integer maxConcurrency,
        Integer connectTimeout,
        Integer readTimeout,
        Integer maxRetries,
        Integer retryDelay,
        Boolean useAccelerateEndpoint,
        Boolean usePathStyleAccess,
        String endpoint
) {
    /**
     * 创建默认配置
     */
    public static S3StorageConfig createDefault() {
        return S3StorageConfig.builder()
                .region("us-east-1")
                .maxConcurrency(10)
                .connectTimeout(30000)
                .readTimeout(60000)
                .maxRetries(3)
                .retryDelay(1000)
                .useAccelerateEndpoint(false)
                .usePathStyleAccess(false)
                .build();
    }

    /**
     * 是否使用默认凭证链（没有提供 accessKeyId）
     */
    public boolean useDefaultCredentials() {
        return accessKeyId == null || accessKeyId.isEmpty();
    }

    /**
     * 获取有效的最大并发数
     */
    public int getEffectiveMaxConcurrency() {
        return maxConcurrency != null && maxConcurrency > 0 ? maxConcurrency : 10;
    }

    /**
     * 获取有效的连接超时时间
     */
    public int getEffectiveConnectTimeout() {
        return connectTimeout != null && connectTimeout > 0 ? connectTimeout : 30000;
    }

    /**
     * 获取有效的读取超时时间
     */
    public int getEffectiveReadTimeout() {
        return readTimeout != null && readTimeout > 0 ? readTimeout : 60000;
    }

    /**
     * 获取有效的最大重试次数
     */
    public int getEffectiveMaxRetries() {
        return maxRetries != null && maxRetries >= 0 ? maxRetries : 3;
    }

    /**
     * 获取有效的重试延迟时间
     */
    public int getEffectiveRetryDelay() {
        return retryDelay != null && retryDelay > 0 ? retryDelay : 1000;
    }

    /**
     * 是否使用加速端点
     */
    public boolean isUseAccelerateEndpoint() {
        return useAccelerateEndpoint != null && useAccelerateEndpoint;
    }

    /**
     * 是否使用路径样式访问
     */
    public boolean isUsePathStyleAccess() {
        return usePathStyleAccess != null && usePathStyleAccess;
    }

    /**
     * 是否配置了自定义端点
     */
    public boolean hasCustomEndpoint() {
        return endpoint != null && !endpoint.isEmpty();
    }
}

