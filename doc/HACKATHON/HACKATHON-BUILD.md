# 黑客松评审工具 - 构建指南

## 概述

本文档说明如何构建黑客松评审工具的可执行 JAR 文件（`hackathon-reviewer.jar`）。

---

## 🚀 快速构建

### 方法 1: 使用 Maven 命令（推荐）

```bash
# 快速构建（跳过测试）
mvn clean package -f hackathon-pom.xml

# 输出文件：target/hackathon-reviewer.jar
```

### 方法 2: 使用构建脚本

#### Linux/Mac

```bash
# 添加执行权限
chmod +x build-hackathon.sh

# 运行构建脚本
./build-hackathon.sh
```

#### Windows

```cmd
# 直接运行
build-hackathon.bat
```

---

## 📋 构建选项

### 1. 快速构建（默认）

跳过测试，快速生成 JAR 文件：

```bash
mvn clean package -f hackathon-pom.xml -Pquick
```

**适用场景**:
- 快速迭代开发
- 测试命令行工具
- 生成演示版本

### 2. 完整构建

包含所有测试：

```bash
mvn clean package -f hackathon-pom.xml
```

**适用场景**:
- 正式发布前
- 确保代码质量
- CI/CD 流水线

### 3. 生产构建

包含源码和文档：

```bash
mvn clean package -f hackathon-pom.xml -Pproduction
```

**适用场景**:
- 正式发布
- 需要源码包
- 需要 Javadoc

---

## 📦 输出文件

构建成功后，将在 `target/` 目录下生成：

```
target/
├── hackathon-reviewer.jar          # 可执行 JAR（主要文件）
├── hackathon-reviewer-sources.jar  # 源码包（生产构建）
└── hackathon-reviewer-javadoc.jar  # 文档包（生产构建）
```

### 主要文件

- **`hackathon-reviewer.jar`**: 
  - 包含所有依赖的 fat JAR
  - 可直接运行：`java -jar hackathon-reviewer.jar`
  - 大小约 100-150 MB

---

## 🔧 构建配置

### 主类配置

在 `hackathon-pom.xml` 中配置：

```xml
<properties>
    <!-- 主类：黑客松命令行工具入口 -->
    <main.class>top.yumbo.ai.reviewer.application.hackathon.cli.HackathonCommandLineApp</main.class>
    
    <!-- 输出 JAR 文件名 -->
    <final.name>hackathon-reviewer</final.name>
</properties>
```

### 包含的依赖

- **核心框架**: Guice (依赖注入)
- **JSON/YAML**: Jackson, FastJSON, SnakeYAML
- **HTTP 客户端**: OkHttp
- **日志**: SLF4J + Logback
- **代码解析**: JavaParser, ANTLR
- **AWS SDK**: Bedrock (AI), S3 (存储)
- **Git 操作**: JGit
- **其他**: Apache POI, PDFBox, Tika, etc.

---

## ✅ 验证构建

### 1. 检查 JAR 是否生成

```bash
ls -lh target/hackathon-reviewer.jar
```

### 2. 查看 JAR 内容

```bash
jar tf target/hackathon-reviewer.jar | head -20
```

### 3. 验证主类

```bash
java -jar target/hackathon-reviewer.jar --help
```

**预期输出**:
```
🏆 黑客松项目评审工具

用法:
  java -jar hackathon-reviewer.jar [选项]

选项:
  --github-url <URL>      GitHub 仓库 URL
  --gitee-url <URL>       Gitee 仓库 URL
  --directory <路径>      本地项目目录
  --zip <文件>            ZIP 压缩包文件路径
  --s3-path <路径>        S3 存储路径
  ...
```

### 4. 测试运行

```bash
# 测试本地项目评审
java -jar target/hackathon-reviewer.jar \
  -d ./test-project \
  -t "Test Team" \
  -o test-score.json
```

---

## 🐛 常见问题

### Q1: 构建失败 - "Cannot find symbol"

**原因**: 依赖未正确下载或源码有错误

**解决**:
```bash
# 清理并重新下载依赖
mvn clean
mvn dependency:purge-local-repository
mvn clean package -f hackathon-pom.xml
```

### Q2: JAR 无法运行 - "no main manifest attribute"

**原因**: Maven Shade 插件未正确配置

**解决**: 检查 `hackathon-pom.xml` 中的 `maven-shade-plugin` 配置

### Q3: 运行时 ClassNotFoundException

**原因**: 某些依赖未包含在 fat JAR 中

**解决**: 检查依赖的 `scope`，确保不是 `provided` 或 `test`

