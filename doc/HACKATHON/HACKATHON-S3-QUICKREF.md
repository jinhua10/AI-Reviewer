# 黑客松 S3 集成 - 快速参考

## ✅ S3 功能已集成

黑客松命令行工具现在支持从 AWS S3 下载项目进行评审！

---

## 🚀 使用方法

### 1. 配置 S3 (config.yaml)

```yaml
s3Storage:
  region: "us-east-1"
  bucketName: "my-hackathon-bucket"  # 必填
  # 不需要配置 accessKeyId - 自动使用 IAM 角色
  maxConcurrency: 10
  connectTimeout: 30000
  readTimeout: 60000
  maxRetries: 3
  retryDelay: 1000
```

**关键配置：**
- ✅ `bucketName`: 必须配置（S3 存储桶名称）
- ✅ `region`: AWS 区域（默认 us-east-1）
- ❌ `accessKeyId`: 不需要（使用 IAM 角色）

### 2. 使用命令

#### 基本用法
```bash
java -jar hackathon-reviewer.jar \
  --s3-path projects/team-awesome/ \
  --team "Team Awesome" \
  --output score.json \
  --report report.md
```

#### 短选项
```bash
java -jar hackathon-reviewer.jar -s projects/team-awesome/ -t "Team Awesome" -o score.json
```

#### Windows
```cmd
java -jar hackathon-reviewer.jar ^
  --s3-path projects/team-awesome/ ^
  --team "Team Awesome" ^
  --output score.json
```

---

## 📋 四种输入方式

现在支持 **4 种**项目输入方式：

| 方式 | 命令 | 示例 |
|------|------|------|
| **S3 路径** ✨ | `--s3-path <路径>` 或 `-s` | `--s3-path projects/team-a/` |
| **Git URL** | `--github-url <URL>` | `--github-url https://github.com/user/repo` |
| **ZIP 文件** | `--zip <文件>` 或 `-z` | `--zip project.zip` |
| **本地目录** | `--directory <路径>` 或 `-d` | `-d /path/to/project` |

---

## 🔧 S3 路径格式

### 标准格式
```bash
--s3-path projects/team-awesome/
```

### 相对于 bucket 根目录
- ✅ `projects/team-a/`
- ✅ `submissions/2025/team-b/`
- ✅ `hackathon/round1/team-c/`

### 自动功能
- ✅ 自动下载整个文件夹
- ✅ 智能识别项目根目录
- ✅ 显示下载进度和统计
- ✅ 评审完成后自动清理临时文件

---

## 📊 输出示例

```
正在从 S3 下载项目: projects/team-awesome/
Bucket: my-hackathon-bucket
路径: projects/team-awesome/

S3 项目下载完成:
  - 总文件数: 150
  - 成功: 150
  - 失败: 0
  - 总大小: 5.32 MB
  - 耗时: 2.45 秒
  - 本地目录: /tmp/hackathon-s3-download/team-awesome-1763079145

正在扫描项目...
项目信息:
  - 团队: Team Awesome
  - 名称: team-awesome
  - 类型: Java
  - 文件数: 150
  - 代码行数: 8520

正在分析项目...
```

---

## 🎯 完整示例

### 示例 1: 评审单个团队
```bash
java -jar hackathon-reviewer.jar \
  --s3-path projects/team-awesome/ \
  --team "Team Awesome" \
  --output results/team-awesome-score.json \
  --report results/team-awesome-report.md
```

### 示例 2: 批量评审（Bash）
```bash
#!/bin/bash

# S3 中的团队列表
teams=(
  "team-a"
  "team-b"
  "team-c"
)

for team in "${teams[@]}"; do
  echo "评审团队: $team"
  java -jar hackathon-reviewer.jar \
    --s3-path "projects/$team/" \
    --team "$team" \
    --output "results/${team}-score.json" \
    --report "results/${team}-report.md"
  echo "---"
done

echo "批量评审完成！"
```

### 示例 3: 批量评审（Windows）
```cmd
@echo off

for %%t in (team-a team-b team-c) do (
  echo 评审团队: %%t
  java -jar hackathon-reviewer.jar ^
    --s3-path "projects/%%t/" ^
    --team "%%t" ^
    --output "results\%%t-score.json" ^
    --report "results\%%t-report.md"
  echo ---
)

echo 批量评审完成！
```

