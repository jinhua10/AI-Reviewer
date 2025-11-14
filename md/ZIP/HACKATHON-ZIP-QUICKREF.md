# 黑客松 ZIP 评审快速参考

## 基本命令

```bash
# 评审 ZIP 文件
java -jar ai-reviewer.jar \
  --zip project.zip \
  --team "Team Name" \
  --output score.json \
  --report report.md
```

## 短选项

```bash
java -jar ai-reviewer.jar -z project.zip -t "Team Name" -o score.json -r report.md
```

## 三种输入方式

| 方式 | 命令 | 说明 |
|------|------|------|
| **ZIP 文件** | `--zip file.zip` 或 `-z file.zip` | ✨ 新增 |
| **Git URL** | `--github-url URL` 或 `--gitee-url URL` | 原有 |
| **本地目录** | `--directory path` 或 `-d path` | 原有 |

## 完整选项

```bash
java -jar ai-reviewer.jar \
  --zip <ZIP文件>              # ZIP 压缩包路径
  --team <团队名>              # 团队名称
  --output <文件>              # 输出 JSON 评分
  --report <文件>              # 输出 Markdown 报告
```

## Windows 示例

```cmd
java -jar ai-reviewer.jar ^
  --zip "D:\submissions\team-awesome.zip" ^
  --team "Team Awesome" ^
  --output "results\team-awesome-score.json" ^
  --report "results\team-awesome-report.md"
```

## Linux/Mac 示例

```bash
java -jar ai-reviewer.jar \
  --zip /path/to/submissions/team-awesome.zip \
  --team "Team Awesome" \
  --output results/team-awesome-score.json \
  --report results/team-awesome-report.md
```

## 批量评审

### Bash 脚本
```bash
for zipfile in submissions/*.zip; do
    team=$(basename "$zipfile" .zip)
    java -jar ai-reviewer.jar \
      --zip "$zipfile" \
      --team "$team" \
      --output "results/${team}-score.json"
done
```

### Windows 批处理
```cmd
for %%f in (submissions\*.zip) do (
    java -jar ai-reviewer.jar ^
      --zip "%%f" ^
      --team "%%~nf" ^
      --output "results\%%~nf-score.json"
)
```

## ZIP 要求

✅ **格式**: 标准 ZIP (`.zip`)
✅ **结构**: 可直接包含项目文件，或有一层目录包裹
✅ **大小**: 建议 < 500MB
✅ **内容**: 只包含源代码和配置文件

❌ **排除**: 
- `node_modules/`
- `target/`
- `build/`
- `.git/`
- 二进制文件

## 推荐 ZIP 结构

```
project.zip
└── project-name/
    ├── src/
    ├── pom.xml / package.json / requirements.txt
    ├── README.md
    └── 其他源文件
```

## 临时文件

- **位置**: `%TEMP%\hackathon-zip-extract\` (Windows) 或 `/tmp/hackathon-zip-extract/` (Linux/Mac)
- **清理**: 自动清理（评审完成后）

## 常见问题

**Q: ZIP 文件不存在**
```
错误: ZIP 文件不存在: /path/to/project.zip
```
检查文件路径是否正确

**Q: 不是有效的 ZIP 文件**
```
错误: 不是有效的 ZIP 文件
```
确保文件是 ZIP 格式，重新压缩

**Q: 如何创建 ZIP**
```bash
# 推荐方式
cd parent-directory
zip -r project.zip project-name/
```

## 帮助信息

```bash
java -jar ai-reviewer.jar --help
```

## 更多信息

详见: `doc/HACKATHON-ZIP-SUPPORT.md`

