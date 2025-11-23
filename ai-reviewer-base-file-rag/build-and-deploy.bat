@echo off
:: ========================================================================
:: 打包和部署脚本
:: Build and Deploy Script
:: ========================================================================

chcp 65001 >nul 2>&1
setlocal EnableDelayedExpansion

echo ================================================================================
echo   知识库问答系统 - 打包和部署
echo ================================================================================
echo.

:: 步骤1: 清理旧的构建
echo [步骤1] 清理旧的构建...
call mvn clean -q
if errorlevel 1 (
    echo [错误] 清理失败
    pause
    exit /b 1
)
echo [完成] 清理完成
echo.

:: 步骤2: 编译和打包
echo [步骤2] 编译和打包...
echo [信息] 正在编译项目（跳过测试）...
call mvn package -DskipTests -q
if errorlevel 1 (
    echo [错误] 打包失败
    echo.
    echo 请检查编译错误，然后重试
    pause
    exit /b 1
)
echo [完成] 打包成功
echo.

:: 步骤3: 查找生成的JAR文件
echo [步骤3] 查找JAR文件...
set JAR_FILE=
for %%f in (target\ai-reviewer-base-file-rag-*.jar) do (
    set JAR_FILE=%%f
    goto :found_jar
)

:found_jar
if "%JAR_FILE%"=="" (
    echo [错误] 未找到JAR文件
    pause
    exit /b 1
)

echo [完成] 找到JAR文件: %JAR_FILE%
echo.

:: 步骤4: 复制JAR到发布目录
echo [步骤4] 复制文件到发布目录...

:: 确保发布目录存在
if not exist "release" mkdir release
if not exist "release\config" mkdir release\config
if not exist "release\data" mkdir release\data
if not exist "release\data\documents" mkdir release\data\documents
if not exist "release\logs" mkdir release\logs

:: 复制JAR文件
copy /Y "%JAR_FILE%" "release\" >nul
if errorlevel 1 (
    echo [错误] 复制JAR文件失败
    pause
    exit /b 1
)

:: 获取JAR文件名
for %%f in ("%JAR_FILE%") do set JAR_NAME=%%~nxf

echo [完成] 已复制: %JAR_NAME%
echo.

:: 步骤5: 显示发布目录结构
echo [步骤5] 发布包已准备完成
echo.
echo ================================================================================
echo   发布包目录结构
echo ================================================================================
echo.
echo release\
echo ├── %JAR_NAME%
echo ├── start.bat           (启动脚本)
echo ├── stop.bat            (停止脚本)
echo ├── README.md           (使用说明)
echo ├── config\
echo │   └── application.yml (外置配置)
echo ├── data\
echo │   └── documents\       (文档目录)
echo └── logs\               (日志目录)
echo.

:: 步骤6: 显示文件大小
for %%f in ("release\%JAR_NAME%") do (
    set size=%%~zf
    set /a sizeMB=!size! / 1024 / 1024
    echo [信息] JAR文件大小: !sizeMB! MB
)
echo.

:: 步骤7: 提示下一步
echo ================================================================================
echo   打包完成！
echo ================================================================================
echo.
echo 下一步操作:
echo   1. 将要索引的文档放到: release\data\documents\
echo   2. 根据需要修改配置: release\config\application.yml
echo   3. 进入发布目录: cd release
echo   4. 启动应用: start.bat
echo.
echo 或者将整个 release 目录复制到目标服务器
echo.

:: 询问是否打开发布目录
set /p OPEN="是否打开发布目录? (Y/N): "
if /i "%OPEN%"=="Y" (
    explorer release
)

echo.
echo [完成] 打包和部署脚本执行完成
echo.
pause

endlocal

