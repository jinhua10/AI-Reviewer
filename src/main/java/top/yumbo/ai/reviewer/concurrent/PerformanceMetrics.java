package top.yumbo.ai.reviewer.concurrent;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能指标统计
 * 记录并发执行的各项指标
 */
public class PerformanceMetrics {

    // 请求统计
    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger successfulRequests = new AtomicInteger(0);
    private final AtomicInteger failedRequests = new AtomicInteger(0);
    private final AtomicInteger retriedRequests = new AtomicInteger(0);

    // 错误分类
    private final AtomicInteger networkErrors = new AtomicInteger(0);
    private final AtomicInteger aiErrors = new AtomicInteger(0);
    private final AtomicInteger rateLimitErrors = new AtomicInteger(0);
    private final AtomicInteger otherErrors = new AtomicInteger(0);

    // 时间统计
    private final AtomicLong totalLatencyMs = new AtomicLong(0);
    private final AtomicLong minLatencyMs = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxLatencyMs = new AtomicLong(0);

    // 断路器统计
    private final AtomicInteger circuitBreakerOpenCount = new AtomicInteger(0);

    /**
     * 记录请求开始
     */
    public void recordRequestStart() {
        totalRequests.incrementAndGet();
    }

    /**
     * 记录请求成功
     */
    public void recordSuccess(long latencyMs) {
        successfulRequests.incrementAndGet();
        recordLatency(latencyMs);
    }

    /**
     * 记录请求失败
     */
    public void recordFailure(String errorType) {
        failedRequests.incrementAndGet();

        switch (errorType.toUpperCase()) {
            case "NETWORK_ERROR" -> networkErrors.incrementAndGet();
            case "AI_ERROR" -> aiErrors.incrementAndGet();
            case "RATE_LIMIT" -> rateLimitErrors.incrementAndGet();
            default -> otherErrors.incrementAndGet();
        }
    }

    /**
     * 记录重试
     */
    public void recordRetry() {
        retriedRequests.incrementAndGet();
    }

    /**
     * 记录断路器打开
     */
    public void recordCircuitBreakerOpen() {
        circuitBreakerOpenCount.incrementAndGet();
    }

    /**
     * 记录延迟
     */
    private void recordLatency(long latencyMs) {
        totalLatencyMs.addAndGet(latencyMs);

        // 更新最小延迟
        minLatencyMs.updateAndGet(current -> Math.min(current, latencyMs));

        // 更新最大延迟
        maxLatencyMs.updateAndGet(current -> Math.max(current, latencyMs));
    }

    /**
     * 获取平均延迟
     */
    public double getAverageLatencyMs() {
        int successful = successfulRequests.get();
        if (successful == 0) {
            return 0;
        }
        return (double) totalLatencyMs.get() / successful;
    }

    /**
     * 获取成功率
     */
    public double getSuccessRate() {
        int total = totalRequests.get();
        if (total == 0) {
            return 0;
        }
        return (double) successfulRequests.get() / total * 100;
    }

    /**
     * 生成统计报告
     */
    public String generateReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== 性能指标报告 ===\n");
        report.append(String.format("总请求数: %d\n", totalRequests.get()));
        report.append(String.format("成功请求: %d\n", successfulRequests.get()));
        report.append(String.format("失败请求: %d\n", failedRequests.get()));
        report.append(String.format("重试次数: %d\n", retriedRequests.get()));
        report.append(String.format("成功率: %.2f%%\n", getSuccessRate()));
        report.append(String.format("\n延迟统计:\n"));
        report.append(String.format("  平均: %.2f ms\n", getAverageLatencyMs()));
        report.append(String.format("  最小: %d ms\n", minLatencyMs.get() == Long.MAX_VALUE ? 0 : minLatencyMs.get()));
        report.append(String.format("  最大: %d ms\n", maxLatencyMs.get()));
        report.append(String.format("\n错误分类:\n"));
        report.append(String.format("  网络错误: %d\n", networkErrors.get()));
        report.append(String.format("  AI 错误: %d\n", aiErrors.get()));
        report.append(String.format("  限流错误: %d\n", rateLimitErrors.get()));
        report.append(String.format("  其他错误: %d\n", otherErrors.get()));
        report.append(String.format("\n断路器打开次数: %d\n", circuitBreakerOpenCount.get()));
        return report.toString();
    }

    // Getters
    public int getTotalRequests() { return totalRequests.get(); }
    public int getSuccessfulRequests() { return successfulRequests.get(); }
    public int getFailedRequests() { return failedRequests.get(); }
    public int getRetriedRequests() { return retriedRequests.get(); }
    public long getMinLatencyMs() { return minLatencyMs.get() == Long.MAX_VALUE ? 0 : minLatencyMs.get(); }
    public long getMaxLatencyMs() { return maxLatencyMs.get(); }
}

