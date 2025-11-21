package top.yumbo.ai.rag.example;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.LocalFileRAG;
import top.yumbo.ai.rag.example.llm.LLMClient;
import top.yumbo.ai.rag.example.llm.MockLLMClient;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.model.Query;
import top.yumbo.ai.rag.model.SearchResult;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Excel知识库查询示例
 * 展示如何使用已构建的知识库进行AI问答
 *
 * 重要：知识库是持久化的，重启后数据仍然存在！
 */
@Slf4j
public class ExcelKnowledgeQueryExample {

    private final LocalFileRAG rag;
    private final LLMClient llmClient;

    public ExcelKnowledgeQueryExample(String knowledgeBasePath, LLMClient llmClient) {
        // 连接到已有的知识库（重启后自动加载）
        this.rag = LocalFileRAG.builder()
            .storagePath(knowledgeBasePath)
            .enableCache(true)
            .build();

        this.llmClient = llmClient;

        // 显示知识库信息
        var stats = rag.getStatistics();
        log.info("已连接到知识库: {}", knowledgeBasePath);
        log.info("文档数: {}, 索引数: {}", stats.getDocumentCount(), stats.getIndexedDocumentCount());

        if (stats.getDocumentCount() == 0) {
            log.warn("知识库为空！请先使用ExcelKnowledgeBuilder构建知识库。");
        }
    }

    /**
     * 查询Excel知识库并生成AI答案
     */
    public String queryKnowledgeBase(String question) {
        log.info("用户问题: {}", question);
        long startTime = System.currentTimeMillis();

        try {
            // 1. 从知识库检索相关文档
            SearchResult searchResult = rag.search(Query.builder()
                .queryText(question)
                .limit(5)  // 获取Top-5最相关的文档
                .build());

            log.info("检索到 {} 个相关文档，耗时: {}ms",
                searchResult.getTotalHits(),
                searchResult.getQueryTimeMs());

            if (searchResult.getTotalHits() == 0) {
                return "抱歉，在知识库中没有找到相关信息。请确认：\n" +
                       "1. 知识库是否已构建\n" +
                       "2. 问题关键词是否准确";
            }

            // 2. 构建上下文（从Excel文档中提取的内容）
            String context = buildContext(searchResult.getDocuments());

            // 3. 构建Prompt
            String prompt = buildPrompt(question, context, searchResult.getDocuments());

            // 4. 调用LLM生成答案
            String answer = llmClient.generate(prompt);

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("总耗时: {}ms", totalTime);

            return answer;

        } catch (Exception e) {
            log.error("查询失败", e);
            return "查询过程中出现错误: " + e.getMessage();
        }
    }

    /**
     * 构建上下文（包含Excel文件信息）
     */
    private String buildContext(List<Document> documents) {
        return documents.stream()
            .map(doc -> {
                String fileName = doc.getMetadata().get("fileName").toString();
                String content = doc.getContent();

                // 限制每个文档的长度，避免超过LLM的token限制
                if (content.length() > 2000) {
                    content = content.substring(0, 2000) + "...";
                }

                return String.format("""
                    【Excel文件: %s】
                    内容:
                    %s
                    """, fileName, content);
            })
            .collect(Collectors.joining("\n\n" + "=".repeat(50) + "\n\n"));
    }

    /**
     * 构建LLM Prompt
     */
    private String buildPrompt(String question, String context, List<Document> docs) {
        // 获取文件来源列表
        String sources = docs.stream()
            .map(doc -> doc.getMetadata().get("fileName").toString())
            .distinct()
            .collect(Collectors.joining(", "));

        return String.format("""
            你是一个专业的数据分析助手。请基于以下Excel文档内容回答用户问题。
            
            # 数据来源
            以下内容来自Excel文件: %s
            
            # Excel文档内容
            %s
            
            # 用户问题
            %s
            
            # 回答要求
            1. **必须基于提供的Excel数据回答**，不要编造信息
            2. 如果Excel中没有相关数据，明确告知用户
            3. 回答要准确、清晰、有条理
            4. 如果涉及数据，尽可能提供具体数值
            5. 引用数据时注明来源文件
            6. 保持专业友好的语气
            
            # 你的回答：
            """, sources, context, question);
    }

    /**
     * 搜索Excel文档（不使用LLM）
     */
    public List<Document> searchDocuments(String keywords) {
        SearchResult result = rag.search(Query.builder()
            .queryText(keywords)
            .limit(10)
            .build());

        return result.getDocuments();
    }

    /**
     * 关闭连接
     */
    public void close() {
        rag.close();
    }

    /**
     * 主方法 - 演示使用
     */
    public static void main(String[] args) {
        String knowledgeBasePath = "./data/excel-knowledge-base";

        // 从命令行参数读取路径
        if (args.length >= 1) {
            knowledgeBasePath = args[0];
        }

        System.out.println("=".repeat(80));
        System.out.println("Excel知识库查询系统");
        System.out.println("=".repeat(80));
        System.out.println("知识库路径: " + knowledgeBasePath);
        System.out.println("=".repeat(80));

        // 创建查询客户端（使用模拟LLM）
        LLMClient llmClient = new MockLLMClient();
        ExcelKnowledgeQueryExample querySystem = new ExcelKnowledgeQueryExample(
            knowledgeBasePath,
            llmClient
        );

        try {
            // 测试问题
            String[] testQuestions = {
                "请总结一下Excel表格中的主要数据",
                "有哪些重要的统计信息？",
                "请列出关键指标"
            };

            for (String question : testQuestions) {
                System.out.println("\n问题: " + question);
                System.out.println("-".repeat(80));

                String answer = querySystem.queryKnowledgeBase(question);
                System.out.println("回答: " + answer);
                System.out.println("=".repeat(80));
            }

            // 演示直接搜索
            System.out.println("\n直接搜索演示:");
            System.out.println("-".repeat(80));
            List<Document> docs = querySystem.searchDocuments("数据");
            System.out.println("找到 " + docs.size() + " 个相关文档:");
            docs.forEach(doc -> {
                String fileName = doc.getMetadata().get("fileName").toString();
                System.out.println("  - " + fileName);
            });

        } finally {
            querySystem.close();
        }
    }
}


