package top.yumbo.ai.rag.example;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.LocalFileRAG;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.model.Query;
import top.yumbo.ai.rag.model.SearchResult;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI系统集成示例：智能问答系统
 * 展示如何使用LocalFileRAG替代传统RAG实现智能问答
 */
@Slf4j
public class AIQASystemExample {

    private final LocalFileRAG rag;
    private final LLMClient llmClient;

    public AIQASystemExample(LocalFileRAG rag, LLMClient llmClient) {
        this.rag = rag;
        this.llmClient = llmClient;
    }

    /**
     * 主要问答方法
     */
    public AIAnswer answer(String question) {
        long startTime = System.currentTimeMillis();

        try {
            // 步骤1: 提取关键词
            String keywords = extractKeywords(question);
            log.info("Extracted keywords: {}", keywords);

            // 步骤2: 检索相关文档
            SearchResult searchResult = rag.search(Query.builder()
                .queryText(keywords)
                .limit(5)  // Top-5最相关文档
                .build());

            log.info("Found {} relevant documents in {}ms",
                searchResult.getTotalHits(),
                searchResult.getQueryTimeMs());

            // 步骤3: 构建上下文
            String context = buildContext(searchResult.getDocuments());

            // 步骤4: 构建Prompt
            String prompt = buildPrompt(question, context);

            // 步骤5: 调用LLM生成答案
            String answer = llmClient.generate(prompt);

            // 步骤6: 提取文档来源
            List<String> sources = searchResult.getDocuments().stream()
                .map(Document::getTitle)
                .toList();

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("Total answer time: {}ms", totalTime);

            return new AIAnswer(answer, sources, totalTime);

        } catch (Exception e) {
            log.error("Error answering question", e);
            return new AIAnswer(
                "抱歉，处理您的问题时出现错误：" + e.getMessage(),
                List.of(),
                System.currentTimeMillis() - startTime
            );
        }
    }

    /**
     * 提取关键词（简单实现）
     */
    private String extractKeywords(String question) {
        // 简单的停用词列表
        List<String> stopWords = Arrays.asList(
            "的", "是", "在", "了", "和", "有", "我", "你", "他", "她",
            "什么", "怎么", "如何", "为什么", "吗", "呢", "啊"
        );

        return Arrays.stream(question.split("\\s+"))
            .filter(word -> !stopWords.contains(word) && word.length() > 1)
            .collect(Collectors.joining(" "));
    }

    /**
     * 构建文档上下文
     */
    private String buildContext(List<top.yumbo.ai.rag.model.Document> documents) {
        return documents.stream()
            .map(doc -> String.format(
                "【文档：%s】\n%s",
                doc.getTitle(),
                doc.getContent()
            ))
            .collect(Collectors.joining("\n\n---\n\n"));
    }

    /**
     * 构建LLM Prompt
     */
    private String buildPrompt(String question, String context) {
        return String.format("""
            你是一个专业的知识助手。请基于以下文档内容回答用户问题。
            
            # 相关文档
            %s
            
            # 用户问题
            %s
            
            # 回答要求
            1. 必须基于文档内容回答，不要编造信息
            2. 如果文档中没有相关信息，明确告知用户
            3. 回答要清晰、准确、有条理
            4. 可以引用文档名称作为信息来源
            5. 保持专业友好的语气
            
            # 请提供你的回答：
            """, context, question);
    }

    /**
     * 批量索引文档
     */
    public void indexDocuments(List<KnowledgeDoc> docs) {
        log.info("Indexing {} documents...", docs.size());

        for (KnowledgeDoc doc : docs) {
            try {
                rag.index(Document.builder()
                    .title(doc.getTitle())
                    .content(doc.getContent())
                    .metadata(doc.getMetadata())
                    .build());
            } catch (Exception e) {
                log.error("Failed to index document: " + doc.getTitle(), e);
            }
        }

        rag.commit();
        log.info("Indexing completed");
    }

    /**
     * 主方法 - 演示使用
     */
    public static void main(String[] args) {
        // 1. 初始化LocalFileRAG
        LocalFileRAG rag = LocalFileRAG.builder()
            .storagePath("./data/qa-system")
            .enableCache(true)
            .enableCompression(true)
            .build();

        // 2. 初始化LLM客户端（这里使用模拟实现）
        LLMClient llmClient = new MockLLMClient();

        // 3. 创建QA系统
        AIQASystemExample qaSystem = new AIQASystemExample(rag, llmClient);

        // 4. 索引示例文档
        List<KnowledgeDoc> sampleDocs = createSampleDocuments();
        qaSystem.indexDocuments(sampleDocs);

        // 5. 测试问答
        System.out.println("\n=== 智能问答系统演示 ===\n");

        String[] testQuestions = {
            "什么是LocalFileRAG框架？",
            "如何使用Builder模式创建LocalFileRAG实例？",
            "LocalFileRAG支持哪些文档格式？"
        };

        for (String question : testQuestions) {
            System.out.println("问题：" + question);
            AIAnswer answer = qaSystem.answer(question);
            System.out.println("回答：" + answer.getAnswer());
            System.out.println("来源：" + String.join(", ", answer.getSources()));
            System.out.println("耗时：" + answer.getResponseTimeMs() + "ms");
            System.out.println("\n" + "=".repeat(50) + "\n");
        }

        // 6. 关闭
        rag.close();
    }

