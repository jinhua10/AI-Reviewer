package top.yumbo.ai.reviewer;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.reviewer.cache.AnalysisCache;
import top.yumbo.ai.reviewer.cache.FileBasedAnalysisCache;
import top.yumbo.ai.reviewer.config.Config;
import top.yumbo.ai.reviewer.entity.AnalysisResult;
import top.yumbo.ai.reviewer.exception.AnalysisException;
import top.yumbo.ai.reviewer.report.ReportBuilder;
import top.yumbo.ai.reviewer.report.template.ReportTemplate;
import top.yumbo.ai.reviewer.report.template.TemplateEngine;
import top.yumbo.ai.reviewer.scoring.ScoringEngine;
import top.yumbo.ai.reviewer.scoring.ScoringRule;
import top.yumbo.ai.reviewer.service.AsyncAIService;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * AI Reviewer 演示类 - 展示重构后的新功能
 */
@Slf4j
public class AIReviewerDemo {

    public static void main(String[] args) {
        log.info("=== AI Reviewer v2.0 演示 - 重构后版本 ===");

        try {
            // 演示1: 分析当前项目（展示完整功能）
            analyzeCurrentProject();

            // 演示2: 异步AI调用演示
            demonstrateAsyncAI();

            // 演示3: 缓存系统演示
            demonstrateCacheSystem();

            // 演示4: 评分引擎演示
            demonstrateScoringEngine();

            // 演示5: 模板引擎演示
            demonstrateTemplateEngine();

            // 演示6: 配置验证
            demonstrateConfigValidation();

        } catch (Exception e) {
            log.error("演示执行失败", e);
            System.err.println("错误: " + e.getMessage());
        }
    }

    /**
     * 分析当前项目 - 展示完整重构功能
     */
    private static void analyzeCurrentProject() throws IOException, AnalysisException {
        log.info("🎯 演示1: 分析当前AI Reviewer项目（展示重构后完整功能）");

        // 加载配置
        Config config = Config.loadDefault();
        log.info("✅ 配置加载成功");

        // 创建AI评审器
        AIReviewer reviewer = AIReviewer.builder()
                .withConfig(config)
                .build();

        // 分析项目
        String projectPath = System.getProperty("user.dir");
        log.info("📁 项目路径: {}", projectPath);

        long startTime = System.currentTimeMillis();
        AnalysisResult result = reviewer.analyzeProject(projectPath);
        long endTime = System.currentTimeMillis();

        // 输出结果 - 包含所有6个维度的评分
        log.info("✅ 分析完成! 耗时: {}ms", (endTime - startTime));
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🎯 项目分析结果 (重构后版本)");
        System.out.println("=".repeat(60));
        System.out.println("📁 项目: " + result.getProjectName());
        System.out.println("📊 总体评分: " + result.getOverallScore() + "/100");
        System.out.println();
        System.out.println("📈 各维度评分详情:");
        System.out.println("  🏗️  架构设计: " + result.getArchitectureScore() + "/100 (权重: 20%)");
        System.out.println("  💻 代码质量: " + result.getCodeQualityScore() + "/100 (权重: 20%)");
        System.out.println("  ⚠️  技术债务: " + result.getTechnicalDebtScore() + "/100 (权重: 15%)");
        System.out.println("  ⚙️  功能完整性: " + result.getFunctionalityScore() + "/100 (权重: 20%)");
        System.out.println("  💰 商业价值: " + result.getBusinessValueScore() + "/100 (权重: 15%)");
        System.out.println("  🧪 测试覆盖率: " + result.getTestCoverageScore() + "/100 (权重: 10%)");
        System.out.println("=".repeat(60));

        // 生成报告 - 使用新的模板引擎
        ReportBuilder reportBuilder = new ReportBuilder();
        String markdownReport = reportBuilder.generateMarkdownReport(result);
        String htmlReport = reportBuilder.generateHtmlReport(result);

        // 保存报告
        reportBuilder.saveReport(result, "ai-reviewer-analysis-report.md", "markdown");
        reportBuilder.saveReport(result, "ai-reviewer-analysis-report.html", "html");

        log.info("📄 报告已生成: ai-reviewer-analysis-report.md 和 ai-reviewer-analysis-report.html");

        // 打印摘要
        if (result.getSummaryReport() != null) {
            System.out.println("\n📋 分析摘要:");
            System.out.println("-".repeat(40));
            System.out.println(result.getSummaryReport().getContent());
        }
    }