### Q4: JAR 文件太大

**原因**: 包含了所有传递依赖

**优化**:
```xml
<!-- 在 maven-shade-plugin 中启用最小化 -->
<minimizeJar>true</minimizeJar>
```

### Q5: 构建时间太长

**解决**:
```bash
# 使用快速构建模式
mvn clean package -f hackathon-pom.xml -Pquick -T 4
# -T 4: 使用 4 个线程并行构建
```

---

## 🎯 构建最佳实践

### 1. 开发阶段

```bash
# 快速构建 + 跳过测试
mvn clean package -f hackathon-pom.xml -Pquick

# 增量构建（不清理）
mvn package -f hackathon-pom.xml -Pquick
```

### 2. 测试阶段

```bash
# 包含测试
mvn clean package -f hackathon-pom.xml

# 只运行测试
mvn test -f hackathon-pom.xml
```

### 3. 发布阶段

```bash
# 完整构建（包含源码和文档）
mvn clean package -f hackathon-pom.xml -Pproduction

# 安装到本地仓库
mvn clean install -f hackathon-pom.xml -Pproduction
```

### 4. CI/CD 流水线

```bash
# 完整构建 + 并行化
mvn clean package -f hackathon-pom.xml -T 1C

# 部署到远程仓库
mvn deploy -f hackathon-pom.xml -Pproduction
```

---

## 📊 构建性能

### 典型构建时间

| 构建模式 | 时间 | 说明 |
|---------|------|------|
| 快速构建 | 30-60 秒 | 跳过测试 |
| 完整构建 | 2-5 分钟 | 包含测试 |
| 生产构建 | 3-6 分钟 | 包含源码和文档 |

### 优化建议

1. **使用 Maven Daemon**:
   ```bash
   # 安装 Maven Daemon
   brew install mvnd  # Mac
   # 或从 https://github.com/apache/maven-mvnd 下载
   
   # 使用 mvnd 替代 mvn
   mvnd clean package -f hackathon-pom.xml
   ```

2. **并行构建**:
   ```bash
   mvn clean package -f hackathon-pom.xml -T 1C
   # -T 1C: 每个 CPU 核心一个线程
   ```

3. **离线模式**（依赖已下载）:
   ```bash
   mvn clean package -f hackathon-pom.xml -o
   ```

---

## 🔄 持续集成示例

### GitHub Actions

```yaml
name: Build Hackathon Reviewer

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: maven
    
    - name: Build with Maven
      run: mvn clean package -f hackathon-pom.xml -Pquick
    
    - name: Upload JAR
      uses: actions/upload-artifact@v3
      with:
        name: hackathon-reviewer
        path: target/hackathon-reviewer.jar
```

### GitLab CI

```yaml
build:
  image: maven:3.9-eclipse-temurin-17
  stage: build
  script:
    - mvn clean package -f hackathon-pom.xml -Pquick
  artifacts:
    paths:
      - target/hackathon-reviewer.jar
    expire_in: 1 week
```

---

## 📝 版本管理

### 修改版本号

编辑 `hackathon-pom.xml`:

```xml
<groupId>top.yumbo.ai</groupId>
<artifactId>hackathon-reviewer</artifactId>
<version>2.1</version>  <!-- 修改这里 -->
```

或使用 Maven 命令:

```bash
mvn versions:set -DnewVersion=2.1 -f hackathon-pom.xml
```

### 版本标记

```bash
# Git 标记
git tag -a v2.0 -m "Release version 2.0"
git push origin v2.0
```

---

## 🚀 发布检查清单

构建正式发布版本前，请确认：

- [ ] 代码已提交到 Git
- [ ] 版本号已更新
- [ ] 所有测试通过
- [ ] `config.yaml` 配置正确
- [ ] 文档已更新
- [ ] 更新日志已记录
- [ ] 使用生产构建模式
- [ ] JAR 文件已测试
- [ ] 帮助信息正确显示

---

## 🎊 总结

### 日常开发

```bash
mvn clean package -f hackathon-pom.xml -Pquick
```

### 正式发布

```bash
mvn clean package -f hackathon-pom.xml -Pproduction
```

### 使用构建脚本

```bash
# Linux/Mac
./build-hackathon.sh

# Windows
build-hackathon.bat
```

---

**构建完成后，您将得到一个完整的、可独立运行的黑客松评审工具！** 🎉

---

**相关文档**:
- 使用指南: `doc/HACKATHON/README.md`
- 配置说明: `config.yaml`
- 开发文档: `doc/HACKATHON/HACKATHON-IMPLEMENTATION-GUIDE.md`

