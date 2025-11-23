# 🚀 OCR功能快速启动指南

## 第一步：下载语言包（只需一次）

在`release`目录下双击运行：

```
download-tessdata.bat
```

等待下载完成（约16MB）。

---

## 第二步：启动应用

双击运行：

```
start.bat
```

**✅ OCR已默认启用！** 无需修改任何配置。

---

## 第三步：验证OCR生效

查看启动日志，应该看到：

```
📊 TikaDocumentParser 初始化完成:
  └─ 图片处理策略: Tesseract OCR

🔍 OCR配置:
  ├─ ENABLE_OCR: true
  ├─ TESSDATA_PREFIX: D:\...\release\tessdata
  └─ OCR_LANGUAGE: chi_sim+eng
```

---

## 第四步：重建知识库

1. 访问：http://localhost:8080
2. 进入"统计信息"标签
3. 点击"重建索引"
4. 等待处理完成

处理时会看到：

```
📷 提取图片: slide1_image1.png (245KB)
✅ 图片内容提取成功: slide1_image1.png -> 156 字符
```

---

## 第五步：测试效果

回到"智能问答"页面，上传包含图片的PPTX/DOCX文档，然后提问！

---

## 🔧 故障排查

### 如果OCR没生效？

运行诊断脚本：

```batch
check-ocr.bat
```

它会自动检查：
- ✅ tessdata目录和语言包
- ✅ start.bat配置
- ✅ JAR文件状态
- ✅ 日志中的OCR信息

### 查看详细指南

- `图片识别快速启用.md` - 完整使用指南
- `OCR诊断指南.md` - 详细问题排查

---

## 💡 提示

1. **首次使用必须下载语言包**
   - 运行 `download-tessdata.bat`
   - 确保网络连接正常

2. **重建索引才能应用OCR**
   - 上传新文档会自动使用OCR
   - 已上传的文档需要重建索引

3. **查看日志确认效果**
   - 日志位置：`logs/ai-reviewer-rag.log`
   - 搜索关键字：OCR、Tesseract、图片

4. **禁用OCR（如果需要）**
   - 编辑 `start.bat`
   - 在这三行前添加 `::`
     ```batch
     :: set ENABLE_OCR=true
     :: set TESSDATA_PREFIX=%~dp0tessdata
     :: set OCR_LANGUAGE=chi_sim+eng
     ```

---

## ❓ 常见问题

**Q: 语言包下载失败？**

A: 手动下载并放到tessdata目录：
- 中文：https://github.com/tesseract-ocr/tessdata/raw/main/chi_sim.traineddata
- 英文：https://github.com/tesseract-ocr/tessdata/raw/main/eng.traineddata

**Q: OCR识别效果不好？**

A: 确保图片质量：
- 分辨率 ≥ 300 DPI
- 文字清晰、对比度高
- 背景简单

**Q: 处理速度慢？**

A: 正常现象，OCR需要时间：
- 小图片：0.5-1秒
- 中等图片：1-3秒
- 大图片：3-10秒

批量上传后统一重建索引会更快。

---

## 📚 完整功能

除了OCR，系统还支持：

- ✅ 多格式文档解析（PDF、Word、Excel、PPT、Markdown等）
- ✅ 智能分块（大文档自动分段）
- ✅ 关键词检索（Lucene）
- ✅ 向量检索（可选，语义搜索）
- ✅ AI问答（OpenAI、通义千问等）

---

**🎉 开始使用吧！**

有问题？查看 `OCR诊断指南.md` 或运行 `check-ocr.bat`

