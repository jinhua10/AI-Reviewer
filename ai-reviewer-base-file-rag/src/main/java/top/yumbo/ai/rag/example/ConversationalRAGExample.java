package top.yumbo.ai.rag.example;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.LocalFileRAG;
import top.yumbo.ai.rag.example.llm.LLMClient;
import top.yumbo.ai.rag.example.llm.MockLLMClient;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.model.Query;
import top.yumbo.ai.rag.model.SearchResult;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 多轮对话RAG系统示例
 * 展示如何维护对话上下文并提供连贯的对话体验
 */
@Slf4j
public class ConversationalRAGExample {

    private final LocalFileRAG rag;
    private final LLMClient llmClient;
    private final Map<String, Conversation> conversations = new ConcurrentHashMap<>();

    public ConversationalRAGExample(LocalFileRAG rag, LLMClient llmClient) {
        this.rag = rag;
        this.llmClient = llmClient;
    }

    /**
     * 处理用户消息（带会话管理）
     */
    public ChatResponse chat(String sessionId, String userMessage) {
        long startTime = System.currentTimeMillis();

        // 1. 获取或创建会话
        Conversation conversation = conversations.computeIfAbsent(
            sessionId,
            k -> new Conversation(sessionId)
        );

        // 2. 添加用户消息
        conversation.addMessage("user", userMessage);

        // 3. 构建增强查询（结合历史上下文）
        String enhancedQuery = buildEnhancedQuery(conversation, userMessage);
        log.info("Enhanced query: {}", enhancedQuery);

        // 4. 检索相关文档
        SearchResult searchResult = rag.search(Query.builder()
            .queryText(enhancedQuery)
            .limit(5)
            .build());

        log.info("Retrieved {} documents", searchResult.getTotalHits());

        // 5. 构建对话Prompt
        String prompt = buildConversationalPrompt(
            conversation,
            userMessage,
            searchResult.getDocuments()
        );

        // 6. 生成回答
        String answer = llmClient.generate(prompt);

        // 7. 添加助手回答
        conversation.addMessage("assistant", answer);

        // 8. 构建响应
        long responseTime = System.currentTimeMillis() - startTime;

        return new ChatResponse(
            answer,
            searchResult.getDocuments().stream()
                .map(Document::getTitle)
                .limit(3)
                .toList(),
            conversation.getMessages().size() / 2,  // 对话轮数
            responseTime
        );
    }

    /**
     * 构建增强查询（结合对话历史）
     */
    private String buildEnhancedQuery(Conversation conversation, String currentMessage) {
        // 获取最近3轮对话
        List<ChatMessage> recent = conversation.getRecentMessages(6);

        // 提取用户消息
        String historyContext = recent.stream()
            .filter(msg -> "user".equals(msg.getRole()))
            .map(ChatMessage::getContent)
            .collect(Collectors.joining(" "));

        // 组合当前消息和历史上下文
        return currentMessage + " " + historyContext;
    }

    /**
     * 构建对话式Prompt
     */
    private String buildConversationalPrompt(
            Conversation conversation,
            String currentMessage,
            List<top.yumbo.ai.rag.model.Document> documents) {

        // 格式化对话历史
        String history = conversation.getMessages().stream()
            .map(msg -> String.format("%s: %s",
                "user".equals(msg.getRole()) ? "用户" : "助手",
                msg.getContent()))
            .collect(Collectors.joining("\n"));

        // 格式化文档
        String context = documents.stream()
            .map(doc -> String.format("【%s】\n%s", doc.getTitle(), doc.getContent()))
            .collect(Collectors.joining("\n\n---\n\n"));

        return String.format("""
            你是一个知识助手，正在进行多轮对话。请基于对话历史和相关文档回答用户问题。
            
            # 对话历史
            %s
            
            # 相关知识文档
            %s
            
            # 当前问题
            用户: %s
            
            # 回答要求
            1. 理解对话上下文，提供连贯的回答
            2. 如果用户提到"它"、"这个"等代词，根据历史推断指代内容
            3. 基于文档内容回答，保持准确性
            4. 如果需要澄清，可以询问用户
            5. 保持友好专业的语气
            
            # 你的回答：
            """, history, context, currentMessage);
    }

    /**
     * 获取会话统计
     */
    public ConversationStats getStats(String sessionId) {
        Conversation conv = conversations.get(sessionId);
        if (conv == null) {
            return new ConversationStats(0, 0, 0);
        }

        return new ConversationStats(
            conv.getMessages().size() / 2,  // 对话轮数
            conv.getMessages().size(),       // 总消息数
            System.currentTimeMillis() - conv.getStartTime()  // 会话时长
        );
    }

