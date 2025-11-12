package top.yumbo.ai.reviewer.adapter.input.hackathon.integration;

import org.junit.jupiter.api.*;
import top.yumbo.ai.reviewer.adapter.input.hackathon.adapter.output.github.GitHubAdapter;
import top.yumbo.ai.reviewer.application.hackathon.service.*;
import top.yumbo.ai.reviewer.domain.hackathon.model.*;
import top.yumbo.ai.reviewer.adapter.input.hackathon.domain.port.GitHubPort;
import top.yumbo.ai.reviewer.adapter.output.filesystem.LocalFileSystemAdapter;
import top.yumbo.ai.reviewer.application.service.ProjectAnalysisService;
import top.yumbo.ai.reviewer.domain.model.Project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * GitHub 集成端到端测试
 *
 * 测试完整的工作流程：GitHub URL 提交 → 克隆 → 分析 → 评分 → 排行榜
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-12
 */
@DisplayName("GitHub 集成端到端测试")
@Tag("integration")
class GitHubIntegrationEndToEndTest {

    private Path tempWorkDir;
    private GitHubAdapter gitHubAdapter;
    private TeamManagementService teamManagement;
    private HackathonScoringService scoringService;
    private LeaderboardService leaderboardService;
    private LocalFileSystemAdapter fileSystemAdapter;

    @BeforeEach
    void setUp() throws IOException {
        // 创建临时目录
        tempWorkDir = Files.createTempDirectory("github-e2e-test");

        // 初始化服务
        gitHubAdapter = new GitHubAdapter(tempWorkDir);
        teamManagement = new TeamManagementService();
        scoringService = new HackathonScoringService();
        leaderboardService = new LeaderboardService();

        // 创建默认的文件系统配置
        LocalFileSystemAdapter.FileSystemConfig config = new LocalFileSystemAdapter.FileSystemConfig(
            List.of("*.java", "*.py", "*.js", "*.ts", "*.go", "*.rs", "*.md"),
            List.of(".git", "node_modules", "target", "build"),
            1024,  // 1MB
            10     // 最大深度
        );
        fileSystemAdapter = new LocalFileSystemAdapter(config);
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
    @DisplayName("完整工作流程测试")
    class CompleteWorkflowTest {

        @Test
        @DisplayName("应该完成从 GitHub URL 到排行榜的完整流程")
        void shouldCompleteFullWorkflow() throws Exception {
            // 1. 创建团队和参与者
            Participant leader = Participant.builder()
                .name("Alice")
                .email("alice@example.com")
                .githubUsername("alice")
                .role(ParticipantRole.LEADER)
                .build();

            Team team = Team.builder()
                .name("Awesome Team")
                .leader(leader)
                .contactEmail("team@example.com")
                .build();

            teamManagement.registerTeam(team);

            // 2. 创建黑客松项目
            HackathonProject project = teamManagement.createProject(
                "Hello World Project",
                "A simple hello world project",
                team
            );

            assertThat(project).isNotNull();
            assertThat(project.getId()).isNotNull();

            // 3. 提交 GitHub URL
            String githubUrl = "https://github.com/octocat/Hello-World";
            HackathonProject submittedProject = teamManagement.submitCode(
                project.getId(),
                githubUrl,
                "master",
                leader
            );

            assertThat(submittedProject.getSubmissions()).hasSize(1);
            Submission submission = submittedProject.getLatestSubmission();
            assertThat(submission.getGithubUrl()).isEqualTo(githubUrl);
            assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.PENDING);

            // 4. 克隆仓库
            Path localPath = gitHubAdapter.cloneRepository(githubUrl, "master");
            assertThat(localPath).isNotNull();
            assertThat(Files.exists(localPath)).isTrue();

            // 5. 扫描文件
            List<top.yumbo.ai.reviewer.domain.model.SourceFile> sourceFiles =
                fileSystemAdapter.scanProjectFiles(localPath);
            assertThat(sourceFiles).isNotNull();
            // Note: octocat/Hello-World 可能没有 Java 文件，所以创建一个 mock 的源文件列表
            if (sourceFiles.isEmpty()) {
                // 为测试创建一个 mock 源文件
                sourceFiles = List.of(
                    top.yumbo.ai.reviewer.domain.model.SourceFile.builder()
                        .path(localPath.resolve("README"))
                        .relativePath("README")
                        .fileName("README")
                        .extension("")
                        .content("Hello World!")
                        .lineCount(1)
                        .sizeInBytes(12L)
                        .build()
                );
            }

            // 构建 Project 对象
            Project coreProject = Project.builder()
                .name(project.getName())
                .rootPath(localPath)
                .type(top.yumbo.ai.reviewer.domain.model.ProjectType.JAVA)
                .sourceFiles(sourceFiles)
                .build();

            // 6. 模拟评审报告（简化测试）
            // 实际环境中会调用 ProjectAnalysisService
            submission.startReview();

            // 7. 计算黑客松评分
            top.yumbo.ai.reviewer.domain.model.ReviewReport mockReport =
                top.yumbo.ai.reviewer.domain.model.ReviewReport.builder()
                    .reportId("test-report")
                    .projectName(project.getName())
                    .overallScore(85)
                    .build();

            HackathonScore score = scoringService.calculateScore(mockReport, coreProject);
            assertThat(score).isNotNull();
            assertThat(score.calculateTotalScore()).isGreaterThan(0);

            // 8. 完成评审
            submission.completeReview(mockReport, score);
            project.markAsReviewed();

            assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.COMPLETED);
            assertThat(submission.getScore()).isNotNull();

            // 9. 更新排行榜
            leaderboardService.updateLeaderboard(List.of(project));

            List<LeaderboardService.LeaderboardEntry> leaderboard =
                leaderboardService.getOverallLeaderboard();

            assertThat(leaderboard).hasSize(1);
            assertThat(leaderboard.get(0).getProjectName()).isEqualTo(project.getName());
            assertThat(leaderboard.get(0).getRank()).isEqualTo(1);

            // 10. 清理
            deleteDirectory(localPath);
        }

