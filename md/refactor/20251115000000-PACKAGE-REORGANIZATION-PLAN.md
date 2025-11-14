# AI文件分析引擎 - 包结构重组方案

**生成时间**: 2025-11-15 00:00:00  
**架构师**: 世界顶级架构师  
**项目定位**: 通用文件分析引擎（利用市面AI服务分析各类文件）  
**黑客松定位**: 应用案例之一，非核心关注点

---

## 📋 执行摘要

本项目本质是一个**通用文件分析引擎**，可以：
1. 读取文件夹中的各类文件（代码、媒体、文档等）
2. 利用市面上的AI服务进行内容分析
3. 支持多种存储方式（本地、S3等）
4. 提供可扩展的插件架构

黑客松只是一个应用场景案例，不应过度关注。

---

## 🎯 重组目标

### 核心原则
1. **功能模块化**: 按功能领域组织包结构
2. **清晰职责**: 每个包有明确的职责边界
3. **易于扩展**: 新增文件类型或AI服务时，路径清晰
4. **六边形架构**: 保持端口-适配器模式

### 包结构设计理念
```
top.yumbo.ai.reviewer/
├── core/              # 核心引擎（文件分析、任务调度）
├── adapter/           # 适配器层
│   ├── storage/       # 存储适配器（S3、本地、缓存）
│   ├── ai/            # AI服务适配器
│   ├── parser/        # 文件解析器（代码、文档、媒体）
│   ├── input/         # 输入适配器（CLI、API）
│   └── output/        # 输出适配器（报告、可视化）
├── domain/            # 领域模型（通用+具体场景）
├── application/       # 应用服务层
└── infrastructure/    # 基础设施（配置、DI、工厂）
```

---

## 🔄 详细重组方案

### 1. 存储相关类 → `adapter.storage`

#### 1.1 S3存储模块 → `adapter.storage.s3`
```
移动前位置:
├── adapter/output/storage/S3StorageAdapter.java
├── adapter/output/storage/S3StorageConfig.java
├── adapter/output/storage/S3StorageExample.java
├── application/port/output/S3StoragePort.java
├── application/service/S3StorageService.java
├── domain/model/S3File.java
├── domain/model/S3DownloadResult.java

移动后位置:
├── adapter/storage/s3/S3StorageAdapter.java
├── adapter/storage/s3/S3Config.java
├── adapter/storage/s3/S3Example.java
├── adapter/storage/s3/port/S3StoragePort.java (或保持在port包)
├── application/service/storage/S3StorageService.java
├── domain/model/storage/S3File.java
├── domain/model/storage/S3DownloadResult.java
```

#### 1.2 本地文件系统 → `adapter.storage.local`
```
移动前位置:
├── adapter/output/filesystem/LocalFileSystemAdapter.java
├── application/port/output/FileSystemPort.java

移动后位置:
├── adapter/storage/local/LocalFileSystemAdapter.java
├── adapter/storage/local/port/FileSystemPort.java (或保持在port包)
```

#### 1.3 缓存 → `adapter.storage.cache`
```
移动前位置:
├── adapter/output/cache/FileCacheAdapter.java
├── application/port/output/CachePort.java

移动后位置:
├── adapter/storage/cache/FileCacheAdapter.java
├── adapter/storage/cache/port/CachePort.java (或保持在port包)
```

#### 1.4 压缩归档 → `adapter.storage.archive`
```
移动前位置:
├── adapter/output/archive/ZipArchiveAdapter.java

移动后位置:
├── adapter/storage/archive/ZipArchiveAdapter.java
├── adapter/storage/archive/TarArchiveAdapter.java (未来扩展)
├── adapter/storage/archive/RarArchiveAdapter.java (未来扩展)
```

---

### 2. AI服务相关类 → `adapter.ai`

#### 2.1 通用AI适配器 → `adapter.ai`
```
移动前位置:
├── adapter/output/ai/AIAdapterFactory.java
├── adapter/output/ai/AIServiceConfig.java
├── adapter/output/ai/HttpBasedAIAdapter.java
├── adapter/output/ai/LoggingAIServiceDecorator.java
├── application/port/output/AIServicePort.java

移动后位置:
├── adapter/ai/AIAdapterFactory.java
├── adapter/ai/config/AIServiceConfig.java
├── adapter/ai/http/HttpBasedAIAdapter.java
├── adapter/ai/decorator/LoggingAIServiceDecorator.java
├── adapter/ai/port/AIServicePort.java (或保持在port包)
```

