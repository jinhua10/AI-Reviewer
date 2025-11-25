# 持续监控功能实现总结 (Continuous Monitoring Implementation Summary)

## 实现的功能 (Implemented Features)

### 1. 定时脚本执行 (Scheduled Script Execution)
- 在 `--reviewAll` 模式下，每隔 2 分钟执行一次 `/home/jinhua/AI-Reviewer/download` 脚本
- 脚本执行完成后等待文件系统同步（2秒）
- 如果脚本执行失败，记录错误但继续运行

### 2. 自动重新扫描 (Automatic Rescanning)
- 脚本完成后自动扫描项目目录
- 检查是否有新的 `done.txt` 文件出现
- 只处理之前未评分但现在有 `done.txt` 的项目（通过 CSV 记录避免重复）

### 3. CSV 记录追踪 (CSV Progress Tracking)
- 每次评分后自动记录到 `completed-reviews.csv`
- CSV 包含：FolderB名称、ZIP文件名、分数、报告文件名、完成时间
- 每次打分后立即打印 CSV 已记录的总数

### 4. 实时统计显示 (Real-time Statistics)
- 每次评分完成后显示：`✅ Review completed and recorded. CSV总记录数: X`
- 每轮扫描结束后显示统计信息

## 修改的文件 (Modified Files)

### 1. AIReviewerProperties.java
**位置**: `ai-reviewer-starter/src/main/java/top/yumbo/ai/starter/config/AIReviewerProperties.java`

**新增配置项**:
```java
private String downloadScriptPath = "/home/jinhua/AI-Reviewer/download";
private Integer scanIntervalMinutes = 2;
```

### 2. HackathonAIEngineV2.java
**位置**: `application-demo/hackathonApplication/src/main/java/top/yumbo/ai/application/hackathon/core/HackathonAIEngineV2.java`

**新增方法**:
- `executeDownloadScript()`: 执行下载脚本
- `getCompletedReviewsCount()`: 从 CSV 获取已完成记录数
- `reviewAllProjectsContinuous(String rootDirectory)`: 持续监控和评分的主循环

**修改**:
- 在 CSV 记录追加后添加总数打印功能

### 3. HackathonAutoConfiguration.java
**位置**: `application-demo/hackathonApplication/src/main/java/top/yumbo/ai/application/hackathon/config/HackathonAutoConfiguration.java`

**新增方法**:
- `runBatchReviewContinuous()`: 启动持续监控模式

**修改**:
- 将 `--reviewAll` 模式改为调用持续监控方法

### 4. application.yml
**位置**: `application-demo/hackathonApplication/src/main/resources/application.yml`

**新增配置**:
```yaml
ai-reviewer:
  batch:
    download-script-path: /home/jinhua/AI-Reviewer/download
    scan-interval-minutes: 2
```

## 工作流程 (Workflow)

```
启动应用
    ↓
执行 download 脚本
    ↓
等待 2 秒（文件系统同步）
    ↓
扫描目录结构（FolderA -> FolderB -> ZipC）
    ↓
检查 done.txt 和 CSV 记录
    ↓
处理新项目
    ↓
每个项目评分后：
  - 记录到 CSV
  - 打印 "CSV总记录数: X"
    ↓
所有项目处理完成
    ↓
打印统计信息
    ↓
等待 2 分钟
    ↓
重复上述流程
```

## 配置示例 (Configuration Example)

### Ubuntu 环境配置
```yaml
ai-reviewer:
  batch:
    download-script-path: /home/jinhua/AI-Reviewer/download
    scan-interval-minutes: 2
    thread-pool-size: 4
    temp-extract-dir: /tmp/ai-reviewer-extracted
  processor:
    output-path: /home/jinhua/AI-Reviewer/reports
```

### 启动命令
```bash
java -jar hackathonApplication-1.0.jar --reviewAll=/home/jinhua/projects
```

## 日志示例 (Log Example)

```
2025-11-25 10:00:00 - Starting continuous batch review for all projects in: /home/jinhua/projects
2025-11-25 10:00:00 - Will execute download script and rescan every 2 minutes
2025-11-25 10:00:00 - Executing download script: /home/jinhua/AI-Reviewer/download
2025-11-25 10:00:05 - Download script completed successfully
2025-11-25 10:00:07 - Starting batch code review for all projects in: /home/jinhua/projects
2025-11-25 10:00:08 - Found 3 FolderB directories with done.txt marker
2025-11-25 10:00:08 - Processing: student001
2025-11-25 10:05:30 - ✅ Review completed and recorded. CSV总记录数: 25
2025-11-25 10:05:31 - ========================================
2025-11-25 10:05:31 - CSV总记录数 (Total completed reviews): 25
2025-11-25 10:05:31 - ========================================
2025-11-25 10:05:31 - Waiting 2 minutes before next scan...
```

## 注意事项 (Notes)

1. **脚本权限**: 确保 `/home/jinhua/AI-Reviewer/download` 具有执行权限
   ```bash
   chmod +x /home/jinhua/AI-Reviewer/download
   ```

2. **长时间运行**: 应用会持续运行，建议使用进程管理器（systemd 或 supervisor）

3. **资源管理**: 可以通过调整 `thread-pool-size` 和 `scan-interval-minutes` 控制资源使用

4. **错误处理**: 即使脚本执行失败，应用也会继续运行并在下一个周期重试

5. **CSV 文件**: CSV 文件位于 `{output-path}/completed-reviews.csv`，不应手动修改

## 测试建议 (Testing Recommendations)

1. **单次测试**: 先确保单次 `reviewAllProjects()` 能正常工作
2. **脚本测试**: 单独测试 download 脚本是否能正常执行
3. **监控测试**: 运行持续监控模式，观察至少 2-3 个周期
4. **CSV 验证**: 检查 CSV 记录是否正确，总数统计是否准确
5. **错误恢复**: 测试脚本失败时的恢复能力

## 编译和部署 (Build and Deploy)

```bash
# 编译项目
cd /path/to/AI-Reviewer
mvn clean package -DskipTests

# 运行（前台）
java -jar application-demo/hackathonApplication/target/hackathonApplication-1.0.jar --reviewAll=/home/jinhua/projects

# 运行（后台）
nohup java -jar application-demo/hackathonApplication/target/hackathonApplication-1.0.jar --reviewAll=/home/jinhua/projects > ai-reviewer.log 2>&1 &

# 查看日志
tail -f ai-reviewer.log
```

## 停止服务 (Stop Service)

```bash
# 前台运行时
Ctrl+C

# 后台运行时
ps aux | grep hackathonApplication
kill <PID>

# 或者
pkill -f hackathonApplication
```

