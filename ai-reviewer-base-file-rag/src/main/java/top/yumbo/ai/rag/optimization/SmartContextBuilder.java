package top.yumbo.ai.rag.optimization;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.model.Document;

import java.util.Arrays;
import java.util.List;

/**
 * 智能上下文构建器
 * 解决大文档RAG的上下文窗口限制问题
 *
 * 核心功能：
 * 1. 动态调整文档长度以适应LLM上下文限制
 * 2. 提取最相关的片段而不是简单截断
 * 3. 在句子边界处切分，保持语义完整性
 * 4. 优先保留包含查询关键词的内容
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
public class SmartContextBuilder {

    private static final int DEFAULT_MAX_CONTEXT_LENGTH = 8000;  // 总上下文限制
    private static final int DEFAULT_MAX_DOC_LENGTH = 2000;      // 单个文档最大长度
    private static final int KEYWORD_WINDOW_SIZE = 500;          // 关键词搜索窗口
    private static final int SENTENCE_BOUNDARY_SEARCH = 100;     // 句子边界搜索范围

    private final int maxContextLength;
    private final int maxDocLength;

    public SmartContextBuilder() {
        this(DEFAULT_MAX_CONTEXT_LENGTH, DEFAULT_MAX_DOC_LENGTH);
    }

    public SmartContextBuilder(int maxContextLength, int maxDocLength) {
        this.maxContextLength = maxContextLength;
        this.maxDocLength = maxDocLength;

        log.info("SmartContextBuilder initialized: maxContext={}chars, maxDoc={}chars",
            maxContextLength, maxDocLength);
    }

    /**
     * 构建智能上下文
     *
     * @param query 用户查询
     * @param documents 检索到的文档列表
     * @return 优化后的上下文字符串
     */
    public String buildSmartContext(String query, List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "";
        }

        log.debug("Building smart context for query: {}, documents: {}",
            query, documents.size());

        StringBuilder context = new StringBuilder();
        int remainingLength = maxContextLength;
        int processedDocs = 0;

        for (Document doc : documents) {
            if (remainingLength <= 0) {
                log.debug("Context length limit reached, processed {} documents", processedDocs);
                break;
            }

            // 计算这个文档可以使用的最大长度
            int allowedLength = Math.min(maxDocLength, remainingLength);

            // 提取最相关的片段
            String relevantPart = extractRelevantPart(
                query,
                doc.getContent(),
                allowedLength
            );

            if (!relevantPart.isEmpty()) {
                // 添加文档标记和内容
                String docSection = formatDocumentSection(doc, relevantPart);
                context.append(docSection);

                // 更新剩余长度（包括格式化字符）
                remainingLength -= docSection.length();
                processedDocs++;

                log.trace("Added document: {}, length: {}, remaining: {}",
                    doc.getTitle(), docSection.length(), remainingLength);
            }
        }

        String result = context.toString();
        log.info("Smart context built: {}chars from {} documents ({}% of max)",
            result.length(), processedDocs,
            result.length() * 100 / maxContextLength);

        return result;
    }

    /**
     * 提取最相关的片段
     */
    private String extractRelevantPart(String query, String content, int maxLength) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        // 如果内容本身就不超长，直接返回
        if (content.length() <= maxLength) {
            return content;
        }

        // 提取查询关键词
        String[] keywords = extractKeywords(query);

        // 查找包含最多关键词的位置
        int bestPosition = findBestPosition(content.toLowerCase(), keywords);

        // 以最佳位置为中心提取片段
        int start = Math.max(0, bestPosition - maxLength / 2);
        int end = Math.min(content.length(), start + maxLength);

        // 如果end到达末尾，调整start
        if (end == content.length() && content.length() > maxLength) {
            start = content.length() - maxLength;
        }

        // 调整到句子边界
        start = adjustToSentenceStart(content, start);
        end = adjustToSentenceEnd(content, end);

        // 提取内容
        String extracted = content.substring(start, end).trim();

        // 添加省略标记
        if (start > 0) {
            extracted = "..." + extracted;
        }
        if (end < content.length()) {
            extracted = extracted + "...";
        }

        log.debug("Extracted relevant part: original={}chars, extracted={}chars, " +
                "bestPos={}, range=[{}, {}]",
            content.length(), extracted.length(), bestPosition, start, end);

        return extracted;
    }

    /**
     * 提取关键词（去除停用词）
     */
    private String[] extractKeywords(String query) {
        // 简单的停用词列表
        List<String> stopWords = Arrays.asList(
            "的", "是", "在", "了", "和", "有", "我", "你", "他", "她",
            "什么", "怎么", "如何", "为什么", "吗", "呢", "啊", "了",
            "a", "an", "the", "is", "are", "was", "were", "be", "been",
            "what", "how", "why", "when", "where", "who"
        );

        return Arrays.stream(query.toLowerCase().split("[\\s\\p{Punct}]+"))
            .filter(word -> !stopWords.contains(word) && word.length() > 1)
            .toArray(String[]::new);
    }

    /**
     * 查找最佳位置（关键词密度最高的区域）
     */
    private int findBestPosition(String content, String[] keywords) {
        if (keywords.length == 0) {
            return 0;
        }

        int bestPos = 0;
        int maxScore = 0;

        // 使用滑动窗口找到关键词密度最高的区域
        for (int i = 0; i < content.length() - KEYWORD_WINDOW_SIZE; i += KEYWORD_WINDOW_SIZE / 2) {
            int windowEnd = Math.min(i + KEYWORD_WINDOW_SIZE, content.length());
            String window = content.substring(i, windowEnd);

            // 计算这个窗口的得分（关键词出现次数）
            int score = 0;
            for (String keyword : keywords) {
                score += countOccurrences(window, keyword);
            }

            if (score > maxScore) {
                maxScore = score;
                bestPos = i + KEYWORD_WINDOW_SIZE / 2; // 窗口中心
            }
        }

        log.trace("Best position found at {} with score {}", bestPos, maxScore);
        return bestPos;
    }

    /**
     * 统计词出现次数
     */
    private int countOccurrences(String text, String word) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(word, index)) != -1) {
            count++;
            index += word.length();
        }
        return count;
    }

    /**
     * 调整到句子开始位置
     */
    private int adjustToSentenceStart(String text, int pos) {
        if (pos <= 0) {
            return 0;
        }

        // 向前搜索句子结束符
        int searchStart = Math.max(0, pos - SENTENCE_BOUNDARY_SEARCH);
        for (int i = pos - 1; i >= searchStart; i--) {
            char c = text.charAt(i);
            if (isSentenceEnding(c)) {
                // 找到句子结束符，返回下一个字符位置
                return i + 1;
            }
        }

        // 如果没找到，返回原位置
        return pos;
    }

    /**
     * 调整到句子结束位置
     */
    private int adjustToSentenceEnd(String text, int pos) {
        if (pos >= text.length()) {
            return text.length();
        }

        // 向后搜索句子结束符
        int searchEnd = Math.min(text.length(), pos + SENTENCE_BOUNDARY_SEARCH);
        for (int i = pos; i < searchEnd; i++) {
            char c = text.charAt(i);
            if (isSentenceEnding(c)) {
                // 找到句子结束符，包含这个字符
                return i + 1;
            }
        }

        // 如果没找到，返回原位置
        return pos;
    }

    /**
     * 判断是否是句子结束符
     */
    private boolean isSentenceEnding(char c) {
        return c == '。' || c == '.' || c == '!' || c == '！' ||
               c == '?' || c == '？' || c == '\n' || c == ';' || c == '；';
    }

    /**
     * 格式化文档片段
     */
    private String formatDocumentSection(Document doc, String content) {
        return String.format(
            "\n【文档：%s】\n%s\n",
            doc.getTitle(),
            content
        );
    }

    /**
     * 获取上下文统计信息
     */
    public ContextStats getContextStats(String context) {
        int totalLength = context.length();
        int documentCount = context.split("【文档：").length - 1;
        double utilization = (double) totalLength / maxContextLength * 100;

        return new ContextStats(totalLength, documentCount, utilization);
    }

    /**
     * 上下文统计信息
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ContextStats {
        private int totalLength;
        private int documentCount;
        private double utilization;

        @Override
        public String toString() {
            return String.format("ContextStats[length=%d, docs=%d, utilization=%.1f%%]",
                totalLength, documentCount, utilization);
        }
    }

    /**
     * Builder模式
     */
    public static class Builder {
        private int maxContextLength = DEFAULT_MAX_CONTEXT_LENGTH;
        private int maxDocLength = DEFAULT_MAX_DOC_LENGTH;

        public Builder maxContextLength(int length) {
            this.maxContextLength = length;
            return this;
        }

        public Builder maxDocLength(int length) {
            this.maxDocLength = length;
            return this;
        }

        public SmartContextBuilder build() {
            return new SmartContextBuilder(maxContextLength, maxDocLength);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}

