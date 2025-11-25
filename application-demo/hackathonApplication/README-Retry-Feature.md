# ✅ 低分自动重试功能实现

## 功能说明

当 AI 评分结果为 **0 分或低于 30 分**时，系统会自动重新评分，最多重试 3 次。

### 核心逻辑

1. **第 1 次评分**：正常评分
   - 如果分数 ≥ 30：记录到 CSV，完成 ✅
   - 如果分数 = 0 或 < 30：**重试**，不记录 CSV

2. **第 2 次评分**：重试 1
   - 如果分数 ≥ 30：记录到 CSV，完成 ✅
   - 如果分数 = 0 或 < 30：**继续重试**

3. **第 3 次评分**：重试 2（最后一次）
   - 无论分数多少：**都记录到 CSV** ✅
   - 记录重试次数

## 实现细节

### 1. 新增常量

```java
private static final double MIN_VALID_SCORE = 30.0;  // 最低有效分数阈值
private static final int MAX_RETRY_ATTEMPTS = 3;     // 最大重试次数
```

### 2. CSV 格式更新

**新增 `RetryCount` 列**：

```csv
FolderB,ZipFileName,Score,ReportFileName,CompletedTime,OverallComment,RetryCount
T00001,project.zip,85.0,T00001-85_0-project.md,2025-11-25 15:00:00,"优秀实现",0
T00002,bad.zip,25.0,T00002-25_0-bad.md,2025-11-25 15:05:00,"需要改进",2
```

- `RetryCount = 0`：首次评分成功
- `RetryCount = 1`：重试 1 次后成功
- `RetryCount = 2`：重试 2 次后仍然低分（最终记录）

### 3. 核心重试逻辑

```java
// 只提取一次 ZIP，避免重复解压
extractedPath = ZipUtil.extractZip(task.getZipFilePath(), tempExtractDir);

// 重试循环：最多 3 次
for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
    // 调用 AI 评分
    ProcessResult processResult = baseEngine.execute(context);
    
    // 提取分数
    Double score = ScoreExtractor.extractScore(processResult.getContent());
    
    // 检查分数是否有效
    boolean isValidScore = score != null && score > 0 && score >= MIN_VALID_SCORE;
    
    if (!isValidScore && attempt < MAX_RETRY_ATTEMPTS) {
        // 分数太低且未到最后一次，继续重试
        log.warn("⚠️ 分数过低: {} (尝试 {}/{}). 重试中...", score, attempt, MAX_RETRY_ATTEMPTS);
        continue; // 重试
    }
    
    // 分数有效或已到最后一次，记录结果
    result.setRetryCount(attempt - 1); // 记录实际重试次数
    
    if (!isValidScore) {
        log.error("❌ 重试 {} 次后分数仍然过低: {}. 记录最终结果.", MAX_RETRY_ATTEMPTS, score);
    }
    
    // 生成报告并记录到 CSV
    // ...
    
    break; // 成功，退出重试循环
}
```

## 日志示例

### 场景 1：首次评分成功（≥ 30 分）

```
2025-11-25 15:00:00 - Reviewing project: T00001/project.zip (Attempt 1/3)
2025-11-25 15:00:30 - ✅ Project T00001/project.zip reviewed successfully with score: 85.0 (Retry count: 0)
2025-11-25 15:00:31 - ✅ Review completed and recorded. CSV总记录数: 1
```

### 场景 2：第 2 次评分成功

```
2025-11-25 15:05:00 - Reviewing project: T00002/bad.zip (Attempt 1/3)
2025-11-25 15:05:30 - ⚠️ Project T00002/bad.zip received low score: 15.0 (Attempt 1/3). Retrying...
2025-11-25 15:05:31 - Reviewing project: T00002/bad.zip (Attempt 2/3)
2025-11-25 15:06:00 - ✅ Project T00002/bad.zip reviewed successfully with score: 65.0 (Retry count: 1)
2025-11-25 15:06:01 - ✅ Review completed and recorded. CSV总记录数: 2
```

### 场景 3：3 次后仍然低分（记录最终结果）

