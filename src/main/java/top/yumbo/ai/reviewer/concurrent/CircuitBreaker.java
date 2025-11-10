package top.yumbo.ai.reviewer.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 断路器模式实现
 * 当连续失败次数达到阈值时，自动打开断路器，避免级联失败
 *
 * 状态：CLOSED（正常） → OPEN（断开） → HALF_OPEN（半开） → CLOSED
 */
public class CircuitBreaker {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreaker.class);

    private enum State {
        CLOSED,      // 正常状态，允许请求通过
        OPEN,        // 断开状态，拒绝所有请求
        HALF_OPEN    // 半开状态，允许少量请求测试
    }

    private final int failureThreshold;           // 失败阈值
    private final long resetTimeoutMs;            // 重置超时（毫秒）
    private final AtomicInteger consecutiveFailures;
    private final AtomicInteger successCount;
    private final AtomicLong lastFailureTime;
    private volatile State state;

    public CircuitBreaker(int failureThreshold, long resetTimeoutMs) {
        this.failureThreshold = failureThreshold;
        this.resetTimeoutMs = resetTimeoutMs;
        this.consecutiveFailures = new AtomicInteger(0);
        this.successCount = new AtomicInteger(0);
        this.lastFailureTime = new AtomicLong(0);
        this.state = State.CLOSED;
    }

    /**
     * 默认配置：5 次失败后打开，30 秒后尝试恢复
     */
    public CircuitBreaker() {
        this(5, 30000);
    }

    /**
     * 检查是否允许请求通过
     */
    public boolean allowRequest() {
        if (state == State.CLOSED) {
            return true;
        }

        if (state == State.OPEN) {
            // 检查是否超过重置超时
            long timeSinceLastFailure = System.currentTimeMillis() - lastFailureTime.get();
            if (timeSinceLastFailure >= resetTimeoutMs) {
                log.info("断路器进入半开状态，尝试恢复");
                state = State.HALF_OPEN;
                return true;
            }
            return false;
        }

        // HALF_OPEN 状态：允许少量请求测试
        return true;
    }

    /**
     * 记录成功调用
     */
    public void recordSuccess() {
        consecutiveFailures.set(0);

        if (state == State.HALF_OPEN) {
            int successes = successCount.incrementAndGet();
            if (successes >= 3) {  // 连续 3 次成功后关闭断路器
                log.info("断路器关闭，恢复正常");
                state = State.CLOSED;
                successCount.set(0);
            }
        }
    }

    /**
     * 记录失败调用
     */
    public void recordFailure() {
        lastFailureTime.set(System.currentTimeMillis());
        int failures = consecutiveFailures.incrementAndGet();

        if (state == State.HALF_OPEN) {
            log.warn("半开状态测试失败，重新打开断路器");
            state = State.OPEN;
            successCount.set(0);
        } else if (failures >= failureThreshold) {
            log.warn("连续失败 {} 次，打开断路器", failures);
            state = State.OPEN;
        }
    }

    /**
     * 强制重置断路器
     */
    public void reset() {
        consecutiveFailures.set(0);
        successCount.set(0);
        state = State.CLOSED;
        log.info("断路器已重置");
    }

    /**
     * 获取当前状态
     */
    public boolean isOpen() {
        return state == State.OPEN;
    }

    public State getState() {
        return state;
    }

    public int getFailureCount() {
        return consecutiveFailures.get();
    }
}

