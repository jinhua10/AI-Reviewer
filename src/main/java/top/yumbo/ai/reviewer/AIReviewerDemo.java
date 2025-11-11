package top.yumbo.ai.reviewer;

import top.yumbo.ai.reviewer.config.Config;
import top.yumbo.ai.reviewer.entity.AnalysisResult;
import java.util.Arrays;

/**
 * AI Reviewer 示例代码
 * 
 * 展示如何使用AI Reviewer进行代码分析
 */
public class AIReviewerDemo {

    public static void main(String[] args) {
        // 示例1: 最简单的使用方式
        simpleUsageExample();

        // 示例2: 自定义配置（流式API）
        streamApiExample();

        // 示例3: 完整配置（Builder模式）
        builderPatternExample();

        // 示例4: 异步分析
        asyncAnalysisExample();
    }

    /**
     * 示例1: 最简单的使用方式
     */
    private static void simpleUsageExample() {
        System.out.println("=== 示例1: 最简单的使用方式 ===");

        try (AIReviewer reviewer = AIReviewer.create(".")) {
            AnalysisResult result = reviewer.analyze();
            System.out.println(result.getSummary());
        } catch (Exception e) {
            System.err.println("分析失败: " + e.getMessage());
        }

        System.out.println();
    }

    /**
     * 示例2: 自定义配置（流式API）
     */
    private static void streamApiExample() {
        System.out.println("=== 示例2: 自定义配置（流式API） ===");

        try (AIReviewer reviewer = AIReviewer.create(".")) {
            AnalysisResult result = reviewer
                .configure(config -> {
                    config.setAiPlatform("deepseek");
                    config.setModel("deepseek-chat");
                    config.setConcurrency(5);
                    config.setChunkSize(8000);
                    config.setReportFormats(Arrays.asList("markdown", "json"));
                })
                .analyze();

            System.out.println(result.getSummary());
        } catch (Exception e) {
            System.err.println("分析失败: " + e.getMessage());
        }

        System.out.println();
    }

    /**
     * 示例3: 完整配置（Builder模式）
     */
    private static void builderPatternExample() {
        System.out.println("=== 示例3: 完整配置（Builder模式） ===");

        Config config = Config.builder()
            .projectPath(".")
            .outputDir("ai_reviewer_output")
            .aiPlatform("deepseek")
            .model("deepseek-chat")
            .maxTokens(4096)
            .concurrency(3)
            .retryCount(3)
            .chunkSize(8000)
            .includePatterns(Arrays.asList("*.java", "*.py"))
            .excludePatterns(Arrays.asList("test", "build"))
            .enableCache(true)
            .reportFormats(Arrays.asList("markdown", "json"))
            .build();

        try (AIReviewer reviewer = AIReviewer.create(config)) {
            AnalysisResult result = reviewer.analyze();
            System.out.println(result.getSummary());
        } catch (Exception e) {
            System.err.println("分析失败: " + e.getMessage());
        }

        System.out.println();
    }

    /**
     * 示例4: 异步分析
     */
    private static void asyncAnalysisExample() {
        System.out.println("=== 示例4: 异步分析 ===");

        try (AIReviewer reviewer = AIReviewer.create(".")) {
            reviewer.analyzeAsync()
                .thenAccept(result -> {
                    System.out.println("异步分析完成:");
                    System.out.println(result.getSummary());
                })
                .exceptionally(e -> {
                    System.err.println("异步分析失败: " + e.getMessage());
                    return null;
                })
                .join(); // 等待完成，实际应用中可能不需要
        } catch (Exception e) {
            System.err.println("分析失败: " + e.getMessage());
        }

        System.out.println();
    }
}
