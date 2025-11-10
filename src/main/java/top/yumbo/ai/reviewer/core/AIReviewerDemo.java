package top.yumbo.ai.reviewer.core;

import top.yumbo.ai.reviewer.config.Config;
import top.yumbo.ai.reviewer.entity.AnalysisResult;
import top.yumbo.ai.reviewer.exception.AnalysisException;

import java.nio.file.Paths;

/**
 * AI Reviewer v2.0 演示类
 * 展示简化后的 API 使用方式
 *
 * v2.1 新特性：
 * - 自动加载 v1.0 配置文件（无需修改）
 * - 支持 classpath:/reviewer/*.yml 配置
 * - 支持项目根目录 reviewer.yml 覆盖
 */
public class AIReviewerDemo {

    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("  AI Reviewer v2.1 - 兼容 v1.0 配置文件");
        System.out.println("===========================================\n");

        System.out.println("📝 配置加载说明：");
        System.out.println("  1. 自动加载 classpath:/reviewer/reviewer.yml");
        System.out.println("  2. 自动加载 classpath:/reviewer/ai/{platform}.yml");
        System.out.println("  3. 自动加载 {projectPath}/reviewer.yml (可选覆盖)");
        System.out.println("  4. 环境变量 AI_API_KEY 或 DEEPSEEK_API_KEY");
        System.out.println();

        // 示例 1: 最简单的使用方式（自动加载配置）
        example1_SimpleUsage();

        // 示例 2: 自定义配置
        // example2_CustomConfig();

        // 示例 3: 完整配置
        // example3_FullConfig();
    }

    /**
     * 示例 1: 最简单的使用方式（自动加载 v1.0 配置文件）
     */
    private static void example1_SimpleUsage() {
        System.out.println("\n【示例 1】最简单的使用方式（自动加载配置文件）");
        System.out.println("----------------------------------------");

        String projectPath = System.getProperty("user.dir"); // 当前目录

        try (AIReviewer reviewer = AIReviewer.create(projectPath)) {
            AnalysisResult result = reviewer.analyze();
            System.out.println("\n✅ 分析成功!");
            System.out.println(result.getSummary());

        } catch (AnalysisException e) {
            System.err.println("\n❌ 分析失败: " + e.getMessage());
            System.err.println("错误类型: " + e.getErrorType());
        }
    }

    /**
     * 示例 2: 自定义配置（流式 API）
     */
    private static void example2_CustomConfig() {
        System.out.println("\n【示例 2】自定义配置");
        System.out.println("----------------------------------------");

        String projectPath = System.getProperty("user.dir");

        try (AIReviewer reviewer = AIReviewer.create(projectPath)) {
            AnalysisResult result = reviewer
                .configure(config -> config
                    .aiPlatform("deepseek")
                    .model("deepseek-chat")
                    .concurrency(5)
                    .chunkSize(8000)
                    .enableCache(true)
                    .reportFormats("markdown", "json")
                )
                .analyze();

            System.out.println("\n✅ 分析成功!");
            System.out.println(result.getSummary());

        } catch (AnalysisException e) {
            System.err.println("\n❌ 分析失败: " + e.getMessage());
        }
    }

    /**
     * 示例 3: 完整配置（Builder 模式）
     */
    private static void example3_FullConfig() {
        System.out.println("\n【示例 3】完整配置");
        System.out.println("----------------------------------------");

        Config config = Config.builder()
            .projectPath(System.getProperty("user.dir"))
            .outputDir(Paths.get(System.getProperty("user.dir"), ".ai-review"))
            .aiPlatform("deepseek")
            .apiKey(System.getenv("AI_API_KEY"))
            .apiUrl("https://api.deepseek.com/v1/chat/completions")
            .model("deepseek-chat")
            .maxTokens(4096)
            .concurrency(3)
            .retryCount(3)
            .chunkSize(8000)
            .includePatterns("*.java", "*.py", "*.js")
            .excludePatterns("test", "build", "target")
            .enableCache(true)
            .enableCheckpoint(true)
            .reportFormats("markdown", "json")
            .build();

        try (AIReviewer reviewer = AIReviewer.create(config)) {
            AnalysisResult result = reviewer.analyze();

            System.out.println("\n✅ 分析成功!");
            System.out.println(result.getSummary());
            System.out.println("\n详细报告:");
            System.out.println("- 总文件数: " + result.getTotalFiles());
            System.out.println("- 分析文件数: " + result.getAnalyzedFiles());
            System.out.println("- 成功块数: " + result.getSuccessfulChunks());
            System.out.println("- 失败块数: " + result.getFailedChunks());
            System.out.println("- 质量评分: " + result.getSummaryReport().getQualityScore());

        } catch (AnalysisException e) {
            System.err.println("\n❌ 分析失败: " + e.getMessage());
        }
    }

}
