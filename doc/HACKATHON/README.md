# 黑客松项目评审工具 - 使用指南

## 概述

黑客松项目评审工具是一个专为黑客松赛事设计的 AI 驱动的自动化代码评审系统。支持多种项目输入方式，自动分析代码质量、架构设计、创新性等多个维度，生成详细的评分报告。

**版本**: 2.0  
**更新日期**: 2025-11-14

---

## 🚀 快速开始

### 构建Hackathon
请参考构建指南:
[HACKATHON-BUILD.md](HACKATHON-BUILD.md)

### 基本用法

```bash
java -jar hackathon-reviewer.jar [输入选项] [评审选项]
```

### 最简示例

```bash
# 从本地目录评审
java -jar hackathon-reviewer.jar -d /path/to/project -t "Team Name"

# 从 GitHub 评审
java -jar hackathon-reviewer.jar --github-url https://github.com/user/repo -t "Team Name"

# 从 ZIP 文件评审
java -jar hackathon-reviewer.jar -z project.zip -t "Team Name"

# 从 S3 评审
java -jar hackathon-reviewer.jar -s projects/team-name/ -t "Team Name"
```

---

## 📋 命令行参数

### 输入选项（四选一，必选）

#### 1. GitHub 仓库 URL

```bash
--github-url <URL>
--git-url <URL>        # 同 --github-url
```

**说明**: 从 GitHub 克隆项目进行评审

**示例**:
```bash
java -jar hackathon-reviewer.jar \
  --github-url https://github.com/user/awesome-project \
  --team "Team Awesome"
```

#### 2. Gitee 仓库 URL

```bash
--gitee-url <URL>
```

**说明**: 从 Gitee 克隆项目进行评审（优先于 GitHub URL）

**示例**:
```bash
java -jar hackathon-reviewer.jar \
  --gitee-url https://gitee.com/user/awesome-project \
  --team "Team Awesome"
```

#### 3. 本地项目目录

```bash
--directory <路径>
--dir <路径>          # 同 --directory
-d <路径>             # 短选项
```

**说明**: 评审本地目录中的项目

**示例**:
```bash
java -jar hackathon-reviewer.jar \
  -d /home/user/projects/team-project \
  --team "Team Awesome"
```

#### 4. ZIP 压缩包文件

```bash
--zip <文件路径>
-z <文件路径>         # 短选项
```

**说明**: 解压 ZIP 文件后进行评审

**示例**:
```bash
java -jar hackathon-reviewer.jar \
  --zip /path/to/team-project.zip \
  --team "Team Awesome"
```

**ZIP 要求**:
- 标准 ZIP 格式（`.zip` 扩展名）
- 建议大小 < 500MB
- 只包含源代码和配置文件
- 排除 `node_modules/`, `target/`, `build/`, `.git/` 等

#### 5. S3 存储路径

```bash
--s3-path <S3路径>
--s3 <S3路径>         # 同 --s3-path
-s <S3路径>           # 短选项
```

**说明**: 从 AWS S3 下载项目进行评审（需在 `config.yaml` 中配置 `bucketName`）

**示例**:
```bash
java -jar hackathon-reviewer.jar \
  --s3-path projects/team-awesome/ \
  --team "Team Awesome"
```

**S3 要求**:
- 在 `config.yaml` 中配置 `s3Storage.bucketName`
- EC2/ECS 实例需附加 IAM 角色
- IAM 角色需有 `s3:GetObject` 和 `s3:ListBucket` 权限

---

### 评审选项

#### 团队名称

```bash
--team <团队名称>
-t <团队名称>         # 短选项
```

**默认值**: "Unknown Team"

**说明**: 指定参赛团队名称

**示例**:
```bash
--team "Team Awesome"
-t "创新小队"
```

#### Git 分支

```bash
--branch <分支名称>
-b <分支名称>         # 短选项
```

**默认值**: "main"（或仓库默认分支）

**说明**: 指定要克隆的 Git 分支（仅用于 Git URL）

**示例**:
```bash
--branch develop
-b feature/hackathon
```

#### 输出评分文件

```bash
--output <文件路径>
-o <文件路径>         # 短选项
```

**说明**: 指定输出评分结果的 JSON 文件路径

**示例**:
```bash
--output ./results/team-awesome-score.json
-o score.json
```

**输出格式** (JSON):
```json
{
  "teamName": "Team Awesome",
  "totalScore": 85.5,
  "dimensions": {
    "innovation": 90,
    "codeQuality": 85,
    "architecture": 80,
    "documentation": 75,
    "testing": 88,
    "performance": 82,
    "security": 90,
    "usability": 85
  },
  "comments": "..."
}
```

#### 输出详细报告

```bash
--report <文件路径>
-r <文件路径>         # 短选项
```

**说明**: 指定输出详细评审报告的 Markdown 文件路径

