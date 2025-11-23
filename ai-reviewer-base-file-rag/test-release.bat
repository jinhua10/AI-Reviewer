@echo off
:: ========================================================================
:: 发布包测试脚本
:: Release Package Test Script
:: ========================================================================

chcp 65001 >nul 2>&1

echo ================================================================================
echo   知识库问答系统 - 发布包测试
echo ================================================================================
echo.

cd release

:: 检查文件
echo [检查1] 检查必需文件...
set MISSING=0

if not exist "*.jar" (
    echo [×] JAR文件不存在
    set MISSING=1
) else (
    echo [√] JAR文件存在
)

if not exist "start.bat" (
    echo [×] start.bat 不存在
    set MISSING=1
) else (
    echo [√] start.bat 存在
)

if not exist "config\application.yml" (
    echo [×] config\application.yml 不存在
    set MISSING=1
) else (
    echo [√] config\application.yml 存在
)

if not exist "data\documents" (
    echo [×] data\documents 目录不存在
    set MISSING=1
) else (
    echo [√] data\documents 目录存在
)

echo.

if %MISSING%==1 (
    echo [失败] 缺少必需文件，请先运行 build-and-deploy.bat
    cd ..
    pause
    exit /b 1
)

:: 检查文档
echo [检查2] 检查文档目录...
dir /b data\documents\*.* 2>nul | find /c /v "" > temp_count.txt
set /p DOC_COUNT=<temp_count.txt
del temp_count.txt

echo [信息] 找到 %DOC_COUNT% 个文件
echo.

if %DOC_COUNT%==0 (
    echo [提示] 文档目录为空
    echo        请将测试文档放到: release\data\documents\
    echo.
    set /p CONTINUE="是否继续测试? (Y/N): "
    if /i not "!CONTINUE!"=="Y" (
        cd ..
        exit /b 0
    )
)

:: 测试启动
echo [测试] 准备启动应用...
echo [提示] 启动后，请在新窗口中验证以下内容:
echo        1. 应用是否正常启动
echo        2. 知识库是否正确构建
echo        3. API是否可以访问
echo.
echo        验证完成后，按 Ctrl+C 停止应用
echo.
pause

:: 启动应用
start.bat

cd ..

