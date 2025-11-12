package top.yumbo.ai.reviewer.domain.core.exception;

/**
 * 技术异常基类
 *
 * 所有技术/基础设施异常都应继承此类。技术异常表示系统层面的错误，
 * 如网络故障、文件系统错误、外部服务调用失败等。
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-12
 */
public abstract class TechnicalException extends RuntimeException {

    /**
     * 构造函数
     *
     * @param message 错误消息
     */
    protected TechnicalException(String message) {
        super(message);
    }

    /**
     * 构造函数（带原因）
     *
     * @param message 错误消息
     * @param cause 原始异常
     */
    protected TechnicalException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String toString() {
        return String.format("%s: %s", getClass().getSimpleName(), getMessage());
    }
}

