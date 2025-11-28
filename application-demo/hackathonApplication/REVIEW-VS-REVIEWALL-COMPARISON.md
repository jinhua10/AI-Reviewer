# --review vs --reviewAll 打分逻辑对比分析

## 📊 结论

**✅ 是的，两者的打分逻辑完全一样！**

---

## 🔍 详细对比

### 共同点（核心打分逻辑）

两个功能都使用**完全相同的底层引擎**进行评分：

```java
// --review (reviewSingleProject)
return baseEngine.execute(context);

// --reviewAll (processProject)
ProcessResult processResult = baseEngine.execute(context);
```

**baseEngine = HackathonAIEngine**

这意味着：
- ✅ 使用相同的 AI 模型（Writer Palmyra）
- ✅ 使用相同的配置（temperature: 0, top-p: 0.3）
- ✅ 使用相同的 prompt（user-prompt 和 sys-prompt）
- ✅ 使用相同的文件扫描规则（include/exclude patterns）
- ✅ 使用相同的 maxFileSize 限制（200KB）
- ✅ 使用相同的文件处理逻辑（README 优先、反作弊过滤）
- ✅ 使用相同的评分标准

---

## 📋 ExecutionContext 对比

两者构建的 `ExecutionContext` 参数完全一致：

### --review (reviewSingleProject)
```java
ExecutionContext context = ExecutionContext.builder()
    .targetDirectory(Paths.get(targetPath))           // 目标目录
    .includePatterns(properties.getScanner().getIncludePatterns())  // 包含规则
    .excludePatterns(properties.getScanner().getExcludePatterns())  // 排除规则
    .maxFileSize(maxFileSize)                         // 200KB 限制
    .aiConfig(aiConfig)                               // AI 配置
    .processorConfig(processorConfig)                 // 处理器配置
    .threadPoolSize(properties.getExecutor().getThreadPoolSize())  // 线程池
    .build();
```

### --reviewAll (processProject)
```java
ExecutionContext context = ExecutionContext.builder()
    .targetDirectory(extractedPath)                   // 目标目录（解压后）
    .includePatterns(properties.getScanner().getIncludePatterns())  // 包含规则
    .excludePatterns(properties.getScanner().getExcludePatterns())  // 排除规则
    .maxFileSize(maxFileSize)                         // 200KB 限制
    .aiConfig(aiConfig)                               // AI 配置
    .processorConfig(processorConfig)                 // 处理器配置
    .threadPoolSize(properties.getExecutor().getThreadPoolSize())  // 线程池
    .build();
```

**对比结果：参数完全相同！** ✅

---

## 🎯 HackathonAIEngine.execute() 流程

两者都调用相同的 `baseEngine.execute(context)` 方法，该方法执行以下步骤：

### 步骤 1: 文件扫描
```java
if (context.getMaxFileSize() != null && context.getMaxFileSize() > 0) {
    files = fileScanner.scanWithSizeLimit(context.getTargetDirectory(), context.getMaxFileSize());
}
```
- ✅ 使用 200KB 文件大小限制
- ✅ 相同的扫描逻辑

### 步骤 2: 文件过滤
```java
List<Path> filteredFiles = fileFilter.filter(files,
    context.getIncludePatterns(),
    context.getExcludePatterns());
```
- ✅ 使用相同的 include/exclude patterns
- ✅ 排除 package-lock.json、图片、非 README markdown 等

### 步骤 3: 文件解析和排序
```java
// Separate README.md files from other files
for (PreProcessedData data : preprocessedDataList) {
    if (fileName.equalsIgnoreCase("README.md")) {
        readmeFiles.add(data);  // README 优先
    } else {
        otherFiles.add(data);
    }
}
```
- ✅ README.md 优先放在前面
- ✅ 其他源码文件随后

### 步骤 4: 反作弊过滤
```java
String filteredContent = AntiCheatFilter.filterSuspiciousContent(
    content,
    filePath.toString()
);
```
- ✅ 自动检测和过滤可疑注释
- ✅ 相同的反作弊逻辑

### 步骤 5: 构建 Prompt
```java
// Project Overview (文件树和统计信息)
sb.append(buildProjectOverview(preprocessedDataList));

// README.md Section
sb.append("📖 PROJECT DOCUMENTATION");
for (PreProcessedData readmeData : readmeFiles) {
    sb.append(getFileContent(readmeData));
}

// Source Code Section
sb.append("💻 SOURCE CODE FILES");
for (PreProcessedData otherData : otherFiles) {
    sb.append(getFileContent(otherData));
}
```
- ✅ 相同的 prompt 结构
- ✅ 相同的文件格式化

### 步骤 6: AI 调用
```java
List<AIResponse> aiResponses = invokeAI(Collections.singletonList(oneContent), context);
```
- ✅ 使用相同的 AI 配置
- ✅ Writer Palmyra: temperature=0, top-p=0.3
- ✅ 相同的 sys-prompt 和 user-prompt

### 步骤 7: 结果处理
```java
ProcessResult result = processResults(aiResponses, context);
```
- ✅ 相同的结果处理逻辑

---

## 🔄 区别点（仅流程层面，不影响打分）

