package top.yumbo.ai.rag.example;

import ai.onnxruntime.OrtException;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.LocalFileRAG;
import top.yumbo.ai.rag.example.knowledgeExample.OptimizedExcelKnowledgeBuilder;
import top.yumbo.ai.rag.example.llm.LLMClient;
import top.yumbo.ai.rag.example.llm.MockLLMClient;
import top.yumbo.ai.rag.impl.embedding.LocalEmbeddingEngine;
import top.yumbo.ai.rag.impl.index.SimpleVectorIndexEngine;

import java.io.IOException;

/**
 * Excel知识库智能问答系统（向量检索增强版）
 * 结合OptimizedExcelKnowledgeBuilder和AIQASystemExample
 *
 * 完整流程：
 * 1. 使用OptimizedExcelKnowledgeBuilder构建Excel知识库（自动生成向量索引）
 * 2. 使用AIQASystemExample进行智能问答（支持向量语义检索）
 * 3. 支持自动分块、智能上下文构建、DeepSeek LLM
 *
 * 🆕 P0修复：集成向量检索功能
 * - 语义理解：支持同义词、近义词检索
 * - 向量索引：使用SimpleVectorIndexEngine（适合<10万文档）
 * - 本地存储：完全本地化，无需外部服务
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
public class ExcelKnowledgeQASystem {

    private final String knowledgeBasePath;
    private final String excelFolderPath;
    private final boolean enableVectorSearch;  // 🆕 是否启用向量检索

    private OptimizedExcelKnowledgeBuilder builder;
    private AIQASystemExample qaSystem;
    private LocalFileRAG rag;

    // 🆕 向量检索组件
    private LocalEmbeddingEngine embeddingEngine;
    private SimpleVectorIndexEngine vectorIndexEngine;

    /**
     * 构造函数（默认启用向量检索）
     *
     * @param knowledgeBasePath 知识库存储路径
     * @param excelFolderPath Excel文件夹路径
     */
    public ExcelKnowledgeQASystem(String knowledgeBasePath, String excelFolderPath) {
        this(knowledgeBasePath, excelFolderPath, true);  // 默认启用向量检索
    }

    /**
     * 构造函数（完整版）
     *
     * @param knowledgeBasePath 知识库存储路径
     * @param excelFolderPath Excel文件夹路径
     * @param enableVectorSearch 是否启用向量检索
     */
    public ExcelKnowledgeQASystem(String knowledgeBasePath, String excelFolderPath,
                                   boolean enableVectorSearch) {
        this.knowledgeBasePath = knowledgeBasePath;
        this.excelFolderPath = excelFolderPath;
        this.enableVectorSearch = enableVectorSearch;

        log.info("=".repeat(80));
        log.info("Excel知识库智能问答系统 {}", enableVectorSearch ? "（向量检索增强版）" : "");
        log.info("=".repeat(80));
        log.info("知识库路径: {}", knowledgeBasePath);
        log.info("Excel文件夹: {}", excelFolderPath);
        log.info("向量检索: {}", enableVectorSearch ? "✅ 启用" : "❌ 禁用");
        log.info("=".repeat(80));
    }

    /**
     * 初始化系统（构建知识库）
     *
     * @param rebuildIfExists true=强制重建知识库，false=加载已有知识库（如果不存在则构建）
     * @return 构建结果
     */
    public BuildResult initialize(boolean rebuildIfExists) {
        log.info("\n🔨 步骤1: 初始化知识库\n");

        // 创建构建器（自动分块模式）
        builder = OptimizedExcelKnowledgeBuilder.createWithAutoChunking(
            knowledgeBasePath,
            excelFolderPath
        );

        // 检查已有知识库
        var stats = builder.getStatistics();
        boolean knowledgeBaseExists = stats.getDocumentCount() > 0;

        if (knowledgeBaseExists) {
            if (rebuildIfExists) {
                // true: 强制重建
                log.info("📚 检测到已有知识库 ({} 个文档)", stats.getDocumentCount());
                log.info("🔄 rebuildIfExists=true，准备重建知识库...");
                builder.clearKnowledgeBase();
                log.info("✓ 已清空旧知识库");

                // 重新构建
                OptimizedExcelKnowledgeBuilder.BuildResult buildResult = builder.buildKnowledgeBase();
                builder.close();

                if (buildResult.error != null) {
                    log.error("❌ 知识库重建失败: {}", buildResult.error);
                    return new BuildResult(false, buildResult.error, buildResult);
                }

                log.info("✅ 知识库重建成功！");
                log.info("   - 处理文件: {} 个", buildResult.successCount);
                log.info("   - 生成文档: {} 个", buildResult.totalDocuments);
                log.info("   - 耗时: {}秒", String.format("%.2f", buildResult.buildTimeMs / 1000.0));

                return new BuildResult(true, null, buildResult);
            } else {
                // false: 加载已有知识库
                log.info("📚 检测到已有知识库 ({} 个文档)", stats.getDocumentCount());
                log.info("✅ rebuildIfExists=false，直接加载已有知识库");

                // 🔧 修复：关闭构建器，释放 Lucene 索引锁
                OptimizedExcelKnowledgeBuilder.BuildResult existingResult =
                    new OptimizedExcelKnowledgeBuilder.BuildResult();
                existingResult.totalDocuments = (int) stats.getDocumentCount();
                existingResult.successCount = 0; // 没有新构建的文件
                existingResult.buildTimeMs = 0;

                builder.close();  // 🔧 关键修复：释放索引锁
                builder = null;

                return new BuildResult(true, null, existingResult);
            }
        } else {
            // 知识库不存在，需要首次构建
            log.info("📝 未检测到已有知识库，开始首次构建...");

            OptimizedExcelKnowledgeBuilder.BuildResult buildResult = builder.buildKnowledgeBase();
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

        // 🆕 初始化向量检索组件（如果启用）
        if (enableVectorSearch) {
            try {
                log.info("🚀 初始化向量检索引擎...");

                // 初始化嵌入引擎
                embeddingEngine = new LocalEmbeddingEngine();

                // 加载向量索引
                vectorIndexEngine = new SimpleVectorIndexEngine(
                    knowledgeBasePath,
                    embeddingEngine.getEmbeddingDim()
                );

                log.info("✅ 向量检索引擎已就绪");
                log.info("   - 模型: {}", embeddingEngine.getModelName());
                log.info("   - 向量维度: {}", embeddingEngine.getEmbeddingDim());
                log.info("   - 索引向量数: {}", vectorIndexEngine.size());

            } catch (OrtException | IOException e) {
                log.warn("⚠️  向量检索引擎初始化失败，将使用纯关键词检索", e);
                log.warn("💡 提示：如需启用向量检索，请确保模型文件已下载到 ./models/paraphrase-multilingual/model.onnx");
                embeddingEngine = null;
                vectorIndexEngine = null;
            }
        }

        // 初始化LLM客户端（使用DeepSeek）
        LLMClient llmClient = new MockLLMClient();

        // 创建问答系统（支持向量检索）
        if (embeddingEngine != null && vectorIndexEngine != null) {
            qaSystem = new AIQASystemExample(rag, llmClient, embeddingEngine, vectorIndexEngine);
            log.info("✅ 使用向量检索增强模式");
        } else {
            qaSystem = new AIQASystemExample(rag, llmClient);
            log.info("✅ 使用关键词检索模式");
        }

        // 显示知识库统计
        var stats = rag.getStatistics();
        log.info("\n📚 知识库统计:");
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
        log.info("=".repeat(80));
        log.info("");

        AIAnswer answer = qaSystem.answer(question);

        log.info("");
        log.info("=".repeat(80));
        log.info("💡 回答:");
        log.info(answer.getAnswer());
        log.info("");
        log.info("📚 数据来源 (共{}个文档):", answer.getSources().size());
        answer.getSources().forEach(source -> log.info("   - {}", source));
        log.info("");
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
        // 关闭向量检索组件
        if (embeddingEngine != null) {
            embeddingEngine.close();
            log.info("✅ 向量嵌入引擎已关闭");
        }

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
        String excelFolderPath = "E:\\excel1";

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
//            "内蒙古 15岁以上婚配情况"
//            "北京市 人均住房建筑面积"
            "财务管理专业有那些学校以及分数线是多少"
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

