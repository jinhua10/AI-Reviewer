# Logback 日志配置说明

## 配置概述

已配置 Logback 日志框架，在运行 `java -jar` 时自动生成日志文件。

## 日志文件说明

### 1. 日志文件位置

当你运行 jar 包时，会在**当前目录**下自动创建 `logs` 文件夹，包含以下日志文件：

```
logs/
├── app-debug.log              # 当天的 DEBUG 级别日志（包含详细信息）
├── app-info.log               # 当天的 INFO 级别日志（重要信息）
├── app-debug.20251125.log     # 按日期归档的 DEBUG 日志
└── app-info.20251125.log      # 按日期归档的 INFO 日志
```

### 2. 日志级别说明

#### app-debug.log
- **级别**: DEBUG 及以上
- **内容**: 
  - 应用程序的详细执行信息（`top.yumbo.ai` 包下的所有类）
  - AI 评审的详细过程
  - 文件解压、扫描等详细步骤
- **用途**: 开发调试、问题排查

#### app-info.log
- **级别**: INFO 及以上
- **内容**:
  - 应用程序的重要信息
  - 评审完成通知
  - CSV 记录总数
  - 错误和警告信息
- **用途**: 生产监控、关键信息查看

### 3. 日志格式

```
2025-11-25 12:05:30.123 [main] INFO  top.yumbo.ai.application.hackathon.core.HackathonAIEngineV2 - Starting continuous batch review...
└─ 时间戳            └─ 线程  └─ 级别 └─ 类名                                                    └─ 日志消息
```

## 使用示例

### Windows 环境

```powershell
# 进入 jar 包所在目录
cd D:\Jetbrains\hackathon\AI-Reviewer\application-demo\hackathonApplication\target

# 运行应用（日志会自动生成在当前目录的 logs 文件夹）
java -jar hackathonApplication.jar --reviewAll=D:\projects

# 查看实时日志
Get-Content logs\app-info.log -Wait -Tail 50
```

### Ubuntu/Linux 环境

```bash
# 进入 jar 包所在目录
cd /path/to/application-demo/hackathonApplication/target

# 运行应用
java -jar hackathonApplication.jar --reviewAll=/home/jinhua/projects

# 查看实时日志
tail -f logs/app-info.log

# 查看详细日志
tail -f logs/app-debug.log

# 后台运行（日志自动记录到文件）
nohup java -jar hackathonApplication.jar --reviewAll=/home/jinhua/projects &

# 查看日志
tail -f logs/app-info.log
```

## 日志配置详情

