package top.yumbo.ai.rag.spring.boot.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.LocalFileRAG;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.model.Query;
import top.yumbo.ai.rag.model.SearchResult;

import javax.annotation.PreDestroy;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * 简易 RAG 服务
 * 提供常用的 RAG 操作
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
public class SimpleRAGService {

    private final LocalFileRAG rag;
    private final LocalFileRAGProperties properties;

    public SimpleRAGService(LocalFileRAG rag, LocalFileRAGProperties properties) {
        this.rag = rag;
        this.properties = properties;
    }

    /**
     * 索引文本内容
     */
    public String index(String title, String content) {
        return index(title, content, Map.of());
    }

    /**
     * 索引文本内容（带元数据）
     */
    public String index(String title, String content, Map<String, Object> metadata) {
        Document doc = Document.builder()
            .title(title)
            .content(content)
            .metadata(metadata)
            .build();

        String docId = rag.index(doc);
        log.debug("索引文档: {} -> {}", title, docId);

        return docId;
    }

    /**
     * 索引文件
     */
    public String indexFile(File file) {
        // TODO: 实现文件解析和索引
        throw new UnsupportedOperationException("文件索引功能待实现");
    }

    /**
     * 批量索引
     */
    public int indexBatch(List<Document> documents) {
        int count = rag.indexBatch(documents);
        log.info("批量索引 {} 个文档", count);
        return count;
    }

    /**
     * 搜索
     */
    public List<Document> search(String queryText) {
        return search(queryText, properties.getSearch().getDefaultLimit());
    }

    /**
     * 搜索（指定数量）
     */
    public List<Document> search(String queryText, int limit) {
        SearchResult result = rag.search(Query.builder()
            .queryText(queryText)
            .limit(Math.min(limit, properties.getSearch().getMaxLimit()))
            .build());

        log.debug("搜索 '{}' 找到 {} 个结果", queryText, result.getDocuments().size());

        return result.getDocuments();
    }

    /**
     * 获取文档
     */
    public Document getDocument(String docId) {
        return rag.getDocument(docId);
    }

    /**
     * 删除文档
     */
    public boolean deleteDocument(String docId) {
        return rag.deleteDocument(docId);
    }

    /**
     * 提交更改
     */
    public void commit() {
        rag.commit();
        log.debug("提交索引更改");
    }

    /**
     * 优化索引
     */
    public void optimize() {
        rag.optimizeIndex();
        log.info("索引优化完成");
    }

    /**
     * 获取统计信息
     */
    public LocalFileRAG.Statistics getStatistics() {
        return rag.getStatistics();
    }

    /**
     * 获取底层 RAG 实例（高级用法）
     */
    public LocalFileRAG getRag() {
        return rag;
    }

    @PreDestroy
    public void cleanup() {
        log.info("关闭 RAG 服务...");
        rag.close();
    }
}

