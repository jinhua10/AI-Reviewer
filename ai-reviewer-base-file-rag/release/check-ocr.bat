@echo off
:: ========================================================================
:: OCR配置检查脚本
:: 用于诊断OCR功能是否正确配置
:: ========================================================================

chcp 65001 >nul 2>&1
setlocal EnableDelayedExpansion

echo ================================================================================
echo   OCR 配置检查工具
echo ================================================================================
echo.

:: 检查1: tessdata目录
echo [检查1] 检查tessdata目录...
if exist "tessdata" (
    echo ✅ tessdata目录存在

    if exist "tessdata\chi_sim.traineddata" (
        echo ✅ 中文语言包存在
    ) else (
        echo ❌ 中文语言包缺失: tessdata\chi_sim.traineddata
        echo 💡 运行 download-tessdata.bat 下载
    )

    if exist "tessdata\eng.traineddata" (
        echo ✅ 英文语言包存在
    ) else (
        echo ❌ 英文语言包缺失: tessdata\eng.traineddata
        echo 💡 运行 download-tessdata.bat 下载
    )
) else (
    echo ❌ tessdata目录不存在
    echo 💡 运行 download-tessdata.bat 创建并下载语言包
)
echo.

:: 检查2: start.bat中的OCR配置
echo [检查2] 检查start.bat中的OCR配置...
findstr /C:"set ENABLE_OCR=true" start.bat >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ ENABLE_OCR=true 已设置
) else (
    findstr /C:":: set ENABLE_OCR=true" start.bat >nul 2>&1
    if %errorlevel% equ 0 (
        echo ❌ ENABLE_OCR被注释了
        echo 💡 编辑start.bat，删除 ":: set ENABLE_OCR=true" 前面的 ::
    ) else (
        echo ⚠️  未找到ENABLE_OCR配置
        echo 💡 确认start.bat是否是最新版本
    )
)

findstr /C:"set TESSDATA_PREFIX=" start.bat >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ TESSDATA_PREFIX 已设置
) else (
    echo ❌ TESSDATA_PREFIX 未设置或被注释
)

findstr /C:"set OCR_LANGUAGE=" start.bat >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ OCR_LANGUAGE 已设置
) else (
    echo ❌ OCR_LANGUAGE 未设置或被注释
)
echo.

:: 检查3: JAR文件
echo [检查3] 检查JAR文件...
if exist "ai-reviewer-base-file-rag-1.0.jar" (
    echo ✅ JAR文件存在

    :: 获取JAR文件的修改时间
    for %%F in (ai-reviewer-base-file-rag-1.0.jar) do (
        echo    文件大小: %%~zF 字节
        echo    修改时间: %%~tF
    )
) else (
    echo ❌ JAR文件不存在
    echo 💡 运行 mvn clean package 重新构建
)
echo.

:: 检查4: 日志文件
echo [检查4] 检查日志文件...
if exist "logs\ai-reviewer-rag.log" (
    echo ✅ 日志文件存在

    :: 检查日志中是否有OCR相关信息
    findstr /C:"Tesseract OCR" logs\ai-reviewer-rag.log >nul 2>&1
    if %errorlevel% equ 0 (
        echo ✅ 日志中发现Tesseract OCR信息
        echo.
        echo 最近的OCR相关日志:
        echo ----------------------------------------
        findstr /C:"TikaDocumentParser" /C:"OCR" /C:"Tesseract" logs\ai-reviewer-rag.log | findstr /V "DEBUG" | more +0
    ) else (
        echo ⚠️  日志中未发现Tesseract OCR信息
        echo 💡 可能需要重启应用以应用OCR配置
    )
) else (
    echo ⚠️  日志文件不存在（应用可能未启动）
)
echo.

:: 检查5: 环境变量
echo [检查5] 检查当前环境变量...
if defined ENABLE_OCR (
    echo ✅ ENABLE_OCR = %ENABLE_OCR%
) else (
    echo ⚠️  ENABLE_OCR 环境变量未设置（仅在start.bat中设置）
)

if defined TESSDATA_PREFIX (
    echo ✅ TESSDATA_PREFIX = %TESSDATA_PREFIX%
) else (
    echo ⚠️  TESSDATA_PREFIX 环境变量未设置（仅在start.bat中设置）
)

if defined OCR_LANGUAGE (
    echo ✅ OCR_LANGUAGE = %OCR_LANGUAGE%
) else (
    echo ⚠️  OCR_LANGUAGE 环境变量未设置（仅在start.bat中设置）
)
echo.

:: 总结
echo ================================================================================
echo   检查完成
echo ================================================================================
echo.
echo 💡 建议操作:
echo.
echo 1. 如果tessdata目录或语言包缺失，运行: download-tessdata.bat
echo 2. 如果start.bat中OCR配置被注释，编辑文件取消注释
echo 3. 确保应用完全重启（关闭后重新运行start.bat）
echo 4. 重建知识库索引以应用OCR
echo.

pause

