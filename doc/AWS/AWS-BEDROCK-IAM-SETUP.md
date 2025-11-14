# AWS Bedrock IAM 角色配置指南

## 概述
当您的应用在 AWS 服务器（EC2、ECS、Lambda 等）上运行时，可以使用 IAM 角色自动获取凭证，无需手动配置 Access Key 和 Secret Key。

## 修改内容

### 1. BedrockAdapter.java 修改
已修改 `BedrockAdapter.java` 以支持：
- ✅ ARN 格式的 model ID（如：`arn:aws:bedrock:us-east-1:xxx:inference-profile/us.anthropic.claude-xxx`）
- ✅ AWS 默认凭证链（自动使用 IAM 角色）
- ✅ 向后兼容简短格式的 model ID（如：`anthropic.claude-v2`）

**修改的方法：**
- `buildRequestBody()`: 使用 `contains()` 方法识别 ARN 格式
- `parseResponse()`: 使用 `contains()` 方法识别 ARN 格式

### 2. 配置文件示例
创建了 `config-bedrock-aws-iam.yaml` 配置文件示例。

## 快速使用

### 配置文件 (config.yaml)
```yaml
aiService:
  provider: "bedrock"
  
  # 不设置 apiKey - 将自动使用 IAM 角色凭证
  
  # 使用您的完整 ARN
  model: "arn:aws:bedrock:us-east-1:590184013141:inference-profile/us.anthropic.claude-sonnet-4-5-20250929-v1:0"
  
  region: "us-east-1"
  maxTokens: 8000
  temperature: 0
  
  maxRetries: 2
  retryDelay: 1000
  maxConcurrency: 3
```

### IAM 角色权限
确保您的 EC2/ECS/Lambda 实例的 IAM 角色包含以下权限：

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "bedrock:InvokeModel",
        "bedrock:InvokeModelWithResponseStream"
      ],
      "Resource": [
        "arn:aws:bedrock:us-east-1:590184013141:inference-profile/*",
        "arn:aws:bedrock:*:*:foundation-model/*"
      ]
    }
  ]
}
```

### AWS 凭证链顺序
AWS SDK 会按以下顺序自动查找凭证：
1. ~~环境变量~~ (您不需要设置)
2. ~~Java 系统属性~~ (您不需要设置)
3. ~~AWS 凭证文件~~ (您不需要设置)
4. **Amazon ECS 容器凭证** ← 如果在 ECS 上运行
5. **EC2 实例配置文件凭证 (IAM 角色)** ← 如果在 EC2 上运行

## 运行应用

```bash
# 使用默认配置文件
java -jar ai-reviewer.jar

# 或指定配置文件
java -jar ai-reviewer.jar --config config-bedrock-aws-iam.yaml
```

## 验证配置

### 检查 IAM 角色（在 EC2 实例上）
```bash
# 检查实例元数据
curl http://169.254.169.254/latest/meta-data/iam/security-credentials/

# 查看角色名称
curl http://169.254.169.254/latest/meta-data/iam/security-credentials/YOUR-ROLE-NAME
```

### 日志输出
成功使用默认凭证时，日志会显示：
```
使用默认 AWS 凭证链
AWS Bedrock 适配器初始化完成 - 模型: arn:aws:bedrock:us-east-1:..., 区域: us-east-1, 最大并发: 3
```

## 常见问题

### Q1: 提示 "Unable to load credentials"
**解决方案:**
1. 确认 EC2 实例已附加 IAM 角色
2. 检查 IAM 角色权限是否包含 `bedrock:InvokeModel`
3. 确认实例可以访问元数据服务（169.254.169.254）

### Q2: 提示 "Access Denied"
**解决方案:**
1. 检查 IAM 角色的 Bedrock 权限
2. 确认 Resource ARN 是否正确
3. 确认您的 AWS 账户是否已启用 Bedrock 服务

### Q3: 支持哪些 model ID 格式？
**支持以下格式:**
- 完整 ARN: `arn:aws:bedrock:us-east-1:xxx:inference-profile/us.anthropic.claude-sonnet-4-5-20250929-v1:0`
- 简短格式: `anthropic.claude-v2`
- 版本格式: `anthropic.claude-3-sonnet-20240229-v1:0`

## 支持的模型

### Claude (Anthropic)
- `anthropic.claude-v2`
- `anthropic.claude-instant-v1`
- `anthropic.claude-3-sonnet-20240229-v1:0`
- `anthropic.claude-3-5-sonnet-20240620-v1:0`
- ARN 格式的 Claude 模型

### 其他模型
- Amazon Titan: `amazon.titan-text-express-v1`
- Meta Llama: `meta.llama2-13b-chat-v1`
- Cohere: `cohere.command-text-v14`
- AI21: `ai21.j2-mid-v1`

## 测试

### 简单测试
```bash
# 编译项目
mvn clean package

# 运行测试
java -jar target/ai-reviewer.jar --help

# 执行代码审查（小规模测试）
java -jar target/ai-reviewer.jar --config config-bedrock-aws-iam.yaml --path ./src/main/java
```

## 总结

✅ **已完成:**
1. 修改 `BedrockAdapter.java` 支持 ARN 格式的 model ID
2. 支持 AWS 默认凭证链（IAM 角色）
3. 创建配置文件示例

✅ **您需要做的:**
1. 确保 EC2/ECS 实例附加了正确的 IAM 角色
2. 在配置文件中设置 `provider: "bedrock"` 和您的 model ARN
3. **不要设置 `apiKey`** - 留空即可自动使用 IAM 角色

✅ **运行时:**
- 直接在 AWS 服务器上运行
- 无需配置环境变量或 API Key
- 程序会自动使用 IAM 角色获取临时凭证

