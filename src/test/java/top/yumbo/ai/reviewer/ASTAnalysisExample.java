package top.yumbo.ai.reviewer;

import top.yumbo.ai.reviewer.adapter.output.ast.parser.JavaParserAdapter;
import top.yumbo.ai.reviewer.application.service.prompt.AIPromptBuilder;
import top.yumbo.ai.reviewer.domain.model.Project;
import top.yumbo.ai.reviewer.domain.model.ProjectType;
import top.yumbo.ai.reviewer.domain.model.SourceFile;
import top.yumbo.ai.reviewer.domain.model.ast.CodeInsight;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * AST分析示例程序
 *
 * 演示如何使用JavaParser进行代码分析
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-13
 */
public class ASTAnalysisExample {

    public static void main(String[] args) {
        System.out.println("=== AI-Reviewer AST分析示例 ===\n");

        // 1. 创建测试项目
        Project project = createSampleProject();

        // 2. 使用JavaParser解析
        System.out.println("正在解析Java项目...");
        JavaParserAdapter parser = new JavaParserAdapter();
        CodeInsight insight = parser.parseProject(project);

        // 3. 输出分析结果
        printAnalysisResults(insight);

        // 4. 生成AI提示词
        System.out.println("\n=== 生成的AI提示词 ===\n");
        AIPromptBuilder promptBuilder = new AIPromptBuilder();
        String prompt = promptBuilder.buildEnhancedPrompt(project, insight);
        System.out.println(prompt);
    }

    /**
     * 创建示例项目
     */
    private static Project createSampleProject() {
        try {
            // 创建临时目录
            Path tempDir = Files.createTempDirectory("ast-demo");

            // 创建示例Java文件
            createSampleJavaFile(tempDir, "UserService.java", """
                package com.example.service;
                
                import com.example.model.User;
                import com.example.repository.UserRepository;
                
                public class UserService {
                    private final UserRepository repository;
                    
                    public UserService(UserRepository repository) {
                        this.repository = repository;
                    }
                    
                    public User findById(Long id) {
                        if (id == null) {
                            throw new IllegalArgumentException("ID不能为空");
                        }
                        return repository.findById(id);
                    }
                    
                    public User save(User user) {
                        if (user == null) {
                            throw new IllegalArgumentException("用户对象不能为空");
                        }
                        return repository.save(user);
                    }
                    
                    public void delete(Long id) {
                        if (id == null) {
                            throw new IllegalArgumentException("ID不能为空");
                        }
                        repository.delete(id);
                    }
                }
                """);

            createSampleJavaFile(tempDir, "User.java", """
                package com.example.model;
                
                public class User {
                    private Long id;
                    private String name;
                    private String email;
                    
                    public User() {}
                    
                    public User(String name, String email) {
                        this.name = name;
                        this.email = email;
                    }
                    
                    public Long getId() { return id; }
                    public void setId(Long id) { this.id = id; }
                    
                    public String getName() { return name; }
                    public void setName(String name) { this.name = name; }
                    
                    public String getEmail() { return email; }
                    public void setEmail(String email) { this.email = email; }
                }
                """);

            // 构建项目对象
            Project.ProjectBuilder builder = Project.builder()
                .name("ast-demo-project")
                .rootPath(tempDir)
                .type(ProjectType.JAVA);

            Project project = builder.build();

            // 添加源文件
            try (Stream<Path> paths = Files.walk(tempDir)) {
                paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(path -> {
                        try {
                            String content = Files.readString(path);
                            SourceFile sourceFile = SourceFile.builder()
                                .path(path)
                                .relativePath(tempDir.relativize(path).toString())
                                .fileName(path.getFileName().toString())
                                .extension("java")
                                .content(content)
                                .lineCount((int) content.lines().count())
                                .build();
                            project.addSourceFile(sourceFile);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
            }

            return project;

        } catch (IOException e) {
            throw new RuntimeException("创建示例项目失败", e);
        }
    }

    private static void createSampleJavaFile(Path dir, String filename, String content) throws IOException {
        Path file = dir.resolve(filename);
        Files.writeString(file, content);
    }

    /**
     * 打印分析结果
     */
    private static void printAnalysisResults(CodeInsight insight) {
        System.out.println("\n=== 项目分析结果 ===\n");

        System.out.println("项目名称: " + insight.getProjectName());
        System.out.println("类数量: " + insight.getClasses().size());
        System.out.println("接口数量: " + insight.getInterfaces().size());

        if (insight.getStatistics() != null) {
            System.out.println("\n统计信息:");
            System.out.println("  总方法数: " + insight.getStatistics().getTotalMethods());
            System.out.println("  总字段数: " + insight.getStatistics().getTotalFields());
        }

        if (insight.getComplexityMetrics() != null) {
            System.out.println("\n复杂度指标:");
            System.out.println("  平均圈复杂度: " + String.format("%.2f", insight.getComplexityMetrics().getAvgCyclomaticComplexity()));
            System.out.println("  最高圈复杂度: " + insight.getComplexityMetrics().getMaxCyclomaticComplexity());
            System.out.println("  高复杂度方法数: " + insight.getComplexityMetrics().getHighComplexityMethodCount());
        }

        if (insight.getStructure() != null) {
            System.out.println("\n架构风格:");
            System.out.println("  " + insight.getStructure().getArchitectureStyle());
        }

        if (insight.getDesignPatterns() != null && !insight.getDesignPatterns().getPatterns().isEmpty()) {
            System.out.println("\n检测到的设计模式:");
            insight.getDesignPatterns().getPatterns().forEach(pattern -> {
                System.out.println("  - " + pattern.toString());
            });
        }

        if (!insight.getCodeSmells().isEmpty()) {
            System.out.println("\n代码坏味道:");
            insight.getCodeSmells().stream()
                .limit(5)
                .forEach(smell -> {
                    System.out.println("  - " + smell.toString());
                });
        }

        System.out.println("\n类详情:");
        insight.getClasses().forEach(cls -> {
            System.out.println("\n  类名: " + cls.getClassName());
            System.out.println("    包名: " + cls.getPackageName());
            System.out.println("    方法数: " + cls.getMethodCount());
            System.out.println("    字段数: " + cls.getFieldCount());
            System.out.println("    代码行数: " + cls.getLinesOfCode());

            if (!cls.getMethods().isEmpty()) {
                System.out.println("    方法列表:");
                cls.getMethods().forEach(method -> {
                    System.out.println("      - " + method.getMethodName() +
                        " (复杂度: " + method.getCyclomaticComplexity() +
                        ", 行数: " + method.getLinesOfCode() + ")");
                });
            }
        });
    }
}

