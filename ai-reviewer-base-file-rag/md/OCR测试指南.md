# OCR功能测试说明

## 测试OCR是否正常工作

### 前提条件

1. 已下载语言包（运行 download-tessdata.bat）
2. 已在 start.bat 中启用OCR配置
3. 应用已重启

### 测试步骤

#### 1. 检查启动日志

在应用启动时，查看日志输出，应该看到：

```
✅ Tesseract OCR 可用 (语言: chi_sim+eng)
✅ 选择图片处理策略: Tesseract OCR
TikaDocumentParser initialized with config: maxContentLength=10MB, extractImageMetadata=true, includeImagePlaceholders=true, imageStrategy=Tesseract OCR
```

如果看到 `⚠️ Tesseract OCR 不可用`，说明配置有问题，需要检查：
- ENABLE_OCR 是否设置为 true
- TESSDATA_PREFIX 路径是否正确
- tessdata 目录下是否有语言包文件

#### 2. 上传测试文档

准备一个包含图片的PPTX/DOCX文档：
1. 打开 http://localhost:8080
2. 进入"文档管理"标签页
3. 上传包含图片的文档

#### 3. 查看处理日志

在 `logs/ai-reviewer-rag.log` 中查看处理日志，应该看到：

```
使用Office图片提取器处理PPTX: 测试文档.pptx
开始处理PPTX文件: 测试文档.pptx, 共3张幻灯片
提取图片: slide1_image1.png (245KB)
✅ OCR提取文字 [slide1_image1.png]: 156 字符
✅ PPTX处理完成: 测试文档.pptx
```

如果看到类似日志，说明OCR正在工作。

#### 4. 重建索引

1. 进入"统计信息"标签页
2. 点击"重建索引"按钮
3. 等待处理完成

处理过程中会在日志中显示每个图片的OCR结果。

#### 5. 测试问答

在"智能问答"页面提问，验证是否能够回答基于图片内容的问题。

例如，如果上传的是"节约用水"PPT：
- 提问："为什么要节约用水？"
- 提问："如何节约用水？"

系统应该能够根据图片中提取的文字进行回答。

### 预期结果

✅ **成功**：
- 启动日志显示 OCR 可用
- 处理日志显示提取了图片和文字
- 问答能够使用图片中的内容

❌ **失败**：
- 启动日志显示 OCR 不可用
- 处理日志中没有图片提取信息
- 问答回复"文档内容仅包含图片"

### 常见问题

#### Q: OCR识别速度慢

A: 这是正常的，OCR处理需要时间：
- 小图片（<100KB）：约1-2秒
- 中等图片（100-500KB）：约3-5秒
- 大图片（>500KB）：约5-10秒

建议：批量上传后统一重建索引，避免频繁处理。

#### Q: 识别文字有错误

A: 可能原因：
1. 图片质量差、分辨率低
2. 文字太小或倾斜
3. 背景复杂、对比度低

解决方案：
- 使用更清晰的图片
- 或考虑使用 Vision LLM（需要API Key）

#### Q: 某些图片识别不出来

A: Tesseract主要识别印刷体文字：
- ✅ 适用：海报、PPT截图、书籍扫描
- ❌ 不适用：手写字、艺术字、Logo

对于复杂图片，建议使用 Vision LLM。

### 进阶测试

#### 测试多语言

修改 start.bat 中的语言配置：

```batch
:: 中文+英文+日文
set OCR_LANGUAGE=chi_sim+eng+jpn
```

需要先下载对应的语言包。

#### 测试性能

上传一个包含多张图片的大文档，观察：
1. 处理时间
2. 内存使用
3. CPU使用率

### 调试技巧

#### 1. 查看详细日志

在 start.bat 中添加：

```batch
set JAVA_OPTS=%JAVA_OPTS% -Dlogging.level.top.yumbo.ai.rag.impl.parser=DEBUG
```

#### 2. 测试单个图片

可以使用代码测试单个图片文件：

```java
TesseractOCRStrategy ocr = new TesseractOCRStrategy();
String text = ocr.extractContent(new File("test.png"));
System.out.println(text);
```

#### 3. 查看原始文档内容

在"文档管理"页面查看文档列表，确认文档已被正确解析。

### 获取帮助

如果测试失败，请：
1. 查看完整日志：`logs/ai-reviewer-rag.log`
2. 确认环境配置正确
3. 参考：[OCR配置指南.md](../OCR配置指南.md)

---

**提示**：第一次使用OCR时，Tesseract可能需要加载模型，会稍微慢一些。后续处理会更快。

