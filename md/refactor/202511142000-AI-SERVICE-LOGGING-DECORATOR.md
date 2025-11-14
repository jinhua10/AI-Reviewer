# AI 服务日志装饰器实现文档

## 概述

为 `AIServicePort` 接口添加了日志记录功能，使用**装饰器模式**实现，符合项目的六边形架构设计原则。

## 实现方式

### 1. 装饰器类：`LoggingAIServiceDecorator`

**位置**: `src/main/java/top/yumbo/ai/reviewer/adapter/output/ai/LoggingAIServiceDecorator.java`

**设计原则**:
- ✅ 使用装饰器模式，不修改原有接口和实现
- ✅ 符合开闭原则（对扩展开放，对修改关闭）
- ✅ 符合单一职责原则（日志记录职责独立）
- ✅ 符合依赖倒置原则（依赖抽象接口，不依赖具体实现）

**核心功能**:

#### 1.1 同步分析日志
```java
public String analyze(String prompt) {
    // 记录开始时间和入参信息
    log.info("[{}] 开始同步分析 - 提示词长度: {} 字符", providerName, prompt.length());
    log.debug("[{}] 同步分析输入参数:\n{}", providerName, truncatePrompt(prompt));
    
    try {
        String result = delegate.analyze(prompt);
        // 记录成功结果和耗时
        log.info("[{}] 同步分析完成 - 耗时: {}ms, 结果长度: {} 字符", ...);
        log.debug("[{}] 同步分析返回结果:\n{}", providerName, truncateResult(result));
        return result;
    } catch (Exception e) {
        // 记录失败信息
        log.error("[{}] 同步分析失败 - 耗时: {}ms, 错误: {}", ...);
        throw e;
    }
}
```

#### 1.2 异步分析日志
```java
public CompletableFuture<String> analyzeAsync(String prompt) {
    log.info("[{}] 开始异步分析 - 提示词长度: {} 字符", ...);
    
    return delegate.analyzeAsync(prompt)
        .whenComplete((result, throwable) -> {
            // 异步完成后记录结果或异常
            if (throwable != null) {
                log.error("[{}] 异步分析失败 - 耗时: {}ms", ...);
            } else {
                log.info("[{}] 异步分析完成 - 耗时: {}ms", ...);
            }
        });
}
```

#### 1.3 批量异步分析日志
```java
public CompletableFuture<String[]> analyzeBatchAsync(String[] prompts) {
    log.info("[{}] 开始批量异步分析 - 提示词数量: {}, 总字符数: {}", ...);
    
    return delegate.analyzeBatchAsync(prompts)
        .whenComplete((results, throwable) -> {
            // 记录批量处理结果统计
            log.info("[{}] 批量异步分析完成 - 结果数量: {}, 总字符数: {}", ...);
        });
}
```

#### 1.4 智能日志截断

为避免日志过大，实现了智能截断功能：

```java
private String truncatePrompt(String prompt) {
    int maxLength = 500; // 提示词最多显示 500 字符
    if (prompt.length() <= maxLength) {
        return prompt;
    }
    return prompt.substring(0, maxLength) + "... [截断，总长度: " + prompt.length() + " 字符]";
}

private String truncateResult(String result) {
    int maxLength = 1000; // 结果最多显示 1000 字符
    if (result.length() <= maxLength) {
        return result;
    }
    return result.substring(0, maxLength) + "... [截断，总长度: " + result.length() + " 字符]";
}
```

### 2. 工厂集成：`AIServiceFactory`

**修改位置**: `src/main/java/top/yumbo/ai/reviewer/infrastructure/factory/AIServiceFactory.java`

```java
public static AIServicePort create(Configuration.AIServiceConfig config) {
    return create(config, true); // 默认启用日志
}

public static AIServicePort create(Configuration.AIServiceConfig config, boolean enableLogging) {
    // 创建原始 AI 服务实例
    AIServicePort service = switch (provider) {
        case "deepseek" -> createDeepSeek(adapterConfig);
        case "openai" -> createOpenAI(adapterConfig);
        // ... 其他提供商
    };
    
    // 使用装饰器模式添加日志功能
    if (enableLogging) {
        service = new LoggingAIServiceDecorator(service);
    }
    
    return service;
}
```

