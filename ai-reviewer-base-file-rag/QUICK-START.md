# 🚀 LocalFileRAG Spring Boot Starter - 快速开始

## 5分钟搭建一个 RAG 应用！

### 📦 步骤1：添加依赖

```xml
<dependency>
    <groupId>top.yumbo.ai</groupId>
    <artifactId>ai-reviewer-base-file-rag</artifactId>
    <version>1.0</version>
</dependency>
```

### ⚙️ 步骤2：配置（可选）

创建 `application.yml`：

```yaml
# 极简配置 - 甚至可以不配置，使用默认值！
local-file-rag:
  storage-path: ./data/rag  # 可选，默认值
  auto-qa-service: true     # 自动创建服务
```

### 💻 步骤3：使用

#### 方式1：注入使用（最简单）

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

#### 方式2：直接使用 LocalFileRAG

```java
@Service
public class MyService {
    
    @Autowired
    private LocalFileRAG rag;  // 自动注入原生 RAG
    
    public void myMethod() {
        // 使用完整的 RAG API
        rag.index(Document.builder()...);
        SearchResult result = rag.search(Query.builder()...);
    }
}
```

### 🎯 完整示例

```java
@SpringBootApplication
public class MyRAGApp {
    
    public static void main(String[] args) {
        SpringApplication.run(MyRAGApp.class, args);
    }
    
    // 启动时索引一些文档
    @Bean
    public CommandLineRunner init(SimpleRAGService rag) {
        return args -> {
            rag.index("文档1", "内容1");
            rag.index("文档2", "内容2");
            rag.commit();
            
            // 搜索
            List<Document> results = rag.search("关键词");
            System.out.println("找到 " + results.size() + " 个结果");
        };
    }
}
```

**就这么简单！** 🎉

---

## 📡 REST API 示例

启动后自动提供以下接口（如果启用了 `auto-qa-service`）：

```bash
# 索引文档
curl -X POST http://localhost:8080/api/rag/index \
  -H "Content-Type: application/json" \
  -d '{"title":"标题","content":"内容"}'

# 搜索文档
curl "http://localhost:8080/api/rag/search?q=关键词&limit=10"

# 获取统计
curl http://localhost:8080/api/rag/stats
```

---

## ⚙️ 配置说明

所有配置项都是可选的，有合理的默认值：

```yaml
local-file-rag:
  enabled: true                    # 是否启用（默认true）
  storage-path: ./data/rag         # 存储路径（默认 ./data/rag）
  enable-cache: true               # 缓存（默认true）
  enable-compression: true         # 压缩（默认true）
  auto-qa-service: true            # 自动QA服务（默认false）
  
  # 搜索配置
  search:
    default-limit: 10              # 默认返回数（默认10）
    max-limit: 100                 # 最大返回数（默认100）
```

---

## 🎨 对比：传统 vs 极简

### 传统方式（需要写很多代码）

```java
// 需要自己配置
@Configuration
public class RAGConfig {
    @Bean
    public LocalFileRAG rag() {
        return LocalFileRAG.builder()
            .storagePath("./data")
            .enableCache(true)
            .build();
    }
    
    @Bean
    public RAGService service(LocalFileRAG rag) {
        return new RAGService(rag);
    }
    // ... 还需要更多配置
}

// 需要自己写服务
@Service
public class RAGService {
    private final LocalFileRAG rag;
    // ... 100+ 行代码
}

// 需要自己写控制器
@RestController
public class RAGController {
    // ... 50+ 行代码
}
```

**总计：200+ 行代码**

### 极简方式（Spring Boot Starter）

```java
// application.yml (3行)
local-file-rag:
  auto-qa-service: true

// 使用 (2行)
@Autowired
private SimpleRAGService rag;
```

**总计：5 行代码** 🎉

**减少 97.5% 的代码量！**

---

## 🚀 运行示例

```bash
# 使用极简示例运行
mvn spring-boot:run -Dspring-boot.run.profiles=simple

# 或者
java -jar your-app.jar --spring.profiles.active=simple
```

---

## 📚 更多功能

### 批量索引

```java
List<Document> docs = Arrays.asList(
    Document.builder().title("doc1").content("...").build(),
    Document.builder().title("doc2").content("...").build()
);
rag.indexBatch(docs);
```

### 高级搜索

```java
// 获取原生 RAG 实例进行高级操作
LocalFileRAG rawRag = rag.getRag();
SearchResult result = rawRag.search(Query.builder()
    .queryText("关键词")
    .limit(20)
    .build());
```

### 统计和监控

```java
var stats = rag.getStatistics();
System.out.println("文档数: " + stats.getDocumentCount());
System.out.println("索引数: " + stats.getIndexedDocumentCount());
```

---

## 💡 使用建议

1. **开发环境**：使用默认配置，零配置启动
2. **生产环境**：设置合适的 `storage-path`
3. **性能优化**：启用缓存和压缩（默认启用）
4. **扩展功能**：注入 `LocalFileRAG` 使用完整 API

---

## ❓ FAQ

**Q: 是否必须配置？**
A: 不必须！不配置也能运行，使用默认值。

**Q: 如何禁用自动配置？**
A: 设置 `local-file-rag.enabled=false`

**Q: 如何使用原生 API？**
A: 注入 `LocalFileRAG` 或通过 `ragService.getRag()` 获取

**Q: 是否支持自定义？**
A: 完全支持！可以自己实现 Bean 覆盖默认配置

---

**开始使用吧！只需 5 分钟，你就能拥有一个完整的 RAG 应用！** 🎉

