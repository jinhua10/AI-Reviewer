# Excel知识库问答系统启动脚本
# 确保中文日志正常显示

# 设置控制台编码为 UTF-8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null

Write-Host "===========================================================" -ForegroundColor Green
Write-Host "Excel 知识库智能问答系统" -ForegroundColor Green
Write-Host "===========================================================" -ForegroundColor Green
Write-Host ""

# 设置 Java 环境变量
$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8"
$env:MAVEN_OPTS = "-Dfile.encoding=UTF-8"

Write-Host "✓ 编码设置: UTF-8" -ForegroundColor Cyan
Write-Host "✓ 控制台编码: UTF-8 (65001)" -ForegroundColor Cyan
Write-Host ""

# 运行 Maven 命令
Write-Host "启动系统..." -ForegroundColor Yellow
Write-Host ""

mvn clean compile exec:java `
    -Dexec.mainClass=top.yumbo.ai.rag.example.ExcelKnowledgeQASystem `
    -Dfile.encoding=UTF-8 `
    -Dconsole.encoding=UTF-8

Write-Host ""
Write-Host "===========================================================" -ForegroundColor Green
Write-Host "系统已退出" -ForegroundColor Green
Write-Host "===========================================================" -ForegroundColor Green

