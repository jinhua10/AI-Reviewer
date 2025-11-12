package top.yumbo.ai.reviewer.adapter.output.repository;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.yumbo.ai.reviewer.application.port.output.CloneRequest;
import top.yumbo.ai.reviewer.application.port.output.RepositoryMetrics;
import top.yumbo.ai.reviewer.application.port.output.RepositoryPort;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * GitHub 仓库适配器
 * 实现统一的 RepositoryPort 接口
 *
 * @author AI-Reviewer Team
 * @version 2.0
 * @since 2025-11-12
 */
public class GitHubRepositoryAdapter implements RepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(GitHubRepositoryAdapter.class);

    private final Path workingDirectory;
    private final int cloneTimeout;
    private final int cloneDepth;

    public GitHubRepositoryAdapter(Path workingDirectory, int cloneTimeout, int cloneDepth) {
        this.workingDirectory = workingDirectory;
        this.cloneTimeout = cloneTimeout;
        this.cloneDepth = cloneDepth;

        try {
            Files.createDirectories(workingDirectory);
        } catch (IOException e) {
            throw new RuntimeException("无法创建工作目录: " + workingDirectory, e);
        }
    }

    public GitHubRepositoryAdapter(Path workingDirectory) {
        this(workingDirectory, 300, 1);
    }

    @Override
    public Path cloneRepository(CloneRequest request) throws RepositoryException {
        log.info("克隆 GitHub 仓库: {}, 分支: {}", request.url(), request.branch());

        validateGitHubUrl(request.url());

        String repoName = extractRepositoryName(request.url());
        Path localPath = workingDirectory.resolve(repoName + "-" + System.currentTimeMillis());

        try {
            Files.createDirectories(localPath);

            Git git = Git.cloneRepository()
                    .setURI(request.url())
                    .setDirectory(localPath.toFile())
                    .setBranch(request.branch())
                    .setDepth(cloneDepth)
                    .setTimeout(request.timeoutSeconds())
                    .call();

            git.close();

            log.info("仓库克隆完成: {}", localPath);
            return localPath;

        } catch (GitAPIException | IOException e) {
            cleanupDirectory(localPath);
            log.error("克隆仓库失败", e);
            throw new RepositoryException("克隆 GitHub 仓库失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Path cloneRepositoryAtCommit(String repositoryUrl, String commitHash) throws RepositoryException {
        log.info("克隆 GitHub 仓库到指定 commit: {}, commit: {}", repositoryUrl, commitHash);

        validateGitHubUrl(repositoryUrl);

        String repoName = extractRepositoryName(repositoryUrl);
        Path localPath = workingDirectory.resolve(repoName + "-" + commitHash);

        try {
            Files.createDirectories(localPath);

            Git git = Git.cloneRepository()
                    .setURI(repositoryUrl)
                    .setDirectory(localPath.toFile())
                    .setTimeout(cloneTimeout)
                    .call();

            git.checkout()
                    .setName(commitHash)
                    .call();

            git.close();

            log.info("仓库克隆到指定 commit 完成: {}", localPath);
            return localPath;

        } catch (GitAPIException | IOException e) {
            cleanupDirectory(localPath);
            log.error("克隆到指定 commit 失败", e);
            throw new RepositoryException("克隆失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAccessible(String repositoryUrl) {
        try {
            validateGitHubUrl(repositoryUrl);

            Collection<Ref> refs = Git.lsRemoteRepository()
                    .setRemote(repositoryUrl)
                    .setTimeout(10)
                    .call();

            return refs != null && !refs.isEmpty();

        } catch (Exception e) {
            log.debug("仓库不可访问: {}", repositoryUrl, e);
            return false;
        }
    }

    @Override
    public RepositoryMetrics getMetrics(String repositoryUrl) throws RepositoryException {
        throw new UnsupportedOperationException("GitHub metrics 暂未实现，建议使用 GitHub API");
    }

    @Override
    public String getDefaultBranch(String repositoryUrl) throws RepositoryException {
        try {
            validateGitHubUrl(repositoryUrl);

            Collection<Ref> refs = Git.lsRemoteRepository()
                    .setRemote(repositoryUrl)
                    .setHeads(true)
                    .setTimeout(10)
                    .call();

            for (Ref ref : refs) {
                String refName = ref.getName();
                if (refName.equals("refs/heads/main")) {
                    return "main";
                }
                if (refName.equals("refs/heads/master")) {
                    return "master";
                }
            }

            return "main";

        } catch (Exception e) {
            log.error("获取默认分支失败", e);
            throw new RepositoryException("获取默认分支失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean hasFile(String repositoryUrl, String filePath) {
        // 需要使用 GitHub API 实现
        throw new UnsupportedOperationException("hasFile 暂未实现，建议使用 GitHub API");
    }

    @Override
    public long getRepositorySize(String repositoryUrl) throws RepositoryException {
        // 需要使用 GitHub API 实现
        throw new UnsupportedOperationException("getRepositorySize 暂未实现，建议使用 GitHub API");
    }

    @Override
    public List<String> getBranches(String repositoryUrl) throws RepositoryException {
        try {
            validateGitHubUrl(repositoryUrl);

            Collection<Ref> refs = Git.lsRemoteRepository()
                    .setRemote(repositoryUrl)
                    .setHeads(true)
                    .call();

            return refs.stream()
                    .map(Ref::getName)
                    .map(name -> name.replace("refs/heads/", ""))
                    .toList();

        } catch (Exception e) {
            log.error("获取分支列表失败", e);
            throw new RepositoryException("获取分支列表失败: " + e.getMessage(), e);
        }
    }

    private void validateGitHubUrl(String url) throws RepositoryException {
        if (url == null || url.isBlank()) {
            throw new RepositoryException("仓库 URL 不能为空");
        }

        if (!url.contains("github.com")) {
            throw new RepositoryException("非 GitHub 仓库 URL: " + url);
        }
    }

    private String extractRepositoryName(String url) {
        String[] parts = url.split("/");
        String lastPart = parts[parts.length - 1];
        return lastPart.replace(".git", "");
    }

    private void cleanupDirectory(Path directory) {
        try {
            if (Files.exists(directory)) {
                try (var stream = Files.walk(directory)) {
                    stream.sorted(java.util.Comparator.reverseOrder())
                            .forEach(path -> {
                                try {
                                    Files.delete(path);
                                } catch (IOException e) {
                                    log.warn("删除文件失败: {}", path, e);
                                }
                            });
                }
            }
        } catch (IOException e) {
            log.warn("清理目录失败: {}", directory, e);
        }
    }
}

