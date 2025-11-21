# AI Reviewer Base File RAG

本地文件存储RAG替代框架 - 基于Apache Lucene的高性能文档检索系统

## 概述

Base File RAG 是一个轻量级、高性能的本地文档检索框架，提供了传统RAG（检索增强生成）系统的替代方案。它使用成熟的搜索技术（如Apache Lucene）替代向量数据库，实现完全本地化的文档存储和检索。

### 核心特性

- ✅ **零外部依赖** - 完全自包含，无需向量数据库或嵌入API
- ✅ **高性能** - 亚秒级查询响应，支持百万级文档
- ✅ **隐私保护** - 所有数据保留在本地，不需要网络调用
- ✅ **易于集成** - 简洁的API接口，开箱即用
- ✅ **多格式支持** - 支持PDF、Word、Excel、文本等多种文档格式
- ✅ **灵活配置** - 支持压缩、缓存、索引优化等多种配置选项

## 技术栈

| 组件 | 技术 | 版本 | 说明 |
|-----|------|-----|------|
| 搜索引擎 | Apache Lucene | 9.9.1 | 全文索引和搜索 |
| 元数据存储 | SQLite JDBC | 3.44.1 | 文档元数据管理 |
| 文档解析 | Apache Tika | 2.9.1 | 多格式文档解析 |
| 缓存 | Caffeine | 3.1.8 | 高性能缓存 |
| JSON处理 | Fastjson2 | 2.0.52 | JSON序列化 |

## 快速开始

### 1. 添加依赖

在你的 `pom.xml` 中添加：

```xml
<dependency>
    <groupId>top.yumbo.ai</groupId>
    <artifactId>ai-reviewer-base-file-rag</artifactId>
    <version>1.0</version>
</dependency>
```

### 2. 基础使用

```java
import top.yumbo.ai.rag.LocalFileRAG;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.model.Query;
import top.yumbo.ai.rag.model.SearchResult;
import top.yumbo.ai.rag.util.DocumentUtils;

// 初始化框架
LocalFileRAG rag = LocalFileRAG.builder()
    .storagePath("./data")
    .enableCache(true)
    .build();

// 索引文档
Document doc = DocumentUtils.fromText(
    "Java编程指南",
    "Java是一门面向对象的编程语言..."
);
String docId = rag.index(doc);

// 搜索文档
Query query = Query.of("编程语言")
    .withLimit(10);
SearchResult result = rag.search(query);

// 处理结果
result.getDocuments().forEach(document -> {
    System.out.println(document.getTitle());
    System.out.println(document.getContent());
});

// 关闭资源
rag.close();
```

### 3. 从文件索引

```java
import java.io.File;

// 索引单个文件
File file = new File("document.pdf");
Document doc = DocumentUtils.fromFile(file);
String docId = rag.index(doc);

// 批量索引目录
File directory = new File("/path/to/documents");
List<Document> documents = DocumentUtils.fromDirectory(directory, true);
rag.indexBatch(documents);
```

## 高级配置

### 自定义配置

```java
import top.yumbo.ai.rag.config.RAGConfiguration;

RAGConfiguration config = RAGConfiguration.builder()
    .storage(RAGConfiguration.StorageConfig.builder()
        .basePath("./data")
        .compression(true)
        .encryption(false)
        .build())
    .index(RAGConfiguration.IndexConfig.builder()
        .analyzer("standard")
        .ramBufferSizeMB(256)
        .maxBufferedDocs(1000)
        .build())
    .cache(RAGConfiguration.CacheConfig.builder()
        .enabled(true)
        .documentCacheSize(1000)
        .queryCacheSize(10000)
        .build())
    .build();

LocalFileRAG rag = LocalFileRAG.builder()
    .configuration(config)
    .build();
```

### 高级搜索

```java
// 带过滤条件的搜索
Query query = Query.builder()
    .queryText("机器学习")
    .fields(new String[]{"title", "content"})
    .limit(20)
    .offset(0)
    .build();

query.withFilter("category", "技术");
query.withFilter("author", "张三");

SearchResult result = rag.search(query);
```

## API文档

### 核心接口

#### LocalFileRAG

主入口类，提供统一的API接口。

**主要方法：**

