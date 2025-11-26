@echo off
echo ================================================
echo    修复参数绑定错误 - 重新编译并启动
echo ================================================
echo.

echo [1/3] 清理并重新编译...
cd /d D:\Jetbrains\hackathon\AI-Reviewer\application-demo\hackathonApplication
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo 编译失败！
    pause
    exit /b 1
)

echo.
echo [2/3] 编译成功！
echo.

echo [3/3] 启动应用...
echo.
echo 使用以下命令启动（根据实际路径修改）：
echo.
echo   选项1 - 测试模式（使用本地account.csv）：
echo   java -jar target\hackathonApplication.jar --reviewAll=D:\test-hackathon
echo.
echo   选项2 - 生产模式（使用实际路径）：
echo   java -jar target\hackathonApplication.jar --reviewAll=/home/jinhua/hackathon2025-project-artifacts
echo.
echo 然后访问: http://localhost:8080
echo 测试账户: demo123, test456, pass789
echo.
pause

