package top.yumbo.ai.rag.example;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.LocalFileRAG;
import top.yumbo.ai.rag.example.llm.LLMClient;
import top.yumbo.ai.rag.example.llm.MockLLMClient;
import top.yumbo.ai.rag.impl.embedding.LocalEmbeddingEngine;
import top.yumbo.ai.rag.impl.index.SimpleVectorIndexEngine;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.model.Query;
import top.yumbo.ai.rag.model.SearchResult;
import top.yumbo.ai.rag.optimization.SmartContextBuilder;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI系统集成示例：智能问答系统
 * 展示如何使用LocalFileRAG替代传统RAG实现智能问答
 *
 * 🆕 P0修复：支持向量检索增强
 */
@Slf4j
public class AIQASystemExample {

    private final LocalFileRAG rag;
    private final LLMClient llmClient;
    private final SmartContextBuilder contextBuilder;

    // 🆕 向量检索组件（可选）
    private final LocalEmbeddingEngine embeddingEngine;
    private final SimpleVectorIndexEngine vectorIndexEngine;

    /**
     * 构造函数（纯关键词检索模式）
     */
    public AIQASystemExample(LocalFileRAG rag, LLMClient llmClient) {
        this(rag, llmClient, null, null);
    }

    /**
     * 构造函数（向量检索增强模式）
     *
     * @param rag RAG实例
     * @param llmClient LLM客户端
     * @param embeddingEngine 嵌入引擎
     * @param vectorIndexEngine 向量索引引擎
     */
    public AIQASystemExample(LocalFileRAG rag, LLMClient llmClient,
                            LocalEmbeddingEngine embeddingEngine,
                            SimpleVectorIndexEngine vectorIndexEngine) {
        this.rag = rag;
        this.llmClient = llmClient;
        this.embeddingEngine = embeddingEngine;
        this.vectorIndexEngine = vectorIndexEngine;

        // 初始化智能上下文构建器
        this.contextBuilder = SmartContextBuilder.builder()
            .maxContextLength(8000)  // 8000字符总上下文
            .maxDocLength(2000)      // 单个文档最多2000字符
            .build();

        log.info("AIQASystem initialized with smart context builder");
        if (embeddingEngine != null && vectorIndexEngine != null) {
            log.info("✅ 向量检索增强已启用");
        }
    }

    /**
     * 主要问答方法（支持向量检索增强）
     */
    public AIAnswer answer(String question) {
        long startTime = System.currentTimeMillis();

        try {
            List<Document> documents;

            // 🆕 步骤2A: 向量检索（如果启用）
            if (embeddingEngine != null && vectorIndexEngine != null) {
                documents = hybridSearch(question);
                log.info("✅ 使用混合检索（Lucene + Vector）");
            } else {
                // 步骤1: 提取关键词
                String keywords = extractKeywords(question);
                log.info("Extracted keywords: {}", keywords);

                // 步骤2B: 纯关键词检索
                SearchResult searchResult = rag.search(Query.builder()
                    .queryText(keywords)
                    .limit(5)  // Top-5最相关文档
                    .build());

                log.info("Found {} relevant documents in {}ms",
                    searchResult.getTotalHits(),
                    searchResult.getQueryTimeMs());

                documents = searchResult.getDocuments();
            }

            // 步骤3: 构建智能上下文（优化：提取最相关片段）
            String context = contextBuilder.buildSmartContext(
                question,
                documents
            );

            log.info("Context stats: {}",
                contextBuilder.getContextStats(context));

            // 步骤4: 构建Prompt
            String prompt = buildPrompt(question, context);

            // 步骤5: 调用LLM生成答案
            String answer = llmClient.generate(prompt);

            // 步骤6: 提取文档来源
            List<String> sources = documents.stream()
                .map(Document::getTitle)
                .distinct()
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
     * 🆕 混合检索：结合Lucene关键词检索和向量语义检索
     */
    private List<Document> hybridSearch(String question) {
        try {
            long startTime = System.currentTimeMillis();

            // 1. Lucene关键词检索（快速粗筛 Top-20）
            String keywords = extractKeywords(question);
            SearchResult luceneResult = rag.search(Query.builder()
                .queryText(keywords)
                .limit(20)
                .build());

            log.debug("Lucene找到 {} 个文档", luceneResult.getDocuments().size());

            // 2. 向量检索（语义精排）
            float[] queryVector = embeddingEngine.embed(question);
            List<SimpleVectorIndexEngine.VectorSearchResult> vectorResults =
                vectorIndexEngine.search(queryVector, 20, 0.6f);  // 相似度 >= 0.6

            log.debug("向量检索找到 {} 个文档", vectorResults.size());

            // 3. 混合评分：融合两种检索结果
            Map<String, Double> hybridScores = new HashMap<>();

            // Lucene结果（权重 0.3）
            List<Document> luceneDocs = luceneResult.getDocuments();
            for (int i = 0; i < luceneDocs.size(); i++) {
                String docId = luceneDocs.get(i).getId();
                // 归一化排名分数（第1名=1.0，第20名=0.05）
                double normalizedScore = 1.0 - (i * 0.05);
                hybridScores.put(docId, 0.3 * normalizedScore);
            }

            // 向量结果（权重 0.7）
            for (SimpleVectorIndexEngine.VectorSearchResult result : vectorResults) {
                String docId = result.getDocId();
                double currentScore = hybridScores.getOrDefault(docId, 0.0);
                // 余弦相似度已经在 [0, 1] 范围
                hybridScores.put(docId, currentScore + 0.7 * result.getSimilarity());
            }

            // 4. 按混合分数排序，取Top-5
            List<String> topDocIds = hybridScores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(5)
                .map(Map.Entry::getKey)
                .toList();

            // 5. 从RAG获取完整文档
            List<Document> finalDocs = new ArrayList<>();
            for (String docId : topDocIds) {
                Document doc = rag.getDocument(docId);
                if (doc != null) {
                    finalDocs.add(doc);
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("混合检索完成: 找到 {} 个文档，耗时 {}ms", finalDocs.size(), elapsed);

            return finalDocs;

        } catch (Exception e) {
            log.error("混合检索失败，回退到纯关键词检索", e);
            // 回退到纯关键词检索
            String keywords = extractKeywords(question);
            SearchResult fallbackResult = rag.search(Query.builder()
                .queryText(keywords)
                .limit(5)
                .build());
            return fallbackResult.getDocuments();
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
     * 构建文档上下文（旧实现 - 已被SmartContextBuilder替代）
     *
     * 问题：
     * 1. 简单拼接所有文档内容，可能超出LLM上下文限制
     * 2. 未考虑文档相关性，可能包含无关内容
     * 3. 未优化内容提取，可能错过关键信息
     *
     * 已替换为：SmartContextBuilder.buildSmartContext()
     */
    @Deprecated
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