#### 2.2 AWS Bedrock → `adapter.ai.bedrock`
```
移动前位置:
├── adapter/output/ai/BedrockAdapter.java

移动后位置:
├── adapter/ai/bedrock/BedrockAdapter.java
├── adapter/ai/bedrock/BedrockConfig.java (新增)
├── adapter/ai/bedrock/BedrockModelRegistry.java (新增)
```

#### 2.3 其他AI服务 (未来扩展)
```
建议新增:
├── adapter/ai/openai/OpenAIAdapter.java
├── adapter/ai/azure/AzureOpenAIAdapter.java
├── adapter/ai/anthropic/ClaudeAdapter.java
├── adapter/ai/google/GeminiAdapter.java
├── adapter/ai/local/LocalLLMAdapter.java (Ollama等)
```

---

### 3. 文件解析器相关类 → `adapter.parser`

#### 3.1 代码解析器（AST）→ `adapter.parser.code`
```
移动前位置:
├── adapter/output/ast/parser/AbstractASTParser.java
├── adapter/output/ast/parser/ASTParserFactory.java
├── adapter/output/ast/parser/JavaParserAdapter.java
├── adapter/output/ast/parser/PythonParserAdapter.java
├── adapter/output/ast/parser/JavaScriptParserAdapter.java
├── adapter/output/ast/parser/GoParserAdapter.java
├── adapter/output/ast/parser/CppParserAdapter.java
├── application/port/output/ASTParserPort.java

移动后位置:
├── adapter/parser/code/AbstractASTParser.java
├── adapter/parser/code/ASTParserFactory.java
├── adapter/parser/code/java/JavaParserAdapter.java
├── adapter/parser/code/python/PythonParserAdapter.java
├── adapter/parser/code/javascript/JavaScriptParserAdapter.java
├── adapter/parser/code/go/GoParserAdapter.java
├── adapter/parser/code/cpp/CppParserAdapter.java
├── adapter/parser/code/port/ASTParserPort.java (或保持在port包)
```

#### 3.2 文档解析器 → `adapter.parser.document` (新增)
```
建议新增:
├── adapter/parser/document/PdfParserAdapter.java
├── adapter/parser/document/WordParserAdapter.java
├── adapter/parser/document/ExcelParserAdapter.java
├── adapter/parser/document/PowerPointParserAdapter.java
├── adapter/parser/document/MarkdownParserAdapter.java
├── adapter/parser/document/TextParserAdapter.java
```

#### 3.3 媒体解析器 → `adapter.parser.media` (新增)
```
建议新增:
├── adapter/parser/media/ImageParserAdapter.java (jpg, png, gif等)
├── adapter/parser/media/VideoParserAdapter.java (mp4, avi等)
├── adapter/parser/media/AudioParserAdapter.java (mp3, wav等)
├── adapter/parser/media/metadata/ExifExtractor.java
├── adapter/parser/media/metadata/VideoMetadataExtractor.java
```

#### 3.4 语言检测 → `adapter.parser.detector`
```
移动前位置:
├── adapter/output/filesystem/detector/LanguageDetector.java
├── adapter/output/filesystem/detector/LanguageDetectorRegistry.java
├── adapter/output/filesystem/detector/LanguageFeatures.java
├── adapter/output/filesystem/detector/GoLanguageDetector.java
├── adapter/output/filesystem/detector/CppLanguageDetector.java
├── adapter/output/filesystem/detector/RustLanguageDetector.java

移动后位置:
├── adapter/parser/detector/LanguageDetector.java
├── adapter/parser/detector/LanguageDetectorRegistry.java
├── adapter/parser/detector/LanguageFeatures.java
├── adapter/parser/detector/language/GoLanguageDetector.java
├── adapter/parser/detector/language/CppLanguageDetector.java
├── adapter/parser/detector/language/RustLanguageDetector.java
├── adapter/parser/detector/filetype/FileTypeDetector.java (新增)
├── adapter/parser/detector/filetype/MimeTypeDetector.java (新增)
```

---

### 4. 输入适配器 → `adapter.input`

#### 4.1 命令行接口 → `adapter.input.cli`
```
移动前位置:
├── adapter/input/cli/CommandLineAdapter.java
├── adapter/input/cli/CommandLineInterface.java

移动后位置:
保持不变 (已经在正确位置)
```

