package top.yumbo.ai.reviewer.application.hackathon.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.yumbo.ai.reviewer.adapter.parser.code.ASTParserFactory;
import top.yumbo.ai.reviewer.domain.hackathon.model.HackathonScore;
import top.yumbo.ai.reviewer.domain.model.Project;
import top.yumbo.ai.reviewer.domain.model.ProjectType;
import top.yumbo.ai.reviewer.domain.model.ReviewReport;
import top.yumbo.ai.reviewer.domain.model.SourceFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

/**
 * 黑客松评分服务AST集成测试
 *
 * @author AI-Reviewer Team
 * @version 2.0
 * @since 2025-11-13
 */
class HackathonScoringServiceASTTest {

    private HackathonScoringService scoringService;

    @BeforeEach
    void setUp() {
        // 使用AST解析器工厂
        scoringService = new HackathonScoringService(new ASTParserFactory());
    }

    @Test
    void testCalculateScoreWithJavaProject() throws IOException {
        // 创建一个简单的Java项目
        Project project = createJavaProject();

        // 创建模拟的评审报告
        ReviewReport report = ReviewReport.builder()
            .projectName("test-java-project")
            .overallScore(75)
            .build();

        // 计算评分
        HackathonScore score = scoringService.calculateScore(report, project);

        // 验证
        assertThat(score).isNotNull();
        assertThat(score.getCodeQuality()).isGreaterThan(0);
        assertThat(score.getInnovation()).isGreaterThan(0);
        assertThat(score.getCompleteness()).isGreaterThan(0);
        assertThat(score.getDocumentation()).isGreaterThan(0);
        assertThat(score.getTotalScore()).isGreaterThan(0);

        System.out.println("评分结果:");
        System.out.println("代码质量: " + score.getCodeQuality());
        System.out.println("创新性: " + score.getInnovation());
        System.out.println("完成度: " + score.getCompleteness());
        System.out.println("文档: " + score.getDocumentation());
        System.out.println("总分: " + score.getTotalScore());
    }

    @Test
    void testCalculateScoreWithPythonProject() throws IOException {
        // 创建一个Python项目
        Project project = createPythonProject();

        ReviewReport report = ReviewReport.builder()
            .projectName("test-python-project")
            .overallScore(80)
            .build();

        HackathonScore score = scoringService.calculateScore(report, project);

        assertThat(score).isNotNull();
        assertThat(score.getTotalScore()).isGreaterThan(0);

        System.out.println("\nPython项目评分结果:");
        System.out.println("代码质量: " + score.getCodeQuality());
        System.out.println("创新性: " + score.getInnovation());
        System.out.println("完成度: " + score.getCompleteness());
        System.out.println("总分: " + score.getTotalScore());
    }

    @Test
    void testCalculateScoreWithGoProject() throws IOException {
        // 创建一个Go项目
        Project project = createGoProject();

        ReviewReport report = ReviewReport.builder()
            .projectName("test-go-project")
            .overallScore(85)
            .build();

        HackathonScore score = scoringService.calculateScore(report, project);

        assertThat(score).isNotNull();
        assertThat(score.getTotalScore()).isGreaterThan(0);

        System.out.println("\nGo项目评分结果:");
        System.out.println("代码质量: " + score.getCodeQuality());
        System.out.println("创新性: " + score.getInnovation());
        System.out.println("完成度: " + score.getCompleteness());
        System.out.println("总分: " + score.getTotalScore());
    }

    @Test
    void testCalculateScoreWithHighQualityProject() throws IOException {
        // 创建一个高质量的Java项目
        Project project = createHighQualityJavaProject();

        ReviewReport report = ReviewReport.builder()
            .projectName("high-quality-project")
            .overallScore(90)
            .build();

        HackathonScore score = scoringService.calculateScore(report, project);

        // 高质量项目应该得到高分
        assertThat(score.getCodeQuality()).isGreaterThan(70);
        assertThat(score.getInnovation()).isGreaterThan(60);
        assertThat(score.getTotalScore()).isGreaterThan(250);

        System.out.println("\n高质量项目评分结果:");
        System.out.println("代码质量: " + score.getCodeQuality());
        System.out.println("创新性: " + score.getInnovation());
        System.out.println("完成度: " + score.getCompleteness());
        System.out.println("总分: " + score.getTotalScore());
    }

