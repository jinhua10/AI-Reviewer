# Hackathon-AI 快速参考卡

## 🚀 快速开始

### 1. 构建项目
```bash
# Windows
hackathon-ai_buildStart.bat

# 或使用 Maven
mvn clean package -DskipTests -f hackathon-ai.xml
```

### 2. 运行评审

#### 交互式模式（推荐）
```bash
hackathon-ai-score.bat
```

#### 命令行模式

**GitHub 仓库**:
```bash
java -jar target/hackathon-ai.jar hackathon \
  --github-url https://github.com/user/repo \
  --team "Team Name" \
  --output score.json \
  --report report.md
```

**Gitee 仓库**:
```bash
java -jar target/hackathon-ai.jar hackathon \
  --gitee-url https://gitee.com/user/repo \
  --team "Team Name" \
  --output score.json \
  --report report.md
```

**本地目录**:
```bash
java -jar target/hackathon-ai.jar hackathon \
  --directory /path/to/project \
  --team "Team Name" \
  --output score.json \
  --report report.md
```

## 📊 评分维度

| 维度 | 权重 | 说明 |
|------|------|------|
| 代码质量 | 40% | 代码规范、可维护性 |
| 创新性 | 30% | 技术创新、独特性 |
| 完整性 | 20% | 功能完整度 |
| 文档质量 | 10% | 文档完善度 |

**等级**: S (90+) > A (80-89) > B (70-79) > C (60-69) > D (50-59) > F (<50)

## 🔧 命令行参数

### Hackathon 模式参数
| 参数 | 说明 | 必需 | 示例 |
|------|------|------|------|
| `--github-url` | GitHub 仓库 URL | 否* | `https://github.com/user/repo` |
| `--gitee-url` | Gitee 仓库 URL | 否* | `https://gitee.com/user/repo` |
| `--directory` | 本地项目目录 | 否* | `/path/to/project` |
| `--team` | 团队名称 | 否 | `"Team Awesome"` |
| `--branch` | Git 分支 | 否 | `main` (默认) |
| `--output` | 评分输出文件 | 否 | `score.json` |
| `--report` | 报告输出文件 | 否 | `report.md` |

\* 必须指定 `--github-url`、`--gitee-url` 或 `--directory` 之一

## 🗂️ 文件说明

| 文件 | 说明 |
|------|------|
| `hackathon-ai.xml` | Maven POM 配置文件 |
| `hackathon-ai_buildStart.bat` | 构建脚本 |
| `hackathon-ai-score.bat` | 交互式评审脚本 |
| `target/hackathon-ai.jar` | 生成的可执行 JAR |
| `score.json` | 评分结果（JSON 格式） |
| `report.md` | 详细报告（Markdown 格式） |

## 🛠️ 故障排查

### JAR 文件不存在
```bash
# 解决方案：重新构建
hackathon-ai_buildStart.bat
```

### Git 克隆失败
- 检查网络连接
- 确认仓库 URL 正确
- 尝试使用本地目录模式

### AI 服务调用失败
- 检查 `config.yaml` 配置
- 确认 API Key 有效
- 检查环境变量设置

## 📁 项目结构

```
AI-Reviewer/
├── hackathon-ai.xml              # 构建配置
├── hackathon-ai_buildStart.bat   # 构建脚本
├── hackathon-ai-score.bat        # 评审脚本
├── target/
│   └── hackathon-ai.jar          # 可执行 JAR
├── src/
│   ├── main/
│   │   ├── java/                 # 源代码
│   │   └── resources/
│   │       ├── config.yaml       # 配置文件
│   │       └── prompts/          # AI 提示词
│   └── test/                     # 测试代码
└── README.md                     # 项目说明
```

## 🎯 使用场景

### 场景1：在线黑客松评审
```bash
# 评审多个 GitHub 项目
for repo in repo1 repo2 repo3; do
  java -jar target/hackathon-ai.jar hackathon \
    --github-url "https://github.com/hackathon/$repo" \
    --team "Team-$repo" \
    --output "scores/$repo.json" \
    --report "reports/$repo.md"
done
```

### 场景2：本地项目批量评审
```bash
# 评审本地多个项目
for dir in project1 project2 project3; do
  java -jar target/hackathon-ai.jar hackathon \
    --directory "./projects/$dir" \
    --team "Team-$dir" \
    --output "scores/$dir.json" \
    --report "reports/$dir.md"
done
```

### 场景3：指定分支评审
```bash
# 评审特定分支
java -jar target/hackathon-ai.jar hackathon \
  --github-url https://github.com/user/repo \
  --branch develop \
  --team "Team Alpha" \
  --output score.json \
  --report report.md
```

## 💡 最佳实践

1. **使用环境变量存储 API Key**
   ```bash
   set DEEPSEEK_API_KEY=your-api-key
   ```

2. **增加内存限制（大型项目）**
   ```bash
   java -Xmx4g -jar target/hackathon-ai.jar hackathon ...
   ```

3. **并行评审多个项目**
   使用脚本批量处理，提高效率

4. **定期备份评分结果**
   将 `score.json` 和 `report.md` 归档

## 📞 获取帮助

```bash
# 主帮助
java -jar target/hackathon-ai.jar --help

# Hackathon 模式帮助
java -jar target/hackathon-ai.jar hackathon --help
```

## 🔗 相关文档

- [详细实施指南](HACKATHON-IMPLEMENTATION-GUIDE.md)
- [项目 README](README.md)
- [架构设计文档](md/20251111234200-HEXAGONAL-QUICKSTART-GUIDE.md)

---

**快速上手**: `hackathon-ai-score.bat` → 选择模式 → 输入信息 → 完成评审

