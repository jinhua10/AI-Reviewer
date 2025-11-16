# Hackathon Application - AI Code Reviewer Demo

## Overview

This is a demonstration application for the AI-Reviewer framework, specifically designed for hackathon project evaluation. It automatically reviews project source code using AI models and generates comprehensive assessment reports with scores and detailed feedback.

English | [简体中文](README_CN.md)

## Features

- **Automated Code Review**: Analyzes project source code and generates detailed evaluation reports
- **Comprehensive Scoring System**: Evaluates projects across 5 key dimensions (100 points total):
  - Innovation (25 points) - Uniqueness and creativity of ideas
  - Technical Implementation (25 points) - Code quality, architecture design, technology stack
  - Completeness (20 points) - Feature completeness and code coverage
  - Practicality (15 points) - Real-world application value
  - Code Standards (15 points) - Code style, documentation, maintainability

- **Detailed Analysis**: Provides comprehensive feedback including:
  - Project characteristics (code metrics, design patterns, architecture)
  - Itemized scores with justifications
  - Advantages and strengths
  - Weaknesses and issues
  - Actionable improvement suggestions
  - Overall assessment summary

- **Multiple AI Provider Support**: Compatible with various AI services:
  - AWS Bedrock (Claude models)
  - OpenAI GPT models
  - Custom AI endpoints

- **Flexible File Source Support**:
  - Local file system
  - SFTP servers
  - Git repositories
  - AWS S3 buckets

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- AWS credentials (if using Bedrock) or API keys for other AI providers

### Configuration

1. Configure AI provider in `application.yml` or `application.properties`:

```yaml
ai-reviewer:
  ai:
    provider: bedrock  # or openai, custom
    model: arn:aws:bedrock:us-east-1:590184013141:inference-profile/us.anthropic.claude-sonnet-4-5-20250929-v1:0
  source:
    type: local  # or sftp, git, s3
```

2. Set AWS credentials (if using Bedrock):
```bash
export AWS_ACCESS_KEY_ID=your_access_key
export AWS_SECRET_ACCESS_KEY=your_secret_key
export AWS_REGION=us-east-1
```

### Building

```bash
# Build from root directory
cd AI-Reviewer
mvn clean package -DskipTests

# Or build application only
cd application-demo/hackathonApplication
mvn clean package -DskipTests
```

### Running

```bash
java -jar target/hackathonApplication.jar --review /path/to/project
```

With additional options:
```bash
java -jar target/hackathonApplication.jar \
  --review /path/to/project \
  --output report.md \
  --logging.level.top.yumbo.ai=DEBUG
```

## Review Prompt Template

The application uses a customizable prompt template (see `提示词.txt`) that guides the AI model to:

1. Evaluate projects based on 5 scoring dimensions
2. Calculate total score out of 100 points
3. Analyze project characteristics (code lines, design patterns, architecture, workflow)
4. Provide itemized scores with explanations
5. List advantages (at least 3 points)
6. Identify weaknesses (at least 3 points)
7. Suggest improvements (at least 3 actionable items)
8. Generate comprehensive summary (within 200 words)

### Customizing the Prompt

You can customize the evaluation criteria by modifying the prompt template:

```java
// In your application code
String customPrompt = """
    You are an experienced hackathon reviewer...
    [Your custom evaluation criteria]
    """;
```

## Example Output

Here's an example review report for a project called "LLMSpeech":

### Score Summary
- **Total Score**: 72/100 points
- **Innovation**: 16/25 points
- **Technical Implementation**: 18/25 points
- **Completeness**: 16/20 points
- **Practicality**: 12/15 points
- **Code Standards**: 10/15 points

### Key Findings

**Advantages:**
1. Well-structured multi-threaded architecture with clean separation of concerns
2. Comprehensive interrupt handling mechanism
3. Flexible AI model integration
4. User-friendly interface with color-coded output
5. Practical instruction mode for command execution

**Weaknesses:**
1. Poor code documentation with minimal comments
2. Hard-coded configuration scattered throughout codebase
3. Inadequate error handling in critical sections
4. Platform dependency issues (Windows-specific code)
5. Memory management concerns with temporary files

**Improvement Suggestions:**
1. Refactor configuration management into centralized config files
2. Enhance error handling and implement structured logging
3. Improve code documentation with comprehensive docstrings
4. Abstract platform-specific code for cross-platform support
5. Implement comprehensive testing (unit and integration tests)

See [LLMSpeech-review-report.md](src/test/resources/LLMSpeech-review-report.md) for the complete example report.

## Architecture

The application is built on the AI-Reviewer framework with the following components:

