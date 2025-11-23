# OCR功能诊断指南

## 快速检查清单

### ✅ 检查1：语言包是否已下载

运行以下命令检查tessdata目录：

```powershell
dir release\tessdata
```

**期望结果：**
```
chi_sim.traineddata  (约12MB)
eng.traineddata      (约4MB)
```

**如果没有：** 运行 `download-tessdata.bat`

---

### ✅ 检查2：OCR环境变量是否设置

查看 `release\start.bat`，确认以下三行**没有**被注释（前面没有 `::`）：

```batch
set ENABLE_OCR=true
set TESSDATA_PREFIX=%~dp0tessdata
set OCR_LANGUAGE=chi_sim+eng
```

**如果被注释：** 删除行首的 `:: `

---

### ✅ 检查3：查看启动日志

启动应用后，查看日志文件 `logs\ai-reviewer-rag.log`，搜索以下关键字：

```
TikaDocumentParser 初始化完成
```

**期望看到：**

```log
📊 TikaDocumentParser 初始化完成:
  ├─ 最大内容长度: 10MB
  ├─ 提取图片元数据: true
  ├─ 图片占位符: true
  └─ 图片处理策略: Tesseract OCR

🔍 OCR配置:
  ├─ ENABLE_OCR: true
  ├─ TESSDATA_PREFIX: D:\...\release\tessdata
  └─ OCR_LANGUAGE: chi_sim+eng
```

**如果看到 "PlaceholderImageStrategy"：** OCR未启用

---

### ✅ 检查4：重建索引时的日志

在重建知识库索引时，查看日志中的图片处理信息：

```log
开始处理PPTX文件: 节约用水.pptx, 共10张幻灯片
📷 提取图片: slide1_image1.png (245KB)
✅ 图片内容提取成功: slide1_image1.png -> 156 字符
```

**期望行为：**
- 看到 📷 提取图片
- 看到 ✅ 图片内容提取成功
- 显示提取的字符数

**问题标志：**
- ⚠️ 图片内容为空
- ⚠️ Tesseract OCR 不可用
- OCR处理失败

---

## 常见问题排查

### 问题1：看到 "⚠️ Tesseract OCR 不可用"

**可能原因：**
- tess4j 依赖缺失
- tessdata 路径配置错误

**解决方法：**

1. 检查JAR包是否包含tess4j依赖：
```powershell
jar -tf release\ai-reviewer-base-file-rag-1.0.jar | Select-String "tess4j"
```

2. 检查tessdata目录路径：
```batch
echo %TESSDATA_PREFIX%
dir %TESSDATA_PREFIX%
```

3. 确认路径正确且包含语言包文件

---

### 问题2：看到 "图片处理策略: PlaceholderImageStrategy"

**原因：** OCR环境变量未设置或未生效

**解决方法：**

1. 确认 `start.bat` 中的OCR配置未被注释
2. 完全关闭应用（不是最小化）
3. 重新运行 `start.bat`
4. 查看启动日志确认OCR已启用

---

### 问题3：图片内容为空

**可能原因：**
- 图片中没有文字
- 图片质量太差（模糊、分辨率低）
- 图片格式不支持
- OCR语言包不匹配

**解决方法：**

1. 确认图片中确实有清晰的文字
2. 检查图片格式（支持：PNG、JPG、BMP、TIFF）
3. 如果是纯英文图片，尝试只用英文语言包：
```batch
set OCR_LANGUAGE=eng
```

4. 如果是中文图片，确保使用中文语言包：
```batch
set OCR_LANGUAGE=chi_sim+eng
```

---

### 问题4：OCR识别效果差

**优化建议：**

1. **提高图片质量**
   - 使用高分辨率图片（≥300 DPI）
   - 确保文字清晰、对比度高
   - 避免复杂背景

2. **优化文档格式**
   - 优先使用原始文档格式（DOCX、PPTX）而非扫描件
   - 如果必须使用图片，使用PNG而非JPG

3. **调整OCR参数**
   
   编辑 `TesseractOCRStrategy.java`，可以调整：
   ```java
   tesseract.setPageSegMode(1);  // 自动分段模式
   ```

---

### 问题5：处理速度慢

**原因：** OCR处理需要时间，特别是大图片或多图片文档

**优化方法：**

1. **增加内存**
   
   编辑 `start.bat`：
   ```batch
   set JAVA_OPTS=%JAVA_OPTS% -Xms1g -Xmx4g
   ```

2. **批量处理**
   
   上传所有文档后，统一重建索引，而非逐个上传

3. **使用Vision LLM**（可选）
   
   如果有OpenAI API密钥，Vision LLM识别速度更快：
   ```batch
   set VISION_LLM_API_KEY=sk-your-key
   set VISION_LLM_MODEL=gpt-4o
   ```

---

## 诊断命令

### 查看完整的OCR配置

在PowerShell中运行：

```powershell
$env:ENABLE_OCR
$env:TESSDATA_PREFIX
$env:OCR_LANGUAGE
```

### 查看最近的OCR日志

```powershell
Get-Content logs\ai-reviewer-rag.log -Tail 100 | Select-String "OCR|图片|Tesseract"
```

### 测试tessdata路径

```powershell
Test-Path release\tessdata\chi_sim.traineddata
Test-Path release\tessdata\eng.traineddata
```

### 查看文档处理日志

```powershell
Get-Content logs\ai-reviewer-rag.log | Select-String "处理PPTX|处理DOCX|提取图片"
```

---

## 手动测试OCR

如果想单独测试OCR功能，可以：

1. 准备一张包含文字的图片
2. 放到 `release\data\test-image.png`
3. 查看日志中的识别结果

---

## 高级配置

### 使用不同的OCR引擎

除了Tesseract，还可以配置：

1. **PaddleOCR**（更好的中文识别）
   - 需要额外安装
   - 修改代码使用PaddleOCR策略

2. **Vision LLM**（最强大）
   - 支持手写、复杂图表
   - 需要API密钥
   - 配置方法见下文

### Vision LLM 配置

在 `start.bat` 中添加：

```batch
:: Vision LLM 配置（可选，识别效果最好）
set VISION_LLM_API_KEY=sk-your-openai-api-key
set VISION_LLM_MODEL=gpt-4o
set VISION_LLM_ENDPOINT=https://api.openai.com/v1
```

系统会优先使用Vision LLM，失败时自动降级到OCR。

---

## 联系支持

如果按照以上步骤仍无法解决问题：

1. 收集以下信息：
   - 启动日志（前100行）
   - 处理文档时的日志
   - tessdata 目录截图
   - start.bat 的OCR配置部分

2. 创建 Issue 并附上上述信息

---

## 验证清单

在报告问题前，请确认：

- [ ] tessdata 目录存在且包含语言包文件
- [ ] start.bat 中的OCR配置已启用（没有被注释）
- [ ] 应用已完全重启（不是重载）
- [ ] 启动日志显示 "图片处理策略: Tesseract OCR"
- [ ] 重建索引时看到图片提取日志
- [ ] 测试了简单的中英文图片

---

**提示：** 如果一切配置正确但OCR仍不工作，尝试：
1. 删除 `data\vector-index` 目录
2. 删除 `data\knowledge-base` 目录
3. 完全重建知识库

这将清除所有缓存，确保使用最新的配置。

