package top.yumbo.ai.reviewer.application.port.output;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 代码仓库端口
 *
 * 统一的代码仓库接口，支持多种代码托管平台（GitHub, Gitee, GitLab 等）
 *
 * @author AI-Reviewer Team
 * @version 2.0
 * @since 2025-11-12
 */
public interface RepositoryPort {

    /**
     * 克隆仓库到本地
     *
     * @param request 克隆请求
     * @return 本地路径
     * @throws RepositoryException 如果克隆失败
     */
    Path cloneRepository(CloneRequest request) throws RepositoryException;

    /**
     * 克隆指定 commit 的代码
     *
     * @param repositoryUrl 仓库 URL
     * @param commitHash commit 哈希值
     * @return 本地路径
     * @throws RepositoryException 如果克隆失败
     */
    Path cloneRepositoryAtCommit(String repositoryUrl, String commitHash) throws RepositoryException;

    /**
     * 检查仓库是否可访问
     *
     * @param repositoryUrl 仓库URL
     * @return 如果可访问返回 true
     */
    boolean isAccessible(String repositoryUrl);

    /**
     * 获取仓库指标
     *
     * @param repositoryUrl 仓库URL
     * @return 仓库指标
     * @throws RepositoryException 如果获取失败
     */
    RepositoryMetrics getMetrics(String repositoryUrl) throws RepositoryException;

    /**
     * 获取默认分支
     *
     * @param repositoryUrl 仓库URL
     * @return 默认分支名称
     * @throws RepositoryException 如果获取失败
     */
    String getDefaultBranch(String repositoryUrl) throws RepositoryException;

    /**
     * 检查文件是否存在
     *
     * @param repositoryUrl 仓库URL
     * @param filePath 文件路径
     * @return 如果文件存在返回 true
     */
    boolean hasFile(String repositoryUrl, String filePath);

    /**
     * 获取仓库大小（字节）
     *
     * @param repositoryUrl 仓库 URL
     * @return 仓库大小，-1 表示无法获取
     * @throws RepositoryException 如果获取失败
     */
    long getRepositorySize(String repositoryUrl) throws RepositoryException;

    /**
     * 获取分支列表
     *
     * @param repositoryUrl 仓库 URL
     * @return 分支列表
     * @throws RepositoryException 如果获取失败
     */
    List<String> getBranches(String repositoryUrl) throws RepositoryException;

    /**
     * 提交信息
     */
    record CommitInfo(
        String hash,
        String author,
        String message,
        LocalDateTime timestamp
    ) {}

    /**
     * 仓库异常
     */
    class RepositoryException extends Exception {
        public RepositoryException(String message) {
            super(message);
        }

        public RepositoryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

