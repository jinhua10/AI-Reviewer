@echo off
REM 黑客松评审工具构建脚本（Windows 版本）
REM 使用此脚本快速构建 hackathon-reviewer.jar

echo ==========================================
echo   黑客松评审工具 - 构建脚本
echo ==========================================
echo.

REM 检查 Maven 是否安装
where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo [91m错误: Maven 未安装或不在 PATH 中[0m
    echo 请先安装 Maven: https://maven.apache.org/download.cgi
    pause
    exit /b 1
)

echo [92m Maven 版本:[0m
mvn -version
echo.

REM 选择构建模式
echo 选择构建模式:
echo   1^) 快速构建（默认，跳过测试）
echo   2^) 完整构建（包含测试）
echo   3^) 生产构建（包含源码和文档）
echo.
set /p choice="请选择 [1-3, 默认: 1]: "
if "%choice%"=="" set choice=1

echo.
echo 开始构建...
echo.

if "%choice%"=="1" (
    echo [96m快速构建模式...[0m
    mvn clean package -f hackathon-pom.xml -Pquick
) else if "%choice%"=="2" (
    echo [96m完整构建模式...[0m
    mvn clean package -f hackathon-pom.xml
) else if "%choice%"=="3" (
    echo [96m生产构建模式...[0m
    mvn clean package -f hackathon-pom.xml -Pproduction
) else (
    echo [91m无效选择，使用默认快速构建[0m
    mvn clean package -f hackathon-pom.xml -Pquick
)

REM 检查构建结果
if %errorlevel% equ 0 (
    echo.
    echo ==========================================
    echo   [92m构建成功！[0m
    echo ==========================================
    echo.
    echo [96m输出文件:[0m
    echo   - target\hackathon-reviewer.jar
    echo.
    echo [96m文件大小:[0m
    dir target\hackathon-reviewer.jar | findstr "hackathon-reviewer.jar"
    echo.
    echo [96m使用方法:[0m
    echo   java -jar target\hackathon-reviewer.jar --help
    echo.
    echo [96m示例命令:[0m
    echo   # 评审本地项目
    echo   java -jar target\hackathon-reviewer.jar ^
    echo     -d C:\path\to\project ^
    echo     -t "Team Name" ^
    echo     -o score.json
    echo.
    echo   # 评审 GitHub 项目
    echo   java -jar target\hackathon-reviewer.jar ^
    echo     --github-url https://github.com/user/repo ^
    echo     -t "Team Name" ^
    echo     -o score.json
    echo.
    echo   # 评审 ZIP 文件
    echo   java -jar target\hackathon-reviewer.jar ^
    echo     -z project.zip ^
    echo     -t "Team Name" ^
    echo     -o score.json
    echo.
    echo   # 评审 S3 项目
    echo   java -jar target\hackathon-reviewer.jar ^
    echo     -s projects/team-name/ ^
    echo     -t "Team Name" ^
    echo     -o score.json
    echo.
) else (
    echo.
    echo ==========================================
    echo   [91m构建失败[0m
    echo ==========================================
    echo.
    echo 请检查错误信息并修复后重试
    pause
    exit /b 1
)

pause

