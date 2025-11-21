package top.yumbo.ai.rag.optimization;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.model.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档分块器
 * 将大文档拆分为多个小块，以降低内存占用并提高检索精度
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
public class DocumentChunker {

    private final int chunkSize;
    private final int chunkOverlap;
    private final boolean smartSplit;

    /**
     * 默认分块配置
     */
    public static final int DEFAULT_CHUNK_SIZE = 1000;  // 1000字符
    public static final int DEFAULT_CHUNK_OVERLAP = 200; // 200字符重叠

    /**
     * 句子结束符
     */
    private static final char[] SENTENCE_ENDINGS = {'.', '。', '!', '！', '?', '？', '\n'};

    public DocumentChunker() {
        this(DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP, true);
    }

    public DocumentChunker(int chunkSize, int chunkOverlap, boolean smartSplit) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
        this.smartSplit = smartSplit;

        if (chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException("Chunk overlap must be less than chunk size");
        }

        log.info("DocumentChunker initialized - chunkSize: {}, overlap: {}, smartSplit: {}",
            chunkSize, chunkOverlap, smartSplit);
    }

    /**
     * 将文档分块
     *
     * @param document 原始文档
     * @return 分块后的文档列表
     */
    public List<Document> chunk(Document document) {
        String content = document.getContent();

        // 如果文档小于分块大小，直接返回
        if (content.length() <= chunkSize) {
            log.debug("Document {} is small enough, no chunking needed", document.getId());
            return List.of(document);
        }

        List<Document> chunks = new ArrayList<>();
        int chunkIndex = 0;
        int start = 0;

        log.debug("Chunking document {} with content length: {}",
            document.getId(), content.length());

        while (start < content.length()) {
            int end = Math.min(start + chunkSize, content.length());

            // 智能分割：尝试在句子边界处分割
            if (smartSplit && end < content.length()) {
                int adjustedEnd = findSentenceBoundary(content, start, end);
                if (adjustedEnd > start) {
                    end = adjustedEnd;
                }
            }

            String chunkContent = content.substring(start, end).trim();

            // 跳过空块
            if (chunkContent.isEmpty()) {
                start = end;
                continue;
            }

            Document chunk = createChunk(document, chunkContent, chunkIndex, start, end);
            chunks.add(chunk);

            // 下一个块的起始位置（带重叠）
            start = end - chunkOverlap;
            if (start < 0) start = 0;

            chunkIndex++;
        }

        log.info("Document {} chunked into {} parts", document.getId(), chunks.size());
        return chunks;
    }

    /**
     * 创建分块文档
     */
    private Document createChunk(Document original, String chunkContent,
                                 int chunkIndex, int start, int end) {
        Map<String, Object> metadata = new HashMap<>(original.getMetadata());

        // 添加分块相关元数据
        metadata.put("chunkIndex", chunkIndex);
        metadata.put("chunkStart", start);
        metadata.put("chunkEnd", end);
        metadata.put("parentDocId", original.getId());
        metadata.put("isChunk", true);
        metadata.put("originalLength", original.getContent().length());

        String chunkId = original.getId() + "_chunk_" + chunkIndex;
        String chunkTitle = original.getTitle() + " (Part " + (chunkIndex + 1) + ")";

        return Document.builder()
            .id(chunkId)
            .title(chunkTitle)
            .content(chunkContent)
            .metadata(metadata)
            .build();
    }

    /**
     * 智能查找句子边界
     * 在指定范围内查找最近的句子结束符
     */
    private int findSentenceBoundary(String content, int start, int preferredEnd) {
        // 向后查找最多100个字符
        int searchEnd = Math.min(preferredEnd + 100, content.length());

        // 首先尝试在preferredEnd之后查找句子结束符
        for (int i = preferredEnd; i < searchEnd; i++) {
            if (isSentenceEnding(content.charAt(i))) {
                return i + 1; // 包含句子结束符
            }
        }

        // 如果向后找不到，尝试向前查找（但不超过chunkSize的一半）
        int searchStart = Math.max(preferredEnd - chunkSize / 2, start);
        for (int i = preferredEnd - 1; i >= searchStart; i--) {
            if (isSentenceEnding(content.charAt(i))) {
                return i + 1;
            }
        }

        // 如果都找不到，返回原始位置
        return preferredEnd;
    }

    /**
     * 检查字符是否是句子结束符
     */
    private boolean isSentenceEnding(char c) {
        for (char ending : SENTENCE_ENDINGS) {
            if (c == ending) {
                return true;
            }
        }
        return false;
    }

    /**
     * 批量分块
     *
     * @param documents 原始文档列表
     * @return 分块后的文档列表
     */
    public List<Document> chunkBatch(List<Document> documents) {
        List<Document> allChunks = new ArrayList<>();

        for (Document doc : documents) {
            List<Document> chunks = chunk(doc);
            allChunks.addAll(chunks);
        }

        log.info("Batch chunking completed: {} documents -> {} chunks",
            documents.size(), allChunks.size());

        return allChunks;
    }

    /**
     * 获取分块统计信息
     */
    public ChunkingStats getChunkingStats(Document document) {
        int contentLength = document.getContent().length();
        int estimatedChunks = (int) Math.ceil((double) contentLength / (chunkSize - chunkOverlap));

        return ChunkingStats.builder()
            .originalLength(contentLength)
            .chunkSize(chunkSize)
            .chunkOverlap(chunkOverlap)
            .estimatedChunks(estimatedChunks)
            .needsChunking(contentLength > chunkSize)
            .build();
    }

    /**
     * 分块统计信息
     */
    @lombok.Data
    @lombok.Builder
    public static class ChunkingStats {
        private int originalLength;
        private int chunkSize;
        private int chunkOverlap;
        private int estimatedChunks;
        private boolean needsChunking;

        @Override
        public String toString() {
            return String.format("ChunkingStats[originalLength=%d, chunkSize=%d, overlap=%d, " +
                    "estimatedChunks=%d, needsChunking=%s]",
                originalLength, chunkSize, chunkOverlap, estimatedChunks, needsChunking);
        }
    }

    /**
     * Builder模式
     */
    public static class Builder {
        private int chunkSize = DEFAULT_CHUNK_SIZE;
        private int chunkOverlap = DEFAULT_CHUNK_OVERLAP;
        private boolean smartSplit = true;

        public Builder chunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
            return this;
        }

        public Builder chunkOverlap(int chunkOverlap) {
            this.chunkOverlap = chunkOverlap;
            return this;
        }

        public Builder smartSplit(boolean smartSplit) {
            this.smartSplit = smartSplit;
            return this;
        }

        public DocumentChunker build() {
            return new DocumentChunker(chunkSize, chunkOverlap, smartSplit);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}

