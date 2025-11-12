package top.yumbo.ai.reviewer.domain.core.exception;

/**
 * 文件系统异常
 *
 * 当文件系统操作失败时抛出此异常
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-12
 */
public class FileSystemException extends TechnicalException {

    /**
     * 构造函数
     *
     * @param message 错误消息
     */
    public FileSystemException(String message) {
        super(message);
    }

    /**
     * 构造函数（带原因）
     *
     * @param message 错误消息
     * @param cause 原始异常
     */
    public FileSystemException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 文件不存在异常
     *
     * @param filePath 文件路径
     * @return 文件系统异常
     */
    public static FileSystemException fileNotFound(String filePath) {
        return new FileSystemException("文件不存在: " + filePath);
    }

    /**
     * 文件读取失败异常
     *
     * @param filePath 文件路径
     * @param cause 原始异常
     * @return 文件系统异常
     */
    public static FileSystemException readFailed(String filePath, Throwable cause) {
        return new FileSystemException("文件读取失败: " + filePath, cause);
    }

    /**
     * 文件写入失败异常
     *
     * @param filePath 文件路径
     * @param cause 原始异常
     * @return 文件系统异常
     */
    public static FileSystemException writeFailed(String filePath, Throwable cause) {
        return new FileSystemException("文件写入失败: " + filePath, cause);
    }
}

