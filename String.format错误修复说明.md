# String.format 错误修复说明

## 🐛 问题描述

更新提示词后，调用 AI 时报错：

```
ERROR t.y.a.a.hackathon.ai.BedrockAdapter - 调用 Bedrock 模型失败: Flags = '+ ' 
java.util.IllegalFormatFlagsException: Flags = '+ ' 
	at java.base/java.util.Formatter$FormatSpecifier.checkNumeric(Formatter.java:3307)
	at java.base/java.util.Formatter$FormatSpecifier.checkFloat(Formatter.java:3281)
	...
```

## 🔍 根本原因

### 问题代码
```java
// BedrockAdapter.java:279
String requestBody = buildRequestBody(String.format(userPrompt, data.getContent()));

// HttpBasedAIAdapter.java:116
userMessage.put("content", String.format(config.getUserPrompt(), data.getContent()));
```

### 原因分析
`String.format()` 方法会将字符串中的 `%` 符号识别为格式化标志。当提示词中包含以下内容时会导致错误：

1. **百分比符号**：如 `80%+` 中的 `%+` 
2. **格式化标志**：`+`, `-`, `#`, `0`, `,`, `(` 等
3. **其他特殊字符组合**

### 改进后的提示词中的"陷阱"

```yaml
• 80%+ features working, minor gaps: BASE 13-15 points
  ^^^^^^^^ 这里的 %+ 被 String.format 误认为格式化标志

+ Solves problem in unique way: +2-3 points
^ 加号在某些上下文中也可能引起问题
```

## ✅ 解决方案

将 `String.format()` 改为简单的 `replace()` 方法：

### 修复 1: BedrockAdapter.java

**修改前：**
```java
String requestBody = buildRequestBody(String.format(userPrompt, data.getContent()));
```

**修改后：**
```java
// 使用 replace 而不是 String.format 避免提示词中的特殊字符（如 '+', '%'）被误认为格式化标志
String formattedPrompt = userPrompt.replace("%s", data.getContent());
String requestBody = buildRequestBody(formattedPrompt);
```

### 修复 2: HttpBasedAIAdapter.java

**修改前：**
```java
userMessage.put("content", String.format(config.getUserPrompt(), data.getContent()));
```

**修改后：**
```java
// 使用 replace 而不是 String.format 避免提示词中的特殊字符（如 '+', '%'）被误认为格式化标志
String formattedPrompt = config.getUserPrompt().replace("%s", data.getContent());
userMessage.put("content", formattedPrompt);
```

## 📊 方案对比

| 方案 | 优点 | 缺点 | 适用场景 |
|------|------|------|----------|
| **String.format()** | 功能强大，支持多种格式化 | 对特殊字符敏感，容易出错 | 需要复杂格式化的场景 |
| **replace()** | 简单直接，不会误判特殊字符 | 只能做简单替换 | 只需要简单占位符替换 |
| **MessageFormat** | Java标准，处理参数化消息 | 语法不同（用 {0} 而非 %s） | 国际化消息 |

## 🎯 为什么选择 replace()

1. **需求简单**：我们只需要将 `%s` 替换为项目内容
2. **避免转义**：不需要将提示词中所有 `%` 转义为 `%%`
3. **可读性好**：代码意图清晰
4. **性能足够**：对于单次替换，性能差异可忽略

## 🔄 其他可选方案

### 方案 A: 转义所有 % 符号
```yaml
user-prompt: |-
  • 80%%+ features working, minor gaps: BASE 13-15 points
  ^^^^^^^^ 所有 % 都要写成 %%
```

**缺点**：
- 需要修改整个提示词，将所有 `%` 改为 `%%`
- 容易遗漏，维护困难
- 不直观

### 方案 B: 改用其他占位符
```yaml
user-prompt: |-
  【Please Begin Analysis】
  Project Content: ${content}  # 使用 ${content} 代替 %s
```

```java
String formattedPrompt = userPrompt.replace("${content}", data.getContent());
```

**优点**：
- `${content}` 更具语义化
- 不会与格式化语法冲突

**缺点**：
- 需要修改 YAML 配置
- 与现有 `%s` 约定不一致

## ✅ 验证修复

### 编译检查
```bash
# 已验证，无编译错误
- BedrockAdapter.java: ✅ 通过
- HttpBasedAIAdapter.java: ✅ 通过
```

### 运行测试
运行项目并提交一个包含以下内容的测试项目：
- README 文件
- 源代码文件
- 观察是否能正常生成评审报告

## 📝 注意事项

### ⚠️ 如果将来需要多个占位符

如果将来需要多个占位符（如 `%s` 用于代码，`%s` 用于README），推荐使用命名占位符：

```yaml
user-prompt: |-
  Project README: ${README}
  Project Code: ${CODE}
```

```java
String formattedPrompt = config.getUserPrompt()
    .replace("${README}", data.getReadme())
    .replace("${CODE}", data.getCode());
```

### ✅ 当前占位符使用
- `%s`: 项目完整内容（README + 源代码）
- 位置：提示词末尾 `Project Content: %s`

## 🚀 后续优化建议

1. **统一占位符规范**
   - 建议使用 `${PLACEHOLDER}` 格式
   - 更新文档说明占位符用法

2. **添加单元测试**
   - 测试包含特殊字符的提示词
   - 验证替换逻辑的正确性

3. **配置验证**
   - 启动时检查 user-prompt 是否包含占位符
   - 如果缺少占位符则给出警告

## 📚 相关文件

- ✅ **修复文件 1**: `application-demo/hackathonApplication/src/main/java/top/yumbo/ai/application/hackathon/ai/BedrockAdapter.java`
- ✅ **修复文件 2**: `ai-reviewer-adaptor-ai/src/main/java/top/yumbo/ai/adaptor/ai/HttpBasedAIAdapter.java`
- 📝 **配置文件**: `application-demo/hackathonApplication/src/main/resources/application.yml`
- 📖 **提示词文档**: `application-demo/hackathonApplication/提示词.txt`
- 📖 **改进说明**: `application-demo/hackathonApplication/提示词改进说明.md`

---

**修复时间**: 2025-11-28  
**修复状态**: ✅ 已完成  
**测试状态**: ⏳ 待验证

