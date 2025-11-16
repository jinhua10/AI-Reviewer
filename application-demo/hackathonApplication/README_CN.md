# Hackathon Application - AI 代码评审演示应用

## 概述

这是一个基于 AI-Reviewer 框架构建的演示应用程序，专门为黑客松项目评估设计。它能够自动分析项目源代码，使用 AI 模型生成包含评分和详细反馈的综合评估报告。

[English](README.md) | 简体中文

## 功能特性

- **自动化代码评审**：分析项目源代码并生成详细的评估报告
- **全面的评分体系**：从 5 个关键维度评估项目（总分 100 分）：
  - 创新性（25分）- 想法的独特性和创造性
  - 技术实现（25分）- 代码质量、架构设计、技术选型
  - 完整性（20分）- 功能完整度、代码完备性
  - 实用性（15分）- 项目的实际应用价值
  - 代码规范（15分）- 代码风格、注释、可维护性

- **详细分析**：提供全面的反馈，包括：
  - 项目特征（代码指标、设计模式、架构）
  - 各项评分及理由说明
  - 优点和亮点
  - 不足和问题
  - 可执行的改进建议
  - 综合评语总结

- **多种 AI 服务提供商支持**：兼容多种 AI 服务：
  - AWS Bedrock（Claude 模型）
  - OpenAI GPT 模型
  - 自定义 AI 端点

- **灵活的文件源支持**：
  - 本地文件系统
  - SFTP 服务器
  - Git 仓库
  - AWS S3 存储桶

## 快速开始

### 前置要求

- Java 17 或更高版本
- Maven 3.6+
- AWS 凭证（如果使用 Bedrock）或其他 AI 提供商的 API 密钥

### 配置

1. 在 `application.yml` 或 `application.properties` 中配置 AI 提供商：

```yaml
ai-reviewer:
  ai:
    provider: bedrock  # 或 openai、custom
    model: arn:aws:bedrock:us-east-1:590184013141:inference-profile/us.anthropic.claude-sonnet-4-5-20250929-v1:0
  source:
    type: local  # 或 sftp、git、s3
```

2. 设置 AWS 凭证（如果使用 Bedrock）：
```bash
export AWS_ACCESS_KEY_ID=your_access_key
export AWS_SECRET_ACCESS_KEY=your_secret_key
export AWS_REGION=us-east-1
```

### 构建

```bash
# 从根目录构建
cd AI-Reviewer
mvn clean package -DskipTests

# 或仅构建应用程序
cd application-demo/hackathonApplication
mvn clean package -DskipTests
```

### 运行

```bash
java -jar target/hackathonApplication.jar --review /path/to/project
```

使用额外选项：
```bash
java -jar target/hackathonApplication.jar \
  --review /path/to/project \
  --output report.md \
  --logging.level.top.yumbo.ai=DEBUG
```

## 评审提示词模板

应用程序使用可自定义的提示词模板（参见 `提示词.txt`），引导 AI 模型执行以下操作：

1. 基于 5 个评分维度评估项目
2. 计算总分（满分 100 分）
3. 分析项目特征（代码行数、设计模式、架构、工作流）
4. 提供各项评分及说明
5. 列出优点（至少 3 点）
6. 识别不足之处（至少 3 点）
7. 提出改进建议（至少 3 个可执行项）
8. 生成综合评语（200 字以内）

### 自定义提示词

您可以通过修改提示词模板来自定义评估标准：

```java
// 在您的应用程序代码中
String customPrompt = """
    你是一位经验丰富的黑客松评审专家...
    [您的自定义评估标准]
    """;
```

## 输出示例

以下是名为 "LLMSpeech" 项目的评审报告示例：

### 评分摘要
- **总分**：72/100 分
- **创新性**：16/25 分
- **技术实现**：18/25 分
- **完整性**：16/20 分
- **实用性**：12/15 分
- **代码规范**：10/15 分

### 主要发现

