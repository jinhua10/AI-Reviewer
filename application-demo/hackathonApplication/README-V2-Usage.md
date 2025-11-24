# HackathonAIEngineV2 使用说明

## 功能概述

V2版本支持层次化目录结构的批量项目审查，具有以下特性：

1. **层次化目录结构**：支持 `文件夹A -> 文件夹B -> ZIP包C` 的三层结构
2. **条件处理**：只处理包含 `done.txt` 标记文件的子文件夹
3. **最新ZIP选择**：自动选择每个子文件夹中最新的ZIP包进行审查
4. **智能命名**：生成格式为 `B文件夹名-分数-C.md` 的报告（小数点用下划线替代）
5. **CSV记录**：维护已完成审查的CSV文件，避免重复处理

## 目录结构示例

```
RootFolder/                    # 文件夹A（根目录）
├── Project001/                # 文件夹B（子文件夹1）
│   ├── done.txt              # 标记文件（必需）
│   ├── submission-v1.zip     # 旧版本
│   └── submission-v2.zip     # 最新版本 ← 将被处理
├── Project002/                # 文件夹B（子文件夹2）
│   ├── done.txt              # 标记文件（必需）
│   └── final-code.zip        # ZIP包 ← 将被处理
├── Project003/                # 文件夹B（子文件夹3，无done.txt）
│   └── code.zip              # 将被跳过
└── Project004/                # 文件夹B（子文件夹4）
    ├── done.txt              # 标记文件（必需）
    ├── v1.zip
    ├── v2.zip
    └── v3.zip                # 最新版本 ← 将被处理
```

## 使用方法

### 1. 命令行使用

```bash
java -jar hackathonApplication.jar --reviewAll /path/to/RootFolder
```

### 2. 生成的文件

#### 报告文件

报告文件命名格式：`{B文件夹名}-{分数}-{C}.md`

示例：
- `Project001-85_5-submission-v2.md` （分数85.5）
- `Project002-92_0-final-code.md` （分数92.0）
- `Project004-78_3-v3.md` （分数78.3）

#### CSV记录文件

在输出目录生成 `completed-reviews.csv`，记录所有已完成的审查：

```csv
FolderB,ZipFileName,Score,ReportFileName,CompletedTime
Project001,submission-v2.zip,85.5,Project001-85_5-submission-v2.md,2025-11-25 10:30:15
Project002,final-code.zip,92.0,Project002-92_0-final-code.md,2025-11-25 10:35:22
Project004,v3.zip,78.3,Project004-78_3-v3.md,2025-11-25 10:40:45
```

#### 批量汇总报告

生成格式为 `batch-summary-{timestamp}.md` 的汇总报告，包含：
- 总体统计信息
- 所有项目的结果表格
- 失败项目的详细信息

## 工作流程

1. **扫描根目录**：查找所有子文件夹（文件夹B）
2. **检查标记文件**：只处理包含 `done.txt` 的子文件夹
3. **选择最新ZIP**：在每个子文件夹中找到最新修改的ZIP文件
4. **检查CSV记录**：跳过已经完成的审查（基于文件夹名和ZIP文件名）
5. **并行处理**：使用线程池并行处理多个项目
6. **生成报告**：为每个项目生成带分数的报告文件
7. **更新CSV**：成功完成后将记录追加到CSV文件
8. **生成汇总**：创建批量处理的汇总报告

## 配置参数

在 `application.yml` 中配置：

```yaml
ai-reviewer:
  batch:
    thread-pool-size: 4              # 并行处理的线程数
    temp-extract-dir: ./temp/extracted-projects  # 临时解压目录
  
  processor:
    output-path: ./output/reports    # 报告输出目录（CSV也在这里）
```

## 断点续传

- V2版本支持断点续传功能
- 已完成的审查记录在 `completed-reviews.csv` 中
- 重新运行时会自动跳过已完成的项目
- 判断依据：文件夹B名称 + ZIP文件名的组合

## 注意事项

1. **done.txt 文件**：必须在子文件夹中创建此文件才会被处理（内容可以为空）
2. **ZIP文件选择**：基于文件修改时间选择最新的ZIP包
3. **报告命名**：小数点会被下划线替代（85.5 → 85_5）
4. **CSV格式**：不要手动编辑CSV文件，避免格式错误导致解析失败
5. **并发处理**：默认4个线程，可根据机器性能调整

## 常见问题

**Q: 如何重新处理已完成的项目？**
A: 删除 `completed-reviews.csv` 中对应的行，或删除整个CSV文件重新开始。

**Q: 子文件夹中有多个ZIP，如何确定使用哪一个？**
A: 系统会自动选择最新修改时间的ZIP文件。

**Q: 如果评审失败会怎样？**
A: 失败的项目不会写入CSV，下次运行时会重新处理。

**Q: 可以手动指定处理某些子文件夹吗？**
A: 在不想处理的子文件夹中删除 `done.txt` 文件即可。

## 示例输出

```
2025-11-25 10:30:00 INFO  Starting batch review for all projects in: /path/to/RootFolder
2025-11-25 10:30:01 INFO  Found 3 eligible folders (with done.txt) to process
2025-11-25 10:30:01 INFO  Found 1 already completed reviews in CSV
2025-11-25 10:30:01 INFO  Skipping already reviewed: Project001 - submission-v2.zip
2025-11-25 10:30:01 INFO  Will process 2 new projects with 4 threads
2025-11-25 10:30:02 INFO  Extracting project from folder Project002: final-code.zip
2025-11-25 10:30:05 INFO  Reviewing project: Project002/final-code.zip
2025-11-25 10:31:20 INFO  Successfully reviewed: Project002 - final-code.zip (Score: 92.0)
2025-11-25 10:31:21 INFO  Batch review completed: 2 successful, 0 failed, 1 skipped in 81000 ms
```

