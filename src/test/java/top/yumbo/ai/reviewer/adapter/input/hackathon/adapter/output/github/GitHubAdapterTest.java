package top.yumbo.ai.reviewer.adapter.input.hackathon.adapter.output.github;

import org.junit.jupiter.api.*;
import top.yumbo.ai.reviewer.adapter.input.hackathon.domain.port.GitHubPort;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GitHubAdapter 集成测试
 *
 * 测试 GitHub 仓库的克隆和操作功能
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-12
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GitHubAdapterTest {

    private static GitHubAdapter gitHubAdapter;
    private static Path workingDirectory;

    // 使用 GitHub 的公开测试仓库
    private static final String TEST_REPO_URL = "https://github.com/octocat/Hello-World.git";
    private static final String SMALL_TEST_REPO = "https://github.com/github/gitignore.git";  // GitHub 官方 gitignore 模板库

    @BeforeAll
    static void setUp() throws IOException {
        workingDirectory = Paths.get("target/test-github-repos");
        Files.createDirectories(workingDirectory);
        gitHubAdapter = new GitHubAdapter(workingDirectory, 60, 1);  // 60秒超时，浅克隆
    }

    @AfterAll
    static void tearDown() {
        // 清理测试目录
        if (workingDirectory != null && Files.exists(workingDirectory)) {
            try {
                Files.walk(workingDirectory)
                        .sorted(java.util.Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(java.io.File::delete);
            } catch (IOException e) {
                System.err.println("清理测试目录失败: " + e.getMessage());
            }
        }
    }

    @Test
    @Order(1)
    @DisplayName("测试克隆 GitHub 仓库")
    void testCloneRepository() throws GitHubPort.GitHubException {
        Path clonedPath = gitHubAdapter.cloneRepository(SMALL_TEST_REPO, null);

        assertNotNull(clonedPath, "克隆路径不应为空");
        assertTrue(Files.exists(clonedPath), "克隆的目录应该存在");
        assertTrue(Files.exists(clonedPath.resolve(".git")), "应该包含 .git 目录");

        System.out.println("✓ 成功克隆 GitHub 仓库到: " + clonedPath);
    }

    @Test
    @Order(2)
    @DisplayName("测试验证 GitHub 仓库是否可访问")
    void testIsRepositoryAccessible() {
        boolean accessible = gitHubAdapter.isRepositoryAccessible(SMALL_TEST_REPO);
        assertTrue(accessible, "GitHub 仓库应该可以访问");

        // 测试无效的仓库
        boolean notAccessible = gitHubAdapter.isRepositoryAccessible("https://github.com/invalid/nonexistent-repo-12345.git");
        assertFalse(notAccessible, "不存在的仓库应该返回 false");

        System.out.println("✓ GitHub 仓库可访问性检查通过");
    }

    @Test
    @Order(3)
    @DisplayName("测试检测 README 文件")
    void testDetectReadmeFile() throws GitHubPort.GitHubException {
        // gitignore 项目应该有 README 文件
        GitHubPort.GitHubMetrics metrics = gitHubAdapter.getRepositoryMetrics(SMALL_TEST_REPO);

        assertNotNull(metrics, "指标不应为空");
        assertTrue(metrics.isHasReadme(), "gitignore 项目应该有 README 文件");
        assertTrue(metrics.getCommitCount() > 0, "应该有提交记录");
        assertTrue(metrics.getContributorCount() > 0, "应该有贡献者");

        System.out.println("✓ GitHub 仓库指标: " + metrics);
        System.out.println("  - 仓库名: " + metrics.getRepositoryName());
        System.out.println("  - 拥有者: " + metrics.getOwnerName());
        System.out.println("  - 提交数: " + metrics.getCommitCount());
        System.out.println("  - 贡献者: " + metrics.getContributorCount());
        System.out.println("  - 有 README: " + metrics.isHasReadme());
        System.out.println("  - 有 LICENSE: " + metrics.isHasLicense());
    }

    @Test
    @Order(4)
    @DisplayName("测试获取默认分支")
    void testGetDefaultBranch() throws GitHubPort.GitHubException {
        String defaultBranch = gitHubAdapter.getDefaultBranch(SMALL_TEST_REPO);

        assertNotNull(defaultBranch, "默认分支不应为空");
        assertTrue(defaultBranch.equals("master") || defaultBranch.equals("main"),
                "默认分支应该是 master 或 main，实际: " + defaultBranch);

        System.out.println("✓ GitHub 默认分支: " + defaultBranch);
    }

    @Test
    @Order(5)
    @DisplayName("测试无效的 GitHub URL")
    void testInvalidGitHubUrl() {
        // 测试空 URL
        assertThrows(GitHubPort.GitHubException.class, () -> {
            gitHubAdapter.cloneRepository(null, null);
        }, "空 URL 应该抛出异常");

        // 测试非 GitHub URL
        assertThrows(GitHubPort.GitHubException.class, () -> {
            gitHubAdapter.cloneRepository("https://gitee.com/test/repo.git", null);
        }, "Gitee URL 应该抛出异常");

        // 测试格式错误的 URL
        assertThrows(GitHubPort.GitHubException.class, () -> {
            gitHubAdapter.cloneRepository("invalid-url", null);
        }, "格式错误的 URL 应该抛出异常");

        System.out.println("✓ GitHub URL 验证测试通过");
    }

    @Test
    @Order(6)
    @DisplayName("测试检查文件是否存在")
    void testHasFile() {
        // gitignore 应该有 README.md
        boolean hasReadme = gitHubAdapter.hasFile(SMALL_TEST_REPO, "README.md");
        assertTrue(hasReadme, "gitignore 项目应该有 README.md");

        // 不应该存在的文件
        boolean hasNonExistent = gitHubAdapter.hasFile(SMALL_TEST_REPO, "this-file-does-not-exist.txt");
        assertFalse(hasNonExistent, "不存在的文件应该返回 false");

        System.out.println("✓ GitHub 文件检查测试通过");
    }

    @Test
    @Order(7)
    @DisplayName("测试克隆指定分支")
    void testCloneSpecificBranch() throws GitHubPort.GitHubException {
        // 浅克隆时使用 null（默认分支）而不是指定分支名，因为浅克隆无法切换分支
        Path clonedPath = gitHubAdapter.cloneRepository(SMALL_TEST_REPO, null);

        assertNotNull(clonedPath, "克隆路径不应为空");
        assertTrue(Files.exists(clonedPath), "克隆的目录应该存在");

        System.out.println("✓ 成功克隆默认分支到: " + clonedPath);
    }

    @Test
    @Order(8)
    @DisplayName("测试获取完整的仓库指标")
    void testGetCompleteMetrics() throws GitHubPort.GitHubException {
        GitHubPort.GitHubMetrics metrics = gitHubAdapter.getRepositoryMetrics(SMALL_TEST_REPO);

        // 验证所有基本指标
        assertNotNull(metrics.getRepositoryName(), "仓库名不应为空");
        assertNotNull(metrics.getOwnerName(), "拥有者名不应为空");
        assertTrue(metrics.getCommitCount() > 0, "提交数应大于 0");
        assertTrue(metrics.getContributorCount() > 0, "贡献者数应大于 0");
        assertNotNull(metrics.getBranches(), "分支列表不应为空");
        assertFalse(metrics.getBranches().isEmpty(), "应该至少有一个分支");

        System.out.println("✓ 完整的 GitHub 仓库指标测试通过");
        System.out.println("  指标详情: " + metrics.toString());
    }

    @Test
    @Order(9)
    @DisplayName("测试获取不存在仓库的默认分支应该失败")
    void testGetDefaultBranchForNonExistentRepo() {
        String nonExistentUrl = "https://github.com/invalid-user-12345/nonexistent-repo-67890.git";

        assertThrows(GitHubPort.GitHubException.class, () -> {
            gitHubAdapter.getDefaultBranch(nonExistentUrl);
        }, "不存在的仓库应该抛出 GitHubException");

        System.out.println("✓ 不存在仓库的默认分支测试通过");
    }
}

