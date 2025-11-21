# Excel知识库快速使用指南

## 问题1: 如何处理Excel文档构建知识库？

### 答案：使用`ExcelKnowledgeBuilder`工具

```java
// 1. 创建构建器
ExcelKnowledgeBuilder builder = new ExcelKnowledgeBuilder(
    "./data/excel-kb",    // 知识库存储路径（持久化到磁盘）
    "./your-excel-folder" // 你的Excel文件夹路径
);

// 2. 构建知识库
BuildResult result = builder.buildKnowledgeBase();

// 3. 查看结果
System.out.println("成功: " + result.successCount);
System.out.println("失败: " + result.failedCount);

// 4. 关闭（数据已自动保存）
builder.close();
```

### 工作原理

1. **自动扫描**: 递归扫描文件夹中所有`.xls`和`.xlsx`文件
2. **自动解析**: 使用Apache Tika自动解析Excel内容（所有sheet的文本）
3. **自动索引**: 使用Lucene建立全文索引
4. **自动持久化**: 所有数据保存到磁盘

---

## 问题2: 知识库重启后会丢失吗？

### 答案：❌ **不会丢失！知识库完全持久化**

### 持久化原理

知识库数据保存在3个地方：

```
./data/excel-kb/           # 你指定的存储路径
├── documents/             # 文档存储（持久化）
│   └── 2025/11/22/
│       ├── doc1.bin
│       └── doc2.bin
├── index/                 # Lucene索引（持久化）
│   ├── segments_1
│   └── _0.cfs
└── metadata/              # 元数据（持久化）
    └── metadata.db
```

**所有数据都在磁盘上，重启后自动加载！**

### 验证持久化

```java
// 第一次运行 - 构建知识库
ExcelKnowledgeBuilder builder = new ExcelKnowledgeBuilder(
    "./data/excel-kb", "./excel-files"
);
builder.buildKnowledgeBase();
builder.close();

// ====== 关闭程序，重启 ======

// 第二次运行 - 重启后直接使用
LocalFileRAG rag = LocalFileRAG.builder()
    .storagePath("./data/excel-kb")  // 相同的路径
    .build();

// ✅ 知识库自动加载，无需重新构建！
var stats = rag.getStatistics();
System.out.println("文档数: " + stats.getDocumentCount());  
// 输出: 文档数: 15 （之前构建的文档）

// 可以直接查询
SearchResult result = rag.search(
    Query.builder().queryText("关键词").limit(10).build()
);
```

---

## 完整使用流程

### 步骤1: 准备Excel文件

将Excel文件放在一个文件夹：

```
D:/my-excel-files/
├── 销售数据2024.xlsx
├── 客户信息.xls
├── 产品目录.xlsx
└── reports/
    └── 财务报表.xlsx
```

### 步骤2: 构建知识库（只需一次）

```java
public class BuildKB {
    public static void main(String[] args) {
        ExcelKnowledgeBuilder builder = new ExcelKnowledgeBuilder(
            "D:/knowledge-base",     // 知识库保存路径
            "D:/my-excel-files"      // Excel文件夹
        );
        
        BuildResult result = builder.buildKnowledgeBase();
        
        System.out.println("构建完成:");
        System.out.println("- 总文件数: " + result.totalFiles);
        System.out.println("- 成功: " + result.successCount);
        System.out.println("- 失败: " + result.failedCount);
        System.out.println("- 耗时: " + result.buildTimeMs/1000.0 + " 秒");
        
        builder.close();
        
        System.out.println("\n✅ 知识库已保存到: D:/knowledge-base");
        System.out.println("✅ 重启后数据仍然存在！");
    }
}
```

### 步骤3: 使用知识库（随时可用）

```java
public class QueryKB {
    public static void main(String[] args) {
        // 连接到已有知识库（重启后也能用）
        LocalFileRAG rag = LocalFileRAG.builder()
            .storagePath("D:/knowledge-base")
            .enableCache(true)
            .build();
        
        // 搜索
        SearchResult result = rag.search(
            Query.builder()
                .queryText("销售数据")
                .limit(10)
                .build()
        );
        
        System.out.println("找到 " + result.getTotalHits() + " 个相关文档");
        
        result.getDocuments().forEach(doc -> {
            System.out.println("文件: " + doc.getMetadata().get("fileName"));
            System.out.println("内容片段: " + 
                doc.getContent().substring(0, Math.min(100, doc.getContent().length())));
            System.out.println("---");
        });
        
        rag.close();
    }
}
```

---

## 命令行运行

### 构建知识库

```bash
cd ai-reviewer-base-file-rag
mvn clean package

# 运行构建工具
java -cp target/ai-reviewer-base-file-rag-1.0.jar \
    top.yumbo.ai.rag.example.knowledgeExample.ExcelKnowledgeBuilder \
    D:/knowledge-base \
    D:/my-excel-files
```

### 使用知识库

```bash
# 重启后查询（数据仍在）
java -cp target/ai-reviewer-base-file-rag-1.0.jar \
    YourQueryClass \
    D:/knowledge-base
```

---

## 常见问题

### Q: Excel内容如何被提取？

**A**: 使用Apache Tika自动解析：
- 提取所有sheet的文本
- 包括单元格的值
- 保留基本表格结构

### Q: 可以删除原始Excel文件吗？

**A**: ✅ **可以**！
- Excel内容已被提取并保存
- 删除原始文件不影响查询
- 知识库是独立的

### Q: 如何清空知识库重新构建？

**A**: 删除存储目录即可
```bash
rm -rf D:/knowledge-base
# 然后重新运行构建命令
```

### Q: 知识库占用多少空间？

**A**: 
- 原始Excel: 100MB
- 知识库: 约40-70MB（启用压缩）
- 比原始文件小30-50%

### Q: 支持增量更新吗？

**A**: ✅ 支持
```java
builder.incrementalUpdate();  // 只处理新文件
```

---

## 关键特性

- ✅ **完全持久化** - 重启后数据仍在
- ✅ **自动解析** - 支持xls和xlsx
- ✅ **全文检索** - BM25算法，亚秒级响应
- ✅ **零依赖** - 不需要向量数据库
- ✅ **隐私保护** - 数据完全本地化
- ✅ **易于使用** - 两行代码完成

---

## 总结

### 核心答案

1. **如何处理Excel？**
   - 使用`ExcelKnowledgeBuilder`
   - 一行代码：`builder.buildKnowledgeBase()`
   - 自动扫描、解析、索引

2. **重启会丢失吗？**
   - ❌ **不会丢失**
   - 完全持久化到磁盘
   - 重启后自动加载

### 使用步骤

```java
// 1. 构建（只需一次）
new ExcelKnowledgeBuilder(kbPath, excelFolder).buildKnowledgeBase();

// 2. 重启后使用（随时可用）
LocalFileRAG rag = LocalFileRAG.builder().storagePath(kbPath).build();
rag.search(...);
```

**就这么简单！** 🎉

---

**创建时间**: 2025-11-22  
**文件**: ExcelKnowledgeBuilder.java  
**状态**: ✅ 可用