    /**
     * 异步AI调用演示
     */
    private static void demonstrateAsyncAI() {
        log.info("🚀 演示2: 异步AI调用功能");

        try {
            Config config = Config.loadDefault();
            AIReviewer reviewer = AIReviewer.builder().withConfig(config).build();

            // 获取异步AI服务实例
            AsyncAIService asyncService = (AsyncAIService) reviewer.getAiService();

            log.info("⚡ 并发限制: {} 个请求", asyncService.getMaxConcurrency());

            // 演示异步调用
            String[] prompts = {
                "请分析Java项目的架构设计原则",
                "请评估代码质量的标准",
                "请解释技术债务的概念"
            };

            log.info("📤 发送 {} 个异步AI请求...", prompts.length);
            long startTime = System.currentTimeMillis();

            CompletableFuture<String[]> future = asyncService.analyzeBatchAsync(prompts);

            // 等待结果
            String[] results = future.get();
            long endTime = System.currentTimeMillis();

            log.info("✅ 异步批处理完成! 耗时: {}ms", (endTime - startTime));
            log.info("📊 活跃请求数: {}", asyncService.getMaxConcurrency() - getAvailablePermits(asyncService));

            for (int i = 0; i < results.length; i++) {
                log.info("📝 结果 {}: {}...", i + 1, results[i].substring(0, Math.min(50, results[i].length())));
            }

        } catch (Exception e) {
            log.error("异步AI演示失败", e);
        }
    }

    /**
     * 缓存系统演示
     */
    private static void demonstrateCacheSystem() {
        log.info("💾 演示3: 缓存系统功能");

        try {
            // 创建缓存实例
            AnalysisCache cache = new FileBasedAnalysisCache();

            // 演示缓存操作
            String key = "demo-cache-key";
            String value = "这是缓存的分析结果数据";

            log.info("📥 存储缓存: key={}, value长度={}", key, value.length());
            cache.put(key, value, 3600); // 1小时过期

            log.info("📤 读取缓存: key={}", key);
            String cachedValue = cache.get(key).orElse("未找到缓存");

            log.info("✅ 缓存命中: {}", cachedValue.equals(value));

            // 显示缓存统计
            AnalysisCache.CacheStats stats = cache.getStats();
            log.info("📊 缓存统计: 命中={}, 未命中={}, 总条目={}, 命中率={:.2f}%",
                    stats.getHits(), stats.getMisses(), stats.getEntries(),
                    stats.getHitRate() * 100);

            // 清理缓存
            cache.clear();
            log.info("🧹 缓存已清理");

        } catch (Exception e) {
            log.error("缓存系统演示失败", e);
        }
    }

    /**
     * 评分引擎演示
     */
    private static void demonstrateScoringEngine() {
        log.info("⚙️  演示4: 评分引擎功能");

        try {
            ScoringEngine scoringEngine = new ScoringEngine();

            // 显示已注册的评分规则
            log.info("📋 已注册的评分规则:");
            scoringEngine.getAllRules().forEach((name, rule) -> {
                log.info("  🔧 {}: {} (权重: {})", name, rule.getDescription(), rule.getWeight());
            });

            // 演示评分计算
            String sampleAnalysis = "这个架构采用了分层设计，低耦合高内聚，代码质量良好，但存在一些技术债务。";
            ScoringRule.ScoringContext context = new ScoringRule.ScoringContext(
                "architecture", "java", 50, 5000, "java"
            );

            int score = scoringEngine.calculateDimensionScore("architecture", sampleAnalysis, context);
            log.info("🎯 架构评分计算: 分析文本 -> 分数 {}/100", score);

            // 显示评分统计
            ScoringEngine.ScoringStats stats = scoringEngine.getStats();
            log.info("📊 评分引擎统计: 总规则数={}, 各类型分布={}",
                    stats.getTotalRules(), stats.getRulesByType());

        } catch (Exception e) {
            log.error("评分引擎演示失败", e);
        }
    }

    /**
     * 模板引擎演示
     */
    private static void demonstrateTemplateEngine() {
        log.info("📋 演示5: 模板引擎功能");

        try {
            TemplateEngine templateEngine = new TemplateEngine();

            // 显示可用模板
            log.info("📄 可用模板:");
            templateEngine.getAllTemplates().forEach((name, template) -> {
                log.info("  📝 {}: {} ({})", name, template.getDescription(), template.getType());
            });

            // 创建一个简单的测试结果
            AnalysisResult mockResult = AnalysisResult.builder()
                    .projectName("DemoProject")
                    .overallScore(85)
                    .architectureScore(88)
                    .codeQualityScore(82)
                    .technicalDebtScore(75)
                    .functionalityScore(90)
                    .businessValueScore(87)
                    .testCoverageScore(78)
                    .analysisTimestamp(System.currentTimeMillis())
                    .build();

            // 渲染模板
            String rendered = templateEngine.renderDefault(mockResult, ReportTemplate.TemplateType.MARKDOWN);
            log.info("🎨 模板渲染成功，输出长度: {} 字符", rendered.length());

            // 显示模板变量
            ReportTemplate defaultTemplate = templateEngine.getTemplate("default-markdown");
            if (defaultTemplate != null) {
                log.info("🔧 模板变量: {}", String.join(", ", defaultTemplate.getVariables()));
            }

        } catch (Exception e) {
            log.error("模板引擎演示失败", e);
        }
    }