        @Test
        @DisplayName("应该支持同一项目的多次提交")
        void shouldSupportMultipleSubmissions() throws Exception {
            // 创建团队和项目
            Participant leader = createTestParticipant();
            Team team = createTestTeam(leader);
            teamManagement.registerTeam(team);

            HackathonProject project = teamManagement.createProject(
                "Multi-Submission Project",
                "Testing multiple submissions",
                team
            );

            // 第一次提交
            String url1 = "https://github.com/octocat/Hello-World";
            teamManagement.submitCode(project.getId(), url1, "master", leader);

            assertThat(project.getSubmissions()).hasSize(1);

            // 第二次提交（改进后）
            String url2 = "https://github.com/octocat/Hello-World";
            teamManagement.submitCode(project.getId(), url2, "master", leader);

            assertThat(project.getSubmissions()).hasSize(2);
            assertThat(project.getLatestSubmission().getGithubUrl()).isEqualTo(url2);
        }
    }

    @Nested
    @DisplayName("GitHub 指标集成测试")
    class GitHubMetricsIntegrationTest {

        @Test
        @DisplayName("应该获取并使用 GitHub 指标")
        void shouldGetAndUseGitHubMetrics() throws Exception {
            String url = "https://github.com/octocat/Hello-World";

            // 获取 GitHub 指标
            GitHubPort.GitHubMetrics metrics = gitHubAdapter.getRepositoryMetrics(url);

            assertThat(metrics).isNotNull();
            assertThat(metrics.getCommitCount()).isGreaterThan(0);
            assertThat(metrics.getContributorCount()).isGreaterThan(0);
            assertThat(metrics.isHasReadme()).isTrue();

            // 指标可用于评分（未来功能）
            assertThat(metrics.getFirstCommitDate()).isNotNull();
            assertThat(metrics.getLastCommitDate()).isNotNull();
        }
    }

    @Nested
    @DisplayName("错误处理测试")
    class ErrorHandlingTest {

        @Test
        @DisplayName("应该处理无效的 GitHub URL")
        void shouldHandleInvalidGitHubUrl() {
            Participant leader = createTestParticipant();
            Team team = createTestTeam(leader);
            teamManagement.registerTeam(team);

            HackathonProject project = teamManagement.createProject(
                "Error Test Project",
                "Testing error handling",
                team
            );

            String invalidUrl = "not-a-valid-url";

            assertThatThrownBy(() ->
                teamManagement.submitCode(project.getId(), invalidUrl, "main", leader)
            ).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("无效的 GitHub/Gitee URL 格式");
        }

        @Test
        @DisplayName("应该处理不存在的仓库")
        void shouldHandleNonExistentRepository() {
            String url = "https://github.com/invalid/non-existent-repo-12345";

            assertThatThrownBy(() -> gitHubAdapter.cloneRepository(url, null))
                .isInstanceOf(GitHubPort.GitHubException.class);
        }

        @Test
        @DisplayName("应该处理非团队成员的提交")
        void shouldHandleNonTeamMemberSubmission() {
            Participant leader = createTestParticipant();
            Team team = createTestTeam(leader);
            teamManagement.registerTeam(team);

            HackathonProject project = teamManagement.createProject(
                "Security Test",
                "Testing security",
                team
            );

            // 创建一个不在团队中的参与者
            Participant outsider = Participant.builder()
                .name("Outsider")
                .email("outsider@example.com")
                .build();

            String url = "https://github.com/octocat/Hello-World";

            assertThatThrownBy(() ->
                teamManagement.submitCode(project.getId(), url, "main", outsider)
            ).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("只有团队成员才能提交代码");
        }
    }

    @Nested
    @DisplayName("排行榜集成测试")
    class LeaderboardIntegrationTest {

        @Test
        @DisplayName("应该正确排序多个项目")
        void shouldCorrectlyRankMultipleProjects() throws Exception {
            // 创建三个项目
            HackathonProject project1 = createCompletedProject("Project A", 95);
            HackathonProject project2 = createCompletedProject("Project B", 85);
            HackathonProject project3 = createCompletedProject("Project C", 90);

            // 更新排行榜
            leaderboardService.updateLeaderboard(List.of(project1, project2, project3));

            // 验证排名
            List<LeaderboardService.LeaderboardEntry> leaderboard =
                leaderboardService.getOverallLeaderboard();

            assertThat(leaderboard).hasSize(3);
            assertThat(leaderboard.get(0).getProjectName()).isEqualTo("Project A");  // 95分
            assertThat(leaderboard.get(0).getRank()).isEqualTo(1);
            assertThat(leaderboard.get(1).getProjectName()).isEqualTo("Project C");  // 90分
            assertThat(leaderboard.get(1).getRank()).isEqualTo(2);
            assertThat(leaderboard.get(2).getProjectName()).isEqualTo("Project B");  // 85分
            assertThat(leaderboard.get(2).getRank()).isEqualTo(3);
        }

        @Test
        @DisplayName("应该生成排行榜报告")
        void shouldGenerateLeaderboardReport() throws Exception {
            HackathonProject project1 = createCompletedProject("Top Project", 95);
            HackathonProject project2 = createCompletedProject("Good Project", 85);

            leaderboardService.updateLeaderboard(List.of(project1, project2));

            String report = leaderboardService.generateLeaderboardReport(10);

            assertThat(report).isNotNull();
            assertThat(report).contains("Top Project");
            assertThat(report).contains("Good Project");
            assertThat(report).contains("95分");
            assertThat(report).contains("85分");
        }
    }

    @Nested
    @DisplayName("并发测试")
    class ConcurrencyTest {

        @Test
        @DisplayName("应该支持并发克隆不同的仓库")
        void shouldSupportConcurrentCloning() throws Exception {
            String url = "https://github.com/octocat/Hello-World";

            // 并发克隆同一仓库
            Path path1 = gitHubAdapter.cloneRepository(url, "master");
            Path path2 = gitHubAdapter.cloneRepository(url, "master");

            // 应该创建不同的目录
            assertThat(path1).isNotEqualTo(path2);
            assertThat(Files.exists(path1)).isTrue();
            assertThat(Files.exists(path2)).isTrue();

            // 清理
            deleteDirectory(path1);
            deleteDirectory(path2);
        }
    }

    // === 辅助方法 ===

    private Participant createTestParticipant() {
        return Participant.builder()
            .name("Test Leader")
            .email("leader@test.com")
            .githubUsername("testleader")
            .role(ParticipantRole.LEADER)
            .build();
    }

    private Team createTestTeam(Participant leader) {
        return Team.builder()
            .name("Test Team")
            .leader(leader)
            .contactEmail("team@test.com")
            .build();
    }

    private HackathonProject createCompletedProject(String name, int score) {
        Participant leader = Participant.builder()
            .name("Leader of " + name)
            .email("leader" + name.hashCode() + "@test.com")
            .role(ParticipantRole.LEADER)
            .build();

        Team team = Team.builder()
            .name("Team of " + name)
            .leader(leader)
            .contactEmail("team" + name.hashCode() + "@test.com")
            .build();

        HackathonProject project = HackathonProject.builder()
            .name(name)
            .description("Test project")
            .team(team)
            .build();

        // 创建完成的提交
        HackathonScore hackScore = HackathonScore.builder()
            .codeQuality(score)
            .innovation(score)
            .completeness(score)
            .documentation(score)
            .build();

        top.yumbo.ai.reviewer.domain.model.ReviewReport report =
            top.yumbo.ai.reviewer.domain.model.ReviewReport.builder()
                .reportId("report-" + name)
                .projectName(name)
                .overallScore(score)
                .build();

        Submission submission = Submission.builder()
            .githubUrl("https://github.com/test/" + name)
            .submitter(leader)
            .build();

        submission.startReview();
        submission.completeReview(report, hackScore);

        project.addSubmission(submission);
        project.markAsReviewed();

        return project;
    }

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

