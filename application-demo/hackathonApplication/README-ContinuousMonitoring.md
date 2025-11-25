# Continuous Monitoring Feature for Batch Review

## 概述 (Overview)

新增了持续监控功能，在 `--reviewAll` 模式下，系统会每隔 2 分钟自动执行同步脚本并重新扫描待评分项目。

The continuous monitoring feature has been added. In `--reviewAll` mode, the system will automatically execute the sync script and rescan for projects to review every 2 minutes.

## 功能特性 (Features)

1. **定时执行同步脚本** - 每 2 分钟执行一次 `/home/jinhua/AI-Reviewer/download` 脚本
2. **自动重新扫描** - 脚本完成后，自动扫描是否有新增的 `done.txt` 文件
3. **增量评分** - 只处理之前未完成但现在有 `done.txt` 的项目
4. **CSV 记录追踪** - 每次评分后记录到 CSV 文件
5. **实时统计显示** - 每次打分后显示 CSV 中已完成的总记录数

1. **Scheduled Sync Script Execution** - Executes `/home/jinhua/AI-Reviewer/download` script every 2 minutes
2. **Automatic Rescanning** - After script completion, automatically scans for new `done.txt` files
3. **Incremental Review** - Only processes projects that previously didn't have `done.txt` but now do
4. **CSV Progress Tracking** - Records each review in CSV file
5. **Real-time Statistics** - Displays total completed count from CSV after each review

## 配置 (Configuration)

在 `application.yml` 或 `application.properties` 中配置：

```yaml
ai-reviewer:
  batch:
    download-script-path: /home/jinhua/AI-Reviewer/download  # 同步脚本路径
    scan-interval-minutes: 2  # 扫描间隔（分钟）
```

或者：

```properties
ai-reviewer.batch.download-script-path=/home/jinhua/AI-Reviewer/download
ai-reviewer.batch.scan-interval-minutes=2
```

## 使用方法 (Usage)

### Ubuntu 环境下运行 (Running on Ubuntu)

```bash
# 编译项目
mvn clean package

# 运行持续监控模式
java -jar target/hackathonApplication-1.0.jar --reviewAll=/path/to/projects
```

### 工作流程 (Workflow)

1. 系统启动后立即执行 `/home/jinhua/AI-Reviewer/download` 脚本
2. 脚本完成后，扫描项目目录结构：
   - FolderA (根目录)
     - FolderB (子文件夹，包含 done.txt)
       - ZipC (ZIP 文件)
3. 对找到的项目进行评分
4. 将评分结果记录到 CSV 文件
5. 打印 CSV 中的总记录数
6. 等待 2 分钟
7. 重复步骤 1-6

### 日志输出示例 (Log Output Example)

```
2025-11-25 10:00:00 INFO  - Executing download script: /home/jinhua/AI-Reviewer/download
2025-11-25 10:00:05 INFO  - Download script completed successfully
2025-11-25 10:00:07 INFO  - Scanning for projects with done.txt...
2025-11-25 10:00:08 INFO  - Found 3 new projects to review
2025-11-25 10:05:30 INFO  - ✅ Review completed and recorded. CSV总记录数: 25
2025-11-25 10:05:31 INFO  - ========================================
2025-11-25 10:05:31 INFO  - CSV总记录数 (Total completed reviews): 25
2025-11-25 10:05:31 INFO  - ========================================
2025-11-25 10:05:31 INFO  - Waiting 2 minutes before next scan...
```

## CSV 文件格式 (CSV File Format)

文件位置：`{output-path}/completed-reviews.csv`

格式：
```csv
FolderB,ZipFileName,Score,ReportFileName,CompletedTime
project01,submission.zip,85,project01-85-submission.md,2025-11-25T10:05:30
project02,code.zip,92,project02-92-code.md,2025-11-25T10:08:45
```

## 脚本要求 (Script Requirements)

下载脚本 `/home/jinhua/AI-Reviewer/download` 需要：

1. 具有可执行权限：`chmod +x /home/jinhua/AI-Reviewer/download`
2. 成功执行后返回退出码 0
3. 负责从远程服务器同步文件到本地项目目录

## 注意事项 (Notes)

1. 持续监控模式会一直运行，直到手动停止（Ctrl+C）
2. 已经评分过的项目不会重复评分（通过 CSV 记录跟踪）
3. 如果脚本执行失败，系统会记录错误但继续运行
4. 建议在生产环境中使用 systemd 或 supervisor 管理进程

## 停止服务 (Stopping the Service)

按 `Ctrl+C` 停止服务，或在使用进程管理器时：

```bash
# 使用 systemd
sudo systemctl stop ai-reviewer

# 或直接 kill 进程
kill <PID>
```

## 故障排查 (Troubleshooting)

### 脚本执行失败
- 检查脚本路径是否正确
- 检查脚本是否有执行权限
- 检查脚本的输出日志

### CSV 记录数不准确
- 检查 CSV 文件是否被手动修改
- 检查文件权限是否正确

### 内存或 CPU 占用过高
- 调整 `batch.thread-pool-size` 减少并发数
- 增加 `scan-interval-minutes` 延长扫描间隔

