package top.yumbo.ai.reviewer.domain.core.exception;

/**
 * 仓库访问异常
 *
 * 当无法访问代码仓库时抛出此异常
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-12
 */
public class RepositoryAccessException extends DomainException {

    private static final String ERROR_CODE = "REPOSITORY_ACCESS_FAILED";

    /**
     * 构造函数
     *
     * @param url 仓库URL
     * @param reason 失败原因
     */
    public RepositoryAccessException(String url, String reason) {
        super("仓库访问失败", ERROR_CODE);
        with("url", url);
        with("reason", reason);
    }

    /**
     * 构造函数（带原因异常）
     *
     * @param url 仓库URL
     * @param reason 失败原因
     * @param cause 原始异常
     */
    public RepositoryAccessException(String url, String reason, Throwable cause) {
        super("仓库访问失败", ERROR_CODE, cause);
        with("url", url);
        with("reason", reason);
    }
}