---

## ⚙️ 配置选项

### S3 配置参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `region` | us-east-1 | AWS 区域 |
| `bucketName` | - | S3 存储桶（**必填**） |
| `accessKeyId` | - | Access Key（留空使用 IAM） |
| `secretAccessKey` | - | Secret Key（留空使用 IAM） |
| `maxConcurrency` | 10 | 最大并发下载数 |
| `connectTimeout` | 30000 | 连接超时（毫秒） |
| `readTimeout` | 60000 | 读取超时（毫秒） |
| `maxRetries` | 3 | 最大重试次数 |
| `retryDelay` | 1000 | 重试延迟（毫秒） |

---

## 🔐 IAM 权限要求

### 最小权限策略
```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Action": [
      "s3:GetObject",
      "s3:ListBucket"
    ],
    "Resource": [
      "arn:aws:s3:::my-hackathon-bucket",
      "arn:aws:s3:::my-hackathon-bucket/*"
    ]
  }]
}
```

### 完整权限（含上传）
```json
{
  "Effect": "Allow",
  "Action": [
    "s3:GetObject",
    "s3:PutObject",
    "s3:ListBucket"
  ],
  "Resource": [
    "arn:aws:s3:::my-hackathon-bucket",
    "arn:aws:s3:::my-hackathon-bucket/*"
  ]
}
```

---

## 📂 S3 目录结构建议

### 推荐结构
```
my-hackathon-bucket/
├── projects/
│   ├── team-a/
│   │   ├── src/
│   │   ├── pom.xml
│   │   └── README.md
│   ├── team-b/
│   │   ├── src/
│   │   └── package.json
│   └── team-c/
│       └── ...
└── results/
    ├── team-a-score.json
    ├── team-a-report.md
    └── ...
```

### 使用方式
```bash
# 评审 team-a
java -jar hackathon-reviewer.jar --s3-path projects/team-a/ -t "Team A" -o results/team-a.json

# 评审 team-b
java -jar hackathon-reviewer.jar --s3-path projects/team-b/ -t "Team B" -o results/team-b.json
```

---

## ❗ 常见问题

### Q1: "S3 服务未初始化"
**错误：** `S3 服务未初始化。请在 config.yaml 中配置 s3Storage.bucketName`

**解决：** 在 `config.yaml` 中添加：
```yaml
s3Storage:
  bucketName: "your-bucket-name"
```

### Q2: "Access Denied"
**原因：** IAM 角色缺少 S3 权限

**解决：** 
1. 确认 EC2/ECS 实例已附加 IAM 角色
2. 检查 IAM 策略包含 `s3:GetObject` 和 `s3:ListBucket`

### Q3: "NoSuchBucket"
**原因：** 存储桶名称错误或不存在

**解决：** 
1. 检查 `config.yaml` 中的 `bucketName`
2. 确认存储桶在正确的区域

### Q4: 下载速度慢
**优化：** 增加并发数
```yaml
s3Storage:
  maxConcurrency: 20  # 增加并发
```

---

## 🎊 集成完成清单

✅ **核心功能：**
- [x] S3 配置解析（Configuration）
- [x] S3 服务初始化（initializeS3Service）
- [x] S3 下载功能（downloadFromS3）
- [x] 命令行参数（--s3-path / -s）
- [x] 帮助信息更新
- [x] 自动清理临时文件
- [x] 智能根目录识别

✅ **使用 IAM 角色：**
- [x] 自动使用默认凭证链
- [x] 无需配置 Access Key
- [x] 安全可靠

✅ **四种输入方式：**
1. [x] Git URL
2. [x] 本地目录
3. [x] ZIP 文件
4. [x] **S3 路径** ← 新增

---

## 📚 相关文档

- AWS S3 集成指南: `doc/AWS-S3-INTEGRATION-GUIDE.md`
- S3 快速参考: `AWS-S3-QUICKREF.md`
- IAM 配置: `doc/AWS-BEDROCK-IAM-SETUP.md`

---

**黑客松评审工具现在支持从 S3 下载项目！** 🎉

