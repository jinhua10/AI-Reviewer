package top.yumbo.ai.rag.example;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.example.llm.MockLLMClient;

/**
 * DeepSeek LLM测试示例
 * 测试API连接和基本功能
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
public class DeepSeekLLMTest {

    public static void main(String[] args) {
        log.info("🚀 DeepSeek LLM 测试开始");
        log.info("=".repeat(80));

        // 1. 创建LLM客户端
        MockLLMClient llmClient = new MockLLMClient();

        // 2. 检查API状态
        if (llmClient.isApiAvailable()) {
            log.info("✅ DeepSeek API 已连接");
            log.info("📊 环境变量 AI_API_KEY 已正确配置");
        } else {
            log.warn("⚠️ 未设置 AI_API_KEY 环境变量");
            log.warn("📝 当前使用Mock模式（模拟回答）");
            log.info("");
            log.info("设置方法：");
            log.info("  Windows:   set AI_API_KEY=your-deepseek-api-key");
            log.info("  Linux/Mac: export AI_API_KEY=your-deepseek-api-key");
            log.info("");
        }

        log.info("=".repeat(80));

        // 3. 测试基本问答
        testBasicQA(llmClient);

        // 4. 测试Excel数据分析
        testExcelDataAnalysis(llmClient);

        // 5. 测试中文理解
        testChineseUnderstanding(llmClient);

        log.info("=".repeat(80));
        log.info("✅ 所有测试完成");
    }

    /**
     * 测试1：基本问答
     */
    private static void testBasicQA(MockLLMClient llmClient) {
        log.info("\n📝 测试1: 基本问答");
        log.info("-".repeat(80));

        String prompt = """
            请用一句话解释什么是RAG（Retrieval-Augmented Generation）。
            """;

        log.info("问题: {}", prompt.trim());
        log.info("回答: ");

        String response = llmClient.generate(prompt);
        System.out.println(response);

        log.info("-".repeat(80));
    }

    /**
     * 测试2：Excel数据分析
     */
    private static void testExcelDataAnalysis(MockLLMClient llmClient) {
        log.info("\n📊 测试2: Excel数据分析");
        log.info("-".repeat(80));

        String prompt = """
            请分析以下Excel数据并给出总结：
            
            【Excel文件: sales_2024Q1.xlsx】
            内容:
            月份  销售额(万元)  同比增长
            1月   150          +15%
            2月   180          +20%
            3月   170          +18%
            
            问题：请总结第一季度的销售情况和趋势。
            """;

        log.info("数据来源: sales_2024Q1.xlsx");
        log.info("分析结果: ");

        String response = llmClient.generate(prompt);
        System.out.println(response);

        log.info("-".repeat(80));
    }

    /**
     * 测试3：中文理解能力
     */
    private static void testChineseUnderstanding(MockLLMClient llmClient) {
        log.info("\n🇨🇳 测试3: 中文理解能力");
        log.info("-".repeat(80));

        String prompt = """
            基于以下文档回答问题：
            
            文档内容：
            公司2024年战略规划强调三个核心方向：
            1. 数字化转型：加速云计算和AI技术应用
            2. 市场拓展：重点布局东南亚和中东市场
            3. 人才培养：建立技术人才梯队
            
            问题：公司2024年的核心战略是什么？
            """;

        log.info("测试: 中文文档理解和总结");
        log.info("回答: ");

        String response = llmClient.generate(prompt);
        System.out.println(response);

        log.info("-".repeat(80));
    }
}

