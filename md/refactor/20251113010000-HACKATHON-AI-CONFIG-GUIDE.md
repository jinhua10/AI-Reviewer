# 黑客松 AI 服务配置指南

> **版本**: 2.0  
> **更新时间**: 2025-11-13  
> **适用于**: AI-Reviewer Hackathon 模式

---

## 📋 目录

1. [快速开始](#快速开始)
2. [配置方式](#配置方式)
3. [支持的 AI 服务](#支持的-ai-服务)
4. [配置示例](#配置示例)
5. [常见问题](#常见问题)

---

## 🚀 快速开始

### 最简单的方式：使用环境变量

```bash
# 设置 AI 服务提供商
set AI_PROVIDER=deepseek

# 设置 API Key
set AI_API_KEY=your-api-key-here

# 运行黑客松评分
java -jar hackathon-ai.jar hackathon ^
  --github-url https://github.com/user/repo ^
  --team "Team Name" ^
  --output score.json
```

**就这么简单！** 🎉

---

## 🔧 配置方式

黑客松支持 **3 种配置方式**，优先级从高到低：

### 1. 系统属性（最高优先级）⭐⭐⭐

通过命令行 `-D` 参数传递：

```bash
java -Dai.provider=openai ^
     -Dai.apiKey=sk-xxx ^
     -Dai.model=gpt-4 ^
     -jar hackathon-ai.jar hackathon ^
     --github-url https://github.com/user/repo ^
     --team "Team Name"
```

**优点**: 
- ✅ 最高优先级
- ✅ 临时覆盖其他配置
- ✅ 适合快速测试

---

### 2. 环境变量 ⭐⭐

在 Windows 中设置：

```cmd
set AI_PROVIDER=deepseek
set AI_API_KEY=your-api-key
set AI_MODEL=deepseek-chat
```

在 Linux/Mac 中设置：

```bash
export AI_PROVIDER=deepseek
export AI_API_KEY=your-api-key
export AI_MODEL=deepseek-chat
```

**优点**: 
- ✅ 持久化配置
- ✅ 适合开发环境
- ✅ 不会泄露到代码中

---

### 3. 配置文件 `config.yaml` ⭐

在项目根目录创建 `config.yaml`：

```yaml
aiService:
  provider: deepseek
  apiKey: your-api-key-here
  model: deepseek-chat
  maxTokens: 4000
  temperature: 0.3
  maxRetries: 3
```

**优点**: 
- ✅ 配置集中管理
- ✅ 支持团队共享（注意不要提交 API Key）
- ✅ 支持高级配置

---

## 🤖 支持的 AI 服务

### 1. DeepSeek（默认）✅

**配置**:
```bash
set AI_PROVIDER=deepseek
set AI_API_KEY=your-deepseek-api-key
```

**或使用 config.yaml**:
```yaml
aiService:
  provider: deepseek
  apiKey: your-deepseek-api-key
  model: deepseek-chat
```

**获取 API Key**: https://platform.deepseek.com/

---

### 2. OpenAI (GPT-4) ✅

**配置**:
```bash
set AI_PROVIDER=openai
set AI_API_KEY=sk-your-openai-api-key
set AI_MODEL=gpt-4
```

**或使用 config.yaml**:
```yaml
aiService:
  provider: openai
  apiKey: sk-your-openai-api-key
  model: gpt-4
  baseUrl: https://api.openai.com/v1/chat/completions
```

**支持的模型**:
- `gpt-4` - 推荐
- `gpt-4-turbo`
- `gpt-3.5-turbo`

**获取 API Key**: https://platform.openai.com/

---

### 3. Claude (Anthropic) ✅

**配置**:
```bash
set AI_PROVIDER=claude
set AI_API_KEY=your-anthropic-api-key
set AI_MODEL=claude-3-sonnet-20240229
```

**或使用别名**:
```bash
set AI_PROVIDER=anthropic
```

**或使用 config.yaml**:
```yaml
aiService:
  provider: claude
  apiKey: your-anthropic-api-key
  model: claude-3-sonnet-20240229
```

**支持的模型**:
- `claude-3-opus-20240229` - 最强
- `claude-3-sonnet-20240229` - 推荐
- `claude-3-haiku-20240307` - 最快

**获取 API Key**: https://console.anthropic.com/

---

### 4. Gemini (Google) ✅

**配置**:
```bash
set AI_PROVIDER=gemini
set AI_API_KEY=your-google-api-key
set AI_MODEL=gemini-pro
```

**或使用别名**:
```bash
set AI_PROVIDER=google
```

**或使用 config.yaml**:
```yaml
aiService:
  provider: gemini
  apiKey: your-google-api-key
  model: gemini-pro
```

**支持的模型**:
- `gemini-pro` - 推荐
- `gemini-1.5-pro`

**获取 API Key**: https://makersuite.google.com/

---

### 5. AWS Bedrock ✅

**配置**:
```bash
set AI_PROVIDER=bedrock
set AWS_REGION=us-east-1
set AWS_ACCESS_KEY_ID=your-access-key
set AWS_SECRET_ACCESS_KEY=your-secret-key
```

**或使用别名**:
```bash
set AI_PROVIDER=aws
```

**或使用 config.yaml**:
```yaml
aiService:
  provider: bedrock
  region: us-east-1
  model: anthropic.claude-3-sonnet-20240229-v1:0

# AWS 凭证通过环境变量设置
```

**支持的模型**:
- `anthropic.claude-v2`
- `anthropic.claude-3-sonnet-20240229-v1:0`
- `anthropic.claude-3-opus-20240229-v1:0`

**配置 AWS 凭证**: https://docs.aws.amazon.com/bedrock/

---

## 📝 配置示例

### 示例1: 使用 DeepSeek（推荐新手）

```cmd
REM 设置配置
set AI_PROVIDER=deepseek
set AI_API_KEY=sk-abc123def456

REM 运行黑客松
java -jar hackathon-ai.jar hackathon ^
  --github-url https://github.com/myteam/hackathon-project ^
  --team "Dream Team" ^
  --output score.json
```

---

### 示例2: 使用 OpenAI GPT-4

```cmd
REM 设置配置
set AI_PROVIDER=openai
set AI_API_KEY=sk-proj-xxx
set AI_MODEL=gpt-4

REM 运行黑客松
java -jar hackathon-ai.jar hackathon ^
  --github-url https://github.com/myteam/hackathon-project ^
  --team "Innovation Squad" ^
  --output gpt4-score.json
```

---

### 示例3: 使用 Claude（高质量分析）

```cmd
REM 设置配置
set AI_PROVIDER=claude
set AI_API_KEY=sk-ant-xxx
set AI_MODEL=claude-3-sonnet-20240229

REM 运行黑客松
java -jar hackathon-ai.jar hackathon ^
  --github-url https://github.com/myteam/hackathon-project ^
  --team "Claude Lovers" ^
  --output claude-score.json
```

---

### 示例4: 使用本地目录（不需要 Git）

```cmd
REM 设置 AI 配置
set AI_PROVIDER=deepseek
set AI_API_KEY=your-api-key

REM 使用本地目录
java -jar hackathon-ai.jar hackathon ^
  --directory "D:\Projects\my-hackathon-project" ^
  --team "Local Team" ^
  --output score.json
```

---

### 示例5: 使用配置文件

**创建 `config.yaml`**:
```yaml
aiService:
  provider: openai
  apiKey: sk-proj-your-key
  model: gpt-4
  maxTokens: 8000
  temperature: 0.3
  maxRetries: 3

fileSystem:
  includePatterns:
    - "*.java"
    - "*.py"
    - "*.js"
    - "*.md"
    - "pom.xml"
    - "package.json"
  excludePatterns:
    - "*test*"
    - "*.class"
    - "node_modules"
    - "target"

cache:
  enabled: true
  ttlHours: 24
```

**运行**:
```cmd
java -jar hackathon-ai.jar hackathon ^
  --github-url https://github.com/myteam/project ^
  --team "Config Masters"
```

---

## 🔑 完整配置参数

### 环境变量列表

| 环境变量 | 说明 | 默认值 | 示例 |
|---------|------|--------|------|
| `AI_PROVIDER` | AI 服务提供商 | `deepseek` | `openai`, `claude`, `gemini` |
| `AI_API_KEY` | API 密钥 | 无（必需） | `sk-xxx` |
| `AI_MODEL` | AI 模型 | 根据 provider | `gpt-4`, `claude-3-sonnet` |
| `AI_BASE_URL` | API 基础 URL | 根据 provider | 自定义 API 端点 |
| `AI_MAX_TOKENS` | 最大 Token 数 | `4000` | `8000` |
| `AI_TEMPERATURE` | 温度参数 | `0.3` | `0.0-2.0` |
| `AI_MAX_RETRIES` | 最大重试次数 | `3` | `5` |
| `AWS_REGION` | AWS 区域（Bedrock） | `us-east-1` | `us-west-2` |
| `AWS_ACCESS_KEY_ID` | AWS 访问密钥 | 无 | - |
| `AWS_SECRET_ACCESS_KEY` | AWS 密钥 | 无 | - |

### 系统属性列表

使用 `-D` 参数传递：

| 系统属性 | 环境变量等价 |
|---------|-------------|
| `-Dai.provider=xxx` | `AI_PROVIDER` |
| `-Dai.apiKey=xxx` | `AI_API_KEY` |
| `-Dai.model=xxx` | `AI_MODEL` |
| `-Dai.baseUrl=xxx` | `AI_BASE_URL` |
| `-Dai.maxTokens=xxx` | `AI_MAX_TOKENS` |
| `-Dai.temperature=xxx` | `AI_TEMPERATURE` |

---

## ✅ 验证配置

### 方法1: 查看启动日志

运行黑客松时，会显示当前配置：

```
[INFO] 正在加载配置...
[INFO] 配置加载成功: provider=deepseek, model=deepseek-chat
[INFO] AI-Reviewer v2.0 已启动
[INFO] AI 服务: deepseek (model: deepseek-chat)
```

### 方法2: 使用帮助命令

```bash
java -jar hackathon-ai.jar hackathon --help
```

---

## 🎯 最佳实践

### 1. 使用环境变量（推荐）⭐⭐⭐

```cmd
REM 一次性设置
set AI_PROVIDER=deepseek
set AI_API_KEY=your-key

REM 多次使用
java -jar hackathon-ai.jar hackathon --github-url ...
java -jar hackathon-ai.jar hackathon --github-url ...
```

**优点**: 配置一次，多次使用

---

### 2. 不同场景使用不同 AI

```cmd
REM 快速评分使用 DeepSeek（便宜快速）
set AI_PROVIDER=deepseek
java -jar hackathon-ai.jar hackathon --github-url ... --output quick.json

REM 详细评分使用 GPT-4（质量高）
set AI_PROVIDER=openai
set AI_MODEL=gpt-4
java -jar hackathon-ai.jar hackathon --github-url ... --output detailed.json
```

---

### 3. 使用配置文件管理团队配置

创建 `config.yaml` 并加入版本控制（**注意**: 不要提交 API Key）：

```yaml
aiService:
  provider: deepseek  # 团队统一使用的 AI
  # apiKey: 通过环境变量设置
  model: deepseek-chat
  maxTokens: 4000

fileSystem:
  includePatterns:
    - "*.java"
    - "*.md"
  excludePatterns:
    - "*test*"
```

然后每个开发者设置自己的 API Key：
```bash
set AI_API_KEY=my-personal-key
```

---

## ❓ 常见问题

### Q1: 如何切换 AI 服务？

**A**: 只需修改 `AI_PROVIDER` 环境变量：

```cmd
set AI_PROVIDER=openai
set AI_API_KEY=your-openai-key
```

---

### Q2: 配置文件在哪里？

**A**: 在以下位置查找（按优先级）：
1. 项目根目录 `config.yaml`
2. Classpath 根目录 `config.yaml`

如果没有配置文件，会使用默认值。

---

### Q3: API Key 如何保密？

**A**: 推荐方式：

1. **使用环境变量**（最安全）:
   ```cmd
   set AI_API_KEY=your-key
   ```

2. **不要提交到 Git**:
   ```gitignore
   config.yaml
   .env
   ```

3. **使用系统属性**（临时）:
   ```cmd
   java -Dai.apiKey=xxx -jar hackathon-ai.jar ...
   ```

---

### Q4: 提示 "API Key 未配置" 错误？

**A**: 确保设置了环境变量：

```cmd
REM 检查是否设置
echo %AI_API_KEY%

REM 如果为空，设置它
set AI_API_KEY=your-actual-api-key
```

或使用系统属性：
```cmd
java -Dai.apiKey=your-key -jar hackathon-ai.jar ...
```

---

### Q5: 支持自定义 API 端点吗？

**A**: 支持！使用 `AI_BASE_URL`：

```cmd
set AI_PROVIDER=openai
set AI_BASE_URL=https://my-custom-api.com/v1/chat/completions
set AI_API_KEY=your-key
```

---

### Q6: 如何使用多个 AI 对比评分？

**A**: 运行多次，每次使用不同的 AI：

```cmd
REM DeepSeek 评分
set AI_PROVIDER=deepseek
java -jar hackathon-ai.jar hackathon --github-url ... --output deepseek-score.json

REM GPT-4 评分
set AI_PROVIDER=openai
java -jar hackathon-ai.jar hackathon --github-url ... --output gpt4-score.json

REM Claude 评分
set AI_PROVIDER=claude
java -jar hackathon-ai.jar hackathon --github-url ... --output claude-score.json
```

---

### Q7: AWS Bedrock 如何配置？

**A**: 需要设置 AWS 凭证：

```cmd
set AI_PROVIDER=bedrock
set AWS_REGION=us-east-1
set AWS_ACCESS_KEY_ID=your-access-key
set AWS_SECRET_ACCESS_KEY=your-secret-key
```

或使用 AWS CLI 配置：
```cmd
aws configure
```

---

### Q8: 如何调整 AI 分析的详细程度？

**A**: 调整 `maxTokens` 和 `temperature`：

```yaml
aiService:
  maxTokens: 8000    # 更详细的分析
  temperature: 0.3   # 0 = 精确，1 = 创意
```

或使用环境变量：
```cmd
set AI_MAX_TOKENS=8000
set AI_TEMPERATURE=0.3
```

---

## 📋 配置检查清单

在运行黑客松之前，确认：

- [ ] 已设��� `AI_PROVIDER`（或使用默认 deepseek）
- [ ] 已设置 `AI_API_KEY`（必需）
- [ ] API Key 有效且有足够额度
- [ ] 网络可以访问 AI 服务
- [ ] 已安装 Java 17+
- [ ] hackathon-ai.jar 文件存在

---

## 🎓 完整示例：黑客松评分

### 场景: 使用 DeepSeek 评估 GitHub 项目

```cmd
@echo off
echo ================================
echo 黑客松项目自动评分
echo ================================
echo.

REM 1. 配置 AI 服务
echo [1/4] 配置 AI 服务...
set AI_PROVIDER=deepseek
set AI_API_KEY=sk-your-deepseek-api-key-here
set AI_MODEL=deepseek-chat
echo AI 服务: %AI_PROVIDER%
echo.

REM 2. 设置项目信息
echo [2/4] 设置项目信息...
set GITHUB_URL=https://github.com/myteam/awesome-project
set TEAM_NAME=Dream Coders
set OUTPUT_FILE=hackathon-score.json
echo 项目: %GITHUB_URL%
echo 团队: %TEAM_NAME%
echo.

REM 3. 运行评分
echo [3/4] 正在评分（这可能需要几分钟）...
java -jar hackathon-ai.jar hackathon ^
  --github-url %GITHUB_URL% ^
  --team "%TEAM_NAME%" ^
  --output %OUTPUT_FILE%

REM 4. 显示结果
echo.
echo [4/4] 评分完成！
echo 结果已保存到: %OUTPUT_FILE%
echo.
echo 查看详细报告:
type %OUTPUT_FILE%

pause
```

保存为 `run-hackathon.bat`，双击运行！

---

## 📞 获取帮助

### 查看帮助信息
```bash
java -jar hackathon-ai.jar hackathon --help
```

### 查看版本信息
```bash
java -jar hackathon-ai.jar --version
```

### 常见问题排查

1. **API Key 无效**: 检查 Key 是否正确，是否有额度
2. **网络错误**: 检查代理设置，确保能访问 AI API
3. **配置未生效**: 检查优先级，系统属性 > 环境变量 > 配置文件
4. **模型不支持**: 查看上面支持的模型列表

---

## 🎉 总结

配置黑客松 AI 服务非常简单：

1. **选择 AI 服务**: DeepSeek / OpenAI / Claude / Gemini / Bedrock
2. **设置环境变量**: `AI_PROVIDER` 和 `AI_API_KEY`
3. **运行黑客松**: `java -jar hackathon-ai.jar hackathon ...`

**就这么简单！** 开始你的黑客松评分之旅吧！ 🚀

---

**文档版本**: 2.0  
**最后更新**: 2025-11-13  
**维护者**: AI-Reviewer Team

