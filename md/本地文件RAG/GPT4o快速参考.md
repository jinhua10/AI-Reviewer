# 🚀 GPT-4o/GPT-5 快速参考卡

## ⚡ 3步快速启用

### 1️⃣ 设置 API Key
```bash
# Windows (PowerShell)
$env:OPENAI_API_KEY="sk-your-key-here"

# Linux/Mac
export OPENAI_API_KEY="sk-your-key-here"
```

### 2️⃣ 修改配置
编辑 `application.yml`:
```yaml
knowledge:
  qa:
    llm:
      provider: openai  # 改这里
      model: gpt-4o     # 选择模型
```

### 3️⃣ 运行
```bash
mvn spring-boot:run
```

---

## 📊 模型选择指南

| 模型 | 特点 | 成本/1M tokens | 推荐场景 |
|------|------|----------------|---------|
| **gpt-4o** ⭐ | 最新多模态 | $2.5/$10 | 🎯 **生产环境** |
| **gpt-4o-mini** | 经济快速 | $0.15/$0.6 | 💰 **成本敏感** |
| **gpt-4-turbo** | 高性能 | $10/$30 | 🧠 **复杂推理** |
| **gpt-3.5-turbo** | 最便宜 | $0.5/$1.5 | 🧪 **开发测试** |

---

## 🎯 完整配置示例

### 推荐配置（GPT-4o）
```yaml
knowledge:
  qa:
    llm:
      provider: openai
      api-key: ${OPENAI_API_KEY:}
      api-url: https://api.openai.com/v1/chat/completions
      model: gpt-4o
      max-context-length: 20000
      max-doc-length: 5000
```

### 开发配置（GPT-3.5）
```yaml
knowledge:
  qa:
    llm:
      provider: openai
      model: gpt-3.5-turbo  # 更便宜
```

### 未来 GPT-5
```yaml
knowledge:
  qa:
    llm:
      provider: openai
      model: gpt-5  # 发布后直接可用
```

---

## 🔧 使用代理

如果无法访问 OpenAI 官方：

```yaml
knowledge:
  qa:
    llm:
      provider: openai
      api-url: https://your-proxy.com/v1/chat/completions
      api-key: ${PROXY_API_KEY:}
```

---

## ✅ 验证配置

**启动日志**:
```
🤖 创建 OpenAI LLM 客户端
   - 模型: gpt-4o
✅ OpenAI LLM 客户端初始化完成
```

**测试问答**:
```bash
curl -X POST http://localhost:8080/api/qa/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "你好"}'
```

---

## ⚠️ 常见错误

### 401 Unauthorized
```
❌ 原因: API Key 无效
✅ 解决: 检查 OPENAI_API_KEY 环境变量
```

### 429 Too Many Requests
```
❌ 原因: 请求频率过高
✅ 解决: 降低请求频率或升级账号
```

### 余额不足
```
❌ 原因: 账户余额不足
✅ 解决: https://platform.openai.com/account/billing
```

---

## 💰 成本估算

**1000次问答** (基于10份文档):
- gpt-4o: ~$36
- gpt-4o-mini: ~$2
- gpt-3.5-turbo: ~$7

---

## 📚 获取 API Key

1. 访问: https://platform.openai.com
2. 登录账号
3. 进入: https://platform.openai.com/api-keys
4. 点击 "Create new secret key"
5. 复制 API Key (sk-xxx...)

---

## 🎉 就这么简单！

完整文档: `20251123010000_使用GPT4o_GPT5完整指南.md`

