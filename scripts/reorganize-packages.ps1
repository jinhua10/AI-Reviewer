# AI-Reviewer 包重组脚本
# 生成时间: 2025-11-15
# 说明: 自动移动类文件并更新import语句

$baseDir = "D:\Jetbrains\hackathon\AI-Reviewer\src\main\java\top\yumbo\ai\reviewer"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "AI-Reviewer 包结构重组脚本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 定义移动映射
$moveMap = @{
    # S3存储相关
    "adapter\output\storage\S3StorageAdapter.java" = "adapter\storage\s3\S3StorageAdapter.java"
    "adapter\output\storage\S3StorageConfig.java" = "adapter\storage\s3\S3StorageConfig.java"
    "adapter\output\storage\S3StorageExample.java" = "adapter\storage\s3\S3StorageExample.java"

    # 本地文件系统
    "adapter\output\filesystem\LocalFileSystemAdapter.java" = "adapter\storage\local\LocalFileSystemAdapter.java"

    # 缓存
    "adapter\output\cache\FileCacheAdapter.java" = "adapter\storage\cache\FileCacheAdapter.java"

    # 压缩归档
    "adapter\output\archive\ZipArchiveAdapter.java" = "adapter\storage\archive\ZipArchiveAdapter.java"

    # AI服务 - Bedrock
    "adapter\output\ai\BedrockAdapter.java" = "adapter\ai\bedrock\BedrockAdapter.java"

    # AI服务 - 配置
    "adapter\output\ai\AIServiceConfig.java" = "adapter\ai\config\AIServiceConfig.java"

    # AI服务 - HTTP客户端
    "adapter\output\ai\HttpBasedAIAdapter.java" = "adapter\ai\http\HttpBasedAIAdapter.java"

    # AI服务 - 装饰器
    "adapter\output\ai\LoggingAIServiceDecorator.java" = "adapter\ai\decorator\LoggingAIServiceDecorator.java"

    # AI服务 - 工厂(保持在adapter\ai)
    "adapter\output\ai\AIAdapterFactory.java" = "adapter\ai\AIAdapterFactory.java"

    # AST解析器 - Java
    "adapter\output\ast\parser\JavaParserAdapter.java" = "adapter\parser\code\java\JavaParserAdapter.java"

    # AST解析器 - Python
    "adapter\output\ast\parser\PythonParserAdapter.java" = "adapter\parser\code\python\PythonParserAdapter.java"

    # AST解析器 - JavaScript
    "adapter\output\ast\parser\JavaScriptParserAdapter.java" = "adapter\parser\code\javascript\JavaScriptParserAdapter.java"

    # AST解析器 - Go
    "adapter\output\ast\parser\GoParserAdapter.java" = "adapter\parser\code\go\GoParserAdapter.java"

    # AST解析器 - C++
    "adapter\output\ast\parser\CppParserAdapter.java" = "adapter\parser\code\cpp\CppParserAdapter.java"

    # AST解析器 - 基类和工厂
    "adapter\output\ast\parser\AbstractASTParser.java" = "adapter\parser\code\AbstractASTParser.java"
    "adapter\output\ast\parser\ASTParserFactory.java" = "adapter\parser\code\ASTParserFactory.java"

    # 语言检测器
    "adapter\output\filesystem\detector\LanguageDetector.java" = "adapter\parser\detector\LanguageDetector.java"
    "adapter\output\filesystem\detector\LanguageDetectorRegistry.java" = "adapter\parser\detector\LanguageDetectorRegistry.java"
    "adapter\output\filesystem\detector\LanguageFeatures.java" = "adapter\parser\detector\LanguageFeatures.java"
    "adapter\output\filesystem\detector\GoLanguageDetector.java" = "adapter\parser\detector\language\GoLanguageDetector.java"
    "adapter\output\filesystem\detector\CppLanguageDetector.java" = "adapter\parser\detector\language\CppLanguageDetector.java"
    "adapter\output\filesystem\detector\RustLanguageDetector.java" = "adapter\parser\detector\language\RustLanguageDetector.java"

    # Git仓库
    "adapter\output\repository\GitRepositoryAdapter.java" = "adapter\repository\git\GitRepositoryAdapter.java"
}

# 包名映射 (用于更新package语句)
$packageMap = @{
    "top.yumbo.ai.reviewer.adapter.output.storage" = "top.yumbo.ai.reviewer.adapter.storage.s3"
    "top.yumbo.ai.reviewer.adapter.output.filesystem" = "top.yumbo.ai.reviewer.adapter.storage.local"
    "top.yumbo.ai.reviewer.adapter.output.cache" = "top.yumbo.ai.reviewer.adapter.storage.cache"
    "top.yumbo.ai.reviewer.adapter.output.archive" = "top.yumbo.ai.reviewer.adapter.storage.archive"
    "top.yumbo.ai.reviewer.adapter.output.ai" = "top.yumbo.ai.reviewer.adapter.ai"
    "top.yumbo.ai.reviewer.adapter.output.ast.parser" = "top.yumbo.ai.reviewer.adapter.parser.code"
    "top.yumbo.ai.reviewer.adapter.output.filesystem.detector" = "top.yumbo.ai.reviewer.adapter.parser.detector"
    "top.yumbo.ai.reviewer.adapter.output.repository" = "top.yumbo.ai.reviewer.adapter.repository.git"
}

