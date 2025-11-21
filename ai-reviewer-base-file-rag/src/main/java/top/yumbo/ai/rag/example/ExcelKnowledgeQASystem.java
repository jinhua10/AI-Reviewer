package top.yumbo.ai.rag.example;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.LocalFileRAG;
import top.yumbo.ai.rag.example.knowledgeExample.OptimizedExcelKnowledgeBuilder;
import top.yumbo.ai.rag.example.llm.LLMClient;
import top.yumbo.ai.rag.example.llm.MockLLMClient;

/**
 * Excel知识库智能问答系统
 * 结合OptimizedExcelKnowledgeBuilder和AIQASystemExample
 *
 * 完整流程：
 * 1. 使用OptimizedExcelKnowledgeBuilder构建Excel知识库
 * 2. 使用AIQASystemExample进行智能问答
 * 3. 支持自动分块、智能上下文构建、DeepSeek LLM
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
public class ExcelKnowledgeQASystem {

    private final String knowledgeBasePath;
    private final String excelFolderPath;
    private OptimizedExcelKnowledgeBuilder builder;
    private AIQASystemExample qaSystem;
    private LocalFileRAG rag;

    /**
     * 构造函数
     *
     * @param knowledgeBasePath 知识库存储路径
     * @param excelFolderPath Excel文件夹路径
     */
    public ExcelKnowledgeQASystem(String knowledgeBasePath, String excelFolderPath) {
        this.knowledgeBasePath = knowledgeBasePath;
        this.excelFolderPath = excelFolderPath;

        log.info("=".repeat(80));
        log.info("Excel知识库智能问答系统");
        log.info("=".repeat(80));
        log.info("知识库路径: {}", knowledgeBasePath);
        log.info("Excel文件夹: {}", excelFolderPath);
        log.info("=".repeat(80));
    }

    /**
     * 初始化系统（构建知识库）
     *
     * @param rebuildIfExists 如果知识库已存在是否重建
     * @return 构建结果
     */
    public BuildResult initialize(boolean rebuildIfExists) {
        log.info("\n🔨 步骤1: 构建Excel知识库\n");

        // 创建构建器（自动分块模式）
        builder = OptimizedExcelKnowledgeBuilder.createWithAutoChunking(
            knowledgeBasePath,
            excelFolderPath
        );

        // 检查是否需要重建
        var stats = builder.getStatistics();
        if (stats.getDocumentCount() > 0) {
            if (rebuildIfExists) {
                log.info("📚 现有知识库已存在 ({} 个文档) - 准备重建", stats.getDocumentCount());
                builder.clearKnowledgeBase();
                log.info("✓ 知识库已清空");
            } else {
                log.info("📚 现有知识库已存在 ({} 个文档) - 跳过构建（增量更新模式）", stats.getDocumentCount());
                // 不关闭构建器，以便后续可以使用
                OptimizedExcelKnowledgeBuilder.BuildResult existingResult =
                    new OptimizedExcelKnowledgeBuilder.BuildResult();
                existingResult.totalDocuments = (int) stats.getDocumentCount();
                existingResult.successCount = 0; // 没有新处理的文件
                return new BuildResult(true, null, existingResult);
            }
        }

        // 构建知识库
        OptimizedExcelKnowledgeBuilder.BuildResult buildResult = builder.buildKnowledgeBase();

        // 关闭构建器
        builder.close();

        if (buildResult.error != null) {
            log.error("❌ 知识库构建失败: {}", buildResult.error);
            return new BuildResult(false, buildResult.error, buildResult);
        }

        log.info("✅ 知识库构建成功！");
        log.info("   - 处理文件: {} 个", buildResult.successCount);
        log.info("   - 生成文档: {} 个", buildResult.totalDocuments);
        log.info("   - 耗时: {}秒", String.format("%.2f", buildResult.buildTimeMs / 1000.0));

        return new BuildResult(true, null, buildResult);
    }

    /**
     * 启动问答系统
     */
    public void startQASystem() {
        log.info("\n🤖 步骤2: 启动智能问答系统\n");

        // 连接到已构建的知识库
        rag = LocalFileRAG.builder()
            .storagePath(knowledgeBasePath)
            .enableCache(true)
            .build();

        // 初始化LLM客户端（使用DeepSeek）
        LLMClient llmClient = new MockLLMClient();

        // 创建问答系统
        qaSystem = new AIQASystemExample(rag, llmClient);

        // 显示知识库统计
        var stats = rag.getStatistics();
        log.info("📚 知识库统计:");
        log.info("   - 文档数: {}", stats.getDocumentCount());
        log.info("   - 索引数: {}", stats.getIndexedDocumentCount());

        log.info("\n✅ 问答系统已就绪，可以开始提问！\n");
    }

    /**
     * 提问
     *
     * @param question 问题
     * @return AI回答
     */
    public AIAnswer ask(String question) {
        if (qaSystem == null) {
            throw new IllegalStateException("请先调用 startQASystem() 启动问答系统");
        }

        log.info("\n" + "=".repeat(80));
        log.info("❓ 问题: {}", question);
        log.info("-".repeat(80));

        AIAnswer answer = qaSystem.answer(question);

        log.info("\n💡 回答:");
        log.info(answer.getAnswer());
        log.info("\n📚 数据来源: {}", String.join(", ", answer.getSources()));
        log.info("⏱️  响应时间: {}ms", answer.getResponseTimeMs());
        log.info("=".repeat(80));

        return answer;
    }

    /**
     * 批量提问
     *
     * @param questions 问题列表
     */
    public void askBatch(String[] questions) {
        log.info("\n🔄 批量问答开始 (共{}个问题)\n", questions.length);

        for (int i = 0; i < questions.length; i++) {
            log.info("问题 {}/{}", i + 1, questions.length);
            ask(questions[i]);
            System.out.println(); // 空行分隔
        }

        log.info("✅ 批量问答完成");
    }

    /**
     * 关闭系统
     */
    public void close() {
        if (rag != null) {
            rag.close();
            log.info("✅ 问答系统已关闭");
        }
    }

    /**
     * 构建结果
     */
    public static class BuildResult {
        private final boolean success;
        private final String error;
        private final OptimizedExcelKnowledgeBuilder.BuildResult details;

        public BuildResult(boolean success, String error,
                          OptimizedExcelKnowledgeBuilder.BuildResult details) {
            this.success = success;
            this.error = error;
            this.details = details;
        }

        public boolean isSuccess() { return success; }
        public String getError() { return error; }
        public OptimizedExcelKnowledgeBuilder.BuildResult getDetails() { return details; }
    }

    /**
     * 主方法 - 完整演示
     */
    public static void main(String[] args) {
        // 配置路径
        String knowledgeBasePath = "./data/excel-qa-system";
        String excelFolderPath = "E:\\excel";

        // 💡 可以指定单个Excel文件（支持中文路径）
        // excelFolderPath = "E:\\月度数据.xls";

        // 💡 或者指定包含Excel文件的文件夹
        // excelFolderPath = "E:\\";
        // excelFolderPath = "./data/excel-files";

        // 从命令行参数读取（可选）
        if (args.length >= 1) {
            knowledgeBasePath = args[0];
        }
        if (args.length >= 2) {
            excelFolderPath = args[1];
        }

        log.info("\n🚀 Excel知识库智能问答系统启动\n");
        log.info("提示: 请确保环境变量 AI_API_KEY 已设置（用于DeepSeek LLM）");
        log.info("      如未设置，系统将使用Mock模式\n");

        // 创建系统
        ExcelKnowledgeQASystem system = new ExcelKnowledgeQASystem(
            knowledgeBasePath,
            excelFolderPath
        );

        try {
            // 步骤1: 构建知识库
            BuildResult buildResult = system.initialize(true);

            if (!buildResult.isSuccess()) {
                log.error("❌ 系统初始化失败: {}", buildResult.getError());
                System.exit(1);
            }

            // 步骤2: 启动问答系统
            system.startQASystem();

            // 步骤3: 测试问答
            runDemoQuestions(system);

            // 步骤4: 交互式问答（可选）
            // runInteractiveMode(system);

        } catch (Exception e) {
            log.error("❌ 系统运行错误", e);
        } finally {
            system.close();
        }

        log.info("\n✅ 系统已安全退出");
    }

    /**
     * 运行演示问题
     */
    private static void runDemoQuestions(ExcelKnowledgeQASystem system) {
        log.info("\n📝 运行演示问题...\n");

        String[] demoQuestions = {
            "进出口总值累计值"
        };

        system.askBatch(demoQuestions);
    }

    /**
     * 交互式问答模式
     */
    private static void runInteractiveMode(ExcelKnowledgeQASystem system) {
        log.info("\n💬 进入交互式问答模式 (输入 'exit' 退出)\n");

        try (java.util.Scanner scanner = new java.util.Scanner(System.in)) {
            while (true) {
                System.out.print("请输入问题: ");
                String question = scanner.nextLine().trim();

                if (question.equalsIgnoreCase("exit") || question.equalsIgnoreCase("quit")) {
                    log.info("👋 退出交互模式");
                    break;
                }

                if (question.isEmpty()) {
                    continue;
                }

                system.ask(question);
            }
        }
    }
}

