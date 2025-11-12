package top.yumbo.ai.reviewer.domain.core.exception;

/**
 * 项目未找到异常
 *
 * 当尝试访问不存在的项目时抛出此异常
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-12
 */
public class ProjectNotFoundException extends DomainException {

    private static final String ERROR_CODE = "PROJECT_NOT_FOUND";

    /**
     * 构造函数
     *
     * @param projectId 项目ID
     */
    public ProjectNotFoundException(String projectId) {
        super("项目不存在", ERROR_CODE);
        with("projectId", projectId);
    }

    /**
     * 构造函数（自定义消息）
     *
     * @param projectId 项目ID
     * @param message 自定义消息
     */
    public ProjectNotFoundException(String projectId, String message) {
        super(message, ERROR_CODE);
        with("projectId", projectId);
    }
}

