# OCR图片文字识别配置指南

## 功能说明

本系统支持从文档中的图片提取文字内容，特别适用于：
- PPTX演示文稿中的图片
- DOCX文档中的图片
- XLSX表格中的图片
- PDF中的图片

## 快速开始

### 1. 下载Tesseract语言包

创建 `tessdata` 目录用于存放语言包：

```bash
# Windows
mkdir tessdata
cd tessdata

# 下载中文简体语言包
curl -L -o chi_sim.traineddata https://github.com/tesseract-ocr/tessdata/raw/main/chi_sim.traineddata

# 下载英文语言包
curl -L -o eng.traineddata https://github.com/tesseract-ocr/tessdata/raw/main/eng.traineddata
```

### 2. 配置环境变量

有两种方式配置OCR：

#### 方式一：通过启动脚本配置（推荐）

编辑 `start.bat` 或 `start.sh`，添加：

```batch
@echo off
:: 启用OCR功能
set ENABLE_OCR=true

:: 设置tessdata路径（改为你的实际路径）
set TESSDATA_PREFIX=D:\Jetbrains\hackathon\AI-Reviewer\ai-reviewer-base-file-rag\tessdata

:: 设置OCR语言（中文+英文）
set OCR_LANGUAGE=chi_sim+eng

:: 启动应用
java -jar ai-reviewer-base-file-rag-1.0.jar
```

#### 方式二：通过系统环境变量配置

Windows系统：
1. 右键"此电脑" -> "属性" -> "高级系统设置" -> "环境变量"
2. 新建用户变量：
   - `ENABLE_OCR` = `true`
   - `TESSDATA_PREFIX` = `D:\path\to\tessdata`
   - `OCR_LANGUAGE` = `chi_sim+eng`

### 3. 验证配置

重启应用后，查看日志，应该看到：

```
✅ Tesseract OCR 可用 (语言: chi_sim+eng)
✅ 选择图片处理策略: Tesseract OCR
```

## 语言包下载地址

### 常用语言

| 语言 | 代码 | 下载链接 |
|------|------|---------|
| 简体中文 | chi_sim | https://github.com/tesseract-ocr/tessdata/raw/main/chi_sim.traineddata |
| 繁体中文 | chi_tra | https://github.com/tesseract-ocr/tessdata/raw/main/chi_tra.traineddata |
| 英文 | eng | https://github.com/tesseract-ocr/tessdata/raw/main/eng.traineddata |
| 日文 | jpn | https://github.com/tesseract-ocr/tessdata/raw/main/jpn.traineddata |
| 韩文 | kor | https://github.com/tesseract-ocr/tessdata/raw/main/kor.traineddata |

### 多语言支持

如需同时识别多种语言，用加号连接：

```batch
set OCR_LANGUAGE=chi_sim+eng+jpn
```

## 目录结构

```
ai-reviewer-base-file-rag/
├── tessdata/                      # 语言包目录
│   ├── chi_sim.traineddata       # 简体中文
│   ├── eng.traineddata           # 英文
│   └── ...
├── start.bat                     # 启动脚本（配置环境变量）
└── ai-reviewer-base-file-rag-1.0.jar
```

## 使用示例

### 上传PPTX文档

1. 打开Web界面：http://localhost:8080
2. 进入"文档管理"标签页
3. 上传包含图片的PPTX文件
4. 系统会自动识别图片中的文字

### 查看提取结果

在"统计信息"页面点击"重建索引"后，系统会重新处理所有文档。

查看日志，应该看到类似输出：

```
提取图片: slide1_image1.png (245KB)
✅ OCR提取文字 [slide1_image1.png]: 156 字符
```

### 问答测试

在"智能问答"页面提问，系统会使用图片中提取的文字进行回答。

例如，上传"节约用水"PPTX后，可以提问：
- "为什么要节约用水？"
- "如何节约用水？"

## 故障排除

### 问题1：OCR不可用

**现象**：日志显示 `⚠️ Tesseract OCR 不可用`

**解决**：
1. 检查环境变量 `ENABLE_OCR` 是否设置为 `true`
2. 重启应用

### 问题2：找不到语言包

**现象**：日志显示 `Error opening data file`

**解决**：
1. 确认 `tessdata` 目录存在
2. 确认语言包文件已下载（如 `chi_sim.traineddata`）
3. 检查 `TESSDATA_PREFIX` 路径是否正确（使用绝对路径）
4. 注意路径中不要有中文字符

### 问题3：识别效果不好

**优化建议**：
1. 使用清晰的图片（分辨率 > 300dpi）
2. 图片中的文字应足够大
3. 背景简单，对比度高
4. 尝试不同的语言包组合

### 问题4：处理速度慢

**说明**：
- OCR处理需要时间，特别是大图片
- 建议：先上传文档，然后在空闲时间重建索引

## 高级配置

### 使用Vision LLM（云端AI识图）

如果有OpenAI API或兼容的API，可以使用更强大的视觉识别：

```batch
set VISION_LLM_API_KEY=your-api-key
set VISION_LLM_MODEL=gpt-4o
set VISION_LLM_ENDPOINT=https://api.openai.com/v1
```

系统会优先使用Vision LLM，失败时自动降级到OCR。

### 混合模式

同时配置OCR和Vision LLM：

```batch
set ENABLE_OCR=true
set TESSDATA_PREFIX=D:\path\to\tessdata
set VISION_LLM_API_KEY=your-api-key
```

系统会智能选择：
1. 优先尝试Vision LLM（语义理解更好）
2. 失败时使用Tesseract OCR（本地处理）
3. 都不可用时使用占位符

## 性能优化

### 批量上传

如果要上传多个文档：
1. 先上传所有文档
2. 统一点击"重建索引"
3. 避免频繁重建

### 缓存

系统会缓存OCR结果，重复处理同一文档时会更快。

## 常见问题

### Q: 需要安装Tesseract软件吗？

A: 不需要。本系统使用tess4j库，已包含Tesseract引擎。只需下载语言包即可。

### Q: 语言包文件很大吗？

A: 每个语言包约 4-15MB。中文简体约 12MB，英文约 4MB。

### Q: 可以识别手写字吗？

A: Tesseract主要用于印刷体文字。手写字识别效果较差，建议使用Vision LLM。

### Q: 识别准确率如何？

A: 取决于图片质量：
- 高清晰印刷体：95%+
- 普通质量：80-90%
- 低质量/手写：<60%

## 相关链接

- Tesseract官方文档：https://github.com/tesseract-ocr/tesseract
- Tesseract语言包：https://github.com/tesseract-ocr/tessdata
- tess4j文档：https://github.com/nguyenq/tess4j

## 技术支持

如有问题，请查看日志文件：
- `logs/ai-reviewer-rag.log`

或提交Issue到项目仓库。

