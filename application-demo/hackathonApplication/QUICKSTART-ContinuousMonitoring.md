# Quick Start Guide - 持续监控模式

## 快速开始 (Quick Start)

### 1. 准备工作

#### 1.1 确保同步脚本存在并可执行
```bash
# 创建脚本（如果还没有）
sudo mkdir -p /home/jinhua/AI-Reviewer
sudo touch /home/jinhua/AI-Reviewer/download

# 添加脚本内容（示例）
sudo nano /home/jinhua/AI-Reviewer/download
```

示例脚本内容：
```bash
#!/bin/bash
# 从远程服务器同步文件
# 例如使用 rsync
rsync -avz user@remote:/path/to/projects/ /home/jinhua/projects/
exit 0
```

添加执行权限：
```bash
sudo chmod +x /home/jinhua/AI-Reviewer/download
```

#### 1.2 测试脚本
```bash
/home/jinhua/AI-Reviewer/download
echo $?  # 应该输出 0 表示成功
```

### 2. 编译项目

```bash
cd /path/to/AI-Reviewer
mvn clean package -DskipTests
```

### 3. 配置文件

编辑 `application.yml`:
```yaml
ai-reviewer:
  batch:
    download-script-path: /home/jinhua/AI-Reviewer/download
    scan-interval-minutes: 2  # 每 2 分钟扫描一次
    thread-pool-size: 4
    temp-extract-dir: /tmp/ai-reviewer-extracted
  processor:
    output-path: /home/jinhua/AI-Reviewer/reports
  ai:
    provider: bedrock
    region: us-west-2
    model: "us.writer.palmyra-x5-v1:0"
```

### 4. 运行应用

#### 4.1 前台运行（测试用）
```bash
cd application-demo/hackathonApplication
java -jar target/hackathonApplication-1.0.jar --reviewAll=/home/jinhua/projects
```

#### 4.2 后台运行（生产环境）
```bash
nohup java -jar target/hackathonApplication-1.0.jar --reviewAll=/home/jinhua/projects > ai-reviewer.log 2>&1 &
```

#### 4.3 查看日志
```bash
tail -f ai-reviewer.log
```

### 5. 验证运行

#### 5.1 检查进程
```bash
ps aux | grep hackathonApplication
```

#### 5.2 查看日志输出
```bash
tail -f ai-reviewer.log | grep "CSV总记录数"
```

应该看到类似输出：
```
2025-11-25 10:05:30 - ✅ Review completed and recorded. CSV总记录数: 25
2025-11-25 10:05:31 - ========================================
2025-11-25 10:05:31 - CSV总记录数 (Total completed reviews): 25
2025-11-25 10:05:31 - ========================================
```

#### 5.3 检查 CSV 文件
```bash
cat /home/jinhua/AI-Reviewer/reports/completed-reviews.csv
```

### 6. 停止应用

#### 6.1 前台运行时
按 `Ctrl+C`

#### 6.2 后台运行时
```bash
# 方法1：通过进程ID
ps aux | grep hackathonApplication
kill <PID>

# 方法2：通过名称
pkill -f hackathonApplication
```

## 目录结构要求

项目必须遵循以下结构：
```
/home/jinhua/projects/          <- --reviewAll 指定的根目录
├── FolderA/                    <- 顶层目录
│   ├── student001/             <- FolderB（学生项目文件夹）
│   │   ├── done.txt           <- 必须存在这个文件才会处理
│   │   ├── project.zip        <- ZIP 文件
│   │   └── submission.zip     <- 可以有多个 ZIP
│   ├── student002/
│   │   ├── done.txt
│   │   └── code.zip
│   └── student003/             <- 没有 done.txt，不会处理
│       └── work.zip
```

## 工作流程

```
时间 T+0
  ↓
执行 /home/jinhua/AI-Reviewer/download
  ↓
等待 2 秒
  ↓
扫描 /home/jinhua/projects
  ↓
找到 student001/done.txt 和 student002/done.txt
  ↓
检查 CSV，student001 未处理，student002 已处理
  ↓
处理 student001
  - 解压 project.zip
  - AI 评分
  - 生成报告：student001-85-project.md
  - 记录到 CSV
  - 打印：CSV总记录数: 26
  ↓
所有项目处理完成
  ↓
打印统计信息
  ↓
等待 2 分钟
  ↓
时间 T+2分钟，重复上述流程
```

## 常见问题

### Q1: 脚本执行失败怎么办？
A: 检查日志中的错误信息：
```bash
grep "Failed to execute download script" ai-reviewer.log
```
确保脚本有执行权限和正确的路径。

### Q2: 为什么某些项目没有被处理？
A: 可能原因：
1. 没有 `done.txt` 文件
2. 已经在 CSV 中记录过了
3. ZIP 文件损坏

检查：
```bash
# 查看哪些项目有 done.txt
find /home/jinhua/projects -name "done.txt"

# 查看 CSV 记录
cat /home/jinhua/AI-Reviewer/reports/completed-reviews.csv
```

### Q3: 如何修改扫描间隔？
A: 编辑 `application.yml`:
```yaml
ai-reviewer:
  batch:
    scan-interval-minutes: 5  # 改为 5 分钟
```

### Q4: 如何查看当前已处理的总数？
A: 
```bash
# 方法1：查看日志
tail -f ai-reviewer.log | grep "CSV总记录数"

# 方法2：直接统计 CSV
wc -l /home/jinhua/AI-Reviewer/reports/completed-reviews.csv
# 结果减 1（去掉表头）
```

### Q5: 内存占用太高怎么办？
A: 调整配置：
```yaml
ai-reviewer:
  batch:
    thread-pool-size: 2  # 减少并发数
  executor:
    thread-pool-size: 5  # 减少处理线程
```

## 性能优化建议

1. **并发数调整**：根据服务器性能调整 `thread-pool-size`
2. **扫描间隔**：如果文件更新不频繁，可以增加 `scan-interval-minutes`
3. **临时目录**：使用 SSD 存储 `temp-extract-dir`
4. **日志管理**：定期清理或轮转日志文件

## systemd 服务配置（推荐）

创建 `/etc/systemd/system/ai-reviewer.service`:
```ini
[Unit]
Description=AI Reviewer Continuous Service
After=network.target

[Service]
Type=simple
User=jinhua
WorkingDirectory=/home/jinhua/AI-Reviewer/application-demo/hackathonApplication
ExecStart=/usr/bin/java -jar target/hackathonApplication-1.0.jar --reviewAll=/home/jinhua/projects
Restart=on-failure
RestartSec=10
StandardOutput=append:/var/log/ai-reviewer/output.log
StandardError=append:/var/log/ai-reviewer/error.log

[Install]
WantedBy=multi-user.target
```

启用服务：
```bash
sudo systemctl daemon-reload
sudo systemctl enable ai-reviewer
sudo systemctl start ai-reviewer
sudo systemctl status ai-reviewer
```

查看日志：
```bash
sudo journalctl -u ai-reviewer -f
```

## 监控建议

### 1. 检查服务状态
```bash
systemctl status ai-reviewer
```

### 2. 监控 CSV 增长
```bash
watch -n 60 'wc -l /home/jinhua/AI-Reviewer/reports/completed-reviews.csv'
```

### 3. 监控资源使用
```bash
top -p $(pgrep -f hackathonApplication)
```

### 4. 查看最近的评分记录
```bash
tail -20 /home/jinhua/AI-Reviewer/reports/completed-reviews.csv
```

