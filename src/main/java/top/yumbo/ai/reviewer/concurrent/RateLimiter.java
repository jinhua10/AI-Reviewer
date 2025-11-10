package top.yumbo.ai.reviewer.concurrent;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 速率限制器
 * 使用令牌桶算法，限制 API 调用频率
 */
public class RateLimiter {

    private final Semaphore semaphore;
    private final int maxPermits;
    private final long refillIntervalMs;
    private final AtomicInteger availablePermits;
    private volatile long lastRefillTime;

    public RateLimiter(int maxPermitsPerSecond) {
        this.maxPermits = maxPermitsPerSecond;
        this.refillIntervalMs = 1000; // 每秒补充
        this.semaphore = new Semaphore(maxPermits);
        this.availablePermits = new AtomicInteger(maxPermits);
        this.lastRefillTime = System.currentTimeMillis();
    }

    /**
     * 获取许可（阻塞）
     */
    public void acquire() throws InterruptedException {
        refillIfNeeded();
        semaphore.acquire();
        availablePermits.decrementAndGet();
    }

    /**
     * 尝试获取许可（非阻塞）
     */
    public boolean tryAcquire() {
        refillIfNeeded();
        boolean acquired = semaphore.tryAcquire();
        if (acquired) {
            availablePermits.decrementAndGet();
        }
        return acquired;
    }

    /**
     * 尝试获取许可（带超时）
     */
    public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
        refillIfNeeded();
        boolean acquired = semaphore.tryAcquire(timeout, unit);
        if (acquired) {
            availablePermits.decrementAndGet();
        }
        return acquired;
    }

    /**
     * 补充令牌
     */
    private void refillIfNeeded() {
        long now = System.currentTimeMillis();
        long timeSinceLastRefill = now - lastRefillTime;

        if (timeSinceLastRefill >= refillIntervalMs) {
            synchronized (this) {
                if (now - lastRefillTime >= refillIntervalMs) {
                    int permitsToAdd = maxPermits - availablePermits.get();
                    if (permitsToAdd > 0) {
                        semaphore.release(permitsToAdd);
                        availablePermits.addAndGet(permitsToAdd);
                    }
                    lastRefillTime = now;
                }
            }
        }
    }

    /**
     * 获取可用许可数
     */
    public int getAvailablePermits() {
        return availablePermits.get();
    }
}

