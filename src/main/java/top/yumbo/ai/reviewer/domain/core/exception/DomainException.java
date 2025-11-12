package top.yumbo.ai.reviewer.domain.core.exception;

import java.util.HashMap;
import java.util.Map;

/**
 * 领域异常基类
 *
 * 所有业务异常都应继承此类。领域异常表示业务规则违反或业务流程错误。
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-12
 */
public abstract class DomainException extends RuntimeException {

    private final String errorCode;
    private final Map<String, Object> context;

    /**
     * 构造函数
     *
     * @param message 错误消息
     * @param errorCode 错误代码
     */
    protected DomainException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.context = new HashMap<>();
    }

    /**
     * 构造函数（带原因）
     *
     * @param message 错误消息
     * @param errorCode 错误代码
     * @param cause 原始异常
     */
    protected DomainException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.context = new HashMap<>();
    }

    /**
     * 添加上下文信息
     *
     * @param key 键
     * @param value 值
     * @return 当前异常实例（支持链式调用）
     */
    public DomainException with(String key, Object value) {
        context.put(key, value);
        return this;
    }

    /**
     * 获取错误代码
     *
     * @return 错误代码
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * 获取上下文信息
     *
     * @return 上下文信息映射
     */
    public Map<String, Object> getContext() {
        return new HashMap<>(context);
    }

    /**
     * 获取完整的错误信息（包含上下文）
     *
     * @return 完整错误信息
     */
    public String getFullMessage() {
        if (context.isEmpty()) {
            return getMessage();
        }
        return String.format("%s [错误代码: %s, 上下文: %s]",
                           getMessage(), errorCode, context);
    }

    @Override
    public String toString() {
        return String.format("%s: %s [%s]",
                           getClass().getSimpleName(), getMessage(), errorCode);
    }
}

