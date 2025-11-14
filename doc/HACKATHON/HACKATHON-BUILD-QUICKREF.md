# 黑客松 JAR 构建 - 快速参考

## 🚀 快速构建

```bash
# 方法 1: 直接使用 Maven
mvn clean package -f hackathon-pom.xml

# 方法 2: 使用构建脚本
./build-hackathon.sh          # Linux/Mac
build-hackathon.bat           # Windows

# 方法 3: 快速模式（跳过测试）
mvn clean package -f hackathon-pom.xml -Pquick
```

## 📦 输出文件

```
target/hackathon-reviewer.jar
```

## ✅ 验证构建

```bash
# 查看文件
ls -lh target/hackathon-reviewer.jar

# 测试运行
java -jar target/hackathon-reviewer.jar --help
```

## 🎯 使用示例

```bash
# 评审本地项目
java -jar target/hackathon-reviewer.jar \
  -d /path/to/project \
  -t "Team Name" \
  -o score.json

# 评审 GitHub 项目
java -jar target/hackathon-reviewer.jar \
  --github-url https://github.com/user/repo \
  -t "Team Name" \
  -o score.json

# 评审 ZIP 文件
java -jar target/hackathon-reviewer.jar \
  -z project.zip \
  -t "Team Name" \
  -o score.json

# 评审 S3 项目
java -jar target/hackathon-reviewer.jar \
  -s projects/team-name/ \
  -t "Team Name" \
  -o score.json
```

## 📋 构建模式

| 模式 | 命令 | 说明 |
|------|------|------|
| 快速 | `-Pquick` | 跳过测试，快速构建 |
| 完整 | 默认 | 包含测试 |
| 生产 | `-Pproduction` | 包含源码和文档 |

## 🔧 主要配置

### 主类
```
top.yumbo.ai.reviewer.application.hackathon.cli.HackathonCommandLineApp
```

### 输出文件名
```
hackathon-reviewer.jar
```

### 打包方式
- Maven Shade Plugin
- Fat JAR（包含所有依赖）
- 可直接运行

## 📝 文件清单

- `hackathon-pom.xml` - Maven 构建配置
- `build-hackathon.sh` - Linux/Mac 构建脚本
- `build-hackathon.bat` - Windows 构建脚本
- `HACKATHON-BUILD.md` - 详细构建指南

## 🐛 故障排除

### 构建失败
```bash
# 清理并重新构建
mvn clean
mvn clean package -f hackathon-pom.xml -Pquick
```

### 依赖下载问题
```bash
# 强制更新依赖
mvn clean package -f hackathon-pom.xml -U
```

### JAR 无法运行
```bash
# 检查 Java 版本（需要 JDK 17+）
java -version

# 查看 JAR 清单
jar xf target/hackathon-reviewer.jar META-INF/MANIFEST.MF
cat META-INF/MANIFEST.MF
```

## ⚡ 性能优化

```bash
# 并行构建
mvn clean package -f hackathon-pom.xml -T 1C

# 离线模式（依赖已下载）
mvn clean package -f hackathon-pom.xml -o -Pquick

# 跳过测试和检查
mvn clean package -f hackathon-pom.xml -DskipTests -Dmaven.test.skip=true
```

## 📊 预期结果

### 构建时间
- 快速模式: 30-60 秒
- 完整模式: 2-5 分钟

### 文件大小
- 约 100-150 MB（包含所有依赖）

### 系统要求
- JDK 17 或更高版本
- Maven 3.6 或更高版本
- 足够的磁盘空间（至少 500 MB）

---

**详细文档**: `HACKATHON-BUILD.md`