**示例**:
```bash
--report ./results/team-awesome-report.md
-r report.md
```

**报告内容**:
- 项目概况
- 各维度详细评分
- 代码质量分析
- 架构设计评价
- 优点和改进建议
- 具体代码示例

#### 帮助信息

```bash
--help
-h                    # 短选项
```

**说明**: 显示帮助信息并退出

---

## 📖 完整示例

### 示例 1: 评审 GitHub 项目

```bash
java -jar hackathon-reviewer.jar \
  --github-url https://github.com/awesome-team/hackathon-project \
  --team "Awesome Team" \
  --branch main \
  --output ./results/awesome-team-score.json \
  --report ./results/awesome-team-report.md
```

### 示例 2: 评审 Gitee 项目

```bash
java -jar hackathon-reviewer.jar \
  --gitee-url https://gitee.com/awesome-team/hackathon-project \
  --team "Awesome Team" \
  --output ./results/awesome-team-score.json \
  --report ./results/awesome-team-report.md
```

### 示例 3: 评审本地项目

```bash
java -jar hackathon-reviewer.jar \
  --directory /home/user/projects/hackathon-project \
  --team "Awesome Team" \
  --output ./results/score.json \
  --report ./results/report.md
```

### 示例 4: 评审 ZIP 文件

```bash
java -jar hackathon-reviewer.jar \
  --zip /path/to/team-awesome-submission.zip \
  --team "Awesome Team" \
  --output ./results/team-awesome-score.json \
  --report ./results/team-awesome-report.md
```

### 示例 5: 评审 S3 项目

```bash
java -jar hackathon-reviewer.jar \
  --s3-path projects/team-awesome/ \
  --team "Awesome Team" \
  --output ./results/team-awesome-score.json \
  --report ./results/team-awesome-report.md
```

### 示例 6: 使用短选项

```bash
java -jar hackathon-reviewer.jar \
  -d /path/to/project \
  -t "Team Name" \
  -o score.json \
  -r report.md
```

---

## 🔄 批量评审

### Bash 脚本（Linux/Mac）

#### 批量评审本地项目

```bash
#!/bin/bash

# 批量评审多个本地项目
projects=(
  "/path/to/team-a"
  "/path/to/team-b"
  "/path/to/team-c"
)

for project in "${projects[@]}"; do
  team_name=$(basename "$project")
  echo "评审团队: $team_name"
  
  java -jar hackathon-reviewer.jar \
    -d "$project" \
    -t "$team_name" \
    -o "results/${team_name}-score.json" \
    -r "results/${team_name}-report.md"
  
  echo "---"
done

echo "批量评审完成！"
```

#### 批量评审 ZIP 文件

```bash
#!/bin/bash

# 批量评审 submissions 目录下的所有 ZIP 文件
for zipfile in submissions/*.zip; do
  team_name=$(basename "$zipfile" .zip)
  echo "评审团队: $team_name"
  
  java -jar hackathon-reviewer.jar \
    --zip "$zipfile" \
    --team "$team_name" \
    --output "results/${team_name}-score.json" \
    --report "results/${team_name}-report.md"
  
  echo "---"
done

echo "批量评审完成！"
```

#### 批量评审 S3 项目

```bash
#!/bin/bash

# 批量评审 S3 中的多个团队项目
teams=("team-a" "team-b" "team-c" "team-d")

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

### Windows 批处理脚本

#### 批量评审 ZIP 文件

```cmd
@echo off
setlocal enabledelayedexpansion

echo 开始批量评审...

for %%f in (submissions\*.zip) do (
  set "filename=%%~nf"
  echo 评审团队: !filename!
  
  java -jar hackathon-reviewer.jar ^
    --zip "%%f" ^
    --team "!filename!" ^
    --output "results\!filename!-score.json" ^
    --report "results\!filename!-report.md"
  
  echo ---
)

echo 批量评审完成！
pause
```

#### 批量评审 S3 项目

```cmd
@echo off

for %%t in (team-a team-b team-c team-d) do (
  echo 评审团队: %%t
  
  java -jar hackathon-reviewer.jar ^
    --s3-path "projects/%%t/" ^
    --team "%%t" ^
    --output "results\%%t-score.json" ^
    --report "results\%%t-report.md"
  
  echo ---
)

echo 批量评审完成！
pause
```

---

## ⚙️ 配置文件

### config.yaml 配置

工具使用 `config.yaml` 配置 AI 服务和 S3 存储。

#### 完整配置示例

```yaml
# AI 服务配置
aiService:
  provider: "bedrock"  # 或 deepseek, openai, gemini, claude
  model: "anthropic.claude-v2"
  region: "us-east-1"
  maxTokens: 8000
  temperature: 0
  maxRetries: 2
  maxConcurrency: 3

