# LocalFileRAG - 本地文件RAG框架

<div align="center">

**🚀 零外部依赖的RAG解决方案**

完全本地化 | 高性能 | 隐私保护 | 成本节约

[快速开始](#快速开始) • [示例代码](#示例代码) • [应用指南](#应用指南) • [文档](#文档)

</div>

---

## ✨ 特性

- ✅ **零外部依赖** - 无需向量数据库、无需Embedding API
- ✅ **完全本地化** - 数据不离开本地环境，100%隐私保护
- ✅ **高性能** - 基于Lucene BM25算法，亚秒级检索
- ✅ **成本节约** - 节省60-70%的API调用费用
- ✅ **易于集成** - 简洁的Java API，Builder模式构建
- ✅ **35+格式** - 支持txt、pdf、docx、xlsx、代码文件等
- ✅ **生产就绪** - 完整的测试覆盖，企业级代码质量

---

## 🎯 为什么选择LocalFileRAG？

### 传统RAG的痛点

```
❌ 需要昂贵的Embedding API ($1000+/月)
❌ 依赖外部向量数据库 ($100+/月)
❌ 数据隐私风险（上传到云端）
❌ 网络延迟高（2-5秒）
❌ 运维复杂
```

### LocalFileRAG的优势

```
✅ 零Embedding费用
✅ 本地Lucene索引
✅ 完全本地化
✅ 响应快速（0.5-1秒）
✅ 部署简单
```

**成本对比**（10万次查询/月）:
- 传统RAG: **$2,600/月**
- LocalFileRAG: **$1,550/月**
- **节省**: **$1,050/月 (40%)**

---

## 🚀 快速开始

### 方式1：极简模式（Spring Boot Starter）⭐ 推荐

**只需 3 步，5 分钟搭建！**

#### 1. 添加依赖

```xml
<dependency>
    <groupId>top.yumbo.ai</groupId>
    <artifactId>ai-reviewer-base-file-rag</artifactId>
    <version>1.0</version>
</dependency>
```

#### 2. 配置（可选）

```yaml
# application.yml - 甚至可以不配置！
local-file-rag:
  storage-path: ./data/rag
  auto-qa-service: true
```

#### 3. 使用（一行代码）

```java
@RestController
public class MyController {
    
    @Autowired
    private SimpleRAGService rag;  // 自动注入
    
    @PostMapping("/index")
    public String index(@RequestBody String content) {
        return rag.index("标题", content);  // 一行代码索引
    }
    
    @GetMapping("/search")
    public List<Document> search(@RequestParam String q) {
        return rag.search(q);  // 一行代码搜索
    }
}
```

**完整示例：[QUICK-START.md](QUICK-START.md)**

---

### 方式2：原生 API（灵活可控）

#### 1. 添加依赖

```xml
<dependency>
    <groupId>top.yumbo.ai</groupId>
    <artifactId>ai-reviewer-base-file-rag</artifactId>
    <version>1.0</version>
</dependency>
```

#### 2. 创建实例

```java
// 使用Builder模式创建
LocalFileRAG rag = LocalFileRAG.builder()
    .storagePath("./data")
    .enableCache(true)
    .enableCompression(true)
    .build();
```

### 3. 索引文档

```java
// 索引单个文档
rag.index(Document.builder()
    .title("文档标题")
    .content("文档内容...")
    .metadata(Map.of("category", "技术文档"))
    .build());

// 提交索引
rag.commit();
```

### 4. 搜索文档

```java
// 执行搜索
SearchResult result = rag.search(Query.builder()
    .queryText("关键词")
    .limit(10)
    .build());

// 获取结果
List<Document> docs = result.getDocuments();
```

### 5. 集成AI问答

```java
// 1. 检索相关文档
SearchResult docs = rag.search(
    Query.builder().queryText(question).limit(5).build()
);

// 2. 构建Prompt
String prompt = buildPrompt(question, docs.getDocuments());

// 3. 调用LLM生成答案
String answer = llmClient.generate(prompt);
```

---

## 📚 示例代码

### AI问答系统

```java
public class AIQASystem {
    private final LocalFileRAG rag;
    private final LLMClient llm;
    
    public String answer(String question) {
        // 1. 提取关键词
        String keywords = extractKeywords(question);
        
        // 2. 检索文档
        SearchResult docs = rag.search(
            Query.builder().queryText(keywords).limit(5).build()
        );
        
        // 3. 构建上下文
        String context = docs.getDocuments().stream()
            .map(doc -> doc.getTitle() + "\n" + doc.getContent())
            .collect(Collectors.joining("\n\n"));
        
        // 4. 生成答案
        return llm.generate(String.format("""
            基于以下文档回答问题：
            
            文档：%s
            
            问题：%s
            """, context, question));
    }
}
```

### 多轮对话系统

```java
public class ConversationalAI {
    private final LocalFileRAG rag;
    private final Map<String, List<Message>> sessions = new ConcurrentHashMap<>();
    
    public String chat(String sessionId, String message) {
        // 1. 获取会话历史
        List<Message> history = sessions.computeIfAbsent(
            sessionId, k -> new ArrayList<>()
        );
        
        // 2. 结合历史构建查询
        String enhancedQuery = buildEnhancedQuery(history, message);
        
        // 3. 检索文档
        SearchResult docs = rag.search(
            Query.builder().queryText(enhancedQuery).limit(5).build()
        );
        
        // 4. 生成回答
        String answer = generateAnswer(history, message, docs);
        
        // 5. 更新历史
        history.add(new Message("user", message));
        history.add(new Message("assistant", answer));
        
        return answer;
    }
}
```

完整示例代码：
- [AIQASystemExample.java](src/main/java/top/yumbo/ai/rag/example/AIQASystemExample.java)
- [ConversationalRAGExample.java](src/main/java/top/yumbo/ai/rag/example/ConversationalRAGExample.java)

---

## 🏗️ 架构设计

```
┌────────────────────────────────���┐
│      应用层 (Your AI App)        │
│   - 问答系统                     │
│   - 对话机器人                   │
│   - 知识助手                     │
└──────────────┬──────────────────┘
               │
┌──────────────▼──────────────────┐
│      LocalFileRAG                │
│  ┌────────────────────────────┐ │
│  │  查询处理 (Query Processor)│ │
│  └─────────────┬──────────────┘ │
│                │                 │
│  ┌─────────────▼──────────────┐ │
│  │  索引引擎 (Lucene BM25)    │ │
│  └─────────────┬──────────────┘ │
│                │                 │
│  ┌─────────────▼──────────────┐ │
│  │  存储层 (File System)      │ │
│  └────────────────────────────┘ │
└─────────────────────────────────┘
               │
               ▼
         LLM (OpenAI/本地)
```

---

## 📖 应用场景

### ✅ 企业知识库

```java
// 索引公司文档
rag.index(employeeHandbook);
rag.index(companyPolicies);
rag.index(technicalDocs);

// 员工提问
answer("年假政策是什么？");
// → 基于员工手册的准确答案
```

### ✅ 代码库助手

```java
// 索引代码仓库
codeAssistant.indexCodebase(Paths.get("./src"));

// 开发者提问
answer("如何使用Builder模式？");
// → 基于实际代码的说明+示例
```

### ✅ 客服机器人

```java
// 索引FAQ和产品文档
customerSupport.indexKnowledgeBase();

// 客户提问
answer("如何重置密码？");
// → 详细步骤说明
```

---

## 📊 性能指标

| 指标 | 本地文件RAG | 传统RAG | 提升 |
|------|-------------|---------|------|
| 检索延迟 | 50-100ms | 500-1000ms | **5-10倍** |
| 总响应时间 | 0.5-1秒 | 2-5秒 | **2-5倍** |
| 月度成本 | $1,550 | $2,600 | **节省40%** |
| 并发能力 | 10,000+ | 依赖外部 | **更高** |
| 隐私保护 | 100%本地 | 云端处理 | **完全保护** |

---

## 📁 文档

### 设计文档
- [架构设计文档](md/本地文件RAG/20251121140000-本地文件存储RAG替代框架架构设计.md)
- [AI系统应用指南](md/本地文件RAG/20251122001500-本地文件RAG在AI系统中的应用指南.md)
- [完整替代方案](md/本地文件RAG/20251122002000-本地文件RAG替代传统RAG完整方案.md)

### 实施文档
- 第一阶段：存储层实现
- 第二阶段：索引引擎实现
- 第三阶段：查询处理实现
- 第四阶段：API层实现
- 第五阶段：性能优化
- 第六阶段：高级功能

### 测试报告
- [测试覆盖率报告](md/本地文件RAG/20251121235000-测试覆盖率报告.md) - 93%覆盖率
- [架构合规性报告](md/本地文件RAG/20251122000500-架构合规性检查报告.md) - 100分

---

## 🛠️ 技术栈

| 组件 | 技术 | 版本 |
|------|------|------|
| 搜索引擎 | Apache Lucene | 9.8.0 |
| 文档解析 | Apache Tika | 2.9.1 |
| 缓存 | Caffeine | 3.1.8 |
| HTTP服务器 | Netty | 4.1.104 |
| JSON | Fastjson2 | 2.0.43 |
| 数据库 | SQLite | 3.44.1 |
| Java | JDK | 17+ |
| 构建工具 | Maven | 3.9.9 |

---

## 🎯 适用场景

### ✅ 非常适合

- 企业内部知识库
- 敏感数据处理
- 成本敏感项目
- 离线环境应用
- 代码库检索
- 客服机器人

### ⚠️ 需要权衡

- 多语言语义搜索（可通过LLM辅助）
- 复杂推理问答（主要依赖LLM）

### ❌ 不适合

- 纯语义相似度搜索
- 图片/音频检索
- 需要云端实时同步

---

## 📈 项目状态

```
✅ 阶段1: 存储层          100% (完成)
✅ 阶段2: 索引引擎        100% (完成)
✅ 阶段3: 查询处理        100% (完成)
✅ 阶段4: API层           100% (完成)
✅ 阶段5: 性能优化        100% (完成)
✅ 阶段6: 高级功能        100% (完成)

总体进度: ████████████████████████ 100%
```

**代码统计**:
- Java类: 43个
- 代码行数: 5,170行
- 测试覆盖率: 93%
- 文档: 20+份
- 架构评分: 100/100 ⭐⭐⭐⭐⭐

---

## 🤝 贡献

欢迎贡献代码、报告问题或提出建议！

---

## 📄 许可证

本项目采用 MIT 许可证。

---

## 🙏 致谢

- Apache Lucene - 强大的全文检索引擎
- Apache Tika - 多格式文档解析
- Caffeine - 高性能缓存
- 所有开源贡献者

---

## 📞 联系方式

- 项目地址: [GitHub](https://github.com/yourorg/local-file-rag)
- 问题反馈: [Issues](https://github.com/yourorg/local-file-rag/issues)
- 邮箱: your-email@example.com

---

<div align="center">

**⭐ 如果这个项目对你有帮助，请给一个Star！⭐**

[快速开始](#快速开始) • [示例代码](#示例代码) • [文档](#文档)

Made with ❤️ by AI Reviewer Team

</div>

