# 🚀 部署指南 - 完整版

## ✅ 已完成的工作

### 1. 项目配置更新
- ✅ 修改 `pom.xml`，排除 `simpleExcel` 目录
- ✅ 配置 Maven 资源插件，打包时不包含测试文档

### 2. 发布包创建
- ✅ 创建 `release/` 发布目录
- ✅ 生成可执行 JAR 包（587 MB）
- ✅ 创建外置配置文件
- ✅ 创建启动/停止脚本
- ✅ 创建完整的使用文档

### 3. 打包脚本
- ✅ `build-and-deploy.bat` - 自动化打包部署
- ✅ `test-release.bat` - 发布包测试脚本

---

## 📦 发布包结构

```
release/
├── ai-reviewer-base-file-rag-1.0.jar  # 应用JAR包（587 MB）
│                                       # 已排除simpleExcel目录
│
├── start.bat                           # Windows启动脚本
├── stop.bat                            # Windows停止脚本
├── README.md                           # 完整使用文档
│
├── config/
│   └── application.yml                 # 外置配置文件
│                                       # - 文档路径: ./data/documents
│                                       # - 重建模式: false（生产环境）
│                                       # - 所有配置项都可修改
│
├── data/
│   ├── documents/                      # 文档目录（需要用户添加）
│   ├── knowledge-base/                 # 知识库存储（自动生成）
│   └── vector-index/                   # 向量索引（自动生成）
│
└── logs/
    └── knowledge-qa-system.log         # 应用日志（自动生成）
```

---

## 🎯 使用方式

### 方式1: 开发环境重新打包

如果需要重新打包（例如代码有更新）：

```batch
cd D:\Jetbrains\hackathon\AI-Reviewer\ai-reviewer-base-file-rag

# 执行打包脚本
build-and-deploy.bat
```

脚本会自动：
1. 清理旧的构建
2. 编译项目（排除 simpleExcel）
3. 打包 JAR
4. 复制到 release 目录
5. 显示发布包结构

### 方式2: 直接使用现有发布包

```batch
cd D:\Jetbrains\hackathon\AI-Reviewer\ai-reviewer-base-file-rag\release

# 1. 放入文档
copy E:\你的文档\*.* data\documents\

# 2. 修改配置（可选）
notepad config\application.yml

# 3. 启动应用
start.bat
```

---

## ⚙️ 配置外置应用

### 当前配置 (config/application.yml)

```yaml
knowledge:
  qa:
    knowledge-base:
      # 文档路径指向外部目录
      source-path: ./data/documents
      
      # 生产环境不自动重建
      rebuild-on-startup: false
      
    llm:
      # 默认使用Mock LLM
      provider: mock
```

### 启动时加载外置配置

`start.bat` 已配置自动加载外置配置：

```batch
java %JAVA_OPTS% ^
  -jar %JAR_FILE% ^
  --spring.config.location=file:./config/application.yml ^
  --logging.file.name=./logs/knowledge-qa-system.log
```

**说明**:
- `--spring.config.location=file:./config/application.yml` - 加载外置配置
- `--logging.file.name=./logs/knowledge-qa-system.log` - 指定日志文件

---

## 🔧 配置修改示例

### 1. 修改文档路径

```yaml
# config/application.yml
knowledge:
  qa:
    knowledge-base:
      source-path: E:/公司文档/知识库  # 使用绝对路径
```

### 2. 修改端口

```yaml
server:
  port: 9090  # 改为9090端口
```

### 3. 启用真实LLM

```yaml
knowledge:
  qa:
    llm:
      provider: openai
      api-key: sk-your-api-key
      model: gpt-4o
```

### 4. 禁用向量检索

```yaml
knowledge:
  qa:
    vector-search:
      enabled: false  # 仅使用关键词检索
```

---

## 🚀 部署到生产服务器

### 步骤1: 复制发布包

将整个 `release/` 目录复制到目标服务器：

```batch
# 例如复制到 D:\apps\knowledge-qa\
xcopy /E /I release D:\apps\knowledge-qa\
```

### 步骤2: 准备文档

将文档放到 `data\documents\` 目录：

```batch
cd D:\apps\knowledge-qa
copy E:\文档\*.* data\documents\
```

### 步骤3: 修改配置

根据实际环境修改 `config\application.yml`：

```yaml
knowledge:
  qa:
    knowledge-base:
      source-path: ./data/documents
      rebuild-on-startup: false  # 生产环境
      
    llm:
      provider: openai  # 使用真实LLM
      api-key: ${AI_API_KEY}  # 从环境变量读取
```

### 步骤4: 设置环境变量（可选）

```batch
# 设置API Key
set AI_API_KEY=sk-your-api-key

# 或永久设置
setx AI_API_KEY "sk-your-api-key"
```

### 步骤5: 首次启动

```batch
# 进入应用目录
cd D:\apps\knowledge-qa

# 首次启动（构建知识库）
# 临时启用rebuild模式
java -jar ai-reviewer-base-file-rag-1.0.jar ^
  --spring.config.location=file:./config/application.yml ^
  --knowledge.qa.knowledge-base.rebuild-on-startup=true

# 等待知识库构建完成后，按Ctrl+C停止
```

### 步骤6: 正常启动

```batch
# 使用启动脚本
start.bat

# 或使用命令行
java -jar ai-reviewer-base-file-rag-1.0.jar ^
  --spring.config.location=file:./config/application.yml
