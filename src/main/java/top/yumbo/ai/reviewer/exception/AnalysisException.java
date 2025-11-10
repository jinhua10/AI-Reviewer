package top.yumbo.ai.reviewer.exception;

/**
 * 统一的分析异常类
 * 简化了异常处理，所有业务异常都继承此类
 */
public class AnalysisException extends Exception {

    private final ErrorType errorType;

    public enum ErrorType {
        CONFIG_ERROR,       // 配置错误
        FILE_ERROR,         // 文件错误
        AI_ERROR,           // AI 调用错误
        NETWORK_ERROR,      // 网络错误
        VALIDATION_ERROR,   // 验证错误
        UNKNOWN_ERROR       // 未知错误
    }

    public AnalysisException(String message) {
        this(message, ErrorType.UNKNOWN_ERROR);
    }

    public AnalysisException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    public AnalysisException(String message, Throwable cause) {
        this(message, ErrorType.UNKNOWN_ERROR, cause);
    }

    public AnalysisException(String message, ErrorType errorType, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    // 静态工厂方法
    public static AnalysisException configError(String message) {
        return new AnalysisException(message, ErrorType.CONFIG_ERROR);
    }

    public static AnalysisException fileError(String message, Throwable cause) {
        return new AnalysisException(message, ErrorType.FILE_ERROR, cause);
    }

    public static AnalysisException aiError(String message, Throwable cause) {
        return new AnalysisException(message, ErrorType.AI_ERROR, cause);
    }

    public static AnalysisException networkError(String message, Throwable cause) {
        return new AnalysisException(message, ErrorType.NETWORK_ERROR, cause);
    }

    public static AnalysisException validationError(String message) {
        return new AnalysisException(message, ErrorType.VALIDATION_ERROR);
    }
}