## 使用方式

### 方式 1：默认启用日志（推荐）

```java
Configuration.AIServiceConfig config = configuration.getAIServiceConfig();
AIServicePort aiService = AIServiceFactory.create(config);
// 默认已启用日志装饰器
```

### 方式 2：显式控制日志开关

```java
Configuration.AIServiceConfig config = configuration.getAIServiceConfig();
AIServicePort aiService = AIServiceFactory.create(config, true);  // 启用日志
// 或
AIServicePort aiService = AIServiceFactory.create(config, false); // 禁用日志
```

### 方式 3：手动创建装饰器

```java
AIServicePort originalService = new DeepSeekAIAdapter(config);
AIServicePort loggingService = new LoggingAIServiceDecorator(originalService);
```

## 日志输出示例

### 同步分析日志

```
[INFO ] [DeepSeek] 开始同步分析 - 提示词长度: 1234 字符
[DEBUG] [DeepSeek] 同步分析输入参数:
        分析以下Java代码的质量问题...
[INFO ] [DeepSeek] 同步分析完成 - 耗时: 2345ms, 结果长度: 5678 字符
[DEBUG] [DeepSeek] 同步分析返回结果:
        该代码存在以下问题：
        1. 缺少异常处理...
```

### 异步分析日志

```
[INFO ] [Claude] 开始异步分析 - 提示词长度: 987 字符
[DEBUG] [Claude] 异步分析输入参数:
        请评审以下Python代码...
[INFO ] [Claude] 异步分析完成 - 耗时: 3210ms, 结果长度: 4567 字符
```

### 批量分析日志

```
[INFO ] [Bedrock] 开始批量异步分析 - 提示词数量: 10, 总字符数: 12345
[DEBUG] [Bedrock] 批量异步分析输入参数 - 提示词数量: 10
[INFO ] [Bedrock] 批量异步分析完成 - 耗时: 8765ms, 结果数量: 10, 总字符数: 23456
```

### 错误日志

```
[INFO ] [OpenAI] 开始同步分析 - 提示词长度: 456 字符
[ERROR] [OpenAI] 同步分析失败 - 耗时: 1234ms, 错误: Connection timeout
java.net.SocketTimeoutException: Connection timeout
    at ...
```

## 日志级别配置

### Logback 配置示例

在 `logback.xml` 中配置日志级别：

```xml
<!-- INFO 级别：记录关键操作和性能数据 -->
<logger name="top.yumbo.ai.reviewer.adapter.output.ai.LoggingAIServiceDecorator" level="INFO"/>

<!-- DEBUG 级别：记录详细的入参和返回值（建议仅在开发环境使用）-->
<logger name="top.yumbo.ai.reviewer.adapter.output.ai.LoggingAIServiceDecorator" level="DEBUG"/>

<!-- WARN 级别：仅记录警告和错误 -->
<logger name="top.yumbo.ai.reviewer.adapter.output.ai.LoggingAIServiceDecorator" level="WARN"/>
```

## 性能影响

### 影响分析

1. **INFO 级别**：性能影响可忽略
   - 仅记录操作摘要和耗时统计
   - 字符串长度计算复杂度 O(1)（Java String.length() 方法）

2. **DEBUG 级别**：轻微性能影响
   - 记录完整或截断的入参和返回值
   - 截断操作复杂度 O(n)，n = min(实际长度, 截断长度)
   - 建议仅在开发和测试环境启用

3. **内存影响**：
   - 不额外保存数据，仅在日志输出时截断
   - 不影响原有数据流转

### 性能优化建议