    /**
     * 配置验证演示
     */
    private static void demonstrateConfigValidation() {
        log.info("⚙️  演示6: 配置验证功能");

        try {
            Config config = Config.loadDefault();

            System.out.println("\n" + "=".repeat(50));
            System.out.println("🔍 配置验证结果");
            System.out.println("=".repeat(50));

            // AI服务配置验证
            System.out.println("🤖 AI服务配置:");
            if (config.getAiService().getApiKey() == null ||
                config.getAiService().getApiKey().startsWith("$")) {
                System.out.println("  ⚠️  API密钥未配置，请设置环境变量 DEEPSEEK_API_KEY");
            } else {
                System.out.println("  ✅ API密钥已配置");
            }
            System.out.println("  📍 服务地址: " + config.getAiService().getBaseUrl());
            System.out.println("  🧠 模型: " + config.getAiService().getModel());

            // 文件扫描配置验证
            System.out.println("\n📁 文件扫描配置:");
            System.out.println("  ✅ 包含模式: " + config.getFileScan().getIncludePatterns().size() + " 个");
            System.out.println("  ❌ 排除模式: " + config.getFileScan().getExcludePatterns().size() + " 个");
            System.out.println("  🎯 核心文件模式: " + config.getFileScan().getCoreFilePatterns().size() + " 个");

            // 分析配置验证
            System.out.println("\n📊 分析配置:");
            System.out.println("  📏 分析维度: " + config.getAnalysis().getAnalysisDimensions().size() + " 个");
            System.out.println("  ⚖️  权重配置: " + (config.getAnalysis().getDimensionWeights() != null ?
                config.getAnalysis().getDimensionWeights().size() + " 个" : "未配置"));

            // 验证权重总和
            if (config.getAnalysis().getDimensionWeights() != null) {
                double totalWeight = config.getAnalysis().getDimensionWeights().values().stream()
                        .mapToDouble(Double::doubleValue).sum();
                System.out.println("  📈 权重总和: " + String.format("%.2f", totalWeight) +
                    (Math.abs(totalWeight - 1.0) < 0.01 ? " ✅" : " ⚠️  (应为1.0)"));
            }

            System.out.println("=".repeat(50));
            log.info("✅ 配置验证完成");

        } catch (Exception e) {
            log.error("配置验证失败", e);
        }
    }

    /**
     * 分析外部项目
     */
    private static void analyzeExternalProject(String projectPath) throws IOException, AnalysisException {
        log.info("🌐 演示: 分析外部项目 - {}", projectPath);

        // 加载配置
        Config config = Config.loadDefault();

        // 创建AI评审器
        AIReviewer reviewer = AIReviewer.builder()
                .withConfig(config)
                .build();

        // 执行分析
        AnalysisResult result = reviewer.analyzeProject(projectPath);

        // 输出结果
        System.out.println("=== 外部项目分析结果 ===");
        System.out.println("项目: " + result.getProjectName());
        System.out.println("总体评分: " + result.getOverallScore() + "/100");
        System.out.println("架构评分: " + result.getArchitectureScore() + "/100");
        System.out.println("代码质量评分: " + result.getCodeQualityScore() + "/100");
        System.out.println("技术债务评分: " + result.getTechnicalDebtScore() + "/100");
        System.out.println("功能评分: " + result.getFunctionalityScore() + "/100");
        System.out.println("商业价值评分: " + result.getBusinessValueScore() + "/100");
        System.out.println("测试覆盖率评分: " + result.getTestCoverageScore() + "/100");

        // 生成报告
        ReportBuilder reportBuilder = new ReportBuilder();
        reportBuilder.saveReport(result, projectPath + "/analysis-report.md", "markdown");
        reportBuilder.saveReport(result, projectPath + "/analysis-report.html", "html");

        log.info("外部项目分析完成，报告已保存到项目目录");
    }

    /**
     * 获取信号量的可用许可数（用于演示）
     */
    private static int getAvailablePermits(AsyncAIService asyncService) {
        try {
            // 通过反射获取信号量的可用许可数
            java.lang.reflect.Field field = asyncService.getClass().getDeclaredField("concurrencyLimiter");
            field.setAccessible(true);
            java.util.concurrent.Semaphore semaphore = (java.util.concurrent.Semaphore) field.get(asyncService);
            return semaphore.availablePermits();
        } catch (Exception e) {
            log.warn("无法获取信号量状态: {}", e.getMessage());
            return 0;
        }
    }
}
