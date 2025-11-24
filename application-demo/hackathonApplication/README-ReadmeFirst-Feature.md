# README.md 优先级排序功能说明

## 功能概述

在构造提示词从 ZIP 压缩包读取文件时，系统会将 **README.md** 文件放到提示词的最开头，然后再添加其他源码文件的信息。

这样做的好处：
1. **项目概述优先**：README.md 通常包含项目的整体描述、架构说明和使用指南
2. **更好的上下文**：AI 可以先了解项目的全局信息，再分析具体代码
3. **提高评审质量**：有了项目背景，AI 能做出更准确的评审判断

## 实现位置

修改位置：`HackathonAIEngine.java` 的 `execute()` 方法

### 修改前的逻辑

```java
// 简单地按扫描顺序拼接所有文件
StringBuilder sb = new StringBuilder();
for (PreProcessedData preProcessedData : preprocessedDataList) {
    sb.append(getFileContent(preProcessedData));
}
```

### 修改后的逻辑

```java
// 1. 分离 README.md 和其他文件
List<PreProcessedData> readmeFiles = new ArrayList<>();
List<PreProcessedData> otherFiles = new ArrayList<>();

for (PreProcessedData data : preprocessedDataList) {
    String fileName = data.getMetadata().getFileName();
    if (fileName != null && fileName.equalsIgnoreCase("README.md")) {
        readmeFiles.add(data);  // README.md 文件
    } else {
        otherFiles.add(data);   // 其他源码文件
    }
}

// 2. 构建内容：README.md 在前，源码文件在后
StringBuilder sb = new StringBuilder();

// 先添加 README.md
for (PreProcessedData readmeData : readmeFiles) {
    sb.append(getFileContent(readmeData));
}

// 再添加其他源码文件
for (PreProcessedData otherData : otherFiles) {
    sb.append(getFileContent(otherData));
}
```

## 文件格式

每个文件的内容格式化如下：

```
file path: /path/to/file
file content:
```
<actual file content here>
```

```

## 示例

假设项目结构如下：

```
project/
├── README.md
├── src/
│   ├── Main.java
│   ├── Utils.java
│   └── Config.java
└── pom.xml
```

**生成的提示词顺序：**

1. README.md（项目说明）
2. Main.java（源码）
3. Utils.java（源码）
4. Config.java（源码）
5. pom.xml（配置文件）

## 特性说明

### 大小写不敏感

使用 `equalsIgnoreCase("README.md")` 匹配，支持以下文件名：
- README.md
- readme.md
- ReadMe.md
- README.MD
- 等等...

### 支持多个 README

如果项目中有多个 README.md 文件（例如在不同的子目录），所有的 README 都会被放在最前面。

### 日志输出

处理时会输出日志，方便调试：

```
Found README.md file: /path/to/project/README.md
Built prompt with 1 README.md file(s) at the beginning, followed by 15 source file(s)
```

## 对现有功能的影响

✅ **无破坏性影响**
- 不影响文件扫描逻辑
- 不影响文件过滤逻辑
- 不影响文件解析逻辑
- 只改变了文件在提示词中的排列顺序

✅ **向后兼容**
- 如果项目中没有 README.md，行为与之前完全一致
- 其他文件的处理方式保持不变

## 测试建议

### 测试场景 1：包含 README.md 的项目

```
项目结构：
- README.md
- src/Main.java
- src/Utils.java

预期结果：提示词中 README.md 内容在最前面
```

### 测试场景 2：不包含 README.md 的项目

```
项目结构：
- src/Main.java
- src/Utils.java

预期结果：提示词按原有顺序排列
```

### 测试场景 3：多个 README 文件

```
项目结构：
- README.md（根目录）
- docs/README.md（子目录）
- src/Main.java

预期结果：两个 README 都在最前面，然后是源码
```

### 测试场景 4：不同大小写的 README

```
项目结构：
- readme.md
- ReadMe.md
- src/Main.java

预期结果：所有的 readme 文件都在最前面
```

## 配置说明

此功能是**硬编码**的，无需配置。

如果需要自定义排序逻辑，可以修改 `HackathonAIEngine.java` 中的文件分类条件。

例如，如果还想将 `pom.xml` 或 `package.json` 也放在前面：

```java
if (fileName != null && 
    (fileName.equalsIgnoreCase("README.md") || 
     fileName.equalsIgnoreCase("pom.xml") ||
     fileName.equalsIgnoreCase("package.json"))) {
    priorityFiles.add(data);
} else {
    otherFiles.add(data);
}
```

## 相关文件

- `HackathonAIEngine.java` - 实现了文件排序逻辑
- `HackathonAIEngineV2.java` - 批量处理引擎，使用 HackathonAIEngine
- `HttpBasedAIAdapter.java` - AI 服务适配器，接收构建好的提示词

## 注意事项

1. **文件名匹配规则**：只匹配完整的 `README.md` 文件名，不匹配 `README.txt` 或其他扩展名
2. **路径无关性**：无论 README.md 在哪个目录下，都会被识别并提前
3. **性能影响**：额外的文件分类操作对性能影响可忽略不计（O(n) 复杂度）
4. **编码支持**：README.md 支持 UTF-8 编码的中英文内容

## 后续优化建议

如果需要更灵活的文件排序策略，可以考虑：

1. **配置化**：在 `application.yml` 中配置优先级文件列表
   ```yaml
   ai-reviewer:
     prompt:
       priority-files:
         - README.md
         - ARCHITECTURE.md
         - pom.xml
         - package.json
   ```

2. **自定义排序器**：支持插件式的文件排序策略

3. **智能识别**：根据文件内容自动识别项目说明文档

---

**版本**：v1.0  
**修改日期**：2025-11-25  
**状态**：✅ 已实现并测试通过

