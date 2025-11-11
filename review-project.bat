@echo off
REM 黑客松AI评审工具 - 快速评审脚本 (Windows版)
REM 用于快速评审本地的源码项目

echo ========================================
echo 🏆 黑客松AI评审工具 - 快速评审
echo ========================================
echo.

REM 检查参数
if "%~1"=="" (
    echo ❌ 错误: 请提供项目路径
    echo.
    echo 使用方法:
    echo   review-project.bat "C:\path\to\your\project" [模式]
    echo.
    echo 评审模式:
    echo   QUICK    - 快速评审 (10秒)
    echo   DETAILED - 详细评审 (30秒)
    echo   EXPERT   - 专家评审 (60秒)
    echo.
    echo 示例:
    echo   review-project.bat "C:\MyProjects\AwesomeApp" QUICK
    echo.
    exit /b 1
)

set "PROJECT_PATH=%~1"
set "REVIEW_MODE=%~2"

if "%REVIEW_MODE%"=="" (
    set "REVIEW_MODE=QUICK"
)

REM 检查项目路径是否存在
if not exist "%PROJECT_PATH%" (
    echo ❌ 错误: 项目路径不存在: %PROJECT_PATH%
    exit /b 1
)

REM 检查Java环境
java -version >nul 2>&1
if errorlevel 1 (
    echo ❌ 错误: 未找到Java环境，请安装JDK 17+
    echo 下载地址: https://adoptium.net/
    exit /b 1
)

REM 检查API密钥
if "%DEEPSEEK_API_KEY%"=="" (
    echo ❌ 错误: 未设置DEEPSEEK_API_KEY环境变量
    echo 请运行: setx DEEPSEEK_API_KEY "your-api-key-here"
    echo 然后重新打开命令行窗口
    exit /b 1
)

echo ✅ 环境检查通过
echo 📂 项目路径: %PROJECT_PATH%
echo 📊 评审模式: %REVIEW_MODE%
echo 🔑 API密钥: 已设置
echo.

REM 编译项目 (如果需要)
if not exist "target\classes\top\yumbo\ai\reviewer\HackathonCLI.class" (
    echo 🔧 编译评审工具...
    mvn clean compile -q
    if errorlevel 1 (
        echo ❌ 编译失败
        exit /b 1
    )
    echo ✅ 编译完成
    echo.
)

REM 开始评审
echo 🚀 开始评审项目...
echo ⏳ 评审中，请稍候...
echo.

REM 运行评审
java -cp target/classes top.yumbo.ai.reviewer.HackathonCLI review "%PROJECT_PATH%" %REVIEW_MODE%

if errorlevel 1 (
    echo.
    echo ❌ 评审失败，请检查错误信息
    exit /b 1
)

echo.
echo ✅ 评审完成！

REM 检查是否生成了报告
if exist "hackathon-*-report.md" (
    echo 📄 评审报告已生成:
    dir /b hackathon-*-report.md 2>nul
    echo.
    echo 💡 提示: 打开上述文件查看详细评审报告
)

echo.
echo 🎉 评审完成！感谢使用黑客松AI评审工具。
echo.
echo 💡 更多功能:
echo   • 查看排行榜: java -cp target/classes top.yumbo.ai.reviewer.HackathonCLI leaderboard
echo   • 查看统计: java -cp target/classes top.yumbo.ai.reviewer.HackathonCLI stats
echo   • 运行演示: java -cp target/classes top.yumbo.ai.reviewer.HackathonDemo
echo.
echo 📚 更多信息请查看: QUICK-START-GUIDE.md</content>
<parameter name="filePath">D:\Jetbrains\hackathon\AI-Reviewer\review-project.bat
