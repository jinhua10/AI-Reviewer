package top.yumbo.ai.reviewer.integration.endtoend;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import top.yumbo.ai.reviewer.adapter.storage.cache.FileCacheAdapter;
import top.yumbo.ai.reviewer.adapter.storage.local.LocalFileSystemAdapter;
import top.yumbo.ai.reviewer.application.port.output.AIServicePort;
import top.yumbo.ai.reviewer.application.service.ProjectAnalysisService;
import top.yumbo.ai.reviewer.application.service.ReportGenerationService;
import top.yumbo.ai.reviewer.domain.model.AnalysisTask;
import top.yumbo.ai.reviewer.domain.model.ReviewReport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 命令行端到端测试
 * 测试完整的CLI用户流程
 */
@DisplayName("命令行端到端测试")
class CommandLineEndToEndTest {

    @TempDir
    Path tempOutputDir;

    @TempDir
    Path tempCacheDir;

    private ProjectAnalysisService analysisService;
    private ReportGenerationService reportService;
    private LocalFileSystemAdapter fileSystemAdapter;
    private Path testProjectsRoot;

    @BeforeEach
    void setUp() {
        // 获取测试项目根目录
        testProjectsRoot = Paths.get("src/test/resources/fixtures/projects");

        // 初始化适配器
        LocalFileSystemAdapter.FileSystemConfig fsConfig =
                new LocalFileSystemAdapter.FileSystemConfig(
                        java.util.List.of("*.java", "*.py", "*.js", "*.jsx", "*.ts", "*.tsx"),
                        java.util.List.of("*.class", "node_modules/*", "__pycache__/*", "target/*"),
                        2048,
                        20
                );
        fileSystemAdapter = new LocalFileSystemAdapter(fsConfig);

        // Mock AI服务
        AIServicePort aiServicePort = mock(AIServicePort.class);
        when(aiServicePort.analyze(anyString()))
                .thenReturn("这是一个结构清晰的项目。代码质量良好，建议继续保持良好的编码规范。");

        // 初始化服务
        FileCacheAdapter cacheAdapter = new FileCacheAdapter(tempCacheDir);
        analysisService = new ProjectAnalysisService(aiServicePort, cacheAdapter, fileSystemAdapter);
        reportService = new ReportGenerationService(fileSystemAdapter);
    }

    @Nested
    @DisplayName("小项目分析测试")
    class SmallProjectAnalysisTest {

        @Test
        @DisplayName("应该能够分析小型Java项目")
        void shouldAnalyzeSmallJavaProject() {
            // 准备：获取Java测试项目路径
            Path javaProject = testProjectsRoot.resolve("small-java-project");

            // 验证项目存在
            assertThat(Files.exists(javaProject)).isTrue();

            // 执行：扫描项目文件
            var files = fileSystemAdapter.scanProjectFiles(javaProject);

            // 验证：应该扫描到Java文件
            assertThat(files).isNotEmpty();
            assertThat(files).hasSizeGreaterThanOrEqualTo(3); // Main, Calculator, Utils

            // 验证：文件类型正确
            assertThat(files).anyMatch(f -> f.getFileName().equals("Main.java"));
            assertThat(files).anyMatch(f -> f.getFileName().equals("Calculator.java"));
            assertThat(files).anyMatch(f -> f.getFileName().equals("Utils.java"));
        }

        @Test
        @DisplayName("应该能够生成完整的分析报告")
        void shouldGenerateCompleteAnalysisReport() {
            // 准备
            Path javaProject = testProjectsRoot.resolve("small-java-project");
            var files = fileSystemAdapter.scanProjectFiles(javaProject);

            var project = top.yumbo.ai.reviewer.domain.model.Project.builder()
                    .name("small-java-project")
                    .rootPath(javaProject)
                    .type(top.yumbo.ai.reviewer.domain.model.ProjectType.JAVA)
                    .sourceFiles(files)
                    .build();

            // 执行：分析项目
            AnalysisTask task = analysisService.analyzeProject(project);
            ReviewReport report = analysisService.getAnalysisResult(task.getTaskId());

            // 验证：报告完整性
            assertThat(report).isNotNull();
            assertThat(report.getProjectName()).isEqualTo("small-java-project");
            assertThat(report.getOverallScore()).isGreaterThan(0);
            assertThat(report.getGeneratedAt()).isNotNull();
        }