# 步骤1: 移动文件
Write-Host "步骤1: 移动文件..." -ForegroundColor Yellow
$movedCount = 0
foreach ($entry in $moveMap.GetEnumerator()) {
    $sourcePath = Join-Path $baseDir $entry.Key
    $targetPath = Join-Path $baseDir $entry.Value

    if (Test-Path $sourcePath) {
        $targetDir = Split-Path $targetPath -Parent
        if (-not (Test-Path $targetDir)) {
            New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
        }

        Write-Host "  移动: $($entry.Key)" -ForegroundColor Gray
        Write-Host "    -> $($entry.Value)" -ForegroundColor Green

        Move-Item -Path $sourcePath -Destination $targetPath -Force
        $movedCount++
    } else {
        Write-Host "  [跳过] 文件不存在: $($entry.Key)" -ForegroundColor DarkGray
    }
}
Write-Host "已移动 $movedCount 个文件" -ForegroundColor Green
Write-Host ""

# 步骤2: 更新package语句
Write-Host "步骤2: 更新package语句..." -ForegroundColor Yellow
$updatedPackageCount = 0
foreach ($entry in $moveMap.GetEnumerator()) {
    $targetPath = Join-Path $baseDir $entry.Value

    if (Test-Path $targetPath) {
        $content = Get-Content $targetPath -Raw -Encoding UTF8
        $originalContent = $content

        # 根据目标路径确定新的package
        $newPackage = ""
        if ($entry.Value -match "adapter\\storage\\s3") {
            $newPackage = "top.yumbo.ai.reviewer.adapter.storage.s3"
        } elseif ($entry.Value -match "adapter\\storage\\local") {
            $newPackage = "top.yumbo.ai.reviewer.adapter.storage.local"
        } elseif ($entry.Value -match "adapter\\storage\\cache") {
            $newPackage = "top.yumbo.ai.reviewer.adapter.storage.cache"
        } elseif ($entry.Value -match "adapter\\storage\\archive") {
            $newPackage = "top.yumbo.ai.reviewer.adapter.storage.archive"
        } elseif ($entry.Value -match "adapter\\ai\\bedrock") {
            $newPackage = "top.yumbo.ai.reviewer.adapter.ai.bedrock"
        } elseif ($entry.Value -match "adapter\\ai\\config") {
            $newPackage = "top.yumbo.ai.reviewer.adapter.ai.config"
        } elseif ($entry.Value -match "adapter\\ai\\http") {
            $newPackage = "top.yumbo.ai.reviewer.adapter.ai.http"
        } elseif ($entry.Value -match "adapter\\ai\\decorator") {
            $newPackage = "top.yumbo.ai.reviewer.adapter.ai.decorator"
        } elseif ($entry.Value -match "adapter\\ai\\AIAdapterFactory") {
            $newPackage = "top.yumbo.ai.reviewer.adapter.ai"
        } elseif ($entry.Value -match "adapter\\parser\\code\\java") {
            $newPackage = "top.yumbo.ai.reviewer.adapter.parser.code.java"
        } elseif ($entry.Value -match "adapter\\parser\\code\\python") {
            $newPackage = "top.yumbo.ai.reviewer.adapter.parser.code.python"
        } elseif ($entry.Value -match "adapter\\parser\\code\\javascript") {
            $newPackage = "top.yumbo.ai.reviewer.adapter.parser.code.javascript"
        } elseif ($entry.Value -match "adapter\\parser\\code\\go") {
            $newPackage = "top.yumbo.ai.reviewer.adapter.parser.code.go"
        } elseif ($entry.Value -match "adapter\\parser\\code\\cpp") {
            $newPackage = "top.yumbo.ai.reviewer.adapter.parser.code.cpp"
        } elseif ($entry.Value -match "adapter\\parser\\code\\Abstract|ASTParserFactory") {
            $newPackage = "top.yumbo.ai.reviewer.adapter.parser.code"
        } elseif ($entry.Value -match "adapter\\parser\\detector\\language") {
            $newPackage = "top.yumbo.ai.reviewer.adapter.parser.detector.language"
        } elseif ($entry.Value -match "adapter\\parser\\detector") {
            $newPackage = "top.yumbo.ai.reviewer.adapter.parser.detector"
        } elseif ($entry.Value -match "adapter\\repository\\git") {
            $newPackage = "top.yumbo.ai.reviewer.adapter.repository.git"
        }

        if ($newPackage) {
            # 更新package语句
            $content = $content -replace "^package\s+[\w\.]+;", "package $newPackage;"

            if ($content -ne $originalContent) {
                Set-Content -Path $targetPath -Value $content -Encoding UTF8 -NoNewline
                Write-Host "  更新package: $($entry.Value) -> $newPackage" -ForegroundColor Cyan
                $updatedPackageCount++
            }
        }
    }
}
Write-Host "已更新 $updatedPackageCount 个package语句" -ForegroundColor Green
Write-Host ""