```

---

## 🧪 验证部署

### 1. 检查应用启动

观察启动日志，应该看到：

```
✅ 知识库问答系统初始化完成！
```

### 2. 测试API

```bash
# 健康检查
curl http://localhost:8080/api/qa/health

# 统计信息
curl http://localhost:8080/api/qa/statistics

# 搜索测试
curl "http://localhost:8080/api/qa/search?query=测试&limit=5"
```

### 3. 检查日志

```batch
# 查看日志文件
type logs\knowledge-qa-system.log
```

### 4. 验证数据目录

```batch
# 检查知识库是否生成
dir data\knowledge-base
dir data\vector-index
```

---

## 📊 启动参数说明

### JVM 参数（在 start.bat 中）

```batch
# 内存配置
-Xms512m          # 初始堆内存 512MB
-Xmx2g            # 最大堆内存 2GB

# 垃圾回收
-XX:+UseG1GC      # 使用G1垃圾回收器
-XX:MaxGCPauseMillis=200  # 最大GC暂停时间

# 编码
-Dfile.encoding=UTF-8
-Dsun.stdout.encoding=UTF-8
-Dsun.stderr.encoding=UTF-8
```

### Spring Boot 参数

```batch
# 配置文件位置
--spring.config.location=file:./config/application.yml

# 日志文件位置
--logging.file.name=./logs/knowledge-qa-system.log

# 运行时覆盖配置
--server.port=9090
--knowledge.qa.knowledge-base.rebuild-on-startup=true
```

---

## 🔍 常见问题

### Q1: 如何更新文档？

**A**: 两种方式：

方式1: 重建知识库（推荐小量文档）
```batch
# 停止应用
stop.bat

# 添加/删除文档
copy 新文档.pdf data\documents\

# 临时启用重建
java -jar ai-reviewer-base-file-rag-1.0.jar ^
  --spring.config.location=file:./config/application.yml ^
  --knowledge.qa.knowledge-base.rebuild-on-startup=true
```

方式2: 增量更新
```yaml
# config/application.yml
knowledge:
  qa:
    knowledge-base:
      rebuild-on-startup: false
```
添加新文档后，调用增量索引API（如果实现了）

### Q2: JAR包太大怎么办？

**A**: JAR包大小主要来自：
- 依赖库（Lucene, Tika, POI等）
- ONNX模型文件（如果包含）

优化方案：
1. 排除不需要的模型文件
2. 使用外部模型路径
3. 使用瘦JAR + 依赖目录

### Q3: 如何查看详细日志？

**A**: 
```yaml
# config/application.yml
logging:
  level:
    root: INFO
    top.yumbo.ai.rag: DEBUG  # 详细日志
```

### Q4: 如何设置开机自启动？

**A**: 创建 Windows 任务计划：

```batch
# 创建任务计划
schtasks /create /tn "KnowledgeQA" ^
  /tr "D:\apps\knowledge-qa\start.bat" ^
  /sc onstart /ru SYSTEM
```

---

## 📁 重要文件说明

### 1. ai-reviewer-base-file-rag-1.0.jar

- **大小**: 587 MB
- **内容**: 
  - 应用代码
  - 依赖库
  - ONNX模型文件
  - 默认配置（不包含simpleExcel）
- **不包含**: simpleExcel 测试文档

### 2. config/application.yml

- **作用**: 外置配置文件
- **优先级**: 高于JAR内配置
- **可修改**: 所有配置项
- **编码**: UTF-8

### 3. start.bat

- **功能**: 
  - 检查Java环境
  - 检查配置文件
  - 设置JVM参数
  - 启动应用
  - 加载外置配置

### 4. data/documents/

- **用途**: 存放要索引的文档
- **支持格式**: Excel, Word, PDF, TXT等
- **大小限制**: 单文件 < 200MB

---

## 🎓 最佳实践

### 1. 配置管理

```batch
# 使用版本控制管理配置
git add config/application.yml
git commit -m "更新配置"
```

### 2. 日志管理

```batch
# 定期清理旧日志
forfiles /p logs /s /m *.log /d -7 /c "cmd /c del @path"

# 或在配置中设置日志滚动
logging:
  file:
    max-size: 10MB
    max-history: 7
```

### 3. 数据备份

```batch
# 备份知识库
xcopy /E /I data\knowledge-base backup\knowledge-base-%date%

# 备份向量索引
xcopy /E /I data\vector-index backup\vector-index-%date%
```

### 4. 性能监控

```yaml
# 启用Actuator端点
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,info
```

---

## 📞 技术支持

- **文档**: `release/README.md`
- **日志**: `release/logs/knowledge-qa-system.log`
- **配置**: `release/config/application.yml`

---

## ✅ 部署检查清单

部署前请确认：

- [ ] JAR文件已生成且大小正常（~587MB）
- [ ] 外置配置文件已创建
- [ ] 启动脚本已创建且可执行
- [ ] 文档目录已创建
- [ ] Java环境已安装（JDK 17+）
- [ ] 端口未被占用（默认8080）
- [ ] 有足够的磁盘空间（建议5GB+）
- [ ] 有足够的内存（建议2GB+）

部署后请验证：

- [ ] 应用正常启动
- [ ] 知识库构建成功
- [ ] API可以访问
- [ ] 搜索功能正常
- [ ] 日志文件正常写入

---

<div align="center">

## 🎊 部署完成！

所有文件已准备就绪，可以开始部署了！

**快速开始**:
```batch
cd release
start.bat
```

Made with ❤️ by AI Assistant  
2025-11-23

</div>

