package top.yumbo.ai.rag.example;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.LocalFileRAG;
import top.yumbo.ai.rag.example.llm.MockLLMClient;
import top.yumbo.ai.rag.model.Document;

import java.util.Map;

/**
 * 上下文优化效果演示
 * 对比原始方法和智能上下文构建的效果
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
public class ContextOptimizationDemo {

    public static void main(String[] args) {
        log.info("=".repeat(80));
        log.info("RAG上下文优化效果演示");
        log.info("=".repeat(80));

        // 1. 准备测试数据
        LocalFileRAG rag = setupTestData();

        // 2. 创建问答系统
        AIQASystemExample qaSystem = new AIQASystemExample(rag, new MockLLMClient());

        // 3. 测试问题
        String[] testQuestions = {
            "2024年3月的销售额是多少？",
            "LocalFileRAG的主要优势有哪些？",
            "如何使用Builder模式创建实例？"
        };

        log.info("\n开始问答测试...\n");

        for (int i = 0; i < testQuestions.length; i++) {
            String question = testQuestions[i];
            log.info("=".repeat(80));
            log.info("问题 {}: {}", i + 1, question);
            log.info("-".repeat(80));

            try {
                AIAnswer answer = qaSystem.answer(question);

                System.out.println("\n回答:");
                System.out.println(answer.getAnswer());
                System.out.println("\n数据来源: " + String.join(", ", answer.getSources()));
                System.out.println("响应时间: " + answer.getResponseTimeMs() + "ms");

            } catch (Exception e) {
                log.error("问答失败", e);
            }

            log.info("=".repeat(80) + "\n");
        }

        rag.close();
        log.info("✅ 演示完成");
    }

    /**
     * 准备测试数据 - 模拟大Excel文件分块后的情况
     */
    private static LocalFileRAG setupTestData() {
        LocalFileRAG rag = LocalFileRAG.builder()
            .storagePath("./data/demo-kb")
            .enableCache(true)
            .build();

        log.info("准备测试数据...");

        // 模拟Excel文件 - sales_2024.xlsx 的多个分块
        for (int month = 1; month <= 12; month++) {
            String content = generateSalesData(month);

            rag.index(Document.builder()
                .title(String.format("sales_2024.xlsx_chunk_%d", month))
                .content(content)
                .metadata(Map.of(
                    "fileName", "sales_2024.xlsx",
                    "chunkIndex", month - 1,
                    "month", month,
                    "isChunk", true
                ))
                .build());
        }

        // 添加一些介绍性文档
        rag.index(Document.builder()
            .title("LocalFileRAG简介")
            .content("""
                LocalFileRAG是一个本地文件存储的RAG框架。
                主要优势包括：
                1. 零外部依赖 - 完全本地化运行
                2. 高性能 - 使用BM25算法实现亚秒级检索
                3. 隐私保护 - 数据不离开本地环境
                4. 成本节约 - 无需支付API调用费用
                
                使用Builder模式创建实例：
                LocalFileRAG rag = LocalFileRAG.builder()
                    .storagePath("./data")
                    .enableCache(true)
                    .build();
                """)
            .metadata(Map.of("category", "介绍"))
            .build());

        rag.commit();
        log.info("✓ 测试数据准备完成");

        return rag;
    }

    /**
     * 生成模拟的月度销售数据
     */
    private static String generateSalesData(int month) {
        int baseAmount = 150 + (month - 1) * 10;
        int variance = (month % 2 == 0) ? 20 : -10;
        int salesAmount = baseAmount + variance;
        double growthRate = 15.0 + (month - 1) * 0.5;

        return String.format("""
            销售数据报告 - 2024年%d月
            
            月度总览：
            - 销售额：%d万元
            - 同比增长率：%.1f%%
            - 环比增长率：%.1f%%
            
            地区分布：
            - 华东地区：%.0f万元（占比45%%）
            - 华北地区：%.0f万元（占比30%%）
            - 华南地区：%.0f万元（占比25%%）
            
            产品类别：
            - A类产品：%.0f万元
            - B类产品：%.0f万元
            - C类产品：%.0f万元
            
            客户分析：
            - 新客户：%d家
            - 复购客户：%d家
            - 客户留存率：%.1f%%
            
            备注：
            本月销售情况%s，主要增长来自%s区域。
            下月计划重点推广%s类产品，预期实现%.1f%%的增长。
            """,
            month,
            salesAmount,
            growthRate,
            (month == 1) ? 0.0 : 2.5,
            salesAmount * 0.45,
            salesAmount * 0.30,
            salesAmount * 0.25,
            salesAmount * 0.40,
            salesAmount * 0.35,
            salesAmount * 0.25,
            15 + month * 2,
            40 + month * 3,
            85.0 + month * 0.5,
            (variance > 0) ? "表现良好" : "略有下滑",
            (month % 3 == 0) ? "华东" : (month % 3 == 1) ? "华北" : "华南",
            (char)('A' + (month % 3)),
            10.0 + month * 0.5
        );
    }
}

/**
 * 简单的AIAnswer类（如果不存在）
 */
class AIAnswer {
    private final String answer;
    private final java.util.List<String> sources;
    private final long responseTimeMs;

    public AIAnswer(String answer, java.util.List<String> sources, long responseTimeMs) {
        this.answer = answer;
        this.sources = sources;
        this.responseTimeMs = responseTimeMs;
    }

    public String getAnswer() { return answer; }
    public java.util.List<String> getSources() { return sources; }
    public long getResponseTimeMs() { return responseTimeMs; }
}