虽然打分逻辑完全相同，但两个功能在**外层流程**上有些区别：

### 1. 输入方式不同

| 功能 | 输入 | 说明 |
|-----|-----|-----|
| `--review` | 直接指定项目目录 | 单个项目，直接评分 |
| `--reviewAll` | 指定包含 ZIP 文件的根目录 | 批量评分，需要先解压 ZIP |

### 2. 重试机制（仅 --reviewAll）

**--reviewAll 独有的重试逻辑：**
```java
// Check if score is valid (not 0 and >= 30)
boolean isValidScore = score != null && score > 0 && score >= MIN_VALID_SCORE;

if (!isValidScore && attempt < MAX_RETRY_ATTEMPTS) {
    // Score is too low, retry
    log.warn("⚠️ Project received low score: {}. Retrying...", score);
    result.setRetryCount(attempt);
    continue; // Retry up to 3 times
}
```

**重试条件：**
- 分数为 0
- 或分数 < 30 分（MIN_VALID_SCORE）

**重试次数：**
- 最多 3 次（MAX_RETRY_ATTEMPTS）

**--review 没有重试机制：**
- 一次评分，直接返回结果

**⚠️ 重要说明：**
- 重试**不会改变打分逻辑**
- 重试只是**重新调用相同的打分流程**
- 由于 `temperature: 0`，每次调用结果应该完全一致
- 重试机制的作用是应对可能的 API 错误或异常低分

### 3. 报告文件命名

| 功能 | 文件名格式 |
|-----|----------|
| `--review` | `项目名-review-report.md` |
| `--reviewAll` | `FolderB名-分数-ZIP名.md` |

例如：
- `--review`: `my-project-review-report.md`
- `--reviewAll`: `team01-75_5-my-project.md`

### 4. CSV 记录（仅 --reviewAll）

**--reviewAll 会记录到 CSV：**
```
completed-reviews.csv
FolderB,ZipFileName,Score,ReportFileName,CompletedTime,OverallComment,RetryCount
team01,project.zip,75.5,team01-75_5-project.md,2025-11-28 12:00:00,"Good project",0
```

**--review 不记录 CSV**

### 5. 并行处理（仅 --reviewAll）

**--reviewAll 支持并行：**
```java
ExecutorService executorService = Executors.newFixedThreadPool(batchThreadPoolSize);
```
- 可以同时评审多个项目
- 默认 4 个线程

**--review 单项目：**
- 一次只评审一个项目

---

## 📊 打分一致性保证

由于两者使用**完全相同的底层引擎和配置**，打分结果应该完全一致：

### 测试场景

假设有一个项目 `my-project`：

#### 方式 1：使用 --review
```bash
java -jar app.jar --review /path/to/my-project
```
**预期分数：75.5 分**

#### 方式 2：使用 --reviewAll
```bash
# 1. 将项目打包为 my-project.zip
# 2. 放到 FolderA/FolderB/ 目录下
# 3. 在 FolderB 中创建 done.txt
java -jar app.jar --reviewAll /path/to/FolderA
```
**预期分数：75.5 分** （完全相同！）

### 一致性因素

✅ **保证一致性的因素：**
- `temperature: 0` → 完全确定性输出
- `top-p: 0.3` → 固定的采样范围
- 相同的 AI 配置
- 相同的 prompt
- 相同的文件处理逻辑
- 相同的反作弊过滤

❌ **可能导致差异的因素：**
- **无**（理论上不应该有差异）

---

## 🎯 选择建议

### 使用 --review 的场景

✅ **推荐：**
- 测试单个项目
- 快速验证评分逻辑
- 调试和开发
- 不需要重试机制

❌ **不推荐：**
- 需要批量处理多个项目
- 需要自动重试低分项目

### 使用 --reviewAll 的场景

✅ **推荐：**
- 批量评审多个项目（黑客松场景）
- 需要并行处理提高效率
- 需要 CSV 记录和追踪
- 需要自动重试低分项目
- 需要从 ZIP 文件评审

❌ **不推荐：**
- 只有一个项目需要评审
- 项目已经是解压后的目录

---

## 📝 总结

### 核心结论

```
打分逻辑：--review == --reviewAll ✅

差异点：
1. --reviewAll 有重试机制（但不改变打分逻辑）
2. --reviewAll 支持批量并行处理
3. --reviewAll 记录 CSV
4. --reviewAll 需要 ZIP 文件输入
```

### 关键代码

```java
// 两者都调用相同的核心方法
baseEngine.execute(context)
    ↓
HackathonAIEngine.execute()
    ↓
使用相同的：
- 文件扫描（maxFileSize: 200KB）
- 文件过滤（include/exclude patterns）
- README 优先排序
- 反作弊过滤
- AI 模型调用（Writer Palmyra, temp=0, top-p=0.3）
- 评分提取
```

### 最终答案

**是的，`--review` 和 `--reviewAll` 的打分逻辑完全一样！** 

它们使用相同的：
- AI 模型和配置
- 评分标准
- 文件处理流程
- Prompt 结构

唯一的区别是外层的工作流程（批量 vs 单个、重试、CSV 记录等），但这些不影响打分的核心逻辑。


