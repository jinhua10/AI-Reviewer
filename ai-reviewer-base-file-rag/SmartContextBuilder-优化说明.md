# SmartContextBuilder 内容不丢失优化说明

## 问题描述

在原有的 `SmartContextBuilder` 实现中，当文档内容超过限制时，会截取片段并添加"..."标记，这可能导致重要信息丢失。

## 优化方案

### 1. 新增保留完整内容模式

添加了 `preserveFullContent` 配置选项（默认为 `true`），支持两种工作模式：

- **保留模式** (`preserveFullContent=true`)：智能分块，确保重要内容不丢失
- **提取模式** (`preserveFullContent=false`)：保留原有逻辑，提取最相关片段

### 2. 智能分块策略

当 `preserveFullContent=true` 时，系统会：

1. **查找所有关键词位置**：在文档中定位所有查询关键词的出现位置
2. **按关键词分块**：围绕每个关键词位置提取上下文片段
3. **保持语义完整**：在句子边界处切分，确保每个块语义完整
4. **优先级排序**：优先保留包含关键词的内容
5. **剩余内容提示**：对于未显示的内容，添加明确的提示信息

### 3. 核心改进点

#### 原有逻辑
```java
// 找到一个最佳位置，提取单个片段
int bestPosition = findBestPosition(content, keywords);
String extracted = content.substring(start, end);
// 可能丢失其他重要内容
```

#### 新逻辑
```java
// 找到所有关键词位置
List<Integer> keywordPositions = findAllKeywordPositions(content, keywords);

// 为每个关键词位置提取上下文
for (int keywordPos : keywordPositions) {
    String chunk = extractChunk(content, keywordPos, maxLength);
    result.append(chunk);
}

// 提示未显示的内容
if (hasRemainingContent) {
    result.append("[... 还有 X 字符未显示，内容已按关键词优先级提取]");
}
```

## 使用方法

### 默认使用（推荐）

```java
// 默认启用内容保留模式
SmartContextBuilder builder = new SmartContextBuilder();
```

### 通过Builder配置

```java
SmartContextBuilder builder = SmartContextBuilder.builder()
    .maxContextLength(8000)      // 总上下文限制
    .maxDocLength(2000)          // 单文档最大长度
    .preserveFullContent(true)   // 启用内容保留模式
    .build();
```

### 使用旧的提取模式

```java
SmartContextBuilder builder = SmartContextBuilder.builder()
    .maxContextLength(8000)
    .maxDocLength(2000)
    .preserveFullContent(false)  // 使用原有的片段提取逻辑
    .build();
```

## 测试验证

运行测试验证改进效果：

```bash
cd ai-reviewer-base-file-rag
mvn test -Dtest=SmartContextBuilderTest
```

测试覆盖：
- ✅ 短文档完整保留
- ✅ 长文档智能分块
- ✅ 关键词优先提取
- ✅ 多文档处理
- ✅ 两种模式对比
- ✅ 上下文统计信息

## 性能影响

- **时间复杂度**：O(n×m)，n为文档长度，m为关键词数量
- **空间复杂度**：O(n)，需要存储关键词位置列表
- **实际影响**：对于常见文档大小（< 100KB），性能影响可忽略

## 示例对比

### 原有方式（可能丢内容）
```
查询：重要关键词
文档：[前面1000字] 重要关键词在这里 [后面2000字]
结果：...重要关键词在这里...[只显示中间500字，前后内容丢失]
```

### 优化后（内容不丢失）
```
查询：重要关键词
文档：[前面1000字] 重要关键词在这里 [后面2000字]
结果：
  [块1: 第一个关键词附近的上下文]
  ...
  [块2: 第二个关键词附近的上下文]
  ...
  [... 还有 1500 字符未显示，内容已按关键词优先级提取]
```

## 配置建议

根据使用场景选择合适的配置：

| 场景 | preserveFullContent | maxDocLength | 说明 |
|-----|-------------------|--------------|------|
| 问答系统 | true | 2000 | 确保答案完整性 |
| 摘要生成 | false | 1000 | 只需要关键信息 |
| 代码检索 | true | 3000 | 保留完整代码上下文 |
| 快速预览 | false | 500 | 快速获取概览 |

## 注意事项

1. **上下文限制**：总上下文仍然受 `maxContextLength` 限制
2. **分块数量**：当关键词过多时，可能只显示部分分块
3. **提示信息**：始终会显示未包含内容的字符数，帮助用户了解信息完整度
4. **性能考虑**：超大文档（> 1MB）建议在索引阶段就进行预分块

## 后续优化方向

1. **增加分块重叠**：在相邻分块之间添加重叠区域，增强连贯性
2. **语义分块**：基于语义相似度而非关键词进行分块
3. **动态调整**：根据关键词密度动态调整分块大小
4. **缓存优化**：缓存分块结果，提高重复查询性能

## 更新日期

2025-11-24

## 作者

AI Reviewer Team

