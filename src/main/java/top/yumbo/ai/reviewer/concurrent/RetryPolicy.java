package top.yumbo.ai.reviewer.concurrent;

import top.yumbo.ai.reviewer.exception.AnalysisException;

/**
 * 重试策略
 * 实现指数退避算法，避免频繁重试导致的资源浪费
 */
public class RetryPolicy {

    private final int maxRetries;
    private final int initialDelayMs;
    private final int maxDelayMs;
    private final double multiplier;

    public RetryPolicy(int maxRetries, int initialDelayMs, int maxDelayMs, double multiplier) {
        this.maxRetries = maxRetries;
        this.initialDelayMs = initialDelayMs;
        this.maxDelayMs = maxDelayMs;
        this.multiplier = multiplier;
    }

    /**
     * 默认配置：最多重试 3 次，初始延迟 1 秒，最大延迟 32 秒，每次翻倍
     */
    public RetryPolicy() {
        this(3, 1000, 32000, 2.0);
    }

    /**
     * 判断错误类型是否应该重试
     */
    public boolean shouldRetry(AnalysisException.ErrorType errorType, int attemptNumber) {
        if (attemptNumber >= maxRetries) {
            return false;
        }

        // 只对特定类型的错误进行重试
        return switch (errorType) {
            case NETWORK_ERROR -> true;      // 网络错误：重试
            case AI_ERROR -> true;           // AI 服务错误：重试
            case CONFIG_ERROR -> false;      // 配置错误：不重试
            case FILE_ERROR -> false;        // 文件错误：不重试
            case VALIDATION_ERROR -> false;  // 验证错误：不重试
            case UNKNOWN_ERROR -> true;      // 未知错误：重试
        };
    }

    /**
     * 计算延迟时间（指数退避）
     * delay = initialDelay * (multiplier ^ attemptNumber)
     */
    public int calculateDelay(int attemptNumber) {
        double delay = initialDelayMs * Math.pow(multiplier, attemptNumber);
        return (int) Math.min(delay, maxDelayMs);
    }

    /**
     * 添加随机抖动，避免惊群效应
     */
    public int calculateDelayWithJitter(int attemptNumber) {
        int baseDelay = calculateDelay(attemptNumber);
        // 添加 ±25% 的随机抖动
        double jitter = baseDelay * 0.25 * (Math.random() * 2 - 1);
        return (int) (baseDelay + jitter);
    }

    public int getMaxRetries() {
        return maxRetries;
    }
}

