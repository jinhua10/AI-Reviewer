# AWS Bedrock IAM 配置快速参考

## 核心修改

### ✅ BedrockAdapter.java
已修改以支持 ARN 格式的 model ID 和默认 IAM 凭证。

**关键变化：**
- 使用 `contains()` 方法识别模型类型（支持 ARN 格式）
- 默认使用 AWS 凭证链（不需要 apiKey）

---

## 配置示例

### 最简配置 (config.yaml)
```yaml
aiService:
  provider: "bedrock"
  model: "arn:aws:bedrock:us-east-1:590184013141:inference-profile/us.anthropic.claude-sonnet-4-5-20250929-v1:0"
  region: "us-east-1"
  maxTokens: 8000
  temperature: 0
```

**注意：** 
- ❌ **不要设置 `apiKey`** 
- ✅ 程序会自动使用 EC2/ECS 的 IAM 角色凭证

---

## IAM 角色权限（必须）

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Action": [
      "bedrock:InvokeModel",
      "bedrock:InvokeModelWithResponseStream"
    ],
    "Resource": "arn:aws:bedrock:*:*:*"
  }]
}
```

---

## 运行命令

```bash
# 直接运行（使用默认 config.yaml）
java -jar ai-reviewer.jar

# 或指定配置文件
java -jar ai-reviewer.jar --config config-bedrock-aws-iam.yaml
```

---

## 验证清单

在 AWS EC2/ECS 上运行前检查：

- [ ] EC2/ECS 实例已附加 IAM 角色
- [ ] IAM 角色有 `bedrock:InvokeModel` 权限
- [ ] config.yaml 中 `provider: "bedrock"`
- [ ] config.yaml 中 **没有设置 `apiKey`**
- [ ] model ID 使用您的完整 ARN
- [ ] region 与 ARN 中的区域一致

---

## 成功日志示例

```
使用默认 AWS 凭证链
AWS Bedrock 适配器初始化完成 - 模型: arn:aws:bedrock:us-east-1:..., 区域: us-east-1, 最大并发: 3
```

---

## 故障排除

| 问题 | 解决方案 |
|------|---------|
| `Unable to load credentials` | 确认 IAM 角色已附加到实例 |
| `Access Denied` | 检查 IAM 角色 Bedrock 权限 |
| `Model not found` | 确认 model ARN 正确且已启用 |

---

## 详细文档

参考：[doc/AWS-BEDROCK-IAM-SETUP.md](AWS-BEDROCK-IAM-SETUP.md)

