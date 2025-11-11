package top.yumbo.ai.reviewer.exception;

/**
 * 分析异常类
 * 
 * 用于封装分析过程中发生的异常
 */
public class AnalysisException extends RuntimeException {

    /**
     * 错误类型枚举
     */
    public enum ErrorType {
        FILE_SCAN_ERROR,      // 文件扫描错误
        FILE_PROCESS_ERROR,    // 文件处理错误
        AI_SERVICE_ERROR,      // AI服务错误
        REPORT_GENERATION_ERROR, // 报告生成错误
        CONFIGURATION_ERROR,   // 配置错误
        IO_ERROR,              // IO错误
        UNKNOWN_ERROR          // 未知错误
    }

    private final ErrorType errorType;

    /**
     * 构造函数
     * 
     * @param message 错误消息
     */
    public AnalysisException(String message) {
        this(message, ErrorType.UNKNOWN_ERROR, null);
    }

    /**
     * 构造函数
     * 
     * @param message 错误消息
     * @param cause 原因
     */
    public AnalysisException(String message, Throwable cause) {
        this(message, ErrorType.UNKNOWN_ERROR, cause);
    }

    /**
     * 构造函数
     * 
     * @param message 错误消息
     * @param errorType 错误类型
     */
    public AnalysisException(String message, ErrorType errorType) {
        this(message, errorType, null);
    }

    /**
     * 构造函数
     * 
     * @param message 错误消息
     * @param errorType 错误类型
     * @param cause 原因
     */
    public AnalysisException(String message, ErrorType errorType, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    /**
     * 获取错误类型
     * 
     * @return 错误类型
     */
    public ErrorType getErrorType() {
        return errorType;
    }
}
