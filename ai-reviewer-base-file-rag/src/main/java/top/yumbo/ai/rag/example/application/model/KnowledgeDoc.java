package top.yumbo.ai.rag.example.application.model;

import java.util.Map;

/**
 * 知识文档类
 */
public class KnowledgeDoc {
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