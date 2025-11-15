package top.yumbo.ai.reviewer.infrastructure.exception;

import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

/**
 * Global exception handler
 * Unified exception handling, retry, and fallback logic
 *
 * @author AI-Reviewer Team
 * @version 2.0
 */
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Execute operation with fallback value
     */
    public <T> T handleWithFallback(Supplier<T> operation, T fallbackValue, String operationName) {
        try {
            return operation.get();
        } catch (DomainException e) {
            log.warn("{} failed: {}", operationName, e.getMessage());
            return fallbackValue;
        } catch (TechnicalException e) {
            log.error("{} technical error", operationName, e);
            return fallbackValue;
        } catch (Exception e) {
            log.error("{} unexpected error", operationName, e);
            return fallbackValue;
        }
    }

    /**
     * Execute operation or throw exception
     */
    public <T> T handleOrThrow(Supplier<T> operation, String operationName) {
        try {
            return operation.get();
        } catch (DomainException e) {
            log.warn("{} failed: {}", operationName, e.getMessage());
            throw e;
        } catch (TechnicalException e) {
            log.error("{} technical error", operationName, e);
            throw e;
        } catch (Exception e) {
            log.error("{} unexpected error", operationName, e);
            throw new TechnicalException(operationName + " failed", e);
        }
    }

    /**
     * Execute operation with retry (exponential backoff)
     */
    public void handleWithRetry(Runnable operation, int maxRetries, String operationName) {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetries) {
            try {
                operation.run();
                if (attempt > 0) {
                    log.info("{} succeeded after {} attempts", operationName, attempt + 1);
                }
                return;
            } catch (Exception e) {
                lastException = e;
                attempt++;
                log.warn("{} failed (attempt {}/{}): {}",
                    operationName, attempt, maxRetries, e.getMessage());

                if (attempt < maxRetries) {
                    try {
                        long sleepTime = (long) Math.pow(2, attempt - 1) * 1000;
                        log.debug("Waiting {} ms before retry", sleepTime);
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("{} interrupted", operationName);
                        break;
                    }
                }
            }
        }

        log.error("{} finally failed after {} attempts", operationName, maxRetries, lastException);
        throw new TechnicalException(operationName + " failed", lastException);
    }

    /**
     * Execute operation with retry (with return value)
     */
    public <T> T handleWithRetry(Supplier<T> operation, int maxRetries, String operationName) {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetries) {
            try {
                T result = operation.get();
                if (attempt > 0) {
                    log.info("{} succeeded after {} attempts", operationName, attempt + 1);
                }
                return result;
            } catch (Exception e) {
                lastException = e;
                attempt++;
                log.warn("{} failed (attempt {}/{}): {}",
                    operationName, attempt, maxRetries, e.getMessage());

                if (attempt < maxRetries) {
                    try {
                        long sleepTime = (long) Math.pow(2, attempt - 1) * 1000;
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("{} interrupted", operationName);
                        break;
                    }
                }
            }
        }

        log.error("{} finally failed after {} attempts", operationName, maxRetries, lastException);
        throw new TechnicalException(operationName + " failed", lastException);
    }

    /**
     * Execute operation silently (ignore exceptions)
     */
    public void handleSilently(Runnable operation, String operationName) {
        try {
            operation.run();
        } catch (Exception e) {
            log.debug("{} failed (ignored): {}", operationName, e.getMessage());
        }
    }
}