#### 4.2 API接口 → `adapter.input.api`
```
移动前位置:
├── adapter/input/api/APIAdapter.java

移动后位置:
├── adapter/input/api/RestAPIAdapter.java (重命名)
├── adapter/input/api/GraphQLAdapter.java (未来扩展)
├── adapter/input/api/WebSocketAdapter.java (未来扩展)
```

#### 4.3 文件监控 → `adapter.input.watcher` (新增)
```
建议新增:
├── adapter/input/watcher/FileSystemWatcher.java
├── adapter/input/watcher/S3EventWatcher.java
```

---

### 5. 输出适配器 → `adapter.output`

#### 5.1 报告生成 → `adapter.output.report`
```
当前没有独立的报告适配器，建议新增:
├── adapter/output/report/MarkdownReportAdapter.java
├── adapter/output/report/HtmlReportAdapter.java
├── adapter/output/report/PdfReportAdapter.java
├── adapter/output/report/JsonReportAdapter.java
```

#### 5.2 可视化 → `adapter.output.visualization`
```
移动前位置:
├── adapter/output/visualization/ChartGenerator.java

移动后位置:
├── adapter/output/visualization/ChartGenerator.java
├── adapter/output/visualization/GraphRenderer.java (新增)
├── adapter/output/visualization/DashboardGenerator.java (新增)
```

#### 5.3 CI/CD集成 → `adapter.output.cicd`
```
移动前位置:
├── adapter/output/cicd/CICDIntegration.java

移动后位置:
├── adapter/output/cicd/GitHubActionsAdapter.java
├── adapter/output/cicd/GitLabCIAdapter.java
├── adapter/output/cicd/JenkinsAdapter.java
```

---

### 6. 仓库相关 → `adapter.repository`

```
移动前位置:
├── adapter/output/repository/GitRepositoryAdapter.java
├── application/port/output/RepositoryPort.java
├── application/port/output/RepositoryMetrics.java
├── application/port/output/CloneRequest.java

移动后位置:
├── adapter/repository/git/GitRepositoryAdapter.java
├── adapter/repository/git/port/RepositoryPort.java (或保持在port包)
├── adapter/repository/git/model/RepositoryMetrics.java
├── adapter/repository/git/model/CloneRequest.java
├── adapter/repository/svn/SvnRepositoryAdapter.java (未来扩展)
├── adapter/repository/mercurial/MercurialAdapter.java (未来扩展)
```

---

### 7. 核心引擎 → `core`

#### 7.1 文件分析引擎 → `core.engine`
```
建议新增:
├── core/engine/FileAnalysisEngine.java (核心分析引擎)
├── core/engine/TaskScheduler.java (任务调度器)
├── core/engine/PipelineOrchestrator.java (流程编排器)
```

#### 7.2 插件系统 → `core.plugin`
```
建议新增:
├── core/plugin/PluginManager.java
├── core/plugin/PluginRegistry.java
├── core/plugin/PluginLoader.java
├── core/plugin/Plugin.java (接口)
```

#### 7.3 事件系统 → `core.event`
```
建议新增:
├── core/event/EventBus.java
├── core/event/Event.java
├── core/event/EventHandler.java
├── core/event/events/FileAnalyzedEvent.java
├── core/event/events/AnalysisStartedEvent.java
```

---

### 8. 领域模型重组 → `domain`

#### 8.1 核心领域模型 → `domain.model`
```
移动前位置:
├── domain/model/Project.java
├── domain/model/ProjectMetadata.java
├── domain/model/ProjectType.java
├── domain/model/SourceFile.java
├── domain/model/ReviewReport.java
├── domain/model/AnalysisTask.java
├── domain/model/AnalysisProgress.java
├── domain/model/AnalysisConfiguration.java

移动后位置:
保持不变 (核心领域模型)
```

#### 8.2 AST模型 → `domain.model.ast`
```
移动前位置:
├── domain/model/ast/*.java (所有AST相关类)

移动后位置:
保持不变 (已经在正确位置)
```

#### 8.3 存储模型 → `domain.model.storage`
```
移动前位置:
├── domain/model/S3File.java
├── domain/model/S3DownloadResult.java

移动后位置:
├── domain/model/storage/StorageFile.java (重命名，通用化)
├── domain/model/storage/S3File.java (S3特定)
├── domain/model/storage/LocalFile.java (本地特定)
├── domain/model/storage/DownloadResult.java (通用化)
```