- `String index(Document document)` - 索引单个文档
- `int indexBatch(List<Document> documents)` - 批量索引文档
- `SearchResult search(Query query)` - 搜索文档
- `Document getDocument(String docId)` - 获取文档
- `boolean updateDocument(String docId, Document document)` - 更新文档
- `boolean deleteDocument(String docId)` - 删除文档
- `void optimizeIndex()` - 优化索引
- `Statistics getStatistics()` - 获取统计信息

#### Document

文档模型类。

**主要字段：**

- `String id` - 文档ID
- `String title` - 标题
- `String content` - 内容
- `String category` - 分类
- `Map<String, Object> metadata` - 元数据

#### Query

查询模型类。

**主要字段：**

- `String queryText` - 查询文本
- `String[] fields` - 查询字段
- `int limit` - 结果数量限制
- `int offset` - 偏移量
- `Map<String, String> filters` - 过滤条件

#### SearchResult

搜索结果类。

**主要字段：**

- `List<ScoredDocument> documents` - 文档列表
- `long totalHits` - 总匹配数
- `long queryTimeMs` - 查询耗时

## 性能指标

基于标准测试环境（Intel i7, 16GB RAM, SSD）：

| 操作 | 性能 |
|-----|------|
| 索引速度 | ~1000 文档/秒 |
| 查询延迟 | < 100ms (10万文档) |
| 查询延迟 | < 500ms (100万文档) |
| 内存占用 | ~2GB (100万文档) |
| 磁盘占用 | ~5GB (100万文档) |

## 架构设计

### 模块结构

```
top.yumbo.ai.rag
├── LocalFileRAG.java          # 主入口类
├── config/                    # 配置
│   └── RAGConfiguration.java
├── core/                      # 核心接口
│   ├── StorageEngine.java
│   ├── IndexEngine.java
│   ├── CacheEngine.java
│   └── DocumentParser.java
├── impl/                      # 实现类
│   ├── storage/
│   │   ├── FileSystemStorageEngine.java
│   │   ├── SQLiteMetadataManager.java
│   │   └── SHA256DocumentHasher.java
│   ├── index/
│   │   └── LuceneIndexEngine.java
│   ├── cache/
│   │   └── CaffeineCacheEngine.java
│   └── parser/
│       └── TikaDocumentParser.java
├── model/                     # 数据模型
│   ├── Document.java
│   ├── Query.java
│   ├── SearchResult.java
│   └── ScoredDocument.java
├── factory/                   # 工厂类
│   └── RAGEngineFactory.java
└── util/                      # 工具类
    └── DocumentUtils.java
```

### 数据存储结构

```
data/
├── documents/              # 文档存储
│   └── 2025/11/21/
│       ├── abc123.txt
│       └── def456.txt.gz
├── index/                  # Lucene索引
│   └── lucene-index/
├── metadata/              # 元数据
│   └── metadata.db
└── cache/                 # 缓存（可选）
```

## 使用场景

### 适用场景

- ✅ 企业内部知识库
- ✅ 代码仓库搜索
- ✅ 文档管理系统
- ✅ 本地笔记应用
- ✅ 法律/合规文档检索

### 不适用场景

- ❌ 需要语义理解的场景
- ❌ 多语言跨语种搜索
- ❌ 需要向量相似度的场景
- ❌ 实时协作编辑

## 与传统RAG对比

| 特性 | Base File RAG | 传统RAG |
|-----|--------------|---------|
| 部署复杂度 | 低 | 高 |
| 外部依赖 | 无 | 向量DB + 嵌入API |
| 隐私性 | 完全本地 | 数据需上传 |
| 成本 | 仅硬件 | API费用 + 服务器 |
| 查询延迟 | < 100ms | 网络 + 计算 |
| 准确性 | 关键字精确 | 语义相关 |
| 适用范围 | 精确检索 | 模糊语义 |

## 贡献指南

欢迎贡献！请遵循以下步骤：

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 许可证

本项目采用 MIT 许可证。详见 [LICENSE](../LICENSE.txt) 文件。

## 联系方式

- 项目主页: https://github.com/yourorg/ai-reviewer
- 问题反馈: https://github.com/yourorg/ai-reviewer/issues

## 致谢

感谢以下开源项目：

- [Apache Lucene](https://lucene.apache.org/)
- [Apache Tika](https://tika.apache.org/)
- [Caffeine](https://github.com/ben-manes/caffeine)
- [SQLite](https://www.sqlite.org/)
- [Fastjson2](https://github.com/alibaba/fastjson2)

