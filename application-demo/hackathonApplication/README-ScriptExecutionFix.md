# 脚本执行错误修复

## 问题描述

在 Linux 系统上运行时，出现以下错误：

```
Failed to execute download script: Cannot run program "/home/jinhua/AI-Reviewer/download": 
error=0, Failed to exec spawn helper: pid: 930642, exit value: 1
```

## 问题原因

**根本原因**：直接使用 `ProcessBuilder(scriptPath)` 无法正确执行 shell 脚本。

在 Linux/Unix 系统上，shell 脚本需要通过 shell 解释器（如 `/bin/bash`）来执行，而不能像可执行文件那样直接调用。

原来的代码：
```java
ProcessBuilder processBuilder = new ProcessBuilder(scriptPath);
```

这种方式在 Linux 上会失败，因为它试图直接执行脚本文件，而不是通过 shell 来解释执行。

## 解决方案

修改 `executeDownloadScript()` 方法，根据操作系统类型选择正确的执行方式：

### 修改后的代码

```java
private boolean executeDownloadScript() {
    String scriptPath = properties.getBatch() != null && properties.getBatch().getDownloadScriptPath() != null
            ? properties.getBatch().getDownloadScriptPath()
            : "/home/jinhua/AI-Reviewer/download";

    log.info("Executing download script: {}", scriptPath);

    try {
        // Check if script file exists
        File scriptFile = new File(scriptPath);
        if (!scriptFile.exists()) {
            log.error("Download script not found: {}", scriptPath);
            return false;
        }

        // Use shell to execute the script on Unix-like systems
        ProcessBuilder processBuilder;
        String osName = System.getProperty("os.name").toLowerCase();
        
        if (osName.contains("win")) {
            // Windows
            processBuilder = new ProcessBuilder("cmd.exe", "/c", scriptPath);
        } else {
            // Unix-like systems (Linux, Mac)
            processBuilder = new ProcessBuilder("/bin/bash", scriptPath);
        }
        
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        // Capture and log output
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("Script output: {}", line);
            }
        }

        // Wait for script to complete
        int exitCode = process.waitFor();

        if (exitCode == 0) {
            log.info("Download script completed successfully");
            return true;
        } else {
            log.error("Download script failed with exit code: {}", exitCode);
            return false;
        }
    } catch (IOException e) {
        log.error("Failed to execute download script: {}", e.getMessage(), e);
        return false;
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.error("Download script execution interrupted", e);
        return false;
    }
}
```

## 关键改进

### 1. **操作系统检测**
```java
String osName = System.getProperty("os.name").toLowerCase();
```
自动检测操作系统类型，确保跨平台兼容。

### 2. **Shell 解释器执行（Linux/Mac）**
```java
processBuilder = new ProcessBuilder("/bin/bash", scriptPath);
```
在 Linux/Mac 上使用 `/bin/bash` 来执行脚本，这是正确的方式。

### 3. **Windows 兼容**
```java
processBuilder = new ProcessBuilder("cmd.exe", "/c", scriptPath);
```
在 Windows 上使用 `cmd.exe` 执行脚本。

### 4. **文件存在性检查**
```java
File scriptFile = new File(scriptPath);
if (!scriptFile.exists()) {
    log.error("Download script not found: {}", scriptPath);
    return false;
}
```
在执行前检查脚本文件是否存在，提供更清晰的错误信息。

### 5. **输出捕获和日志记录**
```java
try (java.io.BufferedReader reader = new java.io.BufferedReader(
        new java.io.InputStreamReader(process.getInputStream()))) {
    String line;
    while ((line = reader.readLine()) != null) {
        log.debug("Script output: {}", line);
    }
}
```
捕获脚本的输出并记录到日志中，便于调试。

## 验证方法

### 1. 确保脚本有执行权限
```bash
chmod +x /home/jinhua/AI-Reviewer/download
```

### 2. 测试脚本
```bash
/bin/bash /home/jinhua/AI-Reviewer/download
echo $?  # 应该输出 0
```

### 3. 重新运行应用
```bash
java -jar hackathonApplication.jar --reviewAll=/home/jinhua/projects
```

## 预期日志输出

修复后，你应该看到：

```
2025-11-25 14:00:00 - Executing download script: /home/jinhua/AI-Reviewer/download
2025-11-25 14:00:00 - Script output: Syncing files...
2025-11-25 14:00:05 - Script output: Transfer complete
2025-11-25 14:00:05 - Download script completed successfully
```

而不是之前的错误信息。

## 脚本示例

如果你的下载脚本还没有创建，这里是一个示例：

```bash
#!/bin/bash
# /home/jinhua/AI-Reviewer/download

# 同步文件示例
SOURCE_DIR="user@remote:/path/to/projects/"
DEST_DIR="/home/jinhua/hackathon2025-project-artifacts/"

echo "Starting file sync..."
rsync -avz "$SOURCE_DIR" "$DEST_DIR"

if [ $? -eq 0 ]; then
    echo "Sync completed successfully"
    exit 0
else
    echo "Sync failed"
    exit 1
fi
```

记得给脚本执行权限：
```bash
chmod +x /home/jinhua/AI-Reviewer/download
```

## 注意事项

1. **脚本路径**：确保配置文件中的路径正确
   ```yaml
   ai-reviewer:
     batch:
       download-script-path: /home/jinhua/AI-Reviewer/download
   ```

2. **脚本返回值**：脚本必须返回 0 表示成功，非 0 表示失败
   ```bash
   exit 0  # 成功
   exit 1  # 失败
   ```

3. **脚本输出**：脚本的标准输出和错误输出都会被捕获并记录到日志

4. **权限问题**：确保运行 Java 应用的用户有权限执行该脚本

## 故障排查

如果仍然有问题：

### 1. 检查脚本是否可执行
```bash
ls -l /home/jinhua/AI-Reviewer/download
# 应该显示 -rwxr-xr-x
```

### 2. 手动测试脚本
```bash
/bin/bash /home/jinhua/AI-Reviewer/download
```

### 3. 检查日志级别
确保日志级别设置为 DEBUG 以查看详细输出：
```yaml
logging:
  level:
    top.yumbo.ai: DEBUG
```

### 4. 检查文件路径
确保路径中没有空格或特殊字符，或者使用引号包裹。

## 总结

✅ **问题已修复**：使用 `/bin/bash` 执行脚本而不是直接调用  
✅ **跨平台支持**：自动检测 Windows/Linux/Mac  
✅ **更好的错误处理**：文件存在性检查和详细日志  
✅ **输出捕获**：脚本输出会记录到日志中  

现在脚本应该可以正常执行了！

