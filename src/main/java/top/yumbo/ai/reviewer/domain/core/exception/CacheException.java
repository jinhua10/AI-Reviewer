package top.yumbo.ai.reviewer.domain.core.exception;

/**
 * 缓存异常
 *
 * 当缓存操作失败时抛出此异常
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-12
 */
public class CacheException extends TechnicalException {

    /**
     * 构造函数
     *
     * @param operation 操作名称
     */
    public CacheException(String operation) {
        super("缓存操作失败: " + operation);
    }

    /**
     * 构造函数（带原因）
     *
     * @param operation 操作名称
     * @param cause 原始异常
     */
    public CacheException(String operation, Throwable cause) {
        super("缓存操作失败: " + operation, cause);
    }
}