**优点：**
1. 结构良好的多线程架构，关注点清晰分离
2. 全面的中断处理机制
3. 灵活的 AI 模型集成
4. 用户友好的界面，带有彩色编码输出
5. 实用的指令模式，可执行命令

**不足之处：**
1. 代码文档欠缺，注释很少
2. 配置硬编码分散在代码库各处
3. 关键部分的错误处理不足
4. 平台依赖问题（Windows 特定代码）
5. 临时文件的内存管理问题

**改进建议：**
1. 将配置管理重构到集中配置文件中
2. 增强错误处理并实现结构化日志记录
3. 改进代码文档，添加全面的文档字符串
4. 抽象平台特定代码以支持跨平台
5. 实现全面的测试（单元测试和集成测试）

完整示例报告请参见 [LLMSpeech-review-report.md](src/test/resources/LLMSpeech-review-report.md)。

## 架构

该应用程序基于 AI-Reviewer 框架构建，包含以下组件：

```
┌─────────────────────────────────────────────────┐
│         Hackathon Application                   │
│  ┌───────────────────────────────────────┐     │
│  │   AI-Reviewer Starter (Spring Boot)   │     │
│  └───────────────────────────────────────┘     │
│              ▼          ▼          ▼            │
│    ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│    │  源适配器 │  │ 解析器   │  │  AI 服务 │   │
│    │  Source  │  │  Parser  │  │    AI    │   │
│    │ Adaptors │  │ Adaptors │  │ Services │   │
│    └──────────┘  └──────────┘  └──────────┘   │
│         ▼              ▼              ▼         │
│    ┌─────────────────────────────────────┐    │
│    │      AI-Reviewer 核心引擎           │    │
│    └─────────────────────────────────────┘    │
│                     ▼                          │
│    ┌─────────────────────────────────────┐    │
│    │     结果处理器适配器                 │    │
│    └─────────────────────────────────────┘    │
└─────────────────────────────────────────────────┘
```

### 关键组件

- **源适配器**：从各种来源读取项目文件（本地、SFTP、Git、S3）
- **解析器适配器**：解析不同类型的文件（Java、Python、JavaScript 等）
- **AI 服务**：与 AI 提供商集成（Bedrock、OpenAI 等）
- **核心引擎**：编排评审工作流
- **结果处理器**：格式化并输出评审报告

## 配置参考

### 应用程序属性

```properties
# AI 提供商配置
ai-reviewer.ai.provider=bedrock
ai-reviewer.ai.model=arn:aws:bedrock:us-east-1:590184013141:inference-profile/us.anthropic.claude-sonnet-4-5-20250929-v1:0
ai-reviewer.ai.temperature=0.7
ai-reviewer.ai.max-tokens=8000

# 源配置
ai-reviewer.source.type=local
ai-reviewer.source.base-path=/path/to/projects

# 文件过滤
ai-reviewer.source.include-patterns=**/*.java,**/*.py,**/*.js
ai-reviewer.source.exclude-patterns=**/target/**,**/node_modules/**,**/.git/**

# 输出配置
ai-reviewer.output.format=markdown
ai-reviewer.output.path=./reports

# 性能调优
ai-reviewer.core.thread-pool-size=4
ai-reviewer.core.batch-size=10
```

## 性能指标

应用程序跟踪每个阶段的执行时间：

- **文件扫描**：定位和列出源文件的时间
- **文件过滤**：应用包含/排除模式的时间
- **文件解析**：将源代码解析为结构化格式的时间
- **AI 调用**：AI 模型生成评审的时间
- **结果处理**：格式化并保存报告的时间
- **总时间**：端到端执行时间

示例指标：
```
文件扫描：        6 毫秒
文件过滤：        7 毫秒
文件解析：       14 毫秒
AI 调用：    24,622 毫秒
结果处理：        0 毫秒
总时间：     24,649 毫秒
```

## 扩展应用程序

### 添加自定义评审标准

```java
@Configuration
public class CustomReviewConfig {
    
    @Bean
    public ReviewCriteriaCustomizer criteriaCustomizer() {
        return criteria -> criteria
            .addDimension("安全性", 20)
            .addDimension("性能", 15)
            .modifyDimension("创新性", 20);
    }
}
```

