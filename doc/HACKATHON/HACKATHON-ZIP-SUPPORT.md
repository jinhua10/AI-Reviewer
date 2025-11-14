# 黑客松命令行工具 - ZIP 压缩包支持

```bash
# 基本用法
java -jar ai-reviewer.jar --zip project.zip --team "Team Name" -o score.json

# 批量评审
for zipfile in submissions/*.zip; do
    team=$(basename "$zipfile" .zip)
    java -jar ai-reviewer.jar --zip "$zipfile" --team "$team" -o "results/${team}.json"
done

```
## 功能说明

黑客松命令行工具现在支持三种项目输入方式：

1. **Git URL** - 从 GitHub/Gitee 等 Git 仓库克隆
2. **本地目录** - 直接使用本地项目目录
3. **ZIP 压缩包** - 解压 ZIP 文件后进行评审 ✨ **新增**

## 使用方法

### 1. 使用 ZIP 压缩包（新功能）

#### 基本用法
```bash
java -jar ai-reviewer.jar \
  --zip /path/to/project.zip \
  --team "Team Awesome" \
  --output score.json \
  --report report.md
```

#### 短选项
```bash
java -jar ai-reviewer.jar \
  -z project.zip \
  -t "Team Awesome" \
  -o score.json \
  -r report.md
```

#### Windows 路径
```cmd
java -jar ai-reviewer.jar ^
  --zip "D:\Projects\hackathon\team-project.zip" ^
  --team "Team Awesome" ^
  --output score.json ^
  --report report.md
```

### 2. 使用 Git URL（原有功能）

#### GitHub
```bash
java -jar ai-reviewer.jar \
  --github-url https://github.com/user/project \
  --team "Team Awesome" \
  --output score.json
```

#### Gitee
```bash
java -jar ai-reviewer.jar \
  --gitee-url https://gitee.com/user/project \
  --team "Team Awesome" \
  --output score.json
```

#### 指定分支
```bash
java -jar ai-reviewer.jar \
  --github-url https://github.com/user/project \
  --branch develop \
  --team "Team Awesome" \
  --output score.json
```

### 3. 使用本地目录（原有功能）

```bash
java -jar ai-reviewer.jar \
  --directory /path/to/project \
  --team "Team Awesome" \
  --output score.json \
  --report report.md
```

或使用短选项：
```bash
java -jar ai-reviewer.jar \
  -d /path/to/project \
  -t "Team Awesome" \
  -o score.json \
  -r report.md
```

## 命令行选项

### 必选项（三选一）

| 选项 | 短选项 | 说明 | 示例 |
|------|--------|------|------|
| `--github-url` | - | GitHub 仓库 URL | `--github-url https://github.com/user/project` |
| `--gitee-url` | - | Gitee 仓库 URL | `--gitee-url https://gitee.com/user/project` |
| `--directory` | `-d` | 本地项目目录 | `-d /path/to/project` |
| `--zip` | `-z` | ZIP 压缩包文件 | `-z project.zip` |

### 可选项

| 选项 | 短选项 | 默认值 | 说明 |
|------|--------|--------|------|
| `--team` | `-t` | "Unknown Team" | 团队名称 |
| `--branch` | `-b` | "main" | Git 分支（仅用于 Git URL） |
| `--output` | `-o` | - | 输出评分文件路径（JSON） |
| `--report` | `-r` | - | 输出报告文件路径（Markdown） |
| `--help` | `-h` | - | 显示帮助信息 |

## ZIP 文件支持详情

### 支持的功能

✅ **自动解压** - 自动解压 ZIP 文件到临时目录
✅ **智能根目录识别** - 如果 ZIP 只包含一个子目录，自动使用该子目录作为项目根
✅ **文件验证** - 验证 ZIP 文件格式（检查文件头）
✅ **安全路径检查** - 防止路径遍历攻击
✅ **自动清理** - 评审完成后自动清理临时文件

### ZIP 文件要求

1. **格式**：标准 ZIP 格式（`.zip` 扩展名）
2. **结构**：
   - 可以直接包含项目文件
   - 也可以有一层目录包裹（会自动识别）
3. **大小**：建议不超过 500MB

### ZIP 文件结构示例

#### 示例 1：直接包含项目文件
```
project.zip
├── src/
├── pom.xml
├── README.md
└── ...
```

#### 示例 2：有一层目录包裹（推荐）
```
project.zip
└── my-project/
    ├── src/
    ├── pom.xml
    ├── README.md
    └── ...
```
工具会自动识别 `my-project` 作为项目根目录。

## 临时文件处理

### 临时目录位置

ZIP 文件会解压到以下位置：
- **Windows**: `%TEMP%\hackathon-zip-extract\`
- **Linux/Mac**: `/tmp/hackathon-zip-extract/`

### 自动清理

- ✅ 评审完成后自动删除临时文件
- ✅ 评审失败也会清理
- ✅ 每次解压使用唯一的时间戳目录

## 完整示例

### 场景 1: 基本 ZIP 评审

```bash
# 准备 ZIP 文件
# 假设有文件: team-awesome-project.zip

# 运行评审
java -jar ai-reviewer.jar \
  --zip team-awesome-project.zip \
  --team "Team Awesome" \
  --output ./results/team-awesome-score.json \
  --report ./results/team-awesome-report.md