# 步骤3: 扫描并更新所有Java文件的import语句
Write-Host "步骤3: 更新import语句..." -ForegroundColor Yellow
$allJavaFiles = Get-ChildItem -Path "$baseDir\..\..\..\..\..\.." -Recurse -Filter "*.java" -File
$updatedImportCount = 0

foreach ($file in $allJavaFiles) {
    $content = Get-Content $file.FullName -Raw -Encoding UTF8
    $originalContent = $content

    # 替换import语句
    $content = $content -replace "import top\.yumbo\.ai\.reviewer\.adapter\.output\.storage\.", "import top.yumbo.ai.reviewer.adapter.storage.s3."
    $content = $content -replace "import top\.yumbo\.ai\.reviewer\.adapter\.output\.filesystem\.LocalFileSystemAdapter", "import top.yumbo.ai.reviewer.adapter.storage.local.LocalFileSystemAdapter"
    $content = $content -replace "import top\.yumbo\.ai\.reviewer\.adapter\.output\.cache\.", "import top.yumbo.ai.reviewer.adapter.storage.cache."
    $content = $content -replace "import top\.yumbo\.ai\.reviewer\.adapter\.output\.archive\.", "import top.yumbo.ai.reviewer.adapter.storage.archive."
    $content = $content -replace "import top\.yumbo\.ai\.reviewer\.adapter\.output\.ai\.BedrockAdapter", "import top.yumbo.ai.reviewer.adapter.ai.bedrock.BedrockAdapter"
    $content = $content -replace "import top\.yumbo\.ai\.reviewer\.adapter\.output\.ai\.AIServiceConfig", "import top.yumbo.ai.reviewer.adapter.ai.config.AIServiceConfig"
    $content = $content -replace "import top\.yumbo\.ai\.reviewer\.adapter\.output\.ai\.HttpBasedAIAdapter", "import top.yumbo.ai.reviewer.adapter.ai.http.HttpBasedAIAdapter"
    $content = $content -replace "import top\.yumbo\.ai\.reviewer\.adapter\.output\.ai\.LoggingAIServiceDecorator", "import top.yumbo.ai.reviewer.adapter.ai.decorator.LoggingAIServiceDecorator"
    $content = $content -replace "import top\.yumbo\.ai\.reviewer\.adapter\.output\.ai\.AIAdapterFactory", "import top.yumbo.ai.reviewer.adapter.ai.AIAdapterFactory"
    $content = $content -replace "import top\.yumbo\.ai\.reviewer\.adapter\.output\.ast\.parser\.", "import top.yumbo.ai.reviewer.adapter.parser.code."
    $content = $content -replace "import top\.yumbo\.ai\.reviewer\.adapter\.output\.filesystem\.detector\.(?!language)", "import top.yumbo.ai.reviewer.adapter.parser.detector."
    $content = $content -replace "import top\.yumbo\.ai\.reviewer\.adapter\.output\.filesystem\.detector\.language\.", "import top.yumbo.ai.reviewer.adapter.parser.detector.language."
    $content = $content -replace "import top\.yumbo\.ai\.reviewer\.adapter\.output\.repository\.", "import top.yumbo.ai.reviewer.adapter.repository.git."

    if ($content -ne $originalContent) {
        Set-Content -Path $file.FullName -Value $content -Encoding UTF8 -NoNewline
        Write-Host "  更新import: $($file.Name)" -ForegroundColor Gray
        $updatedImportCount++
    }
}
Write-Host "已更新 $updatedImportCount 个文件的import语句" -ForegroundColor Green
Write-Host ""

# 步骤4: 清理空目录
Write-Host "步骤4: 清理空目录..." -ForegroundColor Yellow
$emptyDirs = @(
    "$baseDir\adapter\output\storage"
    "$baseDir\adapter\output\cache"
    "$baseDir\adapter\output\archive"
    "$baseDir\adapter\output\ai"
    "$baseDir\adapter\output\ast\parser"
    "$baseDir\adapter\output\ast"
    "$baseDir\adapter\output\filesystem\detector"
    "$baseDir\adapter\output\repository"
)

foreach ($dir in $emptyDirs) {
    if (Test-Path $dir) {
        $items = Get-ChildItem $dir -Recurse
        if ($items.Count -eq 0) {
            Remove-Item $dir -Recurse -Force
            Write-Host "  删除空目录: $dir" -ForegroundColor DarkGray
        }
    }
}
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "包重组完成!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "统计信息:" -ForegroundColor Yellow
Write-Host "  移动文件: $movedCount" -ForegroundColor White
Write-Host "  更新package: $updatedPackageCount" -ForegroundColor White
Write-Host "  更新import: $updatedImportCount" -ForegroundColor White
Write-Host ""
Write-Host "下一步操作:" -ForegroundColor Yellow
Write-Host "  1. 运行测试: mvn clean test" -ForegroundColor White
Write-Host "  2. 检查错误: mvn compile" -ForegroundColor White
Write-Host "  3. 全量构建: mvn clean package" -ForegroundColor White
Write-Host ""