```
2025-11-25 15:10:00 - Reviewing project: T00003/poor.zip (Attempt 1/3)
2025-11-25 15:10:30 - ⚠️ Project T00003/poor.zip received low score: 10.0 (Attempt 1/3). Retrying...
2025-11-25 15:10:31 - Reviewing project: T00003/poor.zip (Attempt 2/3)
2025-11-25 15:11:00 - ⚠️ Project T00003/poor.zip received low score: 20.0 (Attempt 2/3). Retrying...
2025-11-25 15:11:01 - Reviewing project: T00003/poor.zip (Attempt 3/3)
2025-11-25 15:11:30 - ❌ Project T00003/poor.zip still has low score after 3 attempts: 25.0. Recording final result.
2025-11-25 15:11:31 - ✅ Project T00003/poor.zip reviewed successfully with score: 25.0 (Retry count: 2)
2025-11-25 15:11:32 - ✅ Review completed and recorded. CSV总记录数: 3
```

### 场景 4：0 分重试

```
2025-11-25 15:15:00 - Reviewing project: T00004/empty.zip (Attempt 1/3)
2025-11-25 15:15:30 - ⚠️ Project T00004/empty.zip received low score: 0.0 (Attempt 1/3). Retrying...
2025-11-25 15:15:31 - Reviewing project: T00004/empty.zip (Attempt 2/3)
2025-11-25 15:16:00 - ✅ Project T00004/empty.zip reviewed successfully with score: 45.0 (Retry count: 1)
```

## CSV 数据示例

```csv
FolderB,ZipFileName,Score,ReportFileName,CompletedTime,OverallComment,RetryCount
T00001,project.zip,85.0,T00001-85_0-project.md,2025-11-25 15:00:00,"优秀的实现，代码质量高。",0
T00002,bad.zip,65.0,T00002-65_0-bad.md,2025-11-25 15:06:00,"经过改进后符合要求。",1
T00003,poor.zip,25.0,T00003-25_0-poor.md,2025-11-25 15:11:30,"代码质量较差，需要大幅改进。",2
T00004,empty.zip,45.0,T00004-45_0-empty.md,2025-11-25 15:16:00,"基本功能完成。",1
```

## 重要特性

### ✅ 优点

1. **避免误判**：低分可能是 AI 临时波动，重试可以获得更准确的分数
2. **节省资源**：只解压一次 ZIP，重试时复用已解压的文件
3. **完整记录**：最终都会记录到 CSV，不会丢失数据
4. **透明度高**：RetryCount 字段记录了重试次数，便于分析

### ⚠️ 注意事项

1. **API 调用增加**：最坏情况下每个项目会调用 AI 3 次
2. **时间延长**：重试会增加评审时间
3. **成本增加**：API 调用次数增加意味着成本增加

## 性能影响评估

### 场景分析

假设有 100 个项目：

**正常情况（90% 首次成功）**：
- 90 个项目：1 次 API 调用
- 8 个项目：2 次 API 调用（重试 1 次成功）
- 2 个项目：3 次 API 调用（重试到最后）
- **总 API 调用**: 90 + 16 + 6 = **112 次**
- **额外调用**: 12%

**较差情况（70% 首次成功）**：
- 70 个项目：1 次 API 调用
- 20 个项目：2 次 API 调用
- 10 个项目：3 次 API 调用
- **总 API 调用**: 70 + 40 + 30 = **140 次**
- **额外调用**: 40%

## 配置建议

### 调整阈值

如果需要修改最低分数阈值，编辑代码：

```java
private static final double MIN_VALID_SCORE = 30.0;  // 改为你需要的值
```

### 调整重试次数

```java
private static final int MAX_RETRY_ATTEMPTS = 3;  // 改为你需要的次数
```

## 数据分析

通过 CSV 中的 `RetryCount` 字段，可以分析：

```bash
# 统计重试分布
cut -d',' -f7 completed-reviews.csv | sort | uniq -c

# 输出示例：
# 85 0  （85 个项目首次成功）
# 12 1  （12 个项目重试 1 次）
#  3 2  （3 个项目重试 2 次）
```

## 总结

### ✅ 已实现的功能

1. **智能重试**：0 分或 < 30 分自动重试
2. **最多 3 次**：避免无限重试
3. **最终记录**：3 次后仍低分也会记录
4. **重试计数**：CSV 中记录重试次数
5. **性能优化**：只解压一次 ZIP

### 📊 预期效果

- 减少 AI 临时波动导致的低分误判
- 提高评分的准确性和稳定性
- 保留完整的评审记录和重试历史

---

**编译状态**: ✅ BUILD SUCCESS  
**功能状态**: ✅ 已完成并测试通过

