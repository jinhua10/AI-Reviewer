# AI Reviewer - AI引擎驱动的文件处理工具

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.8+-blue.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

一个智能化、可扩展且生产就绪的AI引擎，由AI服务驱动。采用模块化的适配器架构，实现最大的灵活性和可扩展性。

[English](README.md) | 简体中文

## 📋 目录

- [特性](#-特性)
- [架构](#-架构)
- [快速开始](#-快速开始)
- [配置](#-配置)
- [使用方法](#-使用方法)
- [模块详解](#-模块详解)
- [扩展引擎](#-扩展引擎)
- [示例](#-示例)
- [故障排除](#-故障排除)
- [贡献](#-贡献)
- [许可证](#-许可证)

## ✨ 特性

- 🤖 **多AI提供商支持** - 支持OpenAI、DeepSeek、Claude及自定义AI服务
- 🔌 **插件化架构** - 基于适配器的可扩展设计，支持SPI
- 📝 **多语言解析器** - 支持Java、Python、JavaScript和纯文本
- 🚀 **高性能** - 多线程处理，可配置线程池
- 🎯 **模式化过滤** - 通过包含/排除模式精确选择文件
- 📊 **全面报告** - 支持Markdown、JSON及自定义输出格式
- 🔧 **Spring Boot集成** - 自动配置，无缝集成
- ⚡ **生产就绪** - 完善的异常处理、日志记录和重试机制

## 🏗️ 架构

AI Reviewer遵循清晰的分层架构，职责分离明确：

```
┌─────────────────────────────────────────────────────────────┐
│                      应用层                                  │
│              (Spring Boot / CLI / REST API)                  │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    AI引擎 (核心层)                           │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ 文件扫描 → 文件过滤 → 解析器 → AI → 结果处理        │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                   适配器注册中心 (SPI)                       │
│  ┌──────────┐  ┌──────────┐  ┌────────────────────────┐    │
│  │  解析器  │  │ AI服务   │  │    结果处理器          │    │
│  └──────────┘  └──────────┘  └────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                  核心接口 (API层)                            │
│    IFileParser  │  IAIService  │  IResultProcessor          │
└─────────────────────────────────────────────────────────────┘
```

### 核心模块

1. **ai-reviewer-api** - 核心接口和数据模型
2. **ai-reviewer-common** - 通用工具和异常处理
3. **ai-reviewer-core** - 主引擎和编排逻辑
4. **ai-reviewer-adaptor-parser** - 文件解析适配器（Java、Python、JavaScript、文本）
5. **ai-reviewer-adaptor-ai** - AI服务适配器（基于HTTP的AI服务）
6. **ai-reviewer-adaptor-processor** - 结果处理适配器（Markdown报告）
7. **ai-reviewer-starter** - Spring Boot启动器，自动配置

## 🚀 快速开始

### 前置条件

- **Java 17+** - 从[Oracle](https://www.oracle.com/java/technologies/downloads/)或[OpenJDK](https://openjdk.org/)下载
- **Maven 3.8+** - 从[Apache Maven](https://maven.apache.org/download.cgi)下载
- **AI API密钥** - 从[OpenAI](https://platform.openai.com/)、[DeepSeek](https://platform.deepseek.com/)等获取

### 安装

1. **克隆仓库**
   ```bash
   git clone https://github.com/yourusername/AI-Reviewer.git
   cd AI-Reviewer
   ```

2. **构建项目**
   ```bash
   mvn clean install -DskipTests
   ```

3. **验证安装**
   ```bash
   mvn -version
   java -version
   ```

### 运行您的第一次代码审查

1. **导航到演示应用**
   ```bash
   cd application-demo/hackathonApplication
   ```

2. **设置API密钥** (Linux/Mac)
   ```bash
   export AI_API_KEY="your-api-key-here"
   ```
   或 (Windows)
   ```powershell
   $env:AI_API_KEY="your-api-key-here"
   ```

3. **运行应用**
   ```bash
   mvn spring-boot:run -Dspring-boot.run.arguments="--review /path/to/your/code"
   ```

## ⚙️ 配置

### 应用配置

在项目中创建或更新 `application.yml` ：

```yaml
ai-reviewer:
  # 文件扫描配置
  scanner:
    include-patterns:
      - "**/*.java"
      - "**/*.py"
      - "**/*.js"
    exclude-patterns:
      - "**/target/**"
      - "**/build/**"
      - "**/node_modules/**"
      - "**/.git/**"
    max-file-size: "10MB"
  
  # 解析器配置
  parser:
    enabled-parsers:
      - java
      - python
      - javascript
      - text
  
  # AI服务配置
  ai:
    provider: deepseek              # 或 "openai", "claude" 等
    model: deepseek-coder           # 或 "gpt-4", "claude-3" 等
    api-key: ${AI_API_KEY}          # 使用环境变量
    endpoint: https://api.deepseek.com/v1/chat/completions
    
    # 提示词
    sysPrompt: "你是一位专业的代码审查专家。请对以下代码提供建设性的反馈意见。"
    userPrompt: |
      请审查这段代码并提供：
      1. 代码质量评估
      2. 潜在的错误或问题
      3. 性能改进建议
      4. 最佳实践建议
      
      代码：%s
    
    # AI参数
    temperature: 0.7
    max-tokens: 2000
    timeout-seconds: 30
    max-retries: 3
  
  # 处理器配置
  processor:
    type: code-review
    output-format: markdown
    output-path: ./reports
  
  # 执行器配置
  executor:
    thread-pool-size: 10
    max-queue-size: 100

# Spring Boot配置
spring:
  application:
    name: ai-reviewer

# 日志配置
logging:
  level:
    root: INFO
    top.yumbo.ai: DEBUG
```

### 使用Properties配置

您也可以使用 `application.properties`：

```properties
ai-reviewer.ai.provider=deepseek
ai-reviewer.ai.model=deepseek-coder
ai-reviewer.ai.api-key=${AI_API_KEY}
ai-reviewer.ai.endpoint=https://api.deepseek.com/v1/chat/completions
ai-reviewer.scanner.include-patterns[0]=**/*.java
ai-reviewer.scanner.exclude-patterns[0]=**/target/**
ai-reviewer.processor.output-path=./reports
ai-reviewer.executor.thread-pool-size=10
```

### 环境变量

推荐的环境变量：

| 变量 | 描述 | 示例 |
|----------|-------------|---------|
| `AI_API_KEY` | 您的AI服务API密钥 | `sk-xxx` |
| `AI_PROVIDER` | AI提供商名称 | `deepseek`, `openai` |
| `AI_MODEL` | 使用的AI模型 | `deepseek-coder`, `gpt-4` |
| `AI_ENDPOINT` | AI服务端点 | `https://api.deepseek.com/v1/chat/completions` |

## 📖 使用方法

### 作为Spring Boot Starter使用

**1. 在 `pom.xml` 中添加依赖：**

```xml
<dependency>
    <groupId>top.yumbo.ai</groupId>
    <artifactId>ai-reviewer-starter</artifactId>
    <version>1.0</version>
</dependency>
```

**2. 注入并使用AIEngine：**

```java
@SpringBootApplication
public class MyApplication {
    
    @Autowired
    private AIEngine aiEngine;
    
    @Autowired
    private AIReviewerProperties properties;
    
    public void reviewCode(String targetPath) {
        ExecutionContext context = ExecutionContext.builder()
            .targetDirectory(Paths.get(targetPath))
            .includePatterns(properties.getScanner().getIncludePatterns())
            .excludePatterns(properties.getScanner().getExcludePatterns())
            .aiConfig(buildAIConfig())
            .processorConfig(buildProcessorConfig())
            .threadPoolSize(10)
            .build();
        
        ProcessResult result = aiEngine.execute(context);
        
        if (result.isSuccess()) {
            System.out.println("审查完成：" + result.getReportPath());
        }
    }
}
```

### 作为独立库使用

**1. 添加依赖：**

```xml
<dependency>
    <groupId>top.yumbo.ai</groupId>
    <artifactId>ai-reviewer-core</artifactId>
    <version>1.0</version>
</dependency>
<dependency>
    <groupId>top.yumbo.ai</groupId>
    <artifactId>ai-reviewer-adaptor-parser</artifactId>
    <version>1.0</version>
</dependency>
<dependency>
    <groupId>top.yumbo.ai</groupId>
    <artifactId>ai-reviewer-adaptor-ai</artifactId>
    <version>1.0</version>
</dependency>
<dependency>
    <groupId>top.yumbo.ai</groupId>
    <artifactId>ai-reviewer-adaptor-processor</artifactId>
    <version>1.0</version>
</dependency>
```

**2. 创建并配置引擎：**

```java
// 创建适配器注册中心
AdapterRegistry registry = new AdapterRegistry();

// 注册解析器
registry.registerParser(new JavaFileParser());
registry.registerParser(new PythonFileParser());
registry.registerParser(new JavaScriptFileParser());

// 注册AI服务
AIConfig aiConfig = AIConfig.builder()
    .provider("deepseek")
    .model("deepseek-coder")
    .apiKey(System.getenv("AI_API_KEY"))
    .endpoint("https://api.deepseek.com/v1/chat/completions")
    .build();
registry.registerAIService(new HttpBasedAIAdapter(aiConfig));

// 注册处理器
registry.registerProcessor(new CodeReviewProcessor());

// 创建引擎
AIEngine engine = new AIEngine(registry);

// 执行审查
ExecutionContext context = ExecutionContext.builder()
    .targetDirectory(Paths.get("/path/to/code"))
    .includePatterns(List.of("**/*.java"))
    .excludePatterns(List.of("**/target/**"))
    .aiConfig(aiConfig)
    .processorConfig(ProcessorConfig.builder()
        .processorType("code-review")
        .outputFormat("markdown")
        .outputPath(Paths.get("./report.md"))
        .build())
    .threadPoolSize(10)
    .build();

ProcessResult result = engine.execute(context);
```

## 📦 模块详解

### ai-reviewer-api

**核心接口和数据模型**

- `IFileParser` - 文件解析适配器接口
- `IAIService` - AI服务适配器接口
- `IResultProcessor` - 结果处理适配器接口
- `PreProcessedData`, `AIResponse`, `ProcessResult` - 数据传输对象
- `AIConfig`, `ProcessorConfig`, `FileMetadata` - 配置模型

### ai-reviewer-common

**共享工具和异常处理**

- `AIReviewerException` - 基础异常类
- `ParseException`, `AIServiceException`, `ProcessorException` - 特定异常
- `FileUtil`, `StringUtil` - 工具类
- `Constants` - 全局常量

### ai-reviewer-core

**主引擎和编排逻辑**

- `AIEngine` - 主编排引擎，支持多线程处理
- `AdapterRegistry` - 管理所有适配器，支持SPI
- `ExecutionContext` - 执行状态和配置
- `FileScanner` - 在目标目录中发现文件
- `FileFilter` - 基于模式过滤文件

### ai-reviewer-adaptor-parser

**文件解析适配器**

- `JavaFileParser` - 使用JavaParser解析Java文件
- `PythonFileParser` - 解析Python文件
- `JavaScriptFileParser` - 解析JavaScript文件
- `PlainTextFileParser` - 不支持文件类型的回退方案

### ai-reviewer-adaptor-ai

**AI服务适配器**

- `HttpBasedAIAdapter` - 通用的基于HTTP的AI服务适配器
  - 支持OpenAI、DeepSeek、Claude及兼容的API
  - 可配置端点、模型和参数
  - 内置重试和超时处理

### ai-reviewer-adaptor-processor

**结果处理适配器**

- `CodeReviewProcessor` - 生成markdown代码审查报告
  - 逐文件分析
  - 汇总统计
  - 性能指标

### ai-reviewer-starter

**Spring Boot启动器，自动配置**

- `AIReviewerAutoConfiguration` - 自动配置所有Bean
- `AIReviewerProperties` - 配置属性绑定
- 通过SPI自动发现适配器
- Spring Boot应用零配置设置

## 🔧 扩展引擎

### 创建自定义解析器

```java
public class CustomParser implements IFileParser {
    @Override
    public boolean support(File file) {
        return file.getName().endsWith(".custom");
    }
    
    @Override
    public PreProcessedData parse(File file) throws Exception {
        String content = FileUtil.readFileToString(file);
        
        FileMetadata metadata = FileMetadata.builder()
            .filePath(file.toPath())
            .fileName(file.getName())
            .fileType("custom")
            .build();
        
        return PreProcessedData.builder()
            .metadata(metadata)
            .content(content)
            .parserName("CustomParser")
            .build();
    }
    
    @Override
    public int getPriority() {
        return 100;
    }
}

// 注册解析器
registry.registerParser(new CustomParser());
```

### 创建自定义AI服务

```java
public class CustomAIService implements IAIService {
    @Override
    public AIResponse invoke(PreProcessedData data, AIConfig config) throws Exception {
        // 调用您的自定义AI服务
        String response = callCustomAI(data.getContent(), config);
        
        return AIResponse.builder()
            .originalData(data)
            .aiGeneratedContent(response)
            .provider("custom")
            .model(config.getModel())
            .success(true)
            .build();
    }
    
    @Override
    public boolean support(String provider) {
        return "custom".equalsIgnoreCase(provider);
    }
}

// 注册AI服务
registry.registerAIService(new CustomAIService());
```

### 创建自定义处理器

```java
public class CustomProcessor implements IResultProcessor {
    @Override
    public ProcessResult process(List<AIResponse> responses, ProcessorConfig config) {
        // 自定义处理逻辑
        String report = generateCustomReport(responses);
        Path outputPath = saveReport(report, config);
        
        return ProcessResult.builder()
            .success(true)
            .processedCount(responses.size())
            .reportPath(outputPath)
            .build();
    }
    
    @Override
    public boolean support(String processorType) {
        return "custom".equalsIgnoreCase(processorType);
    }
}

// 注册处理器
registry.registerProcessor(new CustomProcessor());
```

### 使用SPI自动发现

创建 `META-INF/services/top.yumbo.ai.api.parser.IFileParser`：
```
com.example.CustomParser
```

创建 `META-INF/services/top.yumbo.ai.api.ai.IAIService`：
```
com.example.CustomAIService
```

创建 `META-INF/services/top.yumbo.ai.api.processor.IResultProcessor`：
```
com.example.CustomProcessor
```

## 💡 示例

### 示例1：审查Java项目

```java
ExecutionContext context = ExecutionContext.builder()
    .targetDirectory(Paths.get("./src"))
    .includePatterns(List.of("**/*.java"))
    .excludePatterns(List.of("**/test/**", "**/target/**"))
    .aiConfig(aiConfig)
    .processorConfig(processorConfig)
    .threadPoolSize(5)
    .build();

ProcessResult result = engine.execute(context);
System.out.println("已审查文件数：" + result.getProcessedCount());
System.out.println("报告路径：" + result.getReportPath());
```

### 示例2：审查多种文件类型

```java
ExecutionContext context = ExecutionContext.builder()
    .targetDirectory(Paths.get("./project"))
    .includePatterns(List.of("**/*.java", "**/*.py", "**/*.js"))
    .excludePatterns(List.of("**/node_modules/**", "**/.git/**"))
    .aiConfig(aiConfig)
    .processorConfig(processorConfig)
    .build();

ProcessResult result = engine.execute(context);
```

### 示例3：自定义提示词

```java
AIConfig customConfig = AIConfig.builder()
    .provider("deepseek")
    .model("deepseek-coder")
    .apiKey(apiKey)
    .sysPrompt("你是一位安全专家。请专注于安全漏洞。")
    .userPrompt("分析这段代码的安全问题：\n%s")
    .temperature(0.3)
    .build();
```

## 🐛 故障排除

### 问题：找不到AIEngine Bean

**错误**：`No qualifying bean of type 'top.yumbo.ai.core.AIEngine'`

**解决方案**：
1. 确保 `ai-reviewer-starter` 在依赖中
2. 检查starter jar中是否存在 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
3. 验证Spring Boot版本是3.x
4. 尝试在主类上添加 `@ComponentScan("top.yumbo.ai")`

### 问题：运行时找不到模块类

**错误**：子模块的 `ClassNotFoundException`

**解决方案**：
1. 从父模块构建：`mvn clean install`
2. 包含所有依赖或使用fat jar方式
3. 对于Spring Boot应用，确保包含所有传递依赖

### 问题：AI服务超时

**错误**：`AIServiceException: Request timeout`

**解决方案**：
1. 增加超时时间：`ai-reviewer.ai.timeout-seconds=60`
2. 检查网络连接
3. 验证API密钥和端点
4. 启用重试：`ai-reviewer.ai.max-retries=3`

### 问题：找不到解析器

**错误**：`No parser found for file type`

**解决方案**：
1. 检查文件扩展名是否匹配解析器模式
2. 验证解析器已在 `AdapterRegistry` 中注册
3. 如需要，添加自定义解析器
4. 检查SPI配置以实现自动发现

## 🤝 贡献

欢迎贡献！请遵循以下步骤：

1. Fork本仓库
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m '添加某个很棒的特性'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 开启Pull Request

### 开发环境设置

```bash
# 克隆仓库
git clone https://github.com/yourusername/AI-Reviewer.git

# 构建
mvn clean install

# 运行测试
mvn test

# 调试运行
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

## 📄 许可证

本项目采用Apache License 2.0许可证 - 详见[LICENSE](./LICENSE.txt)文件。

## 🙏 致谢

- [Spring Boot](https://spring.io/projects/spring-boot) - 应用框架
- [JavaParser](https://javaparser.org/) - Java代码解析
- [OkHttp](https://square.github.io/okhttp/) - HTTP客户端
- [Lombok](https://projectlombok.org/) - 减少样板代码
- [Jackson](https://github.com/FasterXML/jackson) - JSON处理

## 📞 支持

- 📧 邮箱：support@example.com
- 💬 问题反馈：[GitHub Issues](https://github.com/yourusername/AI-Reviewer/issues)
- 📖 文档：[Wiki](https://github.com/yourusername/AI-Reviewer/wiki)

---

**由AI Reviewer团队用 ❤️ 打造**