### 实现自定义结果处理器

```java
@Component
public class SlackResultProcessor implements IResultProcessor {
    
    @Override
    public String getProcessorName() {
        return "slack";
    }
    
    @Override
    public void process(ReviewResult result, ProcessorConfig config) {
        // 将评审报告发送到 Slack 频道
    }
}
```

## 故障排除

### 常见问题

1. **JSch 或 JGit 的 ClassNotFoundException**
   - 解决方案：在您的 pom.xml 中添加显式依赖
   ```xml
   <dependency>
       <groupId>com.jcraft</groupId>
       <artifactId>jsch</artifactId>
   </dependency>
   <dependency>
       <groupId>org.eclipse.jgit</groupId>
       <artifactId>org.eclipse.jgit</artifactId>
   </dependency>
   ```

2. **AWS Bedrock 连接超时**
   - 检查 AWS 凭证和区域配置
   - 验证到 AWS 端点的网络连接
   - 确保 IAM 权限允许访问 Bedrock 服务

3. **内存不足错误**
   - 增加 JVM 堆大小：`-Xmx2g`
   - 减少配置中的批处理大小
   - 启用文件过滤以排除大型目录

## 贡献

欢迎贡献！请参阅主 [AI-Reviewer 仓库](../../README_CN.md) 了解贡献指南。

## 许可证

本项目是 AI-Reviewer 框架的一部分。详见 [LICENSE](../../LICENSE.txt)。

## 支持

如有问题和疑问：
- GitHub Issues：[创建问题](https://github.com/your-org/AI-Reviewer/issues)
- 文档：[完整文档](../../README_CN.md)
- 示例：查看 [测试资源](src/test/resources/) 获取更多示例

## 致谢

构建工具：
- Spring Boot 3.2.0
- AWS Bedrock SDK
- FastJSON2
- JSch（SFTP 支持）
- JGit（Git 支持）

---

## 评审提示词模板说明

应用程序使用的评审提示词模板（`提示词.txt`）包含以下结构：

```
你是一位经验丰富的黑客松评审专家。请根据以下项目源码进行全面评估，并给出评分和详细评语。

评分标准（总分100分）：
1. 创新性 (25分) - 想法的独特性和创造性
2. 技术实现 (25分) - 代码质量、架构设计、技术选型
3. 完整性 (20分) - 功能完整度、代码完备性
4. 实用性 (15分) - 项目的实际应用价值
5. 代码规范 (15分) - 代码风格、注释、可维护性

项目源码：
%s

请按以下格式输出：
【总分】：X/100分
【项目特征】：X
- 代码行数X行
- 设计模式：X
- 项目架构：X
- 工作流：X
【各项评分】
- 创新性：X/25分
- 技术实现：X/25分
- 完整性：X/20分
- 实用性：X/15分
- 代码规范：X/15分

【优点】
1. ...
2. ...
3. ...

【不足之处】
1. ...
2. ...
3. ...

【改进建议】
1. ...
2. ...
3. ...

【综合评语】
（200字以内的总结性评价）
```

### 自定义提示词模板

您可以根据特定需求修改提示词模板：

1. **修改评分维度**：调整各维度的分值权重
2. **添加评估标准**：增加新的评估维度（如安全性、性能等）
3. **自定义输出格式**：调整报告的结构和内容
4. **多语言支持**：创建不同语言的提示词模板

### 最佳实践

- 保持提示词清晰明确，避免歧义
- 提供具体的评分标准和示例
- 要求结构化输出便于解析
- 根据项目类型调整评估重点
- 定期更新提示词以反映最新评估标准

## 完整示例

详细的评审报告示例请参见 [LLMSpeech-review-report.md](src/test/resources/LLMSpeech-review-report.md)，该示例展示了：

- 完整的评分细分
- 详细的项目特征分析
- 多角度的优缺点评估
- 可执行的改进建议
- 综合评语总结

这个示例可以作为您自定义评审流程的参考模板。

