@echo off
chcp 65001 >nul
echo ========================================
echo Tesseract OCR 语言包下载工具
echo ========================================
echo.

:: 创建tessdata目录
if not exist tessdata mkdir tessdata
cd tessdata

echo [1/3] 下载中文简体语言包...
echo.
curl -L -o chi_sim.traineddata https://github.com/tesseract-ocr/tessdata/raw/main/chi_sim.traineddata
if %errorlevel% neq 0 (
    echo ❌ 下载失败，请检查网络连接
    pause
    exit /b 1
)
echo ✅ 中文简体语言包下载完成 (chi_sim.traineddata)
echo.

echo [2/3] 下载英文语言包...
echo.
curl -L -o eng.traineddata https://github.com/tesseract-ocr/tessdata/raw/main/eng.traineddata
if %errorlevel% neq 0 (
    echo ❌ 下载失败，请检查网络连接
    pause
    exit /b 1
)
echo ✅ 英文语言包下载完成 (eng.traineddata)
echo.

echo [3/3] 验证文件...
if exist chi_sim.traineddata (
    echo ✅ chi_sim.traineddata - 已下载
) else (
    echo ❌ chi_sim.traineddata - 缺失
)

if exist eng.traineddata (
    echo ✅ eng.traineddata - 已下载
) else (
    echo ❌ eng.traineddata - 缺失
)

cd ..

echo.
echo ========================================
echo 语言包下载完成！
echo ========================================
echo.
echo 语言包位置: %cd%\tessdata
echo.
echo 下一步：编辑 start.bat，添加以下配置：
echo.
echo set ENABLE_OCR=true
echo set TESSDATA_PREFIX=%cd%\tessdata
echo set OCR_LANGUAGE=chi_sim+eng
echo.
echo 然后重启应用即可启用OCR功能。
echo.
echo 详细说明请参考: OCR配置指南.md
echo.
pause

