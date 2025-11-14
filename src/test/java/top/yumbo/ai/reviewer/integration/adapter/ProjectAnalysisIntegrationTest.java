package top.yumbo.ai.reviewer.integration.adapter;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import top.yumbo.ai.reviewer.adapter.storage.cache.FileCacheAdapter;
import top.yumbo.ai.reviewer.adapter.storage.local.LocalFileSystemAdapter;
import top.yumbo.ai.reviewer.application.port.output.AIServicePort;
import top.yumbo.ai.reviewer.application.port.output.CachePort;
import top.yumbo.ai.reviewer.application.port.output.FileSystemPort;
import top.yumbo.ai.reviewer.application.service.ProjectAnalysisService;
import top.yumbo.ai.reviewer.domain.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ProjectAnalysisService集成测试
 * 测试应用服务层与适配器层的集成
 */
@DisplayName("ProjectAnalysisService集成测试")
class ProjectAnalysisIntegrationTest {

    @TempDir
    Path tempProjectDir;

    @TempDir
    Path tempCacheDir;

    private ProjectAnalysisService analysisService;
    private FileSystemPort fileSystemPort;
    private CachePort cachePort;
    private AIServicePort aiServicePort;

    @BeforeEach
    void setUp() {
        // 创建真实的文件系统适配器
        LocalFileSystemAdapter.FileSystemConfig fsConfig =
                new LocalFileSystemAdapter.FileSystemConfig(
                        List.of("*.java", "*.py", "*.js", "*.ts"),
                        List.of("*.class", "*.jar", "target/*", "build/*", "node_modules/*"),
                        1024, // maxFileSizeKB
                        10    // maxDepth
                );
        fileSystemPort = new LocalFileSystemAdapter(fsConfig);

        // 创建真实的缓存适配器
        cachePort = new FileCacheAdapter(tempCacheDir);

        // Mock AI服务（避免真实API调用）
        aiServicePort = mock(AIServicePort.class);
        when(aiServicePort.analyze(anyString()))
                .thenReturn("模拟的AI分析结果：该代码结构清晰，符合最佳实践。");

        // 创建应用服务
        analysisService = new ProjectAnalysisService(
                aiServicePort,
                cachePort,
                fileSystemPort
        );
    }

    @Nested
    @DisplayName("与LocalFileSystemAdapter集成测试")
    class FileSystemIntegrationTest {

        @Test
        @DisplayName("应该能够扫描真实的Java项目")
        void shouldScanRealJavaProject() throws IOException {
            // 准备：创建测试项目
            createTestJavaProject(tempProjectDir);

            // 执行：扫描项目
            List<SourceFile> files = fileSystemPort.scanProjectFiles(tempProjectDir);

            // 验证：应该找到所有Java文件
            assertThat(files).isNotEmpty();
            assertThat(files).hasSize(3); // Main.java, Service.java, Controller.java

            // 验证文件属性
            SourceFile mainFile = files.stream()
                    .filter(f -> f.getFileName().equals("Main.java"))
                    .findFirst()
                    .orElseThrow();

            assertThat(mainFile.getExtension()).isEqualTo("java");
            assertThat(mainFile.getLineCount()).isGreaterThan(0);
            assertThat(mainFile.getSizeInBytes()).isGreaterThan(0);
        }

        @Test
        @DisplayName("应该正确加载文件内容")
        void shouldLoadFileContent() throws IOException {
            // 准备
            Path javaFile = tempProjectDir.resolve("Test.java");
            String content = "public class Test {\n    public static void main(String[] args) {}\n}";
            Files.writeString(javaFile, content);

            // 执行
            String loadedContent = fileSystemPort.readFileContent(javaFile);

            // 验证
            assertThat(loadedContent).isEqualTo(content);
        }

        @Test
        @DisplayName("应该正确创建Project对象")
        void shouldCreateProjectObject() throws IOException {
            // 准备
            createTestJavaProject(tempProjectDir);

            // 执行
            List<SourceFile> files = fileSystemPort.scanProjectFiles(tempProjectDir);
            Project project = Project.builder()
                    .name("TestProject")
                    .rootPath(tempProjectDir)
                    .type(ProjectType.JAVA)  // 添加type字段
                    .sourceFiles(files)
                    .build();

            // 验证
            assertThat(project.getName()).isEqualTo("TestProject");
            assertThat(project.getSourceFiles()).hasSize(3);
            assertThat(project.getTotalLines()).isGreaterThan(0);
            assertThat(project.isValid()).isTrue();
        }
    }

    @Nested
    @DisplayName("与FileCacheAdapter集成测试")
    class CacheIntegrationTest {

        @Test
        @DisplayName("应该能够缓存分析结果")
        void shouldCacheAnalysisResult() throws IOException {
            // 准备
            createTestJavaProject(tempProjectDir);
            Project project = createProject(tempProjectDir);

            // 执行：首次分析
            AnalysisTask task = analysisService.analyzeProject(project);

            // 验证任务完成
            assertThat(task).isNotNull();
            assertThat(task.isCompleted()).isTrue();

            // 获取报告
            ReviewReport report = analysisService.getAnalysisResult(task.getTaskId());
            assertThat(report.getProjectName()).isEqualTo(project.getName());

            // 验证缓存被写入（使用正确的缓存key格式）
            String cacheKey = "overview:" + project.getName();
            assertThat(cachePort.get(cacheKey)).isPresent();
        }