# S3 存储配置（使用 IAM 角色）
s3Storage:
  region: "us-east-1"
  bucketName: "my-hackathon-bucket"  # 必填（用于 --s3-path）
  # 不需要配置 accessKeyId - 自动使用 IAM 角色
  maxConcurrency: 10
  connectTimeout: 30000
  readTimeout: 60000
  maxRetries: 3
  retryDelay: 1000

# 文件扫描配置
fileScan:
  includePatterns:
    - "*.java"
    - "*.py"
    - "*.js"
    - "*.ts"
    - "*.go"
    - "*.rs"
    - "*.cpp"
    - "*.c"
    - "*.yaml"
    - "*.json"
    - "*.md"
  excludePatterns:
    - "**/target/**"
    - "**/build/**"
    - "**/node_modules/**"
    - "**/.git/**"
  maxFileSizeKB: 500
  maxTotalFiles: 1000

# 缓存配置
cache:
  enabled: true
  type: "file"
  ttlHours: 24
  maxSize: 1000

# 日志配置
logging:
  level: "INFO"
  file: "./logs/ai-reviewer.log"
  console: true
```

#### 最小配置示例

```yaml
# 最小配置（使用 Bedrock + S3）
aiService:
  provider: "bedrock"
  model: "anthropic.claude-v2"
  region: "us-east-1"

s3Storage:
  bucketName: "my-hackathon-bucket"  # 仅需配置此项用于 S3
```

---

## 🔐 AWS 权限配置

### S3 访问权限（IAM 策略）

#### 最小权限（只读）

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::my-hackathon-bucket",
        "arn:aws:s3:::my-hackathon-bucket/*"
      ]
    }
  ]
}
```

#### 完整权限（含上传）

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:ListBucket",
        "s3:DeleteObject"
      ],
      "Resource": [
        "arn:aws:s3:::my-hackathon-bucket",
        "arn:aws:s3:::my-hackathon-bucket/*"
      ]
    }
  ]
}
```

### Bedrock 访问权限

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
      "Resource": "arn:aws:bedrock:*:*:*"
    }
  ]
}
```

---

## 📊 评分维度

黑客松评审工具从多个维度对项目进行评分：

### 默认评分维度

| 维度 | 权重 | 说明 |
|------|------|------|
| **创新性** (Innovation) | 15% | 项目创意、技术创新、解决方案独特性 |
| **代码质量** (Code Quality) | 10% | 代码规范、可读性、maintainability |
| **架构设计** (Architecture) | 12% | 系统架构、设计模式、可扩展性 |
| **文档完整性** (Documentation) | 8% | README、注释、API 文档 |
| **测试覆盖** (Testing) | 10% | 单元测试、集成测试、测试覆盖率 |
| **性能优化** (Performance) | 8% | 响应时间、资源使用、性能优化 |
| **安全性** (Security) | 12% | 安全漏洞、数据保护、权限控制 |
| **用户体验** (Usability) | 10% | 界面设计、易用性、交互体验 |
| **技术实现** (Implementation) | 10% | 功能完整性、技术难度、实现质量 |
| **最佳实践** (Best Practices) | 5% | 遵循行业标准、编码规范 |

**总分**: 100 分

### 评分等级

| 分数范围 | 等级 | 说明 |
|---------|------|------|
| 90-100 | A | 优秀 |
| 80-89 | B | 良好 |
| 70-79 | C | 中等 |
| 60-69 | D | 及格 |
| 0-59 | F | 不及格 |

---

## 🎯 使用场景

### 场景 1: 线上黑客松（GitHub）

**团队提交**: 将项目推送到 GitHub  
**评委评审**: 使用 Git URL

```bash
java -jar hackathon-reviewer.jar \
  --github-url https://github.com/team-awesome/project \
  -t "Team Awesome" \
  -o results/team-awesome.json
```

### 场景 2: 线下黑客松（ZIP）

**团队提交**: 提交 ZIP 压缩包  
**评委评审**: 使用 ZIP 文件

```bash
java -jar hackathon-reviewer.jar \
  -z submissions/team-awesome.zip \
  -t "Team Awesome" \
  -o results/team-awesome.json
```

### 场景 3: 企业内部黑客松（S3）

**团队提交**: 上传项目到 S3  
**评委评审**: 使用 S3 路径

```bash
# 团队上传
aws s3 sync ./my-project s3://hackathon-bucket/projects/team-awesome/

# 评委评审
java -jar hackathon-reviewer.jar \
  -s projects/team-awesome/ \
  -t "Team Awesome" \
  -o results/team-awesome.json
```

### 场景 4: 本地开发测试

**开发者**: 本地测试评审  
**使用方式**: 使用本地目录

```bash
java -jar hackathon-reviewer.jar \
  -d /path/to/my-project \
  -t "My Team" \
  -o test-score.json
```

---

## ❗ 常见问题