        @Test
        @DisplayName("应该能够生成Markdown格式报告")
        void shouldGenerateMarkdownReport() throws Exception {
            // 准备
            Path javaProject = testProjectsRoot.resolve("small-java-project");
            var files = fileSystemAdapter.scanProjectFiles(javaProject);

            var project = top.yumbo.ai.reviewer.domain.model.Project.builder()
                    .name("small-java-project")
                    .rootPath(javaProject)
                    .type(top.yumbo.ai.reviewer.domain.model.ProjectType.JAVA)
                    .sourceFiles(files)
                    .build();

            AnalysisTask task = analysisService.analyzeProject(project);
            ReviewReport report = analysisService.getAnalysisResult(task.getTaskId());

            // 执行：生成Markdown报告
            String markdown = reportService.generateMarkdownReport(report);

            // 验证：Markdown格式正确
            assertThat(markdown).isNotEmpty();
            assertThat(markdown).contains("# 项目分析报告");
            assertThat(markdown).contains("small-java-project");
            assertThat(markdown).containsPattern("##\\s+总体评分");

            // 执行：保存到文件
            Path outputPath = tempOutputDir.resolve("java-project-report.md");
            reportService.saveReport(report, outputPath, "markdown");

            // 验证：文件已创建
            assertThat(Files.exists(outputPath)).isTrue();
            assertThat(Files.size(outputPath)).isGreaterThan(0);
        }

        @Test
        @DisplayName("应该能够评估项目质量")
        void shouldEvaluateProjectQuality() {
            // 准备
            Path javaProject = testProjectsRoot.resolve("small-java-project");
            var files = fileSystemAdapter.scanProjectFiles(javaProject);

            var project = top.yumbo.ai.reviewer.domain.model.Project.builder()
                    .name("small-java-project")
                    .rootPath(javaProject)
                    .type(top.yumbo.ai.reviewer.domain.model.ProjectType.JAVA)
                    .sourceFiles(files)
                    .build();

            // 执行
            AnalysisTask task = analysisService.analyzeProject(project);
            ReviewReport report = analysisService.getAnalysisResult(task.getTaskId());

            // 验证：评分在合理范围内
            assertThat(report.getOverallScore()).isBetween(0, 100);

            // 验证：至少有一些维度评分
            if (report.getDimensionScores() != null) {
                assertThat(report.getDimensionScores()).isNotEmpty();
            }
        }
    }

    @Nested
    @DisplayName("中项目分析测试")
    class MediumProjectAnalysisTest {

        @Test
        @DisplayName("应该能够分析中型Python项目")
        void shouldAnalyzeMediumPythonProject() {
            // 准备
            Path pythonProject = testProjectsRoot.resolve("medium-python-project");

            // 验证项目存在
            assertThat(Files.exists(pythonProject)).isTrue();

            // 执行
            var files = fileSystemAdapter.scanProjectFiles(pythonProject);

            // 验证
            assertThat(files).isNotEmpty();
            assertThat(files).hasSizeGreaterThanOrEqualTo(4); // 至少4个Python文件

            // 验证包含关键文件
            assertThat(files).anyMatch(f -> f.getFileName().equals("models.py"));
            assertThat(files).anyMatch(f -> f.getFileName().equals("views.py"));
        }