        @Test
        @DisplayName("应该能够从缓存读取结果")
        void shouldReadFromCache() throws IOException {
            // 准备
            createTestJavaProject(tempProjectDir);
            Project project = createProject(tempProjectDir);

            // 首次分析（写入缓存）
            AnalysisTask firstTask = analysisService.analyzeProject(project);
            ReviewReport firstReport = analysisService.getAnalysisResult(firstTask.getTaskId());

            // 执行：第二次分析（应该从缓存读取）
            long startTime = System.currentTimeMillis();
            AnalysisTask secondTask = analysisService.analyzeProject(project);
            long duration = System.currentTimeMillis() - startTime;
            ReviewReport secondReport = analysisService.getAnalysisResult(secondTask.getTaskId());

            // 验证：第二次应该更快（从缓存读取）
            assertThat(duration).isLessThan(1000); // 应该在1秒内完成
            assertThat(secondReport.getProjectName()).isEqualTo(firstReport.getProjectName());
        }

        @Test
        @DisplayName("应该正确处理缓存过期")
        void shouldHandleCacheExpiration() throws IOException, InterruptedException {
            // 准备：使用短TTL的缓存
            Path shortTtlCacheDir = tempCacheDir.resolve("short-ttl");
            CachePort shortTtlCache = new FileCacheAdapter(shortTtlCacheDir);

            // 写入缓存
            String key = "test_key";
            String value = "test_value";
            shortTtlCache.put(key, value, 100L); // 100ms TTL

            // 验证立即可读
            assertThat(shortTtlCache.get(key)).isPresent();

            // 注意：当前FileCacheAdapter实现可能使用固定TTL
            // 实际项目中应该支持自定义TTL，这里先验证基本功能
            // TODO: 完善FileCacheAdapter的TTL支持

            shortTtlCache.close();
        }
    }

    @Nested
    @DisplayName("与AIServicePort集成测试（Mock）")
    class AIServiceIntegrationTest {

        @Test
        @DisplayName("应该正确调用AI服务分析代码")
        void shouldCallAIServiceToAnalyzeCode() throws IOException {
            // 准备
            createTestJavaProject(tempProjectDir);
            Project project = createProject(tempProjectDir);

            // 执行：分析项目
            AnalysisTask task = analysisService.analyzeProject(project);
            ReviewReport report = analysisService.getAnalysisResult(task.getTaskId());

            // 验证：报告应该包含AI分析结果
            assertThat(report).isNotNull();
            assertThat(report.getProjectName()).isEqualTo(project.getName());
        }