    /**
     * 创建示例文档
     */
    private static List<KnowledgeDoc> createSampleDocuments() {
        return Arrays.asList(
            new KnowledgeDoc(
                "LocalFileRAG框架介绍",
                """
                LocalFileRAG是一个本地文件存储的RAG框架，它使用Apache Lucene进行全文检索，
                无需向量数据库和Embedding模型。框架的核心优势包括：
                1. 零外部依赖 - 完全本地化运行
                2. 高性能 - 使用BM25算法实现亚秒级检索
                3. 隐私保护 - 数据不离开本地环境
                4. 成本节约 - 无需支付API调用费用
                5. 易于集成 - 提供简洁的Java API
                """,
                Map.of("category", "介绍", "version", "1.0")
            ),

            new KnowledgeDoc(
                "LocalFileRAG使用指南",
                """
                使用LocalFileRAG非常简单，只需以下步骤：
                
                1. 创建实例（使用Builder模式）：
                LocalFileRAG rag = LocalFileRAG.builder()
                    .storagePath("./data")
                    .enableCache(true)
                    .build();
                
                2. 索引文档：
                rag.index(Document.builder()
                    .title("文档标题")
                    .content("文档内容")
                    .build());
                rag.commit();
                
                3. 搜索文档：
                SearchResult result = rag.search(
                    Query.builder().queryText("关键词").limit(10).build()
                );
                
                4. 关闭资源：
                rag.close();
                """,
                Map.of("category", "教程", "difficulty", "初级")
            ),

            new KnowledgeDoc(
                "支持的文档格式",
                """
                LocalFileRAG通过Apache Tika支持35+种文档格式：
                
                - 文本格式：txt, md, log, csv
                - 办公文档：pdf, doc, docx, xls, xlsx, ppt, pptx
                - 代码文件：java, py, js, ts, go, rs, c, cpp, h
                - 标记语言：html, xml, json, yaml, toml
                - 配置文件：properties, ini, conf
                - 其他格式：epub, mobi等
                
                所有格式都会被自动解析并提取文本内容进行索引。
                """,
                Map.of("category", "功能", "topic", "格式支持")
            ),

            new KnowledgeDoc(
                "性能优化建议",
                """
                为了获得最佳性能，建议：
                
                1. JVM优化：
                   - 使用G1 GC：-XX:+UseG1GC
                   - 设置合适的堆内存：-Xmx4g -Xms2g
                
                2. Lucene优化：
                   - 增加RAM缓冲区：ramBufferSizeMB设为512
                   - 批量索引：使用indexBatch方法
                
                3. 缓存优化：
                   - 启用文档缓存：enableCache(true)
                   - 设置合理的缓存大小
                
                4. 查询优化：
                   - 提取关键词，去除停用词
                   - 使用查询缓存避免重复检索
                """,
                Map.of("category", "优化", "priority", "高")
            ),

            new KnowledgeDoc(
                "与传统RAG的对比",
                """
                LocalFileRAG与传统RAG（向量数据库方案）的主要区别：
                
                传统RAG：
                - 使用Embedding将文本转换为向量
                - 依赖向量数据库（如Pinecone, Weaviate）
                - 需要调用OpenAI等API
                - 成本较高，有网络延迟
                
                LocalFileRAG：
                - 使用BM25关键词匹配算法
                - 基于本地Lucene索引
                - 无需外部API调用
                - 完全本地化，零成本
                
                适用场景：
                - 传统RAG：需要语义理解的场景
                - LocalFileRAG：企业内部、隐私敏感、成本敏感的场景
                """,
                Map.of("category", "对比", "topic", "RAG")
            )
        );
    }
}

/**
 * AI答案封装类
 */
class AIAnswer {
    private final String answer;
    private final List<String> sources;
    private final long responseTimeMs;

    public AIAnswer(String answer, List<String> sources, long responseTimeMs) {
        this.answer = answer;
        this.sources = sources;
        this.responseTimeMs = responseTimeMs;
    }

    public String getAnswer() { return answer; }
    public List<String> getSources() { return sources; }
    public long getResponseTimeMs() { return responseTimeMs; }
}

/**
 * 知识文档类
 */
class KnowledgeDoc {
    private final String title;
    private final String content;
    private final Map<String, Object> metadata;

    public KnowledgeDoc(String title, String content, Map<String, Object> metadata) {
        this.title = title;
        this.content = content;
        this.metadata = metadata;
    }

    public String getTitle() { return title; }
    public String getContent() { return content; }
    public Map<String, Object> getMetadata() { return metadata; }
}

/**
 * LLM客户端接口
 */
interface LLMClient {
    String generate(String prompt);
}

/**
 * 模拟LLM客户端（用于演示）
 * 实际使用时替换为真实的LLM客户端（OpenAI, 本地模型等）
 */
class MockLLMClient implements LLMClient {
    @Override
    public String generate(String prompt) {
        // 这里应该调用真实的LLM API
        // 例如：OpenAI GPT-4, Claude, 或本地Llama模型
        return "基于提供的文档内容，这是一个模拟的AI回答。在实际使用中，这里会调用真实的LLM生成详细、准确的答案。";
    }
}