    /**
     * 创建简单的Java项目
     */
    private Project createJavaProject() throws IOException {
        Path tempDir = Files.createTempDirectory("test-java");

        String javaCode = """
            package com.example;
            
            public class UserService {
                private String name;
                
                public UserService(String name) {
                    this.name = name;
                }
                
                public String getName() {
                    return name;
                }
                
                public void process() {
                    if (name != null) {
                        System.out.println(name);
                    }
                }
            }
            """;

        return createProject("test-java-project", ProjectType.JAVA, tempDir,
            "UserService.java", javaCode);
    }

    /**
     * 创建Python项目
     */
    private Project createPythonProject() throws IOException {
        Path tempDir = Files.createTempDirectory("test-python");

        String pythonCode = """
            class UserService:
                def __init__(self, name):
                    self.name = name
                
                def get_name(self):
                    return self.name
                
                def process(self):
                    if self.name:
                        print(self.name)
            """;

        return createProject("test-python-project", ProjectType.PYTHON, tempDir,
            "user_service.py", pythonCode);
    }

    /**
     * 创建Go项目
     */
    private Project createGoProject() throws IOException {
        Path tempDir = Files.createTempDirectory("test-go");

        String goCode = """
            package service
            
            type UserService struct {
                name string
            }
            
            func NewUserService(name string) *UserService {
                return &UserService{name: name}
            }
            
            func (s *UserService) GetName() string {
                return s.name
            }
            """;

        return createProject("test-go-project", ProjectType.GO, tempDir,
            "user_service.go", goCode);
    }

    /**
     * 创建高质量Java项目
     */
    private Project createHighQualityJavaProject() throws IOException {
        Path tempDir = Files.createTempDirectory("test-high-quality");

        String serviceCode = """
            package com.example.service;
            
            import com.example.repository.UserRepository;
            import com.example.model.User;
            
            public class UserService {
                private final UserRepository repository;
                
                public UserService(UserRepository repository) {
                    this.repository = repository;
                }
                
                public User findById(Long id) {
                    if (id == null) {
                        throw new IllegalArgumentException("ID cannot be null");
                    }
                    return repository.findById(id);
                }
                
                public void save(User user) {
                    if (user == null) {
                        throw new IllegalArgumentException("User cannot be null");
                    }
                    repository.save(user);
                }
            }
            """;

        Path serviceFile = tempDir.resolve("UserService.java");
        Files.writeString(serviceFile, serviceCode);

        String repositoryCode = """
            package com.example.repository;
            
            import com.example.model.User;
            
            public interface UserRepository {
                User findById(Long id);
                void save(User user);
            }
            """;

        Path repoFile = tempDir.resolve("UserRepository.java");
        Files.writeString(repoFile, repositoryCode);

        Project project = Project.builder()
            .name("high-quality-project")
            .rootPath(tempDir)
            .type(ProjectType.JAVA)
            .build();

        addSourceFile(project, serviceFile, serviceCode);
        addSourceFile(project, repoFile, repositoryCode);

        return project;
    }

    private Project createProject(String name, ProjectType type, Path tempDir,
                                  String filename, String code) throws IOException {
        Path file = tempDir.resolve(filename);
        Files.writeString(file, code);

        Project project = Project.builder()
            .name(name)
            .rootPath(tempDir)
            .type(type)
            .build();

        addSourceFile(project, file, code);

        return project;
    }

    private void addSourceFile(Project project, Path file, String code) {
        SourceFile sourceFile = SourceFile.builder()
            .path(file)
            .relativePath(file.getFileName().toString())
            .fileName(file.getFileName().toString())
            .extension(getExtension(file.getFileName().toString()))
            .content(code)
            .lineCount((int) code.lines().count())
            .build();

        project.addSourceFile(sourceFile);
    }

    private String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "";
    }
}

