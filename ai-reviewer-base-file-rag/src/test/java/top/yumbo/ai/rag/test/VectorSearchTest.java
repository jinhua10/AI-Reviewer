package top.yumbo.ai.rag.test;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.example.knowledgeExample.OptimizedExcelKnowledgeBuilder;

/**
 * 向量检索功能测试
 * 验证模型加载是否正常
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
public class VectorSearchTest {

    public static void main(String[] args) {
        log.info("=".repeat(80));
        log.info("🧪 向量检索功能测试");
        log.info("=".repeat(80));

        String storagePath = "./data/test-knowledge-base";
        String excelFolder = "./data/excel-files";

        log.info("📍 知识库路径: {}", storagePath);
        log.info("📍 Excel文件夹: {}", excelFolder);
        log.info("");

        try {
            log.info("🚀 创建 OptimizedExcelKnowledgeBuilder（启用向量检索）...");
            OptimizedExcelKnowledgeBuilder builder = new OptimizedExcelKnowledgeBuilder(
                storagePath,
                excelFolder,
                false, // 自动分块
                true   // 启用向量检索
            );

            log.info("");
            log.info("✅ 测试成功！向量检索引擎初始化正常");
            log.info("=".repeat(80));

            // 清理资源
            builder.close();

        } catch (Exception e) {
            log.error("❌ 测试失败", e);
            log.error("");
            log.error("💡 可能的原因：");
            log.error("   1. 模型文件不存在");
            log.error("   2. 模型文件路径不正确");
            log.error("   3. ONNX Runtime 依赖问题");
            log.error("");
            log.error("🔧 解决方法：");
            log.error("   1. 将模型文件放到 src/main/resources/models/text2vec-base-chinese/model.onnx");
            log.error("   2. 或放到 ./models/text2vec-base-chinese/model.onnx");
            log.error("   3. 检查日志中的详细错误信息");
            log.error("=".repeat(80));
            System.exit(1);
        }
    }
}