### Q1: 如何指定 AI 服务？

在 `config.yaml` 中配置 `aiService.provider`:

```yaml
aiService:
  provider: "bedrock"  # 或 deepseek, openai, gemini, claude
```

### Q2: ZIP 文件有大小限制吗？

建议 ZIP 文件小于 500MB，且只包含源代码和配置文件，排除：
- `node_modules/`
- `target/`
- `build/`
- `.git/`
- 二进制文件

### Q3: S3 下载失败怎么办？

检查：
1. `config.yaml` 中是否配置了 `s3Storage.bucketName`
2. EC2/ECS 实例是否附加了 IAM 角色
3. IAM 角色是否有 S3 访问权限
4. S3 路径是否正确

### Q4: 如何查看详细日志？

设置日志级别为 DEBUG:

```yaml
logging:
  level: "DEBUG"
  console: true
```

或使用命令行参数:

```bash
java -Dlogging.level=DEBUG -jar hackathon-reviewer.jar ...
```

### Q5: Git 克隆超时怎么办？

增加超时时间（在代码中调整 `cloneRequest.timeoutSeconds`）或使用本地目录/ZIP 文件。

### Q6: 支持哪些编程语言？

支持主流编程语言：
- Java
- Python
- JavaScript/TypeScript
- Go
- Rust
- C/C++
- PHP
- Ruby
- C#
- Swift
- Kotlin
- Scala

### Q7: 如何自定义评分权重？

编辑评分配置文件（具体位置见开发文档）或通过配置文件调整。

---

## 📂 输出文件格式

### JSON 评分文件格式

```json
{
  "teamName": "Team Awesome",
  "projectName": "awesome-project",
  "projectType": "Java",
  "totalScore": 85.5,
  "grade": "B",
  "dimensions": {
    "innovation": 90,
    "codeQuality": 85,
    "architecture": 80,
    "documentation": 75,
    "testing": 88,
    "performance": 82,
    "security": 90,
    "usability": 85,
    "implementation": 87,
    "bestPractices": 83
  },
  "dimensionComments": {
    "innovation": "项目创意独特，技术选型新颖...",
    "codeQuality": "代码规范良好，可读性强...",
    "architecture": "架构设计合理，模块划分清晰..."
  },
  "overallSummary": "这是一个优秀的黑客松项目...",
  "strengths": [
    "创新性强",
    "技术实现完整",
    "文档详细"
  ],
  "improvements": [
    "可以增加更多测试",
    "性能优化空间",
    "安全加固"
  ],
  "fileCount": 150,
  "totalLines": 8520,
  "analysisTime": "2025-11-14T12:34:56Z",
  "duration": 45000
}
```

### Markdown 报告格式

```markdown
# 黑客松项目评审报告

## 项目信息
- **团队名称**: Team Awesome
- **项目名称**: awesome-project
- **项目类型**: Java
- **文件数量**: 150
- **代码行数**: 8520

## 评分结果
**总分**: 85.5/100 (B)

### 各维度评分
- 创新性: 90/100
- 代码质量: 85/100
- 架构设计: 80/100
...

## 详细分析
...

## 优点
1. 创新性强
2. 技术实现完整
...

## 改进建议
1. 增加测试覆盖
2. 优化性能
...
```

---

## 🔗 相关文档

- **黑客松快速指南**: `doc/HACKATHON/HACKATHON-GUIDE.md`
- **ZIP 支持说明**: `HACKATHON-ZIP-QUICKREF.md`
- **S3 集成说明**: `HACKATHON-S3-QUICKREF.md`
- **配置参考**: `doc/HACKATHON/HACKATHON-AI-CONFIG-QUICKREF.md`
- **实现指南**: `doc/HACKATHON/HACKATHON-IMPLEMENTATION-GUIDE.md`

---

## 📞 技术支持

### 查看帮助

```bash
java -jar hackathon-reviewer.jar --help
```

### 版本信息

```bash
java -jar hackathon-reviewer.jar --version
```

### 日志位置

- **Linux/Mac**: `./logs/ai-reviewer.log`
- **Windows**: `.\logs\ai-reviewer.log`

---

## 🎊 总结

黑客松项目评审工具支持 **4 种输入方式**：

1. ✅ **Git URL** - 适合线上提交
2. ✅ **本地目录** - 适合本地测试
3. ✅ **ZIP 文件** - 适合离线提交
4. ✅ **S3 路径** - 适合企业级部署

**核心特性**：
- 🤖 AI 驱动的智能评审
- 📊 多维度评分体系
- 📝 详细评审报告
- 🚀 批量评审支持
- ☁️ 云原生部署
- 🔐 IAM 角色认证

**立即开始使用黑客松评审工具！** 🎉

---

**版本**: 2.0  
**最后更新**: 2025-11-14  
**维护者**: AI-Reviewer Team