```

**输出：**
```
正在解压 ZIP 文件: team-awesome-project.zip
ZIP 文件解压完成: /tmp/hackathon-zip-extract/team-awesome-project-1234567890
检测到单一子目录，使用作为项目根: team-awesome-project
正在扫描项目...
项目信息:
  - 团队: Team Awesome
  - 名称: team-awesome-project
  - 类型: Java
  - 文件数: 45
  - 代码行数: 3250

正在分析项目...
分析完成！

=== 黑客松评审结果 ===
团队: Team Awesome
总体评分: 85/100 (B)
...
```

### 场景 2: 批量评审多个 ZIP

创建脚本 `batch-review.sh`:

```bash
#!/bin/bash

# 批量评审多个团队的 ZIP 文件
for zipfile in submissions/*.zip; do
    team_name=$(basename "$zipfile" .zip)
    echo "评审团队: $team_name"
    
    java -jar ai-reviewer.jar \
      --zip "$zipfile" \
      --team "$team_name" \
      --output "results/${team_name}-score.json" \
      --report "results/${team_name}-report.md"
    
    echo "---"
done

echo "批量评审完成！"
```

运行：
```bash
chmod +x batch-review.sh
./batch-review.sh
```

### 场景 3: Windows 批处理

创建 `batch-review.bat`:

```cmd
@echo off
setlocal enabledelayedexpansion

for %%f in (submissions\*.zip) do (
    set "filename=%%~nf"
    echo 评审团队: !filename!
    
    java -jar ai-reviewer.jar ^
      --zip "%%f" ^
      --team "!filename!" ^
      --output "results\!filename!-score.json" ^
      --report "results\!filename!-report.md"
    
    echo ---
)

echo 批量评审完成！
pause
```

## 错误处理

### 常见错误

#### 1. ZIP 文件不存在
```
错误: ZIP 文件不存在: /path/to/project.zip
```
**解决**: 检查文件路径是否正确

#### 2. 不是有效的 ZIP 文件
```
错误: 不是有效的 ZIP 文件: /path/to/file.txt
```
**解决**: 确保文件是 ZIP 格式

#### 3. 解压失败
```
错误: 解压失败: java.util.zip.ZipException
```
**解决**: ZIP 文件可能损坏，重新压缩

#### 4. 磁盘空间不足
```
错误: 无法创建临时目录
```
**解决**: 清理临时目录或增加磁盘空间

## 技术实现

### 新增组件

#### ZipArchiveAdapter
- 位置: `adapter/output/archive/ZipArchiveAdapter.java`
- 职责: ZIP 文件解压和验证
- 特性:
  - 安全路径检查
  - 文件格式验证
  - 自动清理

#### 修改的组件

##### HackathonCommandLineApp
- 添加 `--zip` / `-z` 参数支持
- 添加 `extractZipFile()` 方法
- 添加 `findProjectRoot()` 方法
- 集成自动清理逻辑

### 工作流程

```
1. 用户提供 ZIP 文件路径
   ↓
2. 验证 ZIP 文件存在和格式
   ↓
3. 解压到临时目录 (带时间戳)
   ↓
4. 智能识别项目根目录
   ↓
5. 扫描和分析项目
   ↓
6. 生成评分和报告
   ↓
7. 自动清理临时文件
```

## 性能考虑

### 解压性能

- **小项目** (< 10MB): < 1 秒
- **中等项目** (10-100MB): 1-5 秒
- **大项目** (100-500MB): 5-20 秒

### 磁盘占用

- 临时文件占用空间 = ZIP 文件大小 × 2-3 倍
- 自动清理确保不占用长期空间

## 最佳实践

### 1. ZIP 文件准备

✅ **推荐**:
```bash
# 在项目目录外创建 ZIP
cd /path/to/parent
zip -r project.zip my-project/

# 或者在项目目录内
cd /path/to/my-project
zip -r ../my-project.zip .
```

❌ **避免**:
```bash
# 不要包含 .git 目录（太大）
zip -r project.zip my-project/ -x "*.git*"
```

### 2. 文件大小

- ✅ 只包含源代码和配置文件
- ❌ 不要包含 `node_modules/`, `target/`, `build/` 等
- ❌ 不要包含二进制文件、数据库文件

### 3. 目录结构

推荐的 ZIP 结构：
```
team-awesome-project.zip
└── team-awesome-project/
    ├── src/
    ├── pom.xml / package.json / requirements.txt
    ├── README.md
    └── 其他源文件
```

## 故障排除

### 调试模式

启用详细日志：
```bash
java -Dlogging.level=DEBUG -jar ai-reviewer.jar \
  --zip project.zip \
  --team "Team Awesome"
```

### 查看临时文件

如果需要保留临时文件进行调试，在代码中注释掉清理逻辑：
```java
// 在 execute() 方法的 finally 块中
// cleanupTemporaryDirectory(projectPath);  // 注释掉这行
```

### 手动清理

如果程序异常退出，手动清理：
```bash
# Linux/Mac
rm -rf /tmp/hackathon-zip-extract/

# Windows
rd /s /q %TEMP%\hackathon-zip-extract\
```

## 总结

✅ **新增功能：**
- ZIP 压缩包支持
- 自动解压和清理
- 智能根目录识别
- 安全路径检查

✅ **使用场景：**
- 团队提交 ZIP 文件
- 批量评审
- 离线项目评审
- 无 Git 仓库的项目

✅ **优势：**
- 简单快捷
- 自动清理
- 安全可靠
- 兼容性好

现在黑客松评审工具支持更灵活的项目输入方式！🎉