#### 8.4 场景模型 → `domain.scenario`
```
移动前位置:
├── domain/hackathon/model/*.java

移动后位置:
├── domain/scenario/hackathon/model/*.java (明确这是场景案例)
├── domain/scenario/datascience/model/*.java (未来: 数据科学场景)
├── domain/scenario/compliance/model/*.java (未来: 合规检查场景)
├── domain/scenario/security/model/*.java (未来: 安全审计场景)
```

#### 8.5 文件类型模型 → `domain.model.file` (新增)
```
建议新增:
├── domain/model/file/FileMetadata.java
├── domain/model/file/FileType.java
├── domain/model/file/FileContent.java
├── domain/model/file/code/CodeFile.java
├── domain/model/file/document/DocumentFile.java
├── domain/model/file/media/MediaFile.java
├── domain/model/file/media/ImageFile.java
├── domain/model/file/media/VideoFile.java
```

---

### 9. 应用服务层重组 → `application`

#### 9.1 核心服务 → `application.service.core`
```
移动前位置:
├── application/service/ProjectAnalysisService.java
├── application/service/ReportGenerationService.java
├── application/service/ComparisonReportGenerator.java
├── application/service/QualityGateEngine.java
├── application/service/AIModelSelector.java

移动后位置:
├── application/service/core/ProjectAnalysisService.java
├── application/service/core/ReportGenerationService.java
├── application/service/core/ComparisonReportGenerator.java
├── application/service/core/QualityGateEngine.java
├── application/service/core/AIModelSelector.java
```

#### 9.2 存储服务 → `application.service.storage`
```
移动前位置:
├── application/service/S3StorageService.java

移动后位置:
├── application/service/storage/S3StorageService.java
├── application/service/storage/StorageOrchestrator.java (新增)
```

#### 9.3 场景服务 → `application.service.scenario`
```
移动前位置:
├── application/hackathon/service/*.java
├── application/hackathon/cli/*.java

移动后位置:
├── application/service/scenario/hackathon/HackathonAnalysisService.java
├── application/service/scenario/hackathon/HackathonScoringService.java
├── application/service/scenario/hackathon/HackathonIntegrationService.java
├── application/service/scenario/hackathon/LeaderboardService.java
├── application/service/scenario/hackathon/TeamManagementService.java
├── application/cli/scenario/hackathon/HackathonCommandLineApp.java
├── application/cli/scenario/hackathon/HackathonInteractiveApp.java
```

#### 9.4 文件分析服务 → `application.service.analysis` (新增)
```
建议新增:
├── application/service/analysis/FileAnalysisService.java (通用文件分析)
├── application/service/analysis/CodeAnalysisService.java (代码分析)
├── application/service/analysis/DocumentAnalysisService.java (文档分析)
├── application/service/analysis/MediaAnalysisService.java (媒体分析)
├── application/service/analysis/BatchAnalysisService.java (批量分析)
```

#### 9.5 Prompt构建 → `application.service.prompt`
```
移动前位置:
├── application/service/prompt/AIPromptBuilder.java

移动后位置:
保持不变
```

---

### 10. 端口定义 → `application.port`

#### 10.1 输入端口 → `application.port.input`
```
移动前位置:
├── application/port/input/ProjectAnalysisUseCase.java
├── application/port/input/ReportGenerationUseCase.java

移动后位置:
保持不变，建议新增:
├── application/port/input/FileAnalysisUseCase.java (通用文件分析)
├── application/port/input/BatchAnalysisUseCase.java (批量分析)
```

#### 10.2 输出端口 → `application.port.output`
```
移动前位置:
├── application/port/output/*.java

移动后位置:
保持端口在统一位置，或者按功能分类:
├── application/port/output/storage/CachePort.java
├── application/port/output/storage/FileSystemPort.java
├── application/port/output/storage/S3StoragePort.java
├── application/port/output/ai/AIServicePort.java
├── application/port/output/parser/ASTParserPort.java
├── application/port/output/parser/CodeAnalysisPort.java
├── application/port/output/repository/RepositoryPort.java
```

---

### 11. 基础设施 → `infrastructure`

#### 11.1 配置 → `infrastructure.config`
```
移动前位置:
├── infrastructure/config/Configuration.java
├── infrastructure/config/ConfigurationLoader.java

移动后位置:
保持不变，建议新增:
├── infrastructure/config/YamlConfigLoader.java
├── infrastructure/config/JsonConfigLoader.java
├── infrastructure/config/EnvConfigLoader.java
```

