# AI Reviewer - An AI Engine-Driven Document Processing Tool

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.8+-blue.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

An intelligent, extensible, and production-ready AI engine driven by AI services. It uses a modular, adapter-based architecture to provide maximum flexibility and extensibility.

 English | [简体中文](README_CN.md)

## 📋 Table of Contents

- [Features](#-features)
- [Architecture](#-architecture)
- [Quick Start](#-quick-start)
- [Configuration](#-configuration)
- [Usage](#-usage)
- [Module Details](#-module-details)
- [Extending the Engine](#-extending-the-engine)
- [Examples](#-examples)
- [Troubleshooting](#-troubleshooting)
- [Contributing](#-contributing)
- [License](#-license)

## ✨ Features

- 🤖 **Multi-AI Provider Support** - OpenAI, DeepSeek, Claude, and custom AI services
- 🔌 **Plugin Architecture** - Extensible adapter-based design with SPI support
- 📝 **Multiple Language Parsers** - Java, Python, JavaScript, and plain text
- 🚀 **High Performance** - Multi-threaded processing with configurable thread pools
- 🎯 **Pattern-based Filtering** - Include/exclude patterns for precise file selection
- 📊 **Comprehensive Reporting** - Markdown, JSON, and custom output formats
- 🔧 **Spring Boot Integration** - Auto-configuration and seamless integration
- ⚡ **Production Ready** - Exception handling, logging, and retry mechanisms

## 🏗️ Architecture

The AI Reviewer follows a clean, layered architecture with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────┐
│                    Application Layer                         │
│              (Spring Boot / CLI / REST API)                  │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    AI Engine (Core)                          │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ File Scanner → File Filter → Parser → AI → Processor│   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                   Adapter Registry (SPI)                     │
│  ┌──────────┐  ┌──────────┐  ┌────────────────────────┐    │
│  │ Parsers  │  │ AI Svcs  │  │ Result Processors      │    │
│  └──────────┘  └──────────┘  └────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                  Core Interfaces (API)                       │
│    IFileParser  │  IAIService  │  IResultProcessor          │
└─────────────────────────────────────────────────────────────┘
```

### Core Modules

1. **ai-reviewer-api** - Core interfaces and data models
2. **ai-reviewer-common** - Common utilities and exception handling
3. **ai-reviewer-core** - Main engine and orchestration logic
4. **ai-reviewer-adaptor-parser** - File parsing adapters (Java, Python, JavaScript, Text)
5. **ai-reviewer-adaptor-ai** - AI service adapters (HTTP-based AI services)
6. **ai-reviewer-adaptor-processor** - Result processing adapters (Markdown reports)
7. **ai-reviewer-starter** - Spring Boot starter with auto-configuration

## 🚀 Quick Start

### Prerequisites

- **Java 17+** - Download from [Oracle](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://openjdk.org/)
- **Maven 3.8+** - Download from [Apache Maven](https://maven.apache.org/download.cgi)
- **AI API Key** - Get your key from [OpenAI](https://platform.openai.com/), [DeepSeek](https://platform.deepseek.com/), etc.

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/AI-Reviewer.git
   cd AI-Reviewer
   ```

2. **Build the project**
   ```bash
   mvn clean install -DskipTests
   ```

3. **Verify installation**
   ```bash
   mvn -version
   java -version
   ```

### Running Your First Review

1. **Navigate to the demo application**
   ```bash
   cd application-demo/hackathonApplication
   ```

2. **Set your API key** (Linux/Mac)
   ```bash
   export AI_API_KEY="your-api-key-here"
   ```
   Or (Windows)
   ```powershell
   $env:AI_API_KEY="your-api-key-here"
   ```

3. **Run the application**
   ```bash
   mvn spring-boot:run -Dspring-boot.run.arguments="--review /path/to/your/code"
   ```

## ⚙️ Configuration

### Application Configuration

Create or update `application.yml` in your project:

```yaml
ai-reviewer:
  # File scanner configuration
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
  
  # Parser configuration
  parser:
    enabled-parsers:
      - java
      - python
      - javascript
      - text
  
  # AI service configuration
  ai:
    provider: deepseek              # or "openai", "claude", etc.
    model: deepseek-coder           # or "gpt-4", "claude-3", etc.
    api-key: ${AI_API_KEY}          # Use environment variable
    endpoint: https://api.deepseek.com/v1/chat/completions
    
    # Prompts
    sysPrompt: "You are an expert code reviewer. Provide constructive feedback on the following code."
    userPrompt: |
      Please review this code and provide:
      1. Code quality assessment
      2. Potential bugs or issues
      3. Performance improvements
      4. Best practice recommendations
      
      Code: %s
    
    # AI parameters
    temperature: 0.7
    max-tokens: 2000
    timeout-seconds: 30
    max-retries: 3
  
  # Processor configuration
  processor:
    type: code-review
    output-format: markdown
    output-path: ./reports
  
  # Executor configuration
  executor:
    thread-pool-size: 10
    max-queue-size: 100

# Spring Boot Configuration
spring:
  application:
    name: ai-reviewer

# Logging Configuration
logging:
  level:
    root: INFO
    top.yumbo.ai: DEBUG
```

### Configuration via Properties

You can also use `application.properties`:

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

### Environment Variables

Recommended environment variables:

| Variable | Description | Example |
|----------|-------------|---------|
| `AI_API_KEY` | Your AI service API key | `sk-xxx` |
| `AI_PROVIDER` | AI provider name | `deepseek`, `openai` |
| `AI_MODEL` | AI model to use | `deepseek-coder`, `gpt-4` |
| `AI_ENDPOINT` | AI service endpoint | `https://api.deepseek.com/v1/chat/completions` |

## 📖 Usage

### As a Spring Boot Starter

**1. Add dependency to your `pom.xml`:**

```xml
<dependency>
    <groupId>top.yumbo.ai</groupId>
    <artifactId>ai-reviewer-starter</artifactId>
    <version>1.0</version>
</dependency>
```

**2. Inject and use AIEngine:**

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
            System.out.println("Review completed: " + result.getReportPath());
        }
    }
}
```

### As a Standalone Library

**1. Add dependencies:**

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

**2. Create and configure the engine:**

```java
// Create adapter registry
AdapterRegistry registry = new AdapterRegistry();

// Register parsers
registry.registerParser(new JavaFileParser());
registry.registerParser(new PythonFileParser());
registry.registerParser(new JavaScriptFileParser());

// Register AI service
AIConfig aiConfig = AIConfig.builder()
    .provider("deepseek")
    .model("deepseek-coder")
    .apiKey(System.getenv("AI_API_KEY"))
    .endpoint("https://api.deepseek.com/v1/chat/completions")
    .build();
registry.registerAIService(new HttpBasedAIAdapter(aiConfig));

// Register processor
registry.registerProcessor(new CodeReviewProcessor());

// Create engine
AIEngine engine = new AIEngine(registry);

// Execute review
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

## 📦 Module Details

### ai-reviewer-api

**Core interfaces and data models**

- `IFileParser` - Interface for file parsing adapters
- `IAIService` - Interface for AI service adapters
- `IResultProcessor` - Interface for result processing adapters
- `PreProcessedData`, `AIResponse`, `ProcessResult` - Data transfer objects
- `AIConfig`, `ProcessorConfig`, `FileMetadata` - Configuration models

### ai-reviewer-common

**Shared utilities and exception handling**

- `AIReviewerException` - Base exception class
- `ParseException`, `AIServiceException`, `ProcessorException` - Specific exceptions
- `FileUtil`, `StringUtil` - Utility classes
- `Constants` - Global constants

### ai-reviewer-core

**Main engine and orchestration logic**

- `AIEngine` - Main orchestration engine with multi-threaded processing
- `AdapterRegistry` - Manages all adapters with SPI support
- `ExecutionContext` - Execution state and configuration
- `FileScanner` - Discovers files in target directory
- `FileFilter` - Filters files based on patterns

### ai-reviewer-adaptor-parser

**File parsing adapters**

- `JavaFileParser` - Parses Java files using JavaParser
- `PythonFileParser` - Parses Python files
- `JavaScriptFileParser` - Parses JavaScript files
- `PlainTextFileParser` - Fallback for unsupported file types

### ai-reviewer-adaptor-ai

**AI service adapters**

- `HttpBasedAIAdapter` - Generic HTTP-based AI service adapter
  - Supports OpenAI, DeepSeek, Claude, and compatible APIs
  - Configurable endpoints, models, and parameters
  - Built-in retry and timeout handling

### ai-reviewer-adaptor-processor

**Result processing adapters**

- `CodeReviewProcessor` - Generates markdown code review reports
  - File-by-file analysis
  - Summary statistics
  - Performance metrics

### ai-reviewer-starter

**Spring Boot starter with auto-configuration**

- `AIReviewerAutoConfiguration` - Auto-configures all beans
- `AIReviewerProperties` - Configuration properties binding
- Automatic adapter discovery via SPI
- Zero-configuration setup for Spring Boot applications

## 🔧 Extending the Engine

### Creating a Custom Parser

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

// Register the parser
registry.registerParser(new CustomParser());
```

### Creating a Custom AI Service

```java
public class CustomAIService implements IAIService {
    @Override
    public AIResponse invoke(PreProcessedData data, AIConfig config) throws Exception {
        // Call your custom AI service
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

// Register the AI service
registry.registerAIService(new CustomAIService());
```

### Creating a Custom Processor

```java
public class CustomProcessor implements IResultProcessor {
    @Override
    public ProcessResult process(List<AIResponse> responses, ProcessorConfig config) {
        // Custom processing logic
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

// Register the processor
registry.registerProcessor(new CustomProcessor());
```

### Using SPI for Auto-Discovery

Create `META-INF/services/top.yumbo.ai.api.parser.IFileParser`:
```
com.example.CustomParser
```

Create `META-INF/services/top.yumbo.ai.api.ai.IAIService`:
```
com.example.CustomAIService
```

Create `META-INF/services/top.yumbo.ai.api.processor.IResultProcessor`:
```
com.example.CustomProcessor
```

## 💡 Examples

### Example 1: Review Java Project

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
System.out.println("Files reviewed: " + result.getProcessedCount());
System.out.println("Report: " + result.getReportPath());
```

### Example 2: Review Multiple File Types

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

### Example 3: Custom Prompts

```java
AIConfig customConfig = AIConfig.builder()
    .provider("deepseek")
    .model("deepseek-coder")
    .apiKey(apiKey)
    .sysPrompt("You are a security expert. Focus on security vulnerabilities.")
    .userPrompt("Analyze this code for security issues:\n%s")
    .temperature(0.3)
    .build();
```

## 🐛 Troubleshooting

### Issue: AIEngine Bean Not Found

**Error**: `No qualifying bean of type 'top.yumbo.ai.core.AIEngine'`

**Solution**: 
1. Ensure `ai-reviewer-starter` is in dependencies
2. Check that `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` exists in the starter jar
3. Verify Spring Boot version is 3.x
4. Try adding `@ComponentScan("top.yumbo.ai")` to your main class

### Issue: Module Classes Not Found at Runtime

**Error**: `ClassNotFoundException` for sub-modules

**Solution**:
1. Build from parent: `mvn clean install`
2. Include all dependencies or use the fat jar approach
3. For Spring Boot apps, ensure all transitive dependencies are included

### Issue: AI Service Timeout

**Error**: `AIServiceException: Request timeout`

**Solution**:
1. Increase timeout: `ai-reviewer.ai.timeout-seconds=60`
2. Check network connectivity
3. Verify API key and endpoint
4. Enable retry: `ai-reviewer.ai.max-retries=3`

### Issue: Parser Not Found

**Error**: `No parser found for file type`

**Solution**:
1. Check file extension matches parser patterns
2. Verify parser is registered in `AdapterRegistry`
3. Add custom parser if needed
4. Check SPI configuration for auto-discovery

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Setup

```bash
# Clone the repo
git clone https://github.com/yourusername/AI-Reviewer.git

# Build
mvn clean install

# Run tests
mvn test

# Run with debug
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [Spring Boot](https://spring.io/projects/spring-boot) - Application framework
- [JavaParser](https://javaparser.org/) - Java code parsing
- [OkHttp](https://square.github.io/okhttp/) - HTTP client
- [Lombok](https://projectlombok.org/) - Boilerplate code reduction
- [Jackson](https://github.com/FasterXML/jackson) - JSON processing

## 📞 Support

- 📧 Email: support@example.com
- 💬 Issues: [GitHub Issues](https://github.com/yourusername/AI-Reviewer/issues)
- 📖 Documentation: [Wiki](https://github.com/yourusername/AI-Reviewer/wiki)

---

**Made with ❤️ by the AI Reviewer Team**
- `AIEngine` - Orchestrates the entire pipeline
- `AdapterRegistry` - Manages all adapters
- `FileScanner` - Discovers files
- `FileFilter` - Applies include/exclude patterns
### Adaptor Modules
**Parser Adapters:**
- `JavaFileParser` - Parses Java files using JavaParser
- `PlainTextFileParser` - Fallback parser for all text files
**AI Adapters:**
- `OpenAIAdapter` - Integrates with OpenAI API
**Processor Adapters:**
- `CodeReviewProcessor` - Generates code review reports
## 🔧 Extension Points
### Adding a Custom Parser
```java
public class MyCustomParser implements IFileParser {
    @Override
    public boolean support(File file) {
        return file.getName().endsWith(".custom");
    }
    @Override
    public PreProcessedData parse(File file) throws Exception {
        // Your parsing logic
    }
}
```
Register via SPI in `META-INF/services/top.yumbo.ai.api.parser.IFileParser`:
```
com.yourpackage.MyCustomParser
```
### Adding a Custom AI Service
```java
public class CustomAIAdapter implements IAIService {
    @Override
    public AIResponse invoke(PreProcessedData data, AIConfig config) throws Exception {
        // Your AI integration logic
    }
    @Override
    public String getProviderName() {
        return "custom-ai";
    }
}
```
### Adding a Custom Processor
```java
public class CustomProcessor implements IResultProcessor {
    @Override
    public ProcessResult process(List<AIResponse> responses, ProcessorConfig config) throws Exception {
        // Your processing logic
    }
    @Override
    public String getProcessorType() {
        return "custom-processor";
    }
}
```
## 🔄 Processing Flow
```
1. File Scanning → Discover files in target directory
2. File Filtering → Apply include/exclude patterns
3. File Parsing → Parse files using appropriate parsers
4. AI Processing → Send to AI service for analysis
5. Result Processing → Generate final output/reports
```
## 📊 Configuration Reference
### Scanner Configuration
| Property | Default | Description |
|----------|---------|-------------|
| `include-patterns` | `[]` | Glob patterns for files to include |
| `exclude-patterns` | `[]` | Glob patterns for files to exclude |
| `max-file-size` | `10MB` | Maximum file size to process |
### AI Configuration
| Property | Default | Description |
|----------|---------|-------------|
| `provider` | `openai` | AI service provider |
| `model` | `gpt-4` | Model to use |
| `api-key` | - | API key (required) |
| `temperature` | `0.7` | Temperature for AI generation |
| `max-tokens` | `2000` | Maximum tokens to generate |
### Processor Configuration
| Property | Default | Description |
|----------|---------|-------------|
| `type` | `code-review` | Processor type |
| `output-format` | `markdown` | Output format |
| `output-path` | `./reports` | Output directory |
## 🧪 Testing
Run tests:
```bash
mvn test
```
## 📝 License
[Apache License 2.0](./LICENSE.txt)
## 🤝 Contributing
Contributions are welcome! Please feel free to submit a Pull Request.
## 📧 Contact
[Add your contact information]
## 🎯 Roadmap
- [ ] Add support for Python file parsing
- [ ] Integrate Claude AI service
- [ ] Add HTML report generation
- [ ] Add REST API endpoints
- [ ] Add WebSocket for real-time updates
- [ ] Add distributed processing support
---
Built with ❤️ using Spring Boot and modern Java practices.
