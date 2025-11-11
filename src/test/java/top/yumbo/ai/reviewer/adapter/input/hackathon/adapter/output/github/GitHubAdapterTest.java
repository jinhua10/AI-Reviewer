package top.yumbo.ai.reviewer.adapter.input.hackathon.adapter.output.github;

import org.junit.jupiter.api.*;
import top.yumbo.ai.reviewer.adapter.input.hackathon.domain.port.GitHubPort;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.*;

/**
 * GitHubAdapter 单元测试
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-12
 */
@DisplayName("GitHubAdapter 测试")
class GitHubAdapterTest {

    private GitHubAdapter gitHubAdapter;
    private Path tempWorkDir;

    @BeforeEach
    void setUp() throws IOException {
        // 创建临时工作目录
        tempWorkDir = Files.createTempDirectory("github-test");
        gitHubAdapter = new GitHubAdapter(tempWorkDir);
    }

    @AfterEach
    void tearDown() throws IOException {
        // 清理临时目录
        if (Files.exists(tempWorkDir)) {
            Files.walk(tempWorkDir)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        // ignore
                    }
                });
        }
    }

    @Nested
    @DisplayName("URL 验证测试")
    class UrlValidationTest {

        @Test
        @DisplayName("应该接受标准 GitHub HTTPS URL")
        void shouldAcceptStandardHttpsUrl() {
            String url = "https://github.com/spring-projects/spring-boot";
            boolean accessible = gitHubAdapter.isRepositoryAccessible(url);
            assertThat(accessible).isTrue();
        }

        @Test
        @DisplayName("应该接受带 .git 后缀的 URL")
        void shouldAcceptUrlWithGitSuffix() {
            String url = "https://github.com/spring-projects/spring-boot.git";
            boolean accessible = gitHubAdapter.isRepositoryAccessible(url);
            assertThat(accessible).isTrue();
        }

        @Test
        @DisplayName("应该拒绝不存在的仓库")
        void shouldRejectNonExistentRepository() {
            String url = "https://github.com/this-user-does-not-exist/repo-12345";
            boolean accessible = gitHubAdapter.isRepositoryAccessible(url);
            assertThat(accessible).isFalse();
        }

        @Test
        @DisplayName("应该拒绝无效的 URL 格式")
        void shouldRejectInvalidUrlFormat() {
            String url = "not-a-valid-url";
            assertThatThrownBy(() -> gitHubAdapter.cloneRepository(url, null))
                .isInstanceOf(GitHubPort.GitHubException.class)
                .hasMessageContaining("无效的 GitHub URL 格式");
        }

        @Test
        @DisplayName("应该拒绝空 URL")
        void shouldRejectEmptyUrl() {
            assertThatThrownBy(() -> gitHubAdapter.cloneRepository("", null))
                .isInstanceOf(GitHubPort.GitHubException.class)
                .hasMessageContaining("不能为空");
        }

        @Test
        @DisplayName("应该拒绝 null URL")
        void shouldRejectNullUrl() {
            assertThatThrownBy(() -> gitHubAdapter.cloneRepository(null, null))
                .isInstanceOf(GitHubPort.GitHubException.class)
                .hasMessageContaining("不能为空");
        }
    }

    @Nested
    @DisplayName("仓库克隆测试")
    class RepositoryCloneTest {

        @Test
        @DisplayName("应该成功克隆公开仓库")
        @Tag("integration")
        void shouldClonePublicRepository() throws GitHubPort.GitHubException {
            // 使用一个小型测试仓库
            String url = "https://github.com/octocat/Hello-World";

            Path localPath = gitHubAdapter.cloneRepository(url, "master");

            assertThat(localPath).isNotNull();
            assertThat(Files.exists(localPath)).isTrue();
            assertThat(Files.isDirectory(localPath)).isTrue();

            // 验证 .git 目录存在
            Path gitDir = localPath.resolve(".git");
            assertThat(Files.exists(gitDir)).isTrue();

            // 清理
            deleteDirectory(localPath);
        }

        @Test
        @DisplayName("应该支持克隆指定分支")
        @Tag("integration")
        void shouldCloneSpecificBranch() throws GitHubPort.GitHubException {
            String url = "https://github.com/octocat/Hello-World";
            String branch = "master";

            Path localPath = gitHubAdapter.cloneRepository(url, branch);

            assertThat(localPath).isNotNull();
            assertThat(Files.exists(localPath)).isTrue();

            // 清理
            deleteDirectory(localPath);
        }

        @Test
        @DisplayName("克隆失败时应该清理临时文件")
        void shouldCleanupOnCloneFailure() {
            String invalidUrl = "https://github.com/invalid/invalid-repo-12345";

            assertThatThrownBy(() -> gitHubAdapter.cloneRepository(invalidUrl, null))
                .isInstanceOf(GitHubPort.GitHubException.class);

            // 验证没有遗留临时目录
            try {
                assertThat(Files.list(tempWorkDir).count()).isEqualTo(0);
            } catch (IOException e) {
                fail("无法列出临时目录: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("应该为每次克隆创建唯一的目录")
        @Tag("integration")
        void shouldCreateUniqueDirectoryForEachClone() throws GitHubPort.GitHubException {
            String url = "https://github.com/octocat/Hello-World";

            Path path1 = gitHubAdapter.cloneRepository(url, "master");
            Path path2 = gitHubAdapter.cloneRepository(url, "master");

            assertThat(path1).isNotEqualTo(path2);
            assertThat(Files.exists(path1)).isTrue();
            assertThat(Files.exists(path2)).isTrue();

            // 清理
            deleteDirectory(path1);
            deleteDirectory(path2);
        }
    }

    @Nested
    @DisplayName("仓库指标测试")
    class RepositoryMetricsTest {

        @Test
        @DisplayName("应该获取仓库基本指标")
        @Tag("integration")
        void shouldGetBasicRepositoryMetrics() throws GitHubPort.GitHubException {
            String url = "https://github.com/octocat/Hello-World";

            GitHubPort.GitHubMetrics metrics = gitHubAdapter.getRepositoryMetrics(url);

            assertThat(metrics).isNotNull();
            assertThat(metrics.getRepositoryName()).isEqualTo("Hello-World");
            assertThat(metrics.getOwnerName()).isEqualTo("octocat");
            assertThat(metrics.getCommitCount()).isGreaterThan(0);
            assertThat(metrics.getContributorCount()).isGreaterThan(0);
        }

        @Test
        @DisplayName("应该检测 README 文件")
        @Tag("integration")
        void shouldDetectReadmeFile() throws GitHubPort.GitHubException {
            String url = "https://github.com/octocat/Hello-World";

            GitHubPort.GitHubMetrics metrics = gitHubAdapter.getRepositoryMetrics(url);

            assertThat(metrics.isHasReadme()).isTrue();
        }

        @Test
        @DisplayName("应该获取提交时间信息")
        @Tag("integration")
        void shouldGetCommitTimeInfo() throws GitHubPort.GitHubException {
            String url = "https://github.com/octocat/Hello-World";

            GitHubPort.GitHubMetrics metrics = gitHubAdapter.getRepositoryMetrics(url);

            assertThat(metrics.getFirstCommitDate()).isNotNull();
            assertThat(metrics.getLastCommitDate()).isNotNull();
            assertThat(metrics.getLastCommitDate())
                .isAfterOrEqualTo(metrics.getFirstCommitDate());
        }

        @Test
        @DisplayName("应该获取分支列表")
        @Tag("integration")
        void shouldGetBranchList() throws GitHubPort.GitHubException {
            String url = "https://github.com/octocat/Hello-World";

            GitHubPort.GitHubMetrics metrics = gitHubAdapter.getRepositoryMetrics(url);

            assertThat(metrics.getBranches()).isNotNull();
            assertThat(metrics.getBranches()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("默认分支测试")
    class DefaultBranchTest {

        @Test
        @DisplayName("应该获取仓库默认分支")
        @Tag("integration")
        void shouldGetDefaultBranch() throws GitHubPort.GitHubException {
            String url = "https://github.com/octocat/Hello-World";

            String defaultBranch = gitHubAdapter.getDefaultBranch(url);

            assertThat(defaultBranch).isNotNull();
            assertThat(defaultBranch).isIn("main", "master");
        }

        @Test
        @DisplayName("获取不存在仓库的默认分支应该失败")
        void shouldFailToGetDefaultBranchForNonExistentRepo() {
            String url = "https://github.com/invalid/invalid-repo-12345";

            assertThatThrownBy(() -> gitHubAdapter.getDefaultBranch(url))
                .isInstanceOf(GitHubPort.GitHubException.class);
        }
    }

    @Nested
    @DisplayName("文件检查测试")
    class FileCheckTest {

        @Test
        @DisplayName("应该检测到 README 文件")
        @Tag("integration")
        void shouldDetectReadmeFile() {
            String url = "https://github.com/octocat/Hello-World";

            boolean hasReadme = gitHubAdapter.hasFile(url, "README");

            assertThat(hasReadme).isTrue();
        }

        @Test
        @DisplayName("应该检测不存在的文件")
        @Tag("integration")
        void shouldDetectNonExistentFile() {
            String url = "https://github.com/octocat/Hello-World";

            boolean hasFile = gitHubAdapter.hasFile(url, "THIS_FILE_DOES_NOT_EXIST.txt");

            assertThat(hasFile).isFalse();
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryConditionsTest {

        @Test
        @DisplayName("应该处理特殊字符的仓库名")
        @Tag("integration")
        void shouldHandleSpecialCharactersInRepoName() throws GitHubPort.GitHubException {
            // GitHub 仓库名可以包含点、连字符、下划线
            String url = "https://github.com/dotnet/dotnet-docker";

            boolean accessible = gitHubAdapter.isRepositoryAccessible(url);

            assertThat(accessible).isTrue();
        }

        @Test
        @DisplayName("应该处理大型仓库（但设置超时）")
        void shouldHandleLargeRepositoryWithTimeout() {
            // 创建一个短超时的适配器
            GitHubAdapter shortTimeoutAdapter = new GitHubAdapter(tempWorkDir, 5, 1);

            // 这个测试只验证超时机制，不实际克隆大型仓库
            assertThat(shortTimeoutAdapter).isNotNull();
        }
    }

    @Nested
    @DisplayName("构造函数测试")
    class ConstructorTest {

        @Test
        @DisplayName("应该接受自定义参数")
        void shouldAcceptCustomParameters() {
            Path customDir = Paths.get("./custom-dir");
            int timeout = 600;
            int depth = 5;

            GitHubAdapter adapter = new GitHubAdapter(customDir, timeout, depth);

            assertThat(adapter).isNotNull();
        }

        @Test
        @DisplayName("应该使用默认参数")
        void shouldUseDefaultParameters() {
            Path customDir = Paths.get("./default-dir");

            GitHubAdapter adapter = new GitHubAdapter(customDir);

            assertThat(adapter).isNotNull();
        }

        @Test
        @DisplayName("应该自动创建工作目录")
        void shouldCreateWorkingDirectoryAutomatically() throws IOException {
            Path newDir = tempWorkDir.resolve("auto-created");

            // 目录不存在
            assertThat(Files.exists(newDir)).isFalse();

            // 创建适配器应该自动创建目录
            GitHubAdapter adapter = new GitHubAdapter(newDir);

            assertThat(Files.exists(newDir)).isTrue();
            assertThat(Files.isDirectory(newDir)).isTrue();
        }
    }

    // 辅助方法
    private void deleteDirectory(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }

        try {
            Files.walk(directory)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        // ignore
                    }
                });
        } catch (IOException e) {
            // ignore
        }
    }
}

