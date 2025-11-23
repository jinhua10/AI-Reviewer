# HackathonApplication V2 - Batch Review功能使用说明

## 概述

HackathonApplication V2 支持两种运行模式:
1. **单项目评审模式** (`--review`): 评审单个项目目录
2. **批量评审模式** (`--reviewAll`): 批量评审一个目录下所有ZIP压缩包的项目

## 新增功能

### 1. 批量评审 (--reviewAll)

批量评审模式可以自动:
- 扫描指定目录下的所有ZIP文件
- 使用线程池并发处理多个项目
- 自动解压、评审、清理临时文件
- 从AI响应中提取分数
- 生成带分数的报告文件名(格式: `项目名_分数_review-report.md`)
- 支持断点续传(已生成的报告不会重新评审)
- 生成批量评审汇总报告

### 2. 配置说明

在 `application.yml` 中新增批量评审配置:

```yaml
ai-reviewer:
  # ... 其他配置 ...
  
  # 批量评审配置
  batch:
    thread-pool-size: 4  # 并发处理的线程数,建议设置为2-8之间
    temp-extract-dir: ./temp/extracted-projects  # 临时解压目录
```

## 使用方法

### 方式一: 单项目评审 (原有功能)

```bash
java -jar hackathonApplication.jar --review /path/to/project
```

### 方式二: 批量评审所有ZIP项目

```bash
java -jar hackathonApplication.jar --reviewAll /path/to/zip-directory
```

#### 示例

假设你有以下目录结构:

```
/hackathon-projects/
├── team01-chatbot.zip
├── team02-image-generator.zip
├── team03-code-analyzer.zip
...
└── team60-smart-assistant.zip
```

运行命令:

```bash
java -jar hackathonApplication.jar --reviewAll /hackathon-projects
```

### 生成的输出

#### 1. 单个项目报告

每个项目会生成一个Markdown报告,文件名格式:
```
项目名_分数_review-report.md
```

例如:
- `team01-chatbot_85_5_review-report.md` (表示85.5分)
- `team02-image-generator_92_0_review-report.md` (表示92.0分)
- `team03-code-analyzer_78_5_review-report.md` (表示78.5分)

> 注意: 小数点用下划线`_`表示,如85.5分显示为`85_5`

#### 2. 批量汇总报告

还会生成一个批量汇总报告,文件名格式:
```
batch-summary-YYYYMMDD_HHMMSS.md
```

例如: `batch-summary-20251123_223000.md`

汇总报告包含:
- 整体统计信息(总项目数、成功、失败、跳过)
- 所有项目的评审结果表格
- 失败项目的错误信息

## 断点续传功能

如果批量评审过程中被中断,再次运行相同命令时:
- 已生成报告的项目会被**自动跳过**
- 只会评审未完成的项目
- 这样可以节省时间和AI API调用成本

要重新评审某个项目,只需删除其对应的报告文件即可。

## 性能调优建议

### 1. 线程池大小配置

```yaml
ai-reviewer:
  batch:
    thread-pool-size: 4  # 根据实际情况调整
```

建议值:
- **小项目** (代码量<5000行): 6-8个线程
- **中型项目** (代码量5000-20000行): 4-6个线程  
- **大型项目** (代码量>20000行): 2-4个线程

### 2. AI配置

```yaml
ai-reviewer:
  ai:
    timeout-seconds: 600  # AI请求超时时间
    max-retries: 3  # 失败重试次数
```

## 工作流程

### 单项目模式 (--review)
```
读取项目 → 解析文件 → 调用AI评审 → 生成报告
```

### 批量模式 (--reviewAll)
```
1. 扫描ZIP文件目录
2. 检查已完成的项目(通过报告文件)
3. 创建评审任务队列
4. 使用线程池并发处理:
   a. 解压ZIP到临时目录
   b. 解析项目文件
   c. 调用AI评审
   d. 提取分数
   e. 生成带分数的报告
   f. 清理临时文件
5. 生成批量汇总报告
```

## 错误处理

- **ZIP解压失败**: 记录错误,继续处理下一个项目
- **AI评审失败**: 最多重试3次(可配置),失败后记录错误
- **分数提取失败**: 报告文件名使用`unknown`作为分数标识
- **临时文件清理失败**: 记录警告,不影响流程

## 示例日志输出

```
2025-11-23 22:30:00 - Starting batch review for all projects in: /hackathon-projects
2025-11-23 22:30:01 - Found 60 ZIP files to process
2025-11-23 22:30:01 - Found 15 already completed projects, will skip them
2025-11-23 22:30:01 - Will process 45 new projects with 4 threads
2025-11-23 22:30:05 - Extracting project: team01-chatbot
2025-11-23 22:30:10 - Reviewing project: team01-chatbot
2025-11-23 22:32:45 - Project team01-chatbot reviewed successfully with score: 85.5
...
2025-11-23 23:15:30 - Batch review completed: 45 successful, 0 failed, 15 skipped in 2730000 ms
2025-11-23 23:15:30 - Batch summary report written to: ./reports/batch-summary-20251123_231530.md
```

## 常见问题

### Q: 如何重新评审某个项目?
A: 删除该项目对应的报告文件,然后重新运行批量评审命令。

### Q: 评审过程中可以中断吗?
A: 可以。再次运行时会自动跳过已完成的项目。

### Q: 线程数设置多少合适?
A: 建议根据项目大小和你的机器性能设置。一般4-6个线程是比较合适的选择。

### Q: 临时解压目录会自动清理吗?
A: 是的。每个项目评审完成后会立即清理其临时文件。

### Q: 如果AI评审返回的内容没有分数怎么办?
A: 报告文件名会使用`unknown`作为分数标识,例如: `team01-chatbot_unknown_review-report.md`

## 技术架构

### 核心类

1. **HackathonAIEngineV2**: 批量评审引擎
   - 管理线程池
   - 协调ZIP解压和评审流程
   - 生成汇总报告

2. **ZipUtil**: ZIP文件工具类
   - 安全解压ZIP文件
   - 防止Zip Slip漏洞
   - 清理临时文件

3. **ScoreExtractor**: 分数提取工具类
   - 从AI响应中提取总分
   - 支持多种分数格式
   - 格式化分数用于文件名

### 内部类

- **BatchResult**: 批量评审结果
- **ProjectReviewResult**: 单个项目评审结果
- **ProjectReviewTask**: 项目评审任务

## 更新历史

### Version 2.0 (2025-11-23)
- ✨ 新增批量评审模式 (`--reviewAll`)
- ✨ 支持从ZIP文件批量评审项目
- ✨ 自动提取分数并体现在报告文件名中
- ✨ 支持断点续传,避免重复评审
- ✨ 生成批量汇总报告
- ✨ 可配置的并发线程池
- 🎯 优化临时文件管理
- 🎯 增强错误处理和日志记录

