package top.yumbo.ai.reviewer.adapter.input.hackathon.adapter.output.gitee;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.yumbo.ai.reviewer.adapter.input.hackathon.domain.port.GitHubPort;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gitee（码云） 适配器实现
 *
 * 使用 JGit 实现 Gitee 仓库操作，兼容 GitHubPort 接口
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-12
 */
public class GiteeAdapter implements GitHubPort {

    private static final Logger log = LoggerFactory.getLogger(GiteeAdapter.class);

    private final Path workingDirectory;
    private final int cloneTimeout;  // 克隆超时时间（秒）
    private final int cloneDepth;    // 克隆深度

    /**
     * 构造函数
     *
     * @param workingDirectory 工作目录（存放克隆的代码）
     * @param cloneTimeout 克隆超时时间（秒）
     * @param cloneDepth 克隆深度（1 表示浅克隆）
     */
    public GiteeAdapter(Path workingDirectory, int cloneTimeout, int cloneDepth) {
        this.workingDirectory = workingDirectory;
        this.cloneTimeout = cloneTimeout;
        this.cloneDepth = cloneDepth;

        // 确保工作目录存在
        try {
            Files.createDirectories(workingDirectory);
        } catch (IOException e) {
            throw new RuntimeException("无法创建工作目录: " + workingDirectory, e);
        }
    }

    /**
     * 简化构造函数（使用默认值）
     */
    public GiteeAdapter(Path workingDirectory) {
        this(workingDirectory, 300, 1);  // 5分钟超时，浅克隆
    }

