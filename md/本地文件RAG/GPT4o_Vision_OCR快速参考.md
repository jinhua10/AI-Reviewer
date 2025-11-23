# 🚀 GPT-4o Vision OCR 快速参考

## ⚡ 3步启用

### 1️⃣ 设置 API Key
```bash
export OPENAI_API_KEY="sk-your-key"
```

### 2️⃣ 配置
```yaml
knowledge:
  qa:
    image-processing:
      strategy: vision-llm
      vision-llm:
        enabled: true
        model: gpt-4o
```

### 3️⃣ 运行
```bash
mvn spring-boot:run
```

---

## 📊 方案对比

| 特性 | GPT-4o Vision | Tesseract OCR |
|------|--------------|--------------|
| 打印文字 | 98% ⭐⭐⭐⭐⭐ | 95% ⭐⭐⭐⭐ |
| 手写文字 | 90% ⭐⭐⭐⭐⭐ | 60% ⭐⭐ |
| 图表理解 | ✅ 支持 | ❌ 不支持 |
| 配置 | 简单 | 复杂 |
| 成本 | $0.007/张 | 免费 |

---

## 💰 成本

| 模型 | 每张图片 | 100张 |
|------|---------|-------|
| **gpt-4o** | $0.007 | $0.70 |
| **gpt-4-turbo** | $0.02 | $2.00 |
| **Tesseract** | $0 | $0 |

---

## 🎯 推荐配置

### 生产环境（混合模式）
```yaml
knowledge:
  qa:
    image-processing:
      strategy: hybrid      # 智能选择
      enable-ocr: true      # 简单 → OCR（免费）
      vision-llm:
        enabled: true       # 复杂 → Vision（付费）
        model: gpt-4o
```

### 成本优先
```yaml
strategy: ocr           # 只用 Tesseract
```

### 质量优先
```yaml
strategy: vision-llm    # 只用 GPT-4o
model: gpt-4o
```

---

## ✅ 验证

**启动日志**:
```
✅ Vision LLM 策略可用
✅ 图片处理策略已激活: Vision LLM (gpt-4o)
```

**测试**:
```bash
curl -X POST http://localhost:8080/api/files/index \
  -F "file=@document-with-images.pdf"
```

---

## 🎯 场景选择

```
打印文字 → Tesseract OCR
手写文字 → GPT-4o Vision ⭐
复杂图表 → GPT-4o Vision ⭐⭐
多语言 → GPT-4o Vision ⭐⭐
批量处理 → Tesseract OCR
生产环境 → 混合模式 ⭐⭐⭐
```

---

## 📚 完整文档

`20251123011000_使用GPT4o_Vision进行图片OCR指南.md`