1. **生产环境**：使用 INFO 级别
2. **开发环境**：使用 DEBUG 级别排查问题
3. **性能测试**：可临时禁用日志（`create(config, false)`）

## 架构优势

### 1. 符合六边形架构

```
┌─────────────────────────────────────────────┐
│           Application Core (应用核心)        │
│    ┌──────────────────────────────────┐    │
│    │     AIServicePort (端口接口)      │    │
│    └──────────────────────────────────┘    │
└───────────────────┬─────────────────────────┘
                    │
        ┌───────────┴───────────┐
        │                       │
┌───────▼──────────┐   ┌───────▼──────────┐
│ LoggingDecorator │   │   AI Adapters    │
│  (日志装饰器)     │──▶│  (具体实现)       │
│  - DeepSeek      │   │  - OpenAI        │
│  - Claude        │   │  - Bedrock       │
│  - ...           │   │  - ...           │
└──────────────────┘   └──────────────────┘
```

### 2. 设计模式应用

- ✅ **装饰器模式**：动态添加日志功能
- ✅ **工厂模式**：统一创建和配置
- ✅ **依赖注入**：通过构造函数注入被装饰对象

### 3. SOLID 原则

- ✅ **单一职责原则 (SRP)**：日志记录职责独立
- ✅ **开闭原则 (OCP)**：对扩展开放，对修改关闭
- ✅ **里氏替换原则 (LSP)**：装饰器可完全替代原始服务
- ✅ **接口隔离原则 (ISP)**：实现完整的 AIServicePort 接口
- ✅ **依赖倒置原则 (DIP)**：依赖接口而非具体实现

## 测试覆盖

### 单元测试

**位置**: `src/test/java/top/yumbo/ai/reviewer/adapter/output/ai/LoggingAIServiceDecoratorTest.java`

**测试场景**:
- ✅ 同步分析成功场景
- ✅ 同步分析异常场景
- ✅ 异步分析成功场景
- ✅ 异步分析异常场景
- ✅ 批量异步分析成功场景
- ✅ 批量异步分析异常场景
- ✅ 服务可用性检查
- ✅ 最大并发数获取
- ✅ 服务关闭操作
- ✅ 长文本截断功能

### 运行测试

```bash
# Windows PowerShell
mvn test -Dtest=LoggingAIServiceDecoratorTest

# 或运行所有测试
mvn test
```

## 扩展性

### 添加更多装饰器

基于相同的模式，可以轻松添加其他装饰器：

```java
// 性能监控装饰器
public class PerformanceMonitoringDecorator implements AIServicePort {
    private final AIServicePort delegate;
    // 实现性能统计...
}

// 缓存装饰器
public class CachingDecorator implements AIServicePort {
    private final AIServicePort delegate;
    private final Cache<String, String> cache;
    // 实现缓存逻辑...
}

// 链式装饰
AIServicePort service = new DeepSeekAIAdapter(config);
service = new LoggingAIServiceDecorator(service);
service = new PerformanceMonitoringDecorator(service);
service = new CachingDecorator(service);
```

## 总结

### 实现特点

1. ✅ **零侵入**：不修改原有接口和实现类
2. ✅ **透明化**：对调用者完全透明
3. ✅ **可配置**：支持动态启用/禁用
4. ✅ **高性能**：最小化性能影响
5. ✅ **可扩展**：易于添加新的装饰器

### 适用场景

- ✅ 开发调试：查看 AI 服务的详细请求和响应
- ✅ 问题排查：记录异常情况和性能瓶颈
- ✅ 性能优化：分析 AI 调用耗时
- ✅ 审计追踪：记录重要操作日志

### 最佳实践

1. **生产环境**：使用 INFO 级别，关注性能和错误
2. **开发环境**：使用 DEBUG 级别，查看完整数据
3. **性能测试**：可临时禁用日志装饰器
4. **日志归档**：配置日志滚动和归档策略

---

**作者**: AI-Reviewer Team  
**版本**: 1.0  
**日期**: 2025-11-14

