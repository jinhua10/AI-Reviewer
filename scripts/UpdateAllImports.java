import java.io.*;
import java.nio.file.*;
import java.util.regex.*;

public class UpdateAllImports {
    public static void main(String[] args) throws IOException {
        Path srcRoot = Paths.get("src");
        int count = 0;

        try (var stream = Files.walk(srcRoot)) {
            for (Path file : stream.filter(p -> p.toString().endsWith(".java")).toList()) {
                if (updateImports(file)) {
                    count++;
                    System.out.println("Updated: " + file);
                }
            }
        }

        System.out.println("\nTotal updated: " + count + " files");
    }

    private static boolean updateImports(Path file) throws IOException {
        String content = Files.readString(file);
        String original = content;

        // Update all import statements
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
            "import top\\.yumbo\\.ai\\.reviewer\\.adapter\\.output\\.storage\\.",
            "import top.yumbo.ai.reviewer.adapter.storage.s3.");

        content = content.replaceAll(
            "import top\\.yumbo\\.ai\\.reviewer\\.adapter\\.output\\.ai\\.BedrockAdapter",
            "import top.yumbo.ai.reviewer.adapter.ai.bedrock.BedrockAdapter");

        content = content.replaceAll(
            "import top\\.yumbo\\.ai\\.reviewer\\.adapter\\.output\\.ai\\.AIServiceConfig",
            "import top.yumbo.ai.reviewer.adapter.ai.config.AIServiceConfig");

        content = content.replaceAll(
            "import top\\.yumbo\\.ai\\.reviewer\\.adapter\\.output\\.ai\\.HttpBasedAIAdapter",
            "import top.yumbo.ai.reviewer.adapter.ai.http.HttpBasedAIAdapter");

        content = content.replaceAll(
            "import top\\.yumbo\\.ai\\.reviewer\\.adapter\\.output\\.ai\\.LoggingAIServiceDecorator",
            "import top.yumbo.ai.reviewer.adapter.ai.decorator.LoggingAIServiceDecorator");

        content = content.replaceAll(
            "import top\\.yumbo\\.ai\\.reviewer\\.adapter\\.output\\.ai\\.AIAdapterFactory",
            "import top.yumbo.ai.reviewer.adapter.ai.AIAdapterFactory");

        content = content.replaceAll(
            "import top\\.yumbo\\.ai\\.reviewer\\.adapter\\.output\\.ast\\.parser\\.",
            "import top.yumbo.ai.reviewer.adapter.parser.code.");

        content = content.replaceAll(
            "import top\\.yumbo\\.ai\\.reviewer\\.adapter\\.output\\.filesystem\\.detector\\.(?!language)",
            "import top.yumbo.ai.reviewer.adapter.parser.detector.");

        content = content.replaceAll(
            "import top\\.yumbo\\.ai\\.reviewer\\.adapter\\.output\\.filesystem\\.detector\\.language\\.",
            "import top.yumbo.ai.reviewer.adapter.parser.detector.language.");

        content = content.replaceAll(
            "import top\\.yumbo\\.ai\\.reviewer\\.adapter\\.output\\.repository\\.GitRepositoryAdapter",
            "import top.yumbo.ai.reviewer.adapter.repository.git.GitRepositoryAdapter");

        if (!content.equals(original)) {
            Files.writeString(file, content);
            return true;
        }
        return false;
    }
}