    /**
     * 清除会话
     */
    public void clearSession(String sessionId) {
        conversations.remove(sessionId);
        log.info("Session cleared: {}", sessionId);
    }

    /**
     * 主方法 - 演示使用
     */
    public static void main(String[] args) {
        // 1. 初始化
        LocalFileRAG rag = LocalFileRAG.builder()
            .storagePath("./data/conversational-rag")
            .enableCache(true)
            .build();

        LLMClient llmClient = new MockLLMClient();
        ConversationalRAGExample chatSystem = new ConversationalRAGExample(rag, llmClient);

        // 2. 索引知识库
        indexSampleKnowledge(rag);

        // 3. 模拟多轮对话
        System.out.println("\n=== 多轮对话RAG系统演示 ===\n");

        String sessionId = UUID.randomUUID().toString();

        // 第一轮对话
        simulateChat(chatSystem, sessionId, "什么是LocalFileRAG？");

        // 第二轮对话（使用代词"它"）
        simulateChat(chatSystem, sessionId, "它有什么优势？");

        // 第三轮对话（追问细节）
        simulateChat(chatSystem, sessionId, "如何使用它创建实例？");

        // 第四轮对话（新话题）
        simulateChat(chatSystem, sessionId, "支持哪些文档格式？");

        // 显示会话统计
        ConversationStats stats = chatSystem.getStats(sessionId);
        System.out.println("\n=== 会话统计 ===");
        System.out.println("对话轮数: " + stats.getTurns());
        System.out.println("总消息数: " + stats.getTotalMessages());
        System.out.println("会话时长: " + stats.getDurationMs() + "ms");

        // 4. 清理
        chatSystem.clearSession(sessionId);
        rag.close();
    }

    /**
     * 模拟一轮对话
     */
    private static void simulateChat(
            ConversationalRAGExample system,
            String sessionId,
            String message) {

        System.out.println("👤 用户: " + message);

        ChatResponse response = system.chat(sessionId, message);

        System.out.println("🤖 助手: " + response.getAnswer());
        System.out.println("📚 来源: " + String.join(", ", response.getSources()));
        System.out.println("⏱️  耗时: " + response.getResponseTimeMs() + "ms");
        System.out.println("🔢 第 " + response.getTurnNumber() + " 轮对话");
        System.out.println("\n" + "-".repeat(80) + "\n");
    }

    /**
     * 索引示例知识
     */
    private static void indexSampleKnowledge(LocalFileRAG rag) {
        List<Document> docs = Arrays.asList(
            Document.builder()
                .title("LocalFileRAG简介")
                .content("""
                    LocalFileRAG是一个本地文件RAG框架，使用Lucene实现全文检索。
                    它的主要优势包括：零外部依赖、完全本地化、高性能、隐私保护。
                    """)
                .build(),

            Document.builder()
                .title("LocalFileRAG使用方法")
                .content("""
                    创建LocalFileRAG实例使用Builder模式：
                    LocalFileRAG rag = LocalFileRAG.builder()
                        .storagePath("./data")
                        .enableCache(true)
                        .build();
                    """)
                .build(),

            Document.builder()
                .title("支持的格式")
                .content("""
                    LocalFileRAG支持35+种文档格式，包括：
                    txt, md, pdf, docx, xlsx, pptx, java, py, js等。
                    """)
                .build()
        );

        docs.forEach(rag::index);
        rag.commit();
    }
}

/**
 * 会话类
 */
@Data
class Conversation {
    private final String sessionId;
    private final List<ChatMessage> messages = new ArrayList<>();
    private final long startTime = System.currentTimeMillis();

    public void addMessage(String role, String content) {
        messages.add(new ChatMessage(role, content, System.currentTimeMillis()));
    }

    public List<ChatMessage> getRecentMessages(int count) {
        int size = messages.size();
        int from = Math.max(0, size - count);
        return new ArrayList<>(messages.subList(from, size));
    }
}

/**
 * 聊天消息
 */
@Data
class ChatMessage {
    private final String role;      // user 或 assistant
    private final String content;
    private final long timestamp;
}

/**
 * 聊天响应
 */
@Data
class ChatResponse {
    private final String answer;
    private final List<String> sources;
    private final int turnNumber;
    private final long responseTimeMs;
}

/**
 * 会话统计
 */
@Data
class ConversationStats {
    private final int turns;
    private final int totalMessages;
    private final long durationMs;
}

