package top.yumbo.ai.reviewer.domain.core.exception;

/**
 * 分析失败异常
 *
 * 当项目分析过程失败时抛出此异常
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-12
 */
public class AnalysisFailedException extends DomainException {

    private static final String ERROR_CODE = "ANALYSIS_FAILED";

    /**
     * 构造函数
     *
     * @param reason 失败原因
     */
    public AnalysisFailedException(String reason) {
        super("项目分析失败", ERROR_CODE);
        with("reason", reason);
    }

    /**
     * 构造函数（带原因异常）
     *
     * @param reason 失败原因
     * @param cause 原始异常
     */
    public AnalysisFailedException(String reason, Throwable cause) {
        super("项目分析失败", ERROR_CODE, cause);
        with("reason", reason);
    }

    /**
     * 构造函数（带项目信息）
     *
     * @param projectId 项目ID
     * @param reason 失败原因
     */
    public AnalysisFailedException(String projectId, String reason) {
        super("项目分析失败", ERROR_CODE);
        with("projectId", projectId);
        with("reason", reason);
    }
}

