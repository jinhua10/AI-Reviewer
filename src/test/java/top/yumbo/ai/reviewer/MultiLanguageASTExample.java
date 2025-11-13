package top.yumbo.ai.reviewer;

import top.yumbo.ai.reviewer.adapter.output.ast.parser.ASTParserFactory;
import top.yumbo.ai.reviewer.adapter.output.ast.parser.JavaScriptParserAdapter;
import top.yumbo.ai.reviewer.adapter.output.ast.parser.PythonParserAdapter;
import top.yumbo.ai.reviewer.application.service.prompt.AIPromptBuilder;
import top.yumbo.ai.reviewer.domain.model.Project;
import top.yumbo.ai.reviewer.domain.model.ProjectType;
import top.yumbo.ai.reviewer.domain.model.SourceFile;
import top.yumbo.ai.reviewer.domain.model.ast.CodeInsight;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 多语言AST分析示例
 *
 * 演示Python和JavaScript项目的AST分析
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-13
 */
public class MultiLanguageASTExample {

    public static void main(String[] args) {
        System.out.println("=== 多语言AST分析示例 ===\n");

        try {
            // 1. 测试Python项目
            System.out.println("【1. Python项目分析】");
            testPythonProject();

            System.out.println("\n" + "=".repeat(80) + "\n");

            // 2. 测试JavaScript项目
            System.out.println("【2. JavaScript项目分析】");
            testJavaScriptProject();

            System.out.println("\n" + "=".repeat(80) + "\n");

            // 3. 使用工厂自动选择
            System.out.println("【3. 使用解析器工厂】");
            testParserFactory();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 测试Python项目分析
     */
    private static void testPythonProject() throws IOException {
        // 创建Python示例代码
        String pythonCode = """
            class UserService:
                \"\"\"用户服务类\"\"\"
                
                def __init__(self, repository):
                    self.repository = repository
                
                def find_user(self, user_id: int) -> dict:
                    \"\"\"查找用户\"\"\"
                    if user_id is None:
                        raise ValueError("user_id不能为空")
                    
                    if user_id <= 0:
                        raise ValueError("user_id必须大于0")
                    
                    return self.repository.find(user_id)
                
                @staticmethod
                def validate_email(email: str) -> bool:
                    \"\"\"验证邮箱格式\"\"\"
                    if not email:
                        return False
                    return '@' in email and '.' in email
                
                async def fetch_user_async(self, user_id: int):
                    \"\"\"异步获取用户\"\"\"
                    for attempt in range(3):
                        try:
                            return await self.repository.fetch(user_id)
                        except Exception as e:
                            if attempt == 2:
                                raise
                            continue
            """;

        // 创建项目
        Project project = createProject("python-demo", ProjectType.PYTHON, "user_service.py", pythonCode);

        // 解析
        PythonParserAdapter parser = new PythonParserAdapter();
        CodeInsight insight = parser.parseProject(project);

        // 打印结果
        printAnalysisResults("Python", insight);

        // 生成AI提示词
        AIPromptBuilder promptBuilder = new AIPromptBuilder();
        String prompt = promptBuilder.buildEnhancedPrompt(project, insight);
        System.out.println("\n生成的AI提示词片段:");
        System.out.println(prompt.substring(0, Math.min(500, prompt.length())) + "...\n");
    }

    /**
     * 测试JavaScript项目分析
     */
    private static void testJavaScriptProject() throws IOException {
        // 创建JavaScript示例代码
        String jsCode = """
            /**
             * 用户服务类
             */
            class UserService extends BaseService {
                constructor(repository) {
                    super();
                    this.repository = repository;
                }
                
                /**
                 * 查找用户
                 */
                async findUser(userId) {
                    if (!userId) {
                        throw new Error('User ID is required');
                    }
                    
                    if (userId <= 0) {
                        throw new Error('Invalid user ID');
                    }
                    
                    try {
                        return await this.repository.find(userId);
                    } catch (error) {
                        console.error('Failed to find user:', error);
                        throw error;
                    }
                }
                
                /**
                 * 验证邮箱
                 */
                static validateEmail(email) {
                    if (!email) {
                        return false;
                    }
                    
                    const pattern = /^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$/;
                    return pattern.test(email);
                }
                
                /**
                 * 批量处理用户
                 */
                async batchProcess(userIds) {
                    const results = [];
                    
                    for (const id of userIds) {
                        try {
                            const user = await this.findUser(id);
                            results.push(user);
                        } catch (error) {
                            results.push(null);
                        }
                    }
                    
                    return results.filter(r => r !== null);
                }
            }
            
            // 导出
            export default UserService;
            """;

        // 创建项目
        Project project = createProject("javascript-demo", ProjectType.NODE, "UserService.js", jsCode);

        // 解析
        JavaScriptParserAdapter parser = new JavaScriptParserAdapter();
        CodeInsight insight = parser.parseProject(project);

        // 打印结果
        printAnalysisResults("JavaScript", insight);

        // 生成AI提示词
        AIPromptBuilder promptBuilder = new AIPromptBuilder();
        String prompt = promptBuilder.buildEnhancedPrompt(project, insight);
        System.out.println("\n生成的AI提示词片段:");
        System.out.println(prompt.substring(0, Math.min(500, prompt.length())) + "...\n");
    }

    /**
     * 测试解析器工厂
     */
    private static void testParserFactory() throws IOException {
        System.out.println("创建解析器工厂...");
        ASTParserFactory factory = new ASTParserFactory();

        System.out.println("支持的解析器: " + factory.getSupportedTypes());
        System.out.println();

        // 测试不同类型的项目
        String[] testCases = {
            "Python项目",
            "JavaScript项目",
            "Java项目"
        };

        ProjectType[] types = {
            ProjectType.PYTHON,
            ProjectType.NODE,
            ProjectType.JAVA
        };

        for (int i = 0; i < testCases.length; i++) {
            String projectType = types[i].name();
            boolean supported = factory.supports(projectType);

            System.out.printf("%-20s: %s\n",
                testCases[i],
                supported ? "✅ 支持" : "❌ 不支持");
        }

        System.out.println("\n工厂会自动根据项目类型选择合适的解析器！");
    }

    /**
     * 创建测试项目
     */
    private static Project createProject(String projectName, ProjectType type,
                                        String filename, String code) throws IOException {
        // 创建临时目录
        Path tempDir = Files.createTempDirectory(projectName);
        Path file = tempDir.resolve(filename);
        Files.writeString(file, code);

        // 创建项目
        Project.ProjectBuilder builder = Project.builder()
            .name(projectName)
            .rootPath(tempDir)
            .type(type);

        Project project = builder.build();

        // 添加源文件
        SourceFile sourceFile = SourceFile.builder()
            .path(file)
            .relativePath(filename)
            .fileName(filename)
            .extension(filename.substring(filename.lastIndexOf('.') + 1))
            .content(code)
            .lineCount((int) code.lines().count())
            .build();

        project.addSourceFile(sourceFile);

        return project;
    }

    /**
     * 打印分析结果
     */
    private static void printAnalysisResults(String language, CodeInsight insight) {
        System.out.println("\n项目名称: " + insight.getProjectName());
        System.out.println("语言: " + language);
        System.out.println("类数量: " + insight.getClasses().size());

        if (insight.getStatistics() != null) {
            System.out.println("\n统计信息:");
            System.out.println("  总方法数: " + insight.getStatistics().getTotalMethods());
            System.out.println("  总代码行: " + insight.getStatistics().getTotalLines());
        }

        if (insight.getComplexityMetrics() != null) {
            System.out.println("\n复杂度指标:");
            System.out.printf("  平均圈复杂度: %.2f\n",
                insight.getComplexityMetrics().getAvgCyclomaticComplexity());
            System.out.println("  最高圈复杂度: " +
                insight.getComplexityMetrics().getMaxCyclomaticComplexity());
            System.out.println("  高复杂度方法数: " +
                insight.getComplexityMetrics().getHighComplexityMethodCount());
        }

        if (!insight.getClasses().isEmpty()) {
            System.out.println("\n类详情:");
            insight.getClasses().forEach(cls -> {
                System.out.println("  类名: " + cls.getClassName());
                System.out.println("    方法数: " + cls.getMethodCount());
                System.out.println("    代码行数: " + cls.getLinesOfCode());

                if (!cls.getMethods().isEmpty()) {
                    System.out.println("    方法列表:");
                    cls.getMethods().forEach(method -> {
                        System.out.printf("      - %s (复杂度: %d, 行数: %d)\n",
                            method.getMethodName(),
                            method.getCyclomaticComplexity(),
                            method.getLinesOfCode());
                    });
                }
            });
        }

        if (!insight.getCodeSmells().isEmpty()) {
            System.out.println("\n代码坏味道:");
            insight.getCodeSmells().stream()
                .limit(3)
                .forEach(smell -> {
                    System.out.println("  ⚠️ " + smell.toString());
                });
        }
    }
}

