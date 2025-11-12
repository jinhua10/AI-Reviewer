@echo off
chcp 65001 >nul 2>&1
echo ========================================
echo 黑客松项目评审工具
echo ========================================
echo.

REM 检查 JAR 文件是否存在
if not exist "target\hackathon-ai.jar" (
    echo ❌ 错误: JAR 文件不存在！
    echo.
    echo 请先运行构建脚本:
    echo   hackathon-ai_buildStart.bat
    echo.
    echo 或者手动构建:
    echo   mvn clean package -DskipTests -f hackathon-ai.xml
    echo.
    pause
    exit /b 1
)

REM 检查 Java 是否安装
java -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ❌ 错误: 未安装 Java 或 Java 不在 PATH 中！
    echo.
    echo 请先安装 Java 17 或更高版本
    echo.
    pause
    exit /b 1
)

REM 获取用户输入的模式
echo 请选择评审模式:
echo   1. 使用 Git 仓库 URL
echo   2. 使用本地目录
echo.
set /p MODE="请输入选项 [1 或 2]: "

if "%MODE%"=="1" (
    REM Git 仓库模式
    echo.
    echo 选择仓库类型:
    echo   1. GitHub
    echo   2. Gitee
    echo.
    set /p REPO_TYPE="请输入选项 [1 或 2]: "

    echo.
    set /p GIT_URL="请输入仓库 URL: "
    set /p TEAM_NAME="请输入团队名称: "
    set /p BRANCH="请输入分支名称 (默认: main): "

    if "%BRANCH%"=="" set BRANCH=main

    echo.
    echo ========================================
    echo 正在评审项目...
    echo ========================================
    echo 仓库: %GIT_URL%
    echo 分支: %BRANCH%
    echo 团队: %TEAM_NAME%
    echo.

    if "%REPO_TYPE%"=="2" (
        java -jar target\hackathon-ai.jar hackathon ^
          --gitee-url "%GIT_URL%" ^
          --team "%TEAM_NAME%" ^
          --branch "%BRANCH%" ^
          --output score.json ^
          --report report.md
    ) else (
        java -jar target\hackathon-ai.jar hackathon ^
          --github-url "%GIT_URL%" ^
          --team "%TEAM_NAME%" ^
          --branch "%BRANCH%" ^
          --output score.json ^
          --report report.md
    )
) else if "%MODE%"=="2" (
    REM 本地目录模式
    echo.
    set /p PROJECT_DIR="请输入项目目录路径: "
    set /p TEAM_NAME="请输入团队名称: "

    echo.
    echo ========================================
    echo 正在评审项目...
    echo ========================================
    echo 目录: %PROJECT_DIR%
    echo 团队: %TEAM_NAME%
    echo.

    java -jar target\hackathon-ai.jar hackathon ^
      --directory "%PROJECT_DIR%" ^
      --team "%TEAM_NAME%" ^
      --output score.json ^
      --report report.md
) else (
    echo.
    echo ❌ 无效的选项！
    pause
    exit /b 1
)

echo.
if %ERRORLEVEL% EQU 0 (
  echo ========================================
  echo ✅ 评审完成！
  echo ========================================
  echo.
  echo 📊 评分结果: score.json
  echo 📄 详细报告: report.md
  echo.
  echo 使用以下命令查看结果:
  echo   type score.json
  echo   notepad report.md
  echo.
) else (
  echo ========================================
  echo ❌ 评审失败！
  echo ========================================
  echo.
  echo 错误码: %ERRORLEVEL%
  echo.
  echo 常见问题排查:
  echo 1. 检查网络连接 (Git URL 模式)
  echo 2. 确认项目路径是否正确 (本地目录模式)
  echo 3. 检查 AI 服务配置 (src/main/resources/config.yaml)
  echo 4. 查看详细错误日志
  echo.
)

pause