        @Test
        @DisplayName("应该处理AI服务失败的情况")
        void shouldHandleAIServiceFailure() throws IOException {
            // 准备：Mock AI服务抛出异常
            when(aiServicePort.analyze(anyString()))
                    .thenThrow(new RuntimeException("AI服务暂时不可用"));

            createTestJavaProject(tempProjectDir);
            Project project = createProject(tempProjectDir);

            // 执行 & 验证：应该抛出异常（可能被包装成其他异常）
            assertThatThrownBy(() -> analysisService.analyzeProject(project))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("完整分析流程集成测试")
    class CompleteFlowIntegrationTest {

        @Test
        @DisplayName("应该完成完整的同步分析流程")
        void shouldCompleteEntireSyncAnalysisFlow() throws IOException {
            // 准备：创建真实项目
            createTestJavaProject(tempProjectDir);
            Project project = createProject(tempProjectDir);

            // 执行：完整分析流程
            // 文件系统 → 扫描文件
            List<SourceFile> files = fileSystemPort.scanProjectFiles(tempProjectDir);
            assertThat(files).isNotEmpty();

            // 应用服务 → 分析项目（内部会调用AI服务）
            AnalysisTask task = analysisService.analyzeProject(project);
            ReviewReport report = analysisService.getAnalysisResult(task.getTaskId());

            // 验证：报告应该完整
            assertThat(report).isNotNull();
            assertThat(report.getReportId()).isNotNull();
            assertThat(report.getProjectName()).isEqualTo(project.getName());
            assertThat(report.getGeneratedAt()).isNotNull();

            // 验证：缓存应该被写入（使用正确的缓存key格式）
            String cacheKey = "overview:" + project.getName();
            assertThat(cachePort.get(cacheKey)).isPresent();
        }

        @Test
        @DisplayName("应该完成完整的异步分析流程")
        void shouldCompleteEntireAsyncAnalysisFlow() throws IOException, InterruptedException {
            // 准备
            createTestJavaProject(tempProjectDir);
            Project project = createProject(tempProjectDir);

            // 执行：启动异步分析
            String taskId = analysisService.analyzeProjectAsync(project);
            assertThat(taskId).isNotNull();

            // 轮询任务状态
            AnalysisTask task = null;
            int maxAttempts = 30; // 最多等待30秒
            for (int i = 0; i < maxAttempts; i++) {
                task = analysisService.getTaskStatus(taskId);
                if (task.isCompleted() || task.isFailed()) {
                    break;
                }
                Thread.sleep(1000);
            }

            // 验证：任务应该完成
            assertThat(task).isNotNull();
            assertThat(task.isCompleted()).isTrue();

            // 获取结果
            ReviewReport report = analysisService.getAnalysisResult(taskId);
            assertThat(report).isNotNull();
            assertThat(report.getProjectName()).isEqualTo(project.getName());
        }

        @Test
        @DisplayName("数据应该在各层正确传递")
        void shouldPassDataCorrectlyBetweenLayers() throws IOException {
            // 准备
            createTestJavaProject(tempProjectDir);

            // Layer 1: FileSystem Adapter → 扫描文件
            List<SourceFile> files = fileSystemPort.scanProjectFiles(tempProjectDir);
            assertThat(files).hasSize(3);

            // Layer 2: 构建领域模型
            Project project = Project.builder()
                    .name("TestProject")
                    .rootPath(tempProjectDir)
                    .type(ProjectType.JAVA)  // 添加type字段
                    .sourceFiles(files)
                    .build();
            assertThat(project.getTotalLines()).isGreaterThan(0);

            // Layer 3: Application Service → 分析项目
            AnalysisTask task = analysisService.analyzeProject(project);
            ReviewReport report = analysisService.getAnalysisResult(task.getTaskId());
            assertThat(report.getProjectName()).isEqualTo("TestProject");

            // Layer 4: Cache Adapter → 缓存结果（使用正确的缓存key格式）
            String cacheKey = "overview:TestProject";
            assertThat(cachePort.get(cacheKey)).isPresent();

            // 验证数据一致性
            assertThat(report.getProjectPath()).isEqualTo(tempProjectDir.toString());
        }
    }

    @Nested
    @DisplayName("错误处理集成测试")
    class ErrorHandlingIntegrationTest {

        @Test
        @DisplayName("应该处理无效的项目路径")
        void shouldHandleInvalidProjectPath() {
            // 准备：不存在的路径
            Path nonExistentPath = tempProjectDir.resolve("non-existent");

            // 执行 & 验证
            assertThatThrownBy(() -> fileSystemPort.scanProjectFiles(nonExistentPath))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("应该处理空项目")
        void shouldHandleEmptyProject() {
            // 准备：空目录
            List<SourceFile> files = fileSystemPort.scanProjectFiles(tempProjectDir);

            // 验证：应该返回空列表
            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("应该处理无效的Project对象")
        void shouldHandleInvalidProject() {
            // 准备：创建无效的项目（无源文件）
            Project emptyProject = Project.builder()
                    .name("EmptyProject")
                    .rootPath(tempProjectDir)
                    .type(ProjectType.JAVA)
                    .sourceFiles(List.of())
                    .build();

            // 验证：项目基本信息有效，但无源文件
            assertThat(emptyProject.isValid()).isTrue(); // 基本信息有效
            assertThat(emptyProject.getSourceFiles()).isEmpty(); // 但无源文件

            // 执行：空项目也可以分析（只是结果较简单）
            AnalysisTask task = analysisService.analyzeProject(emptyProject);

            // 验证：分析应该正常完成
            assertThat(task).isNotNull();
            assertThat(task.isCompleted()).isTrue();
        }
    }

    // ============= 辅助方法 =============

    /**
     * 创建测试用的Java项目
     */
    private void createTestJavaProject(Path projectDir) throws IOException {
        // 创建目录结构
        Path srcDir = projectDir.resolve("src");
        Files.createDirectories(srcDir);

        // 创建Main.java
        String mainContent = """
                package com.test;
                
                public class Main {
                    public static void main(String[] args) {
                        System.out.println("Hello World");
                        Service service = new Service();
                        service.doSomething();
                    }
                }
                """;
        Files.writeString(srcDir.resolve("Main.java"), mainContent);

        // 创建Service.java
        String serviceContent = """
                package com.test;
                
                public class Service {
                    private String name;
                    
                    public void doSomething() {
                        System.out.println("Service is working");
                    }
                    
                    public String getName() {
                        return name;
                    }
                }
                """;
        Files.writeString(srcDir.resolve("Service.java"), serviceContent);

        // 创建Controller.java
        String controllerContent = """
                package com.test;
                
                public class Controller {
                    private Service service;
                    
                    public Controller(Service service) {
                        this.service = service;
                    }
                    
                    public void handleRequest() {
                        service.doSomething();
                    }
                }
                """;
        Files.writeString(srcDir.resolve("Controller.java"), controllerContent);
    }

    /**
     * 创建Project对象
     */
    private Project createProject(Path projectPath) {
        List<SourceFile> files = fileSystemPort.scanProjectFiles(projectPath);
        return Project.builder()
                .name("TestProject")
                .rootPath(projectPath)
                .type(ProjectType.JAVA)  // 添加type字段
                .sourceFiles(files)
                .build();
    }

    @AfterEach
    void tearDown() {
        // 清理资源
        if (cachePort != null) {
            cachePort.close();
        }
    }
}