#### 11.2 依赖注入 → `infrastructure.di`
```
移动前位置:
├── infrastructure/di/ApplicationModule.java

移动后位置:
保持不变
```

#### 11.3 工厂 → `infrastructure.factory`
```
移动前位置:
├── infrastructure/factory/AIServiceFactory.java

移动后位置:
├── infrastructure/factory/AIServiceFactory.java
├── infrastructure/factory/ParserFactory.java (新增)
├── infrastructure/factory/StorageFactory.java (新增)
```

---

## 📊 重组统计

### 类移动统计
| 类别 | 当前数量 | 移动数量 | 保持不变 | 新增建议 |
|------|---------|---------|---------|---------|
| 存储适配器 | 7 | 7 | 0 | 2 |
| AI适配器 | 5 | 5 | 0 | 6 |
| 解析器 | 11 | 11 | 0 | 15 |
| 输入适配器 | 3 | 1 | 2 | 3 |
| 输出适配器 | 2 | 2 | 0 | 8 |
| 领域模型 | 30+ | 2 | 28+ | 10 |
| 应用服务 | 12 | 12 | 0 | 5 |
| 基础设施 | 5 | 0 | 5 | 3 |
| **总计** | **75+** | **40** | **35+** | **52** |

---

## 🎯 重组优先级

### P0 - 立即执行（本周）
1. ✅ S3相关类 → `adapter.storage.s3`
2. ✅ AI服务类 → `adapter.ai`
3. ✅ AST解析器 → `adapter.parser.code`
4. ✅ 本地文件系统 → `adapter.storage.local`
5. ✅ 缓存适配器 → `adapter.storage.cache`

### P1 - 短期执行（本月）
6. 语言检测器 → `adapter.parser.detector`
7. 仓库适配器 → `adapter.repository.git`
8. 黑客松服务 → `application.service.scenario.hackathon`
9. 领域模型重组 → `domain.scenario.hackathon`
10. 压缩归档 → `adapter.storage.archive`

### P2 - 中期规划（季度）
11. 新增文档解析器 → `adapter.parser.document`
12. 新增媒体解析器 → `adapter.parser.media`
13. 新增核心引擎 → `core.engine`
14. 新增插件系统 → `core.plugin`
15. 输出适配器重组 → `adapter.output`

---

## 📝 重组后的完整包结构

```
top.yumbo.ai.reviewer/
├── core/                                    # 核心引擎 (新增)
│   ├── engine/                              # 分析引擎
│   ├── plugin/                              # 插件系统
│   └── event/                               # 事件系统
│
├── adapter/                                 # 适配器层
│   ├── storage/                             # 存储适配器 (重组)
│   │   ├── s3/                              # AWS S3
│   │   ├── local/                           # 本地文件系统
│   │   ├── cache/                           # 缓存
│   │   └── archive/                         # 压缩归档
│   │
│   ├── ai/                                  # AI服务适配器 (重组)
│   │   ├── bedrock/                         # AWS Bedrock
│   │   ├── openai/                          # OpenAI (新增)
│   │   ├── azure/                           # Azure (新增)
│   │   ├── config/                          # AI配置
│   │   ├── http/                            # HTTP客户端
│   │   └── decorator/                       # 装饰器
│   │
│   ├── parser/                              # 解析器适配器 (重组)
│   │   ├── code/                            # 代码解析器(AST)
│   │   │   ├── java/
│   │   │   ├── python/
│   │   │   ├── javascript/
│   │   │   ├── go/
│   │   │   └── cpp/
│   │   ├── document/                        # 文档解析器 (新增)
│   │   ├── media/                           # 媒体解析器 (新增)
│   │   └── detector/                        # 类型检测器
│   │
│   ├── repository/                          # 仓库适配器 (重组)
│   │   └── git/                             # Git仓库
│   │
│   ├── input/                               # 输入适配器
│   │   ├── cli/                             # 命令行
│   │   ├── api/                             # REST API
│   │   └── watcher/                         # 文件监控 (新增)
│   │
│   └── output/                              # 输出适配器
│       ├── report/                          # 报告生成 (新增)
│       ├── visualization/                   # 可视化
│       └── cicd/                            # CI/CD集成
│
├── domain/                                  # 领域层
│   ├── model/                               # 核心领域模型
│   │   ├── ast/                             # AST模型
│   │   ├── storage/                         # 存储模型
│   │   └── file/                            # 文件模型 (新增)
│   │
│   ├── scenario/                            # 场景模型 (重组)
│   │   └── hackathon/                       # 黑客松场景
│   │
│   └── core/                                # 核心定义
│       └── exception/                       # 异常
│
├── application/                             # 应用层
│   ├── port/                                # 端口定义
│   │   ├── input/                           # 输入端口
│   │   └── output/                          # 输出端口
│   │
│   ├── service/                             # 应用服务
│   │   ├── core/                            # 核心服务
│   │   ├── storage/                         # 存储服务
│   │   ├── analysis/                        # 分析服务 (新增)
│   │   ├── scenario/                        # 场景服务 (重组)
│   │   │   └── hackathon/
│   │   └── prompt/                          # Prompt构建
│   │
│   └── cli/                                 # 命令行应用
│       └── scenario/                        # 场景CLI
│           └── hackathon/
│
└── infrastructure/                          # 基础设施层
    ├── config/                              # 配置加载
    ├── di/                                  # 依赖注入
    └── factory/                             # 工厂类
```