    @Override
    public Path cloneRepository(String githubUrl, String branch) throws GitHubException {
        log.info("开始克隆 Gitee 仓库: {}, 分支: {}", githubUrl, branch);

        // 验证 URL（兼容 GitHub URL 参数名，但实际验证 Gitee）
        validateGiteeUrl(githubUrl);

        // 创建本地目录
        String repoName = extractRepositoryName(githubUrl);
        Path localPath = workingDirectory.resolve(repoName + "-" + System.currentTimeMillis());

        try {
            Files.createDirectories(localPath);

            // 克隆仓库
            Git git = Git.cloneRepository()
                    .setURI(githubUrl)
                    .setDirectory(localPath.toFile())
                    .setBranch(branch)
                    .setDepth(cloneDepth)  // 浅克隆
                    .setTimeout(cloneTimeout)
                    .call();

            git.close();

            log.info("成功克隆 Gitee 仓库到: {}", localPath);
            return localPath;

        } catch (GitAPIException e) {
            // 清理失败的克隆
            deleteDirectory(localPath);
            throw new GitHubException("克隆 Gitee 仓库失败: " + e.getMessage(), e);
        } catch (IOException e) {
            deleteDirectory(localPath);
            throw new GitHubException("创建本地目录失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Path cloneRepositoryAtCommit(String githubUrl, String commitHash) throws GitHubException {
        log.info("开始克隆 Gitee 仓库到指定 commit: {}, commit: {}", githubUrl, commitHash);

        validateGiteeUrl(githubUrl);

        String repoName = extractRepositoryName(githubUrl);
        Path localPath = workingDirectory.resolve(repoName + "-" + commitHash + "-" + System.currentTimeMillis());

        try {
            Files.createDirectories(localPath);

            // 先克隆整个仓库
            Git git = Git.cloneRepository()
                    .setURI(githubUrl)
                    .setDirectory(localPath.toFile())
                    .setTimeout(cloneTimeout)
                    .call();

            // 切换到指定 commit
            git.checkout()
                    .setName(commitHash)
                    .call();

            git.close();

            log.info("成功克隆 Gitee 仓库到指定 commit: {}", localPath);
            return localPath;

        } catch (GitAPIException | IOException e) {
            deleteDirectory(localPath);
            throw new GitHubException("克隆到指定 commit 失败: " + e.getMessage(), e);
        }
    }

    @Override
    public GitHubMetrics getRepositoryMetrics(String githubUrl) throws GitHubException {
        log.info("获取 Gitee 仓库指标: {}", githubUrl);

        // 克隆仓库（临时）
        Path localPath = cloneRepository(githubUrl, null);

        try {
            // 打开仓库
            Repository repository = openRepository(localPath);
            Git git = new Git(repository);

            // 获取提交历史
            Iterable<RevCommit> commits = git.log().all().call();
            List<RevCommit> commitList = new ArrayList<>();
            commits.forEach(commitList::add);

            // 计算指标
            int commitCount = commitList.size();
            Set<String> contributors = new HashSet<>();
            LocalDateTime firstCommit = null;
            LocalDateTime lastCommit = null;

            for (RevCommit commit : commitList) {
                contributors.add(commit.getAuthorIdent().getName());

                LocalDateTime commitTime = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(commit.getCommitTime()),
                    ZoneId.systemDefault()
                );

                if (firstCommit == null || commitTime.isBefore(firstCommit)) {
                    firstCommit = commitTime;
                }
                if (lastCommit == null || commitTime.isAfter(lastCommit)) {
                    lastCommit = commitTime;
                }
            }

            // 获取分支
            List<Ref> branches = git.branchList().call();
            List<String> branchNames = branches.stream()
                    .map(ref -> Repository.shortenRefName(ref.getName()))
                    .collect(Collectors.toList());

            // 检查文件存在 (支持多种文件名格式)
            boolean hasReadme = hasFile(localPath, "README.md") ||
                               hasFile(localPath, "readme.md") ||
                               hasFile(localPath, "README") ||
                               hasFile(localPath, "readme") ||
                               hasFile(localPath, "README.txt") ||
                               hasFileStartingWith(localPath, "README");
            boolean hasLicense = hasFile(localPath, "LICENSE") ||
                                hasFile(localPath, "license") ||
                                hasFile(localPath, "LICENSE.txt");
            boolean hasGitHubActions = Files.exists(localPath.resolve(".github/workflows"));
            boolean hasGiteeWorkflows = Files.exists(localPath.resolve(".gitee/workflows"));

            // 构建指标
            String[] urlParts = githubUrl.replace(".git", "").split("/");
            String ownerName = urlParts.length >= 4 ? urlParts[urlParts.length - 2] : "unknown";
            String repoName = urlParts.length >= 4 ? urlParts[urlParts.length - 1] : "unknown";

            git.close();
            repository.close();

            // 清理临时目录
            deleteDirectory(localPath);

            return GitHubMetrics.builder()
                    .repositoryName(repoName)
                    .ownerName(ownerName)
                    .commitCount(commitCount)
                    .contributorCount(contributors.size())
                    .firstCommitDate(firstCommit)
                    .lastCommitDate(lastCommit)
                    .branches(branchNames)
                    .hasReadme(hasReadme)
                    .hasLicense(hasLicense)
                    .hasGitHubActions(hasGitHubActions || hasGiteeWorkflows)  // Gitee 也支持 CI/CD
                    .hasIssues(false)  // 需要 Gitee API
                    .hasPullRequests(false)  // 需要 Gitee API
                    .starsCount(0)  // 需要 Gitee API
                    .forksCount(0)  // 需要 Gitee API
                    .build();

        } catch (GitAPIException | IOException e) {
            deleteDirectory(localPath);
            throw new GitHubException("获取 Gitee 仓库指标失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isRepositoryAccessible(String githubUrl) {
        try {
            validateGiteeUrl(githubUrl);

            // 尝试获取远程引用（不克隆）
            Collection<Ref> refs = Git.lsRemoteRepository()
                    .setRemote(githubUrl)
                    .setTimeout(10)  // 10秒超时
                    .call();

            return refs != null && !refs.isEmpty();

        } catch (Exception e) {
            log.warn("Gitee 仓库不可访问: {}, 原因: {}", githubUrl, e.getMessage());
            return false;
        }
    }

    @Override
    public long getRepositorySize(String githubUrl) throws GitHubException {
        // 注意：JGit 无法直接获取远程仓库大小，需要克隆后计算
        Path localPath = cloneRepository(githubUrl, null);
        try {
            long size = calculateDirectorySize(localPath);
            deleteDirectory(localPath);
            return size;
        } catch (IOException e) {
            deleteDirectory(localPath);
            throw new GitHubException("计算 Gitee 仓库大小失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean hasFile(String githubUrl, String fileName) {
        try {
            Path localPath = cloneRepository(githubUrl, null);
            boolean exists = hasFile(localPath, fileName);
            deleteDirectory(localPath);
            return exists;
        } catch (GitHubException e) {
            return false;
        }
    }

    private boolean hasFile(Path directory, String fileName) {
        try {
            return Files.walk(directory)
                    .anyMatch(path -> path.getFileName().toString().equalsIgnoreCase(fileName));
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 检查目录中是否存在以指定前缀开头的文件
     */
    private boolean hasFileStartingWith(Path directory, String prefix) {
        try {
            return Files.walk(directory, 1)  // 只检查根目录
                    .filter(Files::isRegularFile)
                    .anyMatch(path -> path.getFileName().toString().toUpperCase().startsWith(prefix.toUpperCase()));
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public String getDefaultBranch(String githubUrl) throws GitHubException {
        try {
            Collection<Ref> refs = Git.lsRemoteRepository()
                    .setRemote(githubUrl)
                    .setHeads(true)
                    .setTimeout(cloneTimeout)
                    .call();

            // 查找 HEAD 引用
            for (Ref ref : refs) {
                if (ref.getName().equals("HEAD")) {
                    String target = ref.getTarget().getName();
                    return Repository.shortenRefName(target);
                }
            }

            // Gitee 默认分支通常是 master
            return "master";

        } catch (GitAPIException e) {
            throw new GitHubException("获取 Gitee 默认分支失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证 Gitee URL 格式
     * 支持的格式：
     * - https://gitee.com/owner/repo
     * - https://gitee.com/owner/repo.git
     * - http://gitee.com/owner/repo
     */
    private void validateGiteeUrl(String url) throws GitHubException {
        if (url == null || url.trim().isEmpty()) {
            throw new GitHubException("Gitee URL 不能为空");
        }

        if (!url.matches("^https?://gitee\\.com/[\\w-]+/[\\w.-]+.*$")) {
            throw new GitHubException("无效的 Gitee URL 格式: " + url + "，正确格式如：https://gitee.com/owner/repo");
        }
    }

    /**
     * 从 URL 提取仓库名称
     */
    private String extractRepositoryName(String url) {
        String cleaned = url.replace(".git", "");
        String[] parts = cleaned.split("/");
        return parts[parts.length - 1];
    }

    /**
     * 打开本地仓库
     */
    private Repository openRepository(Path localPath) throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        return builder
                .setGitDir(localPath.resolve(".git").toFile())
                .readEnvironment()
                .findGitDir()
                .build();
    }

    /**
     * 计算目录大小
     */
    private long calculateDirectorySize(Path directory) throws IOException {
        return Files.walk(directory)
                .filter(Files::isRegularFile)
                .mapToLong(path -> {
                    try {
                        return Files.size(path);
                    } catch (IOException e) {
                        return 0L;
                    }
                })
                .sum();
    }

    /**
     * 删除目录
     */
    private void deleteDirectory(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }

        try {
            Files.walk(directory)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            log.debug("已删除临时目录: {}", directory);
        } catch (IOException e) {
            log.warn("删除目录失败: {}, 原因: {}", directory, e.getMessage());
        }
    }
}

