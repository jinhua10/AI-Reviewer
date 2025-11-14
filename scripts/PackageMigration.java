import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class PackageMigration {
    private static final String BASE = "D:\\Jetbrains\\hackathon\\AI-Reviewer\\src\\main\\java\\top\\yumbo\\ai\\reviewer";

    private static final Map<String, String> MOVE_MAP = new LinkedHashMap<>();
    private static final Map<String, String> PACKAGE_MAP = new LinkedHashMap<>();

    static {
        // 定义移动映射
        MOVE_MAP.put("adapter/output/filesystem/LocalFileSystemAdapter.java", "adapter/storage/local/LocalFileSystemAdapter.java");
        MOVE_MAP.put("adapter/output/cache/FileCacheAdapter.java", "adapter/storage/cache/FileCacheAdapter.java");
        MOVE_MAP.put("adapter/output/archive/ZipArchiveAdapter.java", "adapter/storage/archive/ZipArchiveAdapter.java");
        MOVE_MAP.put("adapter/output/ai/BedrockAdapter.java", "adapter/ai/bedrock/BedrockAdapter.java");
        MOVE_MAP.put("adapter/output/ai/AIServiceConfig.java", "adapter/ai/config/AIServiceConfig.java");
        MOVE_MAP.put("adapter/output/ai/HttpBasedAIAdapter.java", "adapter/ai/http/HttpBasedAIAdapter.java");
        MOVE_MAP.put("adapter/output/ai/LoggingAIServiceDecorator.java", "adapter/ai/decorator/LoggingAIServiceDecorator.java");
        MOVE_MAP.put("adapter/output/ai/AIAdapterFactory.java", "adapter/ai/AIAdapterFactory.java");
        MOVE_MAP.put("adapter/output/ast/parser/JavaParserAdapter.java", "adapter/parser/code/java/JavaParserAdapter.java");
        MOVE_MAP.put("adapter/output/ast/parser/PythonParserAdapter.java", "adapter/parser/code/python/PythonParserAdapter.java");
        MOVE_MAP.put("adapter/output/ast/parser/JavaScriptParserAdapter.java", "adapter/parser/code/javascript/JavaScriptParserAdapter.java");
        MOVE_MAP.put("adapter/output/ast/parser/GoParserAdapter.java", "adapter/parser/code/go/GoParserAdapter.java");
        MOVE_MAP.put("adapter/output/ast/parser/CppParserAdapter.java", "adapter/parser/code/cpp/CppParserAdapter.java");
        MOVE_MAP.put("adapter/output/ast/parser/AbstractASTParser.java", "adapter/parser/code/AbstractASTParser.java");
        MOVE_MAP.put("adapter/output/ast/parser/ASTParserFactory.java", "adapter/parser/code/ASTParserFactory.java");
        MOVE_MAP.put("adapter/output/filesystem/detector/LanguageDetector.java", "adapter/parser/detector/LanguageDetector.java");
        MOVE_MAP.put("adapter/output/filesystem/detector/LanguageDetectorRegistry.java", "adapter/parser/detector/LanguageDetectorRegistry.java");
        MOVE_MAP.put("adapter/output/filesystem/detector/LanguageFeatures.java", "adapter/parser/detector/LanguageFeatures.java");
        MOVE_MAP.put("adapter/output/filesystem/detector/GoLanguageDetector.java", "adapter/parser/detector/language/GoLanguageDetector.java");
        MOVE_MAP.put("adapter/output/filesystem/detector/CppLanguageDetector.java", "adapter/parser/detector/language/CppLanguageDetector.java");
        MOVE_MAP.put("adapter/output/filesystem/detector/RustLanguageDetector.java", "adapter/parser/detector/language/RustLanguageDetector.java");
        MOVE_MAP.put("adapter/output/repository/GitRepositoryAdapter.java", "adapter/repository/git/GitRepositoryAdapter.java");

        // 包名映射
        PACKAGE_MAP.put("top.yumbo.ai.reviewer.adapter.output.filesystem", "top.yumbo.ai.reviewer.adapter.storage.local");
        PACKAGE_MAP.put("top.yumbo.ai.reviewer.adapter.output.cache", "top.yumbo.ai.reviewer.adapter.storage.cache");
        PACKAGE_MAP.put("top.yumbo.ai.reviewer.adapter.output.archive", "top.yumbo.ai.reviewer.adapter.storage.archive");
        PACKAGE_MAP.put("top.yumbo.ai.reviewer.adapter.output.storage", "top.yumbo.ai.reviewer.adapter.storage.s3");
        PACKAGE_MAP.put("top.yumbo.ai.reviewer.adapter.output.ai.BedrockAdapter", "top.yumbo.ai.reviewer.adapter.ai.bedrock.BedrockAdapter");
        PACKAGE_MAP.put("top.yumbo.ai.reviewer.adapter.output.ai.AIServiceConfig", "top.yumbo.ai.reviewer.adapter.ai.config.AIServiceConfig");
        PACKAGE_MAP.put("top.yumbo.ai.reviewer.adapter.output.ai.HttpBasedAIAdapter", "top.yumbo.ai.reviewer.adapter.ai.http.HttpBasedAIAdapter");
        PACKAGE_MAP.put("top.yumbo.ai.reviewer.adapter.output.ai.LoggingAIServiceDecorator", "top.yumbo.ai.reviewer.adapter.ai.decorator.LoggingAIServiceDecorator");
        PACKAGE_MAP.put("top.yumbo.ai.reviewer.adapter.output.ai.AIAdapterFactory", "top.yumbo.ai.reviewer.adapter.ai.AIAdapterFactory");
        PACKAGE_MAP.put("top.yumbo.ai.reviewer.adapter.output.ai", "top.yumbo.ai.reviewer.adapter.ai");
        PACKAGE_MAP.put("top.yumbo.ai.reviewer.adapter.output.ast.parser", "top.yumbo.ai.reviewer.adapter.parser.code");
        PACKAGE_MAP.put("top.yumbo.ai.reviewer.adapter.output.filesystem.detector", "top.yumbo.ai.reviewer.adapter.parser.detector");
        PACKAGE_MAP.put("top.yumbo.ai.reviewer.adapter.output.repository", "top.yumbo.ai.reviewer.adapter.repository.git");
    }

    public static void main(String[] args) throws IOException {
        System.out.println("开始包迁移...");

        // 1. 移动文件
        int movedCount = moveFiles();
        System.out.println("已移动 " + movedCount + " 个文件");

        // 2. 更新所有Java文件的import和package
        int updatedCount = updateAllJavaFiles();
        System.out.println("已更新 " + updatedCount + " 个文件的import/package");

        System.out.println("迁移完成！");
    }

    private static int moveFiles() throws IOException {
        int count = 0;
        for (Map.Entry<String, String> entry : MOVE_MAP.entrySet()) {
            Path source = Paths.get(BASE, entry.getKey().replace("/", "\\"));
            Path target = Paths.get(BASE, entry.getValue().replace("/", "\\"));

            if (Files.exists(source)) {
                Files.createDirectories(target.getParent());
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("移动: " + entry.getKey() + " -> " + entry.getValue());
                count++;
            }
        }
        return count;
    }

    private static int updateAllJavaFiles() throws IOException {
        int count = 0;
        Path srcRoot = Paths.get(BASE).getParent().getParent().getParent().getParent().getParent();

        try (var stream = Files.walk(srcRoot)) {
            for (Path file : stream.filter(p -> p.toString().endsWith(".java")).toList()) {
                if (updateFile(file)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static boolean updateFile(Path file) throws IOException {
        String content = Files.readString(file);
        String original = content;

        // 更新package声明
        for (Map.Entry<String, String> entry : PACKAGE_MAP.entrySet()) {
            String oldPkg = entry.getKey();
            String newPkg = entry.getValue();

            // 更新package语句
            Pattern pkgPattern = Pattern.compile("package\\s+" + Pattern.quote(oldPkg) + "\\s*;");
            content = pkgPattern.matcher(content).replaceAll("package " + newPkg + ";");

            // 更新import语句
            Pattern importPattern = Pattern.compile("import\\s+" + Pattern.quote(oldPkg));
            content = importPattern.matcher(content).replaceAll("import " + newPkg);
        }

        // 特殊处理：具体的类导入
        content = content.replaceAll(
            "import top\\.yumbo\\.ai\\.reviewer\\.adapter\\.output\\.filesystem\\.LocalFileSystemAdapter",
            "import top.yumbo.ai.reviewer.adapter.storage.local.LocalFileSystemAdapter");
        content = content.replaceAll(
            "import top\\.yumbo\\.ai\\.reviewer\\.adapter\\.output\\.cache\\.FileCacheAdapter",
            "import top.yumbo.ai.reviewer.adapter.storage.cache.FileCacheAdapter");
        content = content.replaceAll(
            "import top\\.yumbo\\.ai\\.reviewer\\.adapter\\.output\\.archive\\.ZipArchiveAdapter",
            "import top.yumbo.ai.reviewer.adapter.storage.archive.ZipArchiveAdapter");
        content = content.replaceAll(
            "import top\\.yumbo\\.ai\\.reviewer\\.adapter\\.output\\.repository\\.GitRepositoryAdapter",
            "import top.yumbo.ai.reviewer.adapter.repository.git.GitRepositoryAdapter");

        if (!content.equals(original)) {
            Files.writeString(file, content);
            System.out.println("更新: " + file.getFileName());
            return true;
        }
        return false;
    }
}

