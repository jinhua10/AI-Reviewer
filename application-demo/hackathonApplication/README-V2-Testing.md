# V2版本快速测试指南

## 准备测试环境

### 1. 创建测试目录结构

```powershell
# 创建测试根目录
mkdir D:\test-review\RootFolder

# 创建子文件夹并添加done.txt
mkdir D:\test-review\RootFolder\Project001
echo "ready" > D:\test-review\RootFolder\Project001\done.txt

mkdir D:\test-review\RootFolder\Project002
echo "ready" > D:\test-review\RootFolder\Project002\done.txt

# 创建一个没有done.txt的文件夹（测试跳过逻辑）
mkdir D:\test-review\RootFolder\Project003
```

### 2. 准备ZIP文件

将你的测试项目代码打包成ZIP文件，放到对应的文件夹中：

```
RootFolder/
├── Project001/
│   ├── done.txt
│   └── mycode-v1.zip          # 放置项目源码ZIP
├── Project002/
│   ├── done.txt
│   ├── code-old.zip           # 旧版本
│   └── code-new.zip           # 新版本（最新）
└── Project003/
    └── test.zip               # 没有done.txt，会被跳过
```

## 运行测试

### 方式1：使用Maven运行

```powershell
cd D:\Jetbrains\hackathon\AI-Reviewer\application-demo\hackathonApplication

mvn spring-boot:run -Dspring-boot.run.arguments="--reviewAll D:\test-review\RootFolder"
```

### 方式2：使用JAR包运行

```powershell
# 先打包
mvn clean package -DskipTests

# 运行
java -jar target\hackathonApplication-1.0.jar --reviewAll D:\test-review\RootFolder
```

## 验证结果

### 1. 检查日志输出

应该看到类似以下的日志：

```
Starting batch review for all projects in: D:\test-review\RootFolder
Found 2 eligible folders (with done.txt) to process
Found 0 already completed reviews in CSV
Will process 2 new projects with 4 threads
Extracting project from folder Project001: mycode-v1.zip
Reviewing project: Project001/mycode-v1.zip
Successfully reviewed: Project001 - mycode-v1.zip (Score: 85.5)
...
```

### 2. 检查生成的文件

在输出目录（默认 `./output/reports`）应该看到：

```
output/reports/
├── Project001-85_5-mycode-v1.md        # 评审报告（分数85.5）
├── Project002-92_0-code-new.md         # 评审报告（分数92.0）
├── completed-reviews.csv               # CSV记录
└── batch-summary-20251125_103000.md    # 批量汇总报告
```

### 3. 检查CSV内容

打开 `completed-reviews.csv`：

```csv
FolderB,ZipFileName,Score,ReportFileName,CompletedTime
Project001,mycode-v1.zip,85.5,Project001-85_5-mycode-v1.md,2025-11-25 10:30:15
Project002,code-new.zip,92.0,Project002-92_0-code-new.md,2025-11-25 10:35:22
```

### 4. 测试断点续传

再次运行相同的命令：

```powershell
java -jar target\hackathonApplication-1.0.jar --reviewAll D:\test-review\RootFolder
```

应该看到：
```
Found 2 already completed reviews in CSV
Skipping already reviewed: Project001 - mycode-v1.zip
Skipping already reviewed: Project002 - code-new.zip
Will process 0 new projects
```

## 测试场景

### 场景1：测试最新ZIP选择

在 `Project001` 中添加新的ZIP文件：

```powershell
# 假设已经有 mycode-v1.zip
# 复制一个新版本（修改时间更新）
copy D:\test-review\RootFolder\Project001\mycode-v1.zip D:\test-review\RootFolder\Project001\mycode-v2.zip
```

重新运行，应该会处理 `mycode-v2.zip`（因为它是最新的）

### 场景2：测试done.txt过滤

```powershell
# Project003没有done.txt，应该被跳过
# 检查日志不应该出现Project003的处理记录
```

### 场景3：测试报告命名

检查生成的报告文件名：
- 分数85.5 → `Project001-85_5-mycode-v1.md`
- 分数92.0 → `Project002-92_0-code-new.md`
- 小数点被下划线替代 ✓

### 场景4：手动清理CSV重新处理

```powershell
# 删除CSV文件
del output\reports\completed-reviews.csv

# 重新运行，所有项目会被重新处理
java -jar target\hackathonApplication-1.0.jar --reviewAll D:\test-review\RootFolder
```

## 配置调整

修改 `src/main/resources/application.yml`：

```yaml
ai-reviewer:
  batch:
    thread-pool-size: 2              # 降低并发数
    temp-extract-dir: D:\temp\extracted  # 自定义解压目录
  
  processor:
    output-path: D:\test-review\output   # 自定义输出目录
```

## 常见问题排查

### 问题1：没有生成报告

检查：
1. 子文件夹是否有 `done.txt`
2. ZIP文件是否存在
3. ZIP文件是否可以正常解压
4. AI服务是否正常响应

### 问题2：CSV格式错误

删除CSV文件重新生成：
```powershell
del output\reports\completed-reviews.csv
```

### 问题3：内存不足

降低并发线程数：
```yaml
ai-reviewer:
  batch:
    thread-pool-size: 1  # 改为单线程处理
```

### 问题4：临时文件未清理

手动清理临时目录：
```powershell
rmdir /s /q .\temp\extracted-projects
```

## 预期输出示例

### 成功的控制台输出

```
2025-11-25 10:30:00.123  INFO --- [main] HackathonAIEngineV2 : Starting batch review for all projects in: D:\test-review\RootFolder
2025-11-25 10:30:00.456  INFO --- [main] HackathonAIEngineV2 : Found 2 eligible folders (with done.txt) to process
2025-11-25 10:30:00.789  INFO --- [main] HackathonAIEngineV2 : Found 0 already completed reviews in CSV
2025-11-25 10:30:01.012  INFO --- [main] HackathonAIEngineV2 : Will process 2 new projects with 4 threads
2025-11-25 10:30:02.345  INFO --- [pool-1-thread-1] HackathonAIEngineV2 : Extracting project from folder Project001: mycode-v1.zip
2025-11-25 10:30:03.456  INFO --- [pool-1-thread-2] HackathonAIEngineV2 : Extracting project from folder Project002: code-new.zip
2025-11-25 10:30:05.789  INFO --- [pool-1-thread-1] HackathonAIEngineV2 : Reviewing project: Project001/mycode-v1.zip
2025-11-25 10:30:06.012  INFO --- [pool-1-thread-2] HackathonAIEngineV2 : Reviewing project: Project002/code-new.zip
2025-11-25 10:31:20.345  INFO --- [pool-1-thread-1] HackathonAIEngineV2 : Project Project001/mycode-v1.zip reviewed successfully with score: 85.5
2025-11-25 10:31:25.678  INFO --- [pool-1-thread-2] HackathonAIEngineV2 : Project Project002/code-new.zip reviewed successfully with score: 92.0
2025-11-25 10:31:25.890  INFO --- [main] HackathonAIEngineV2 : Batch review completed: 2 successful, 0 failed, 0 skipped in 85767 ms
2025-11-25 10:31:26.123  INFO --- [main] HackathonAIEngineV2 : Batch summary report written to: output\reports\batch-summary-20251125_103126.md
```

测试完成后，确认所有功能正常工作！