```
┌─────────────────────────────────────────────────┐
│         Hackathon Application                   │
│  ┌───────────────────────────────────────┐     │
│  │   AI-Reviewer Starter (Spring Boot)   │     │
│  └───────────────────────────────────────┘     │
│              ▼          ▼          ▼            │
│    ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│    │  Source  │  │  Parser  │  │    AI    │   │
│    │ Adaptors │  │ Adaptors │  │ Services │   │
│    └──────────┘  └──────────┘  └──────────┘   │
│         ▼              ▼              ▼         │
│    ┌─────────────────────────────────────┐    │
│    │      AI-Reviewer Core Engine        │    │
│    └─────────────────────────────────────┘    │
│                     ▼                          │
│    ┌─────────────────────────────────────┐    │
│    │     Result Processor Adaptors       │    │
│    └─────────────────────────────────────┘    │
└─────────────────────────────────────────────────┘
```

### Key Components

- **Source Adaptors**: Read project files from various sources (local, SFTP, Git, S3)
- **Parser Adaptors**: Parse different file types (Java, Python, JavaScript, etc.)
- **AI Services**: Integration with AI providers (Bedrock, OpenAI, etc.)
- **Core Engine**: Orchestrates the review workflow
- **Result Processors**: Format and output review reports

## Configuration Reference

### Application Properties

```properties
# AI Provider Configuration
ai-reviewer.ai.provider=bedrock
ai-reviewer.ai.model=arn:aws:bedrock:us-east-1:590184013141:inference-profile/us.anthropic.claude-sonnet-4-5-20250929-v1:0
ai-reviewer.ai.temperature=0.7
ai-reviewer.ai.max-tokens=8000

# Source Configuration
ai-reviewer.source.type=local
ai-reviewer.source.base-path=/path/to/projects

# File Filtering
ai-reviewer.source.include-patterns=**/*.java,**/*.py,**/*.js
ai-reviewer.source.exclude-patterns=**/target/**,**/node_modules/**,**/.git/**

# Output Configuration
ai-reviewer.output.format=markdown
ai-reviewer.output.path=./reports

# Performance Tuning
ai-reviewer.core.thread-pool-size=4
ai-reviewer.core.batch-size=10
```

## Performance Metrics

The application tracks execution time for each stage:

- **File scanning**: Time to locate and list source files
- **File filtering**: Time to apply include/exclude patterns
- **File parsing**: Time to parse source code into structured format
- **AI invocation**: Time for AI model to generate review
- **Result processing**: Time to format and save the report
- **Total time**: End-to-end execution time

Example metrics:
```
File scanning:     6 ms
File filtering:    7 ms
File parsing:     14 ms
AI invocation: 24,622 ms
Result processing: 0 ms
Total time:    24,649 ms
```

## Extending the Application

### Adding Custom Review Criteria

```java
@Configuration
public class CustomReviewConfig {
    
    @Bean
    public ReviewCriteriaCustomizer criteriaCustomizer() {
        return criteria -> criteria
            .addDimension("Security", 20)
            .addDimension("Performance", 15)
            .modifyDimension("Innovation", 20);
    }
}
```

### Implementing Custom Result Processors

```java
@Component
public class SlackResultProcessor implements IResultProcessor {
    
    @Override
    public String getProcessorName() {
        return "slack";
    }
    
    @Override
    public void process(ReviewResult result, ProcessorConfig config) {
        // Send review report to Slack channel
    }
}
```

## Troubleshooting

### Common Issues

1. **ClassNotFoundException for JSch or JGit**
   - Solution: Add explicit dependencies in your pom.xml
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

2. **AWS Bedrock Connection Timeout**
   - Check AWS credentials and region configuration
   - Verify network connectivity to AWS endpoints
   - Ensure IAM permissions for Bedrock service

3. **Out of Memory Errors**
   - Increase JVM heap size: `-Xmx2g`
   - Reduce batch size in configuration
   - Enable file filtering to exclude large directories

## Contributing

Contributions are welcome! Please see the main [AI-Reviewer repository](../../README.md) for contribution guidelines.

## License

This project is part of the AI-Reviewer framework. See [LICENSE](../../LICENSE.txt) for details.

## Support

For issues and questions:
- GitHub Issues: [Create an issue](https://github.com/your-org/AI-Reviewer/issues)
- Documentation: [Full documentation](../../README.md)
- Examples: Check [test resources](src/test/resources/) for more examples

## Acknowledgments

Built with:
- Spring Boot 3.2.0
- AWS Bedrock SDK
- FastJSON2
- JSch (SFTP support)
- JGit (Git support)