### logback.xml 配置内容

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration scan="true">
    <!-- 控制台输出 + 文件输出 -->
    
    <!-- DEBUG 日志 -->
    <appender name="FILE1">
        <file>logs/app-debug.log</file>
        <rollingPolicy>
            <fileNamePattern>logs/app-debug.%d{yyyyMMdd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>  <!-- 保留 30 天 -->
        </rollingPolicy>
    </appender>

    <!-- INFO 日志 -->
    <appender name="FILE2">
        <file>logs/app-info.log</file>
        <rollingPolicy>
            <fileNamePattern>logs/app-info.%d{yyyyMMdd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>  <!-- 保留 30 天 -->
        </rollingPolicy>
    </appender>

    <!-- 应用程序日志 (DEBUG 级别) -->
    <logger name="top.yumbo.ai" level="DEBUG">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE1"/>
        <appender-ref ref="FILE2"/>
    </logger>

    <!-- Spring 框架日志 (INFO 级别) -->
    <logger name="org.springframework" level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE2"/>
    </logger>
</configuration>
```

## 日志内容示例

### app-info.log 内容示例

```
2025-11-25 12:00:00.001 [main] INFO  top.yumbo.ai.application.hackathon.HackathonApplication - Starting HackathonApplication
2025-11-25 12:00:02.123 [main] INFO  top.yumbo.ai.application.hackathon.core.HackathonAIEngineV2 - Starting continuous batch review for all projects in: /home/jinhua/projects
2025-11-25 12:00:02.125 [main] INFO  top.yumbo.ai.application.hackathon.core.HackathonAIEngineV2 - Will execute download script and rescan every 2 minutes
2025-11-25 12:00:02.126 [main] INFO  top.yumbo.ai.application.hackathon.core.HackathonAIEngineV2 - Executing download script: /home/jinhua/AI-Reviewer/download
2025-11-25 12:00:07.456 [main] INFO  top.yumbo.ai.application.hackathon.core.HackathonAIEngineV2 - Download script completed successfully
2025-11-25 12:00:10.234 [main] INFO  top.yumbo.ai.application.hackathon.core.HackathonAIEngineV2 - Found 3 FolderB directories with done.txt marker
2025-11-25 12:05:30.567 [main] INFO  top.yumbo.ai.application.hackathon.core.HackathonAIEngineV2 - ✅ Review completed and recorded. CSV总记录数: 25
2025-11-25 12:05:30.568 [main] INFO  top.yumbo.ai.application.hackathon.core.HackathonAIEngineV2 - ========================================
2025-11-25 12:05:30.569 [main] INFO  top.yumbo.ai.application.hackathon.core.HackathonAIEngineV2 - CSV总记录数 (Total completed reviews): 25
2025-11-25 12:05:30.570 [main] INFO  top.yumbo.ai.application.hackathon.core.HackathonAIEngineV2 - ========================================
```

### app-debug.log 内容示例

```
2025-11-25 12:00:10.234 [main] DEBUG top.yumbo.ai.application.hackathon.core.HackathonAIEngineV2 - Scanning directory: /home/jinhua/projects/FolderA
2025-11-25 12:00:10.235 [main] DEBUG top.yumbo.ai.application.hackathon.core.HackathonAIEngineV2 - Checking FolderB: student001
2025-11-25 12:00:10.236 [main] DEBUG top.yumbo.ai.application.hackathon.core.HackathonAIEngineV2 - Found done.txt in student001
2025-11-25 12:00:10.237 [main] DEBUG top.yumbo.ai.application.hackathon.core.HackathonAIEngineV2 - Found ZIP file: project.zip
2025-11-25 12:00:10.238 [main] DEBUG top.yumbo.ai.application.hackathon.util.ZipUtil - Extracting ZIP: /home/jinhua/projects/FolderA/student001/project.zip
2025-11-25 12:00:12.345 [main] DEBUG top.yumbo.ai.application.hackathon.util.ScoreExtractor - Extracted score: 85.0
2025-11-25 12:00:12.346 [main] DEBUG top.yumbo.ai.application.hackathon.util.ScoreExtractor - Extracted overall comment: 152 chars
2025-11-25 12:00:12.347 [main] DEBUG top.yumbo.ai.application.hackathon.core.HackathonAIEngineV2 - Appended to CSV: student001,project.zip,85.0,...
```

## 日志管理

### 日志归档

- 每天自动生成新的日志文件
- 旧日志自动重命名为 `app-xxx.yyyyMMdd.log` 格式
- 保留最近 30 天的日志（可在 `logback.xml` 中修改 `<maxHistory>` 标签）

### 清理旧日志

```bash
# 手动清理 30 天前的日志
find logs/ -name "*.log" -mtime +30 -delete

# 或者只清理归档日志，保留当天的
find logs/ -name "*.20*.log" -mtime +30 -delete
```

### 日志文件大小管理

当前配置按天滚动，如果需要按大小滚动，可以修改 `logback.xml`：

```xml
<rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
    <fileNamePattern>logs/app-info.%d{yyyyMMdd}.%i.log</fileNamePattern>
    <maxFileSize>100MB</maxFileSize>
    <maxHistory>30</maxHistory>
    <totalSizeCap>3GB</totalSizeCap>
</rollingPolicy>
```

## 常用日志查看命令

### 查看最新日志
```bash
# 查看最后 100 行
tail -n 100 logs/app-info.log

# 实时查看（跟踪）
tail -f logs/app-info.log
```

### 搜索特定内容
```bash
# 搜索错误信息
grep "ERROR" logs/app-info.log

# 搜索特定项目
grep "student001" logs/app-debug.log

# 搜索 CSV 总记录数
grep "CSV总记录数" logs/app-info.log
```

### 查看特定时间段的日志
```bash
# 查看某天的日志
cat logs/app-info.20251125.log

# 查看特定时间段
sed -n '/12:00:00/,/13:00:00/p' logs/app-info.log
```

## 故障排查

### 问题1: 日志文件没有生成

**可能原因**:
- 没有权限创建 logs 目录
- logback.xml 配置文件丢失

**解决方法**:
```bash
# 检查 logback.xml 是否在 jar 包中
jar -tf hackathonApplication.jar | grep logback.xml

# 手动创建 logs 目录
mkdir logs
chmod 755 logs

# 指定日志配置文件位置（如果需要外部配置）
java -Dlogback.configurationFile=/path/to/logback.xml -jar hackathonApplication.jar
```

### 问题2: 日志输出不完整

**解决方法**:
- 确保应用正常退出（不要强制 kill -9）
- 检查磁盘空间是否充足
- 查看是否有权限问题

### 问题3: 日志文件过大

**解决方法**:
- 调整 `maxHistory` 减少保留天数
- 添加文件大小限制
- 定期清理旧日志

## 生产环境建议

### 1. 使用专门的日志目录
```bash
# 修改 logback.xml 中的路径
<file>/var/log/ai-reviewer/app-info.log</file>
```

### 2. 配合 logrotate（Linux）
```bash
# /etc/logrotate.d/ai-reviewer
/var/log/ai-reviewer/*.log {
    daily
    rotate 30
    compress
    delaycompress
    notifempty
    create 0644 appuser appuser
    sharedscripts
}
```

### 3. 集成到监控系统
```bash
# 使用 ELK、Splunk 等日志分析工具
# 或者简单的 grep + cron 监控
*/5 * * * * grep "ERROR" /path/to/logs/app-info.log | mail -s "AI Reviewer Errors" admin@example.com
```

## 总结

✅ **日志文件自动生成**: 运行 jar 包时自动在当前目录创建 logs 文件夹  
✅ **双重日志**: DEBUG 详细日志 + INFO 简要日志  
✅ **自动归档**: 每天自动滚动，保留 30 天  
✅ **控制台同步输出**: 方便实时查看  
✅ **生产就绪**: 已编译打包，可直接使用

现在运行 `java -jar hackathonApplication.jar` 时，所有日志会自动记录到文件中！

