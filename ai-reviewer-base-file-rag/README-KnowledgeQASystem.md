# 📚 知识库问答系统 (Knowledge QA System)

基于 Spring Boot 的本地文件知识库智能问答应用

## ✨ 功能特性

- **多格式支持**: Excel (.xlsx, .xls), Word (.docx), PowerPoint (.pptx), PDF, TXT, Markdown等
- **语义检索**: 使用本地向量嵌入模型进行智能语义检索
- **配置化管理**: 通过 application.yml 配置所有参数
- **REST API**: 提供标准的 HTTP 接口
- **自动检查**: 启动时自动检查模型文件
- **Spring Boot**: 企业级框架，易于集成和部署

## 🚀 快速开始

### 1. 下载模型文件

系统启动前需要下载向量嵌入模型：

```bash
pip install optimum[onnxruntime] transformers

python -c "
from optimum.onnxruntime import ORTModelForFeatureExtraction
from transformers import AutoTokenizer

model = ORTModelForFeatureExtraction.from_pretrained('BAAI/bge-m3', export=True)
tokenizer = AutoTokenizer.from_pretrained('BAAI/bge-m3')

model.save_pretrained('src/main/resources/models/bge-m3')
tokenizer.save_pretrained('src/main/resources/models/bge-m3')
"
```

### 2. 准备文档

```bash
mkdir -p ./data/documents
# 将你的文档文件放到这个目录
```

### 3. 配置应用

编辑 `src/main/resources/application.yml`：

```yaml
knowledge:
  qa:
    knowledge-base:
      source-path: ./data/documents  # 你的文档路径
      rebuild-on-startup: true       # 首次启动设为 true
```

### 4. 启动应用

```bash
mvn spring-boot:run
```

### 5. 使用 API

```bash
# 问答
curl -X POST http://localhost:8080/api/qa/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "你的问题"}'

# 搜索
curl "http://localhost:8080/api/qa/search?query=关键词&limit=10"

# 统计
curl http://localhost:8080/api/qa/statistics
```

## 📁 项目结构

```
ai-reviewer-base-file-rag/
├── src/main/java/top/yumbo/ai/rag/example/application/
│   ├── KnowledgeQASystemApplication.java    # 主应用类
│   ├── config/
│   │   └── KnowledgeQAProperties.java       # 配置类
│   ├── service/
│   │   ├── KnowledgeQAService.java          # 问答服务
│   │   ├── KnowledgeBaseService.java        # 知识库构建服务
│   │   └── ModelCheckService.java           # 模型检查服务
│   └── controller/
│       └── KnowledgeQAController.java       # REST API 控制器
├── src/main/resources/
│   ├── application.yml                      # 配置文件
│   └── models/                              # 模型文件目录
│       ├── bge-m3/                          # BGE-M3 模型
│       ├── paraphrase-multilingual/         # Paraphrase 模型
│       └── ...
└── data/
    ├── documents/                           # 文档目录
    ├── knowledge-base/                      # 知识库存储
    └── vector-index/                        # 向量索引
```

## ⚙️ 配置说明

### 知识库配置

```yaml
knowledge:
  qa:
    knowledge-base:
      storage-path: ./data/knowledge-base     # 知识库存储路径
      source-path: ./data/documents           # 文档源路径
      rebuild-on-startup: false               # 是否重建
      enable-cache: true                      # 是否启用缓存
```

### 向量检索配置

```yaml
knowledge:
  qa:
    vector-search:
      enabled: true                           # 是否启用
      model:
        search-paths:                         # 模型搜索路径
          - bge-m3                            # BGE-M3 (推荐)
          - multilingual-e5-large
          - paraphrase-multilingual
      similarity-threshold: 0.4               # 相似度阈值
      top-k: 20                               # 返回文档数
```

### 文档处理配置

```yaml
knowledge:
  qa:
    document:
      supported-formats:                      # 支持的格式
        - xlsx
        - xls
        - docx
        - pptx
        - pdf
        - txt
      max-file-size-mb: 200                   # 最大文件大小
      chunk-size: 2000                        # 分块大小
      chunk-overlap: 400                      # 分块重叠
```

## 🌐 API 接口

### POST /api/qa/ask - 问答

**请求**:
```json
{
  "question": "蒙古族婚配情况"
}
```

**响应**:
```json
{
  "question": "蒙古族婚配情况",
  "answer": "根据检索到的数据...",
  "sources": ["file1.xlsx", "file2.docx"],
  "responseTimeMs": 1234
}
```

### GET /api/qa/search - 搜索文档

**参数**:
- `query`: 搜索关键词
- `limit`: 返回数量（默认10）

### GET /api/qa/statistics - 统计信息

**响应**:
```json
{
  "documentCount": 1000,
  "indexedDocumentCount": 950
}
```

### GET /api/qa/health - 健康检查

**响应**:
```json
{
  "status": "UP",
  "message": "知识库问答系统运行正常"
}
```

## 📊 推荐模型

| 模型 | 性能 | 大小 | 推荐场景 |
|------|------|------|---------|
| **BGE-M3** ⭐⭐⭐⭐⭐ | 最佳 | 2.2GB | 生产环境 |
| **Multilingual-E5-Large** ⭐⭐⭐⭐ | 优秀 | 1.3GB | 平衡 |
| **Paraphrase-Multilingual** ⭐⭐⭐ | 良好 | 280MB | 开发测试 |

## 🔧 故障排查

### 模型文件不存在

```bash
# 下载模型到 src/main/resources/models/ 目录
# 或者禁用向量检索：
knowledge:
  qa:
    vector-search:
      enabled: false
```

### 知识库构建失败

- 检查文档路径是否存在
- 确认文件格式是否支持
- 查看详细日志

### 检索结果不准确

- 使用更好的模型（BGE-M3）
- 调整相似度阈值
- 增加返回文档数

## 📚 文档

- [详细使用指南](../md/本地文件RAG/*_知识库问答系统使用指南.md)
- [配置参考](src/main/resources/application.yml)
- [API 文档](#api-接口)

## 🎯 与 ExcelKnowledgeQASystem 的区别

| 特性 | ExcelKnowledgeQASystem | KnowledgeQASystemApplication |
|------|----------------------|------------------------------|
| **框架** | 纯 Java | Spring Boot |
| **文件格式** | 仅 Excel | Excel, Word, PowerPoint, PDF, TXT等 |
| **配置** | 硬编码 | application.yml 配置文件 |
| **API** | 无 | REST API |
| **模型检查** | 无 | 启动时自动检查 |
| **部署** | 命令行工具 | Web 应用 |

## 🚀 部署

### 打包

```bash
mvn clean package
```

### 运行

```bash
java -jar target/ai-reviewer-base-file-rag-1.0.jar
```

### Docker (可选)

```dockerfile
FROM openjdk:17-jdk-slim
COPY target/*.jar app.jar
COPY src/main/resources/models /app/models
ENTRYPOINT ["java","-jar","/app.jar"]
```

## 📝 TODO

- [ ] 添加用户认证
- [ ] 支持多租户
- [ ] 添加 Web UI
- [ ] 实现实时索引更新
- [ ] 支持更多 LLM 提供商

## 📄 许可证

MIT License

## 👥 贡献

欢迎提交 Issue 和 Pull Request！

---

**开发团队**: AI Reviewer Team  
**版本**: 1.0.0  
**日期**: 2025-11-22

