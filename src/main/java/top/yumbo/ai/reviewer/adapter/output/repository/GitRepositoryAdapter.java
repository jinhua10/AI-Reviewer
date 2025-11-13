package top.yumbo.ai.reviewer.adapter.output.repository;

import org.apache.commons.lang3.StringUtils;
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
import java.util.regex.Pattern;

/**
 * 统一的Git仓库适配器
 *
 * 支持多个Git托管平台：
 * - GitHub
 * - Gitee
 * - GitLab
 * - 其他自托管Git平台（包括IP地址）
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-13
 */
public class GitRepositoryAdapter implements RepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(GitRepositoryAdapter.class);

    // IP地址匹配模式
    private static final Pattern IP_PATTERN = Pattern.compile(
        "^(https?://|git@|ssh://)?\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"
    );

    private final Path workingDirectory;
    private final int cloneTimeout;
    private final int cloneDepth;

    public GitRepositoryAdapter(Path workingDirectory, int cloneTimeout, int cloneDepth) {
        this.workingDirectory = workingDirectory;
        this.cloneTimeout = cloneTimeout;
        this.cloneDepth = cloneDepth;

        try {
            Files.createDirectories(workingDirectory);
        } catch (IOException e) {
            throw new RuntimeException("无法创建工作目录: " + workingDirectory, e);
        }
    }

    public GitRepositoryAdapter(Path workingDirectory) {
        this(workingDirectory, 300, 1);
    }

    @Override
    public Path cloneRepository(CloneRequest request) throws RepositoryException {
        String platformInfo = detectPlatformInfo(request.url());
        log.info("克隆 {} 仓库: {}, 分支: {}", platformInfo, request.url(), request.branch());

        validateRepositoryUrl(request.url());

        String repoName = extractRepositoryName(request.url());
        Path localPath = workingDirectory.resolve(repoName + "-" + System.currentTimeMillis());

        try {
            Files.createDirectories(localPath);

            // 如果没有指定分支，使用平台默认分支
            String branch = StringUtils.isEmpty(request.branch()) ? null : request.branch();

            Git git = Git.cloneRepository()
                    .setURI(request.url())
                    .setDirectory(localPath.toFile())
                    .setBranch(branch)
                    .setDepth(cloneDepth)
                    .setTimeout(request.timeoutSeconds())
                    .call();

            git.close();

            log.info("仓库克隆完成: {} ({})", localPath, platformInfo);
            return localPath;

        } catch (GitAPIException | IOException e) {
            cleanupDirectory(localPath);
            log.error("克隆 {} 仓库失败", platformInfo, e);
            throw new RepositoryException("克隆仓库失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Path cloneRepositoryAtCommit(String repositoryUrl, String commitHash) throws RepositoryException {
        String platformInfo = detectPlatformInfo(repositoryUrl);
        log.info("克隆 {} 仓库到指定 commit: {}, commit: {}", platformInfo, repositoryUrl, commitHash);

        validateRepositoryUrl(repositoryUrl);

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

            log.info("仓库克隆到指定 commit 完成: {} ({})", localPath, platformInfo);
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
            validateRepositoryUrl(repositoryUrl);

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
    public RepositoryMetrics getMetrics(String repositoryUrl) {
        String platformInfo = detectPlatformInfo(repositoryUrl);
        throw new UnsupportedOperationException(
            platformInfo + " metrics 暂未实现，建议使用对应平台的 API"
        );
    }

    @Override
    public String getDefaultBranch(String repositoryUrl) throws RepositoryException {
        try {
            validateRepositoryUrl(repositoryUrl);

            Collection<Ref> refs = Git.lsRemoteRepository()
                    .setRemote(repositoryUrl)
                    .setHeads(true)
                    .setTimeout(10)
                    .call();

            // 优先检查常见的默认分支（按使用频率）
            String[] commonBranches = {"main", "master", "develop"};

            for (String branch : commonBranches) {
                for (Ref ref : refs) {
                    if (ref.getName().equals("refs/heads/" + branch)) {
                        return branch;
                    }
                }
            }

            // 如果都没有，返回第一个分支
            if (!refs.isEmpty()) {
                String firstBranch = refs.iterator().next().getName();
                return firstBranch.replace("refs/heads/", "");
            }

            // 默认返回 main
            return "main";

        } catch (Exception e) {
            log.error("获取默认分支失败", e);
            throw new RepositoryException("获取默认分支失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean hasFile(String repositoryUrl, String filePath) {
        throw new UnsupportedOperationException(
            "hasFile 暂未实现，建议使用对应平台的 API"
        );
    }

    @Override
    public long getRepositorySize(String repositoryUrl) {
        throw new UnsupportedOperationException(
            "getRepositorySize 暂未实现，建议使用对应平台的 API"
        );
    }

    @Override
    public List<String> getBranches(String repositoryUrl) throws RepositoryException {
        try {
            validateRepositoryUrl(repositoryUrl);

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

    /**
     * 验证仓库URL
     */
    private void validateRepositoryUrl(String url) throws RepositoryException {
        if (url == null || url.isBlank()) {
            throw new RepositoryException("仓库 URL 不能为空");
        }

        // 检查是否是有效的Git URL
        if (!url.startsWith("http://") && !url.startsWith("https://") &&
            !url.startsWith("git@") && !url.startsWith("ssh://")) {
            throw new RepositoryException("无效的 Git 仓库 URL: " + url);
        }
    }

    /**
     * 检测平台信息（用于日志显示）
     */
    private String detectPlatformInfo(String url) {
        if (url == null || url.isBlank()) {
            return "Unknown Git";
        }

        String lowerUrl = url.toLowerCase();

        // 检查是否是IP地址
        if (isIpAddress(url)) {
            return "Self-Hosted Git (IP: " + extractHostFromUrl(url) + ")";
        }

        // 检查公共平台
        if (lowerUrl.contains("github.com")) {
            return "GitHub";
        } else if (lowerUrl.contains("gitee.com")) {
            return "Gitee";
        } else if (lowerUrl.contains("gitlab.com")) {
            return "GitLab";
        } else if (lowerUrl.contains("gitlab")) {
            return "GitLab (Self-Hosted)";
        }

        // 其他自托管Git
        return "Self-Hosted Git (" + extractHostFromUrl(url) + ")";
    }

    /**
     * 检查URL是否是IP地址
     */
    private boolean isIpAddress(String url) {
        return url != null && IP_PATTERN.matcher(url).find();
    }

    /**
     * 从URL提取仓库名称
     */
    private String extractRepositoryName(String url) {
        String[] parts = url.split("/");
        String lastPart = parts[parts.length - 1];
        return lastPart.replace(".git", "");
    }

    /**
     * 从URL提取主机地址（用于IP地址显示）
     */
    private String extractHostFromUrl(String url) {
        try {
            // 处理 git@ 格式
            if (url.startsWith("git@") || url.startsWith("ssh://")) {
                String temp = url.replace("git@", "").replace("ssh://", "");
                int colonIndex = temp.indexOf(':');
                int slashIndex = temp.indexOf('/');
                if (colonIndex > 0) {
                    return temp.substring(0, colonIndex);
                } else if (slashIndex > 0) {
                    return temp.substring(0, slashIndex);
                }
            }

            // 处理 http/https 格式
            String temp = url.replace("https://", "").replace("http://", "");
            int slashIndex = temp.indexOf('/');
            if (slashIndex > 0) {
                return temp.substring(0, slashIndex);
            }
            return temp;
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 清理目录
     */
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

    /**
     * 创建适配器的工厂方法
     */
    public static GitRepositoryAdapter create(Path workingDirectory, String repositoryUrl) {
        GitRepositoryAdapter adapter = new GitRepositoryAdapter(workingDirectory);
        String platformInfo = adapter.detectPlatformInfo(repositoryUrl);
        log.info("创建Git仓库适配器: {}", platformInfo);
        return adapter;
    }
}