        @Test
        @DisplayName("应该处理异步分析流程")
        void shouldHandleAsyncAnalysisFlow() throws InterruptedException {
            // 准备
            Path pythonProject = testProjectsRoot.resolve("medium-python-project");
            var files = fileSystemAdapter.scanProjectFiles(pythonProject);

            var project = top.yumbo.ai.reviewer.domain.model.Project.builder()
                    .name("medium-python-project")
                    .rootPath(pythonProject)
                    .type(top.yumbo.ai.reviewer.domain.model.ProjectType.PYTHON)
                    .sourceFiles(files)
                    .build();

            // 执行：异步分析
            String taskId = analysisService.analyzeProjectAsync(project);
            assertThat(taskId).isNotNull();

            // 轮询任务状态
            AnalysisTask task = null;
            for (int i = 0; i < 30; i++) {
                task = analysisService.getTaskStatus(taskId);
                if (task.isCompleted() || task.isFailed()) {
                    break;
                }
                Thread.sleep(100);
            }

            // 验证：任务应该完成
            assertThat(task).isNotNull();
            assertThat(task.isCompleted()).isTrue();

            // 获取结果
            ReviewReport report = analysisService.getAnalysisResult(taskId);
            assertThat(report).isNotNull();
            assertThat(report.getProjectName()).isEqualTo("medium-python-project");
        }

        @Test
        @DisplayName("应该能够追踪分析进度")
        void shouldTrackAnalysisProgress() {
            // 准备
            Path pythonProject = testProjectsRoot.resolve("medium-python-project");
            var files = fileSystemAdapter.scanProjectFiles(pythonProject);

            var project = top.yumbo.ai.reviewer.domain.model.Project.builder()
                    .name("medium-python-project")
                    .rootPath(pythonProject)
                    .type(top.yumbo.ai.reviewer.domain.model.ProjectType.PYTHON)
                    .sourceFiles(files)
                    .build();

            // 执行
            AnalysisTask task = analysisService.analyzeProject(project);

            // 验证：任务状态
            assertThat(task.getTaskId()).isNotNull();
            assertThat(task.isCompleted()).isTrue();
            assertThat(task.getStartedAt()).isNotNull();
            assertThat(task.getCompletedAt()).isNotNull();

            // 验证：耗时合理
            long duration = task.getDurationMillis();
            assertThat(duration).isGreaterThanOrEqualTo(0);
        }
    }

    @Nested
    @DisplayName("多格式报告测试")
    class MultiFormatReportTest {

        private ReviewReport createTestReport(String projectName) {
            Path projectPath = testProjectsRoot.resolve(projectName);
            var files = fileSystemAdapter.scanProjectFiles(projectPath);

            var project = top.yumbo.ai.reviewer.domain.model.Project.builder()
                    .name(projectName)
                    .rootPath(projectPath)
                    .type(top.yumbo.ai.reviewer.domain.model.ProjectType.JAVA)
                    .sourceFiles(files)
                    .build();

            AnalysisTask task = analysisService.analyzeProject(project);
            return analysisService.getAnalysisResult(task.getTaskId());
        }

        @Test
        @DisplayName("应该能够生成HTML报告")
        void shouldGenerateHtmlReport() {
            // 准备
            ReviewReport report = createTestReport("small-java-project");

            // 执行
            String html = reportService.generateHtmlReport(report);

            // 验证
            assertThat(html).isNotEmpty();
            assertThat(html).contains("<!DOCTYPE html>");
            assertThat(html).contains("<html");
            assertThat(html).contains("small-java-project");

            // 保存文件
            Path outputPath = tempOutputDir.resolve("report.html");
            reportService.saveReport(report, outputPath, "html");
            assertThat(Files.exists(outputPath)).isTrue();
        }

        @Test
        @DisplayName("应该能够生成JSON报告")
        void shouldGenerateJsonReport() {
            // 准备
            ReviewReport report = createTestReport("small-java-project");

            // 执行
            String json = reportService.generateJsonReport(report);

            // 验证
            assertThat(json).isNotEmpty();
            assertThat(json).startsWith("{");
            assertThat(json).endsWith("}");
            assertThat(json).contains("small-java-project");

            // 保存文件
            Path outputPath = tempOutputDir.resolve("report.json");
            reportService.saveReport(report, outputPath, "json");
            assertThat(Files.exists(outputPath)).isTrue();
        }