---

## 🚀 实施步骤

### 步骤1: 创建新包结构 (5分钟)
```bash
# 创建所有新包目录
mkdir -p adapter/storage/{s3,local,cache,archive}
mkdir -p adapter/ai/{bedrock,config,http,decorator}
mkdir -p adapter/parser/{code/{java,python,javascript,go,cpp},detector}
mkdir -p adapter/repository/git
# ... 其他目录
```

### 步骤2: 移动S3相关类 (P0-1) (15分钟)
- 移动 `S3StorageAdapter.java` 等7个类
- 更新所有import语句
- 运行测试验证

### 步骤3: 移动AI服务类 (P0-2) (15分钟)
- 移动 `BedrockAdapter.java` 等5个类
- 更新所有import语句
- 运行测试验证

### 步骤4: 移动解析器类 (P0-3) (20分钟)
- 移动AST解析器11个类
- 按语言分子包
- 更新所有import语句
- 运行测试验证

### 步骤5: 移动其他P0类 (P0-4,5) (15分钟)
- 本地文件系统
- 缓存适配器
- 更新import
- 测试

### 步骤6: 更新配置和文档 (10分钟)
- 更新README
- 更新架构文档
- 更新配置文件

### 步骤7: 全量测试 (20分钟)
```bash
mvn clean test
mvn clean package
```

---

## ✅ 验证清单

### 编译验证
- [ ] 无编译错误
- [ ] 无警告信息
- [ ] 所有import正确

### 测试验证
- [ ] 单元测试全部通过
- [ ] 集成测试全部通过
- [ ] 端到端测试通过

### 功能验证
- [ ] S3存储功能正常
- [ ] AI服务调用正常
- [ ] AST解析功能正常
- [ ] 黑客松场景正常

### 文档验证
- [ ] README更新
- [ ] 架构图更新
- [ ] API文档更新

---

## 📈 预期收益

### 1. 清晰的职责边界
- 每个包有明确的功能定位
- 新人快速理解项目结构

### 2. 易于扩展
- 新增AI服务: 直接在 `adapter.ai` 下新建子包
- 新增文件类型: 直接在 `adapter.parser` 下新建子包
- 新增存储方式: 直接在 `adapter.storage` 下新建子包

### 3. 降低耦合
- 模块间依赖清晰
- 便于单元测试
- 便于模块替换

### 4. 支持多场景
- 黑客松只是 `scenario` 下的一个案例
- 未来可轻松添加数据科学、合规检查等场景

---

## 🎓 架构原则

### 1. 通用化优先
- 核心功能通用化（文件读取、AI调用、解析）
- 场景功能插件化（黑客松、数据科学等）

### 2. 接口隔离
- 每个适配器定义清晰的端口接口
- 便于Mock和测试

### 3. 开闭原则
- 对扩展开放（新增文件类型、AI服务）
- 对修改封闭（核心引擎稳定）

### 4. 依赖倒置
- 高层模块不依赖低层模块
- 都依赖抽象（端口接口）

---

## 📚 参考文档

- [六边形架构最佳实践](../doc/HEXAGONAL-ARCHITECTURE.md)
- [领域驱动设计](../doc/DDD-GUIDE.md)
- [Java包命名规范](../doc/JAVA-PACKAGE-NAMING.md)

---

**报告生成完毕 - 准备开始执行重组**

