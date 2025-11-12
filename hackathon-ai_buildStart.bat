@echo off
echo ========================================
echo 构建 Hackathon-AI 工具
echo ========================================
echo.

mvn clean package -DskipTests -f hackathon-ai.xml

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo ✅ 构建成功！
    echo ========================================
    echo.
    echo JAR 文件: target\hackathon-ai.jar
    echo.
    echo 使用以下命令运行:
    echo   java -jar target\hackathon-ai.jar hackathon --help
    echo.
) else (
    echo.
    echo ========================================
    echo ❌ 构建失败！
    echo ========================================
    echo.
    echo 错误码: %ERRORLEVEL%
    echo.
)

pause