        @Test
        @DisplayName("应该能够同时生成多种格式")
        void shouldGenerateMultipleFormats() throws Exception {
            // 准备
            ReviewReport report = createTestReport("small-java-project");

            // 执行：生成三种格式
            Path mdPath = tempOutputDir.resolve("report.md");
            Path htmlPath = tempOutputDir.resolve("report.html");
            Path jsonPath = tempOutputDir.resolve("report.json");

            reportService.saveReport(report, mdPath, "markdown");
            reportService.saveReport(report, htmlPath, "html");
            reportService.saveReport(report, jsonPath, "json");

            // 验证：所有文件都应该创建
            assertThat(Files.exists(mdPath)).isTrue();
            assertThat(Files.exists(htmlPath)).isTrue();
            assertThat(Files.exists(jsonPath)).isTrue();

            // 验证：文件不为空
            assertThat(Files.size(mdPath)).isGreaterThan(0);
            assertThat(Files.size(htmlPath)).isGreaterThan(0);
            assertThat(Files.size(jsonPath)).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("错误场景测试")
    class ErrorScenarioTest {

        @Test
        @DisplayName("应该处理不存在的项目路径")
        void shouldHandleNonExistentPath() {
            // 准备
            Path nonExistentPath = Paths.get("/non/existent/path");

            // 执行 & 验证
            assertThatThrownBy(() -> fileSystemAdapter.scanProjectFiles(nonExistentPath))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("应该处理空项目目录")
        void shouldHandleEmptyDirectory() {
            // 准备：创建空目录
            Path emptyDir = tempOutputDir.resolve("empty-project");
            assertThatCode(() -> Files.createDirectories(emptyDir)).doesNotThrowAnyException();

            // 执行
            var files = fileSystemAdapter.scanProjectFiles(emptyDir);

            // 验证：应该返回空列表
            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("应该能够取消正在运行的任务")
        void shouldCancelRunningTask() {
            // 准备
            Path project = testProjectsRoot.resolve("small-java-project");
            var files = fileSystemAdapter.scanProjectFiles(project);

            var proj = top.yumbo.ai.reviewer.domain.model.Project.builder()
                    .name("test-project")
                    .rootPath(project)
                    .type(top.yumbo.ai.reviewer.domain.model.ProjectType.JAVA)
                    .sourceFiles(files)
                    .build();

            // 执行：启动异步任务
            String taskId = analysisService.analyzeProjectAsync(proj);

            // 尝试取消任务
            analysisService.cancelTask(taskId);

            // 验证：任务ID应该有效
            assertThat(taskId).isNotNull();
        }
    }

    @Nested
    @DisplayName("React项目分析测试")
    class ReactProjectAnalysisTest {

        @Test
        @DisplayName("应该能够分析React项目")
        void shouldAnalyzeReactProject() {
            // 准备
            Path reactProject = testProjectsRoot.resolve("small-react-project");

            // 验证项目存在
            assertThat(Files.exists(reactProject)).isTrue();

            // 执行
            var files = fileSystemAdapter.scanProjectFiles(reactProject);

            // 验证
            assertThat(files).isNotEmpty();

            // 验证包含React文件
            assertThat(files).anyMatch(f ->
                f.getFileName().endsWith(".jsx") || f.getFileName().endsWith(".js"));
        }

        @Test
        @DisplayName("应该能够识别React组件")
        void shouldIdentifyReactComponents() {
            // 准备
            Path reactProject = testProjectsRoot.resolve("small-react-project");
            var files = fileSystemAdapter.scanProjectFiles(reactProject);

            // 验证：应该找到组件文件
            assertThat(files).anyMatch(f -> f.getFileName().equals("App.jsx"));
            assertThat(files).anyMatch(f -> f.getFileName().equals("Header.jsx"));
            assertThat(files).anyMatch(f -> f.getFileName().equals("TaskList.jsx"));
        }
    }
}

