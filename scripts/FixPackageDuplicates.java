import java.io.*;
import java.nio.file.*;
import java.util.*;

public class FixPackageDuplicates {
    private static final String BASE = "D:\\Jetbrains\\hackathon\\AI-Reviewer\\src\\main\\java\\top\\yumbo\\ai\\reviewer";

    public static void main(String[] args) throws IOException {
        System.out.println("修复包重复问题...");

        // 1. 删除旧位置的文件（如果新位置已存在）
        deleteIfDuplicate("adapter/output/ai/AIServiceConfig.java", "adapter/ai/config/AIServiceConfig.java");
        deleteIfDuplicate("adapter/output/filesystem/detector/CppLanguageDetector.java", "adapter/parser/detector/language/CppLanguageDetector.java");
        deleteIfDuplicate("adapter/output/filesystem/detector/GoLanguageDetector.java", "adapter/parser/detector/language/GoLanguageDetector.java");
        deleteIfDuplicate("adapter/output/filesystem/detector/RustLanguageDetector.java", "adapter/parser/detector/language/RustLanguageDetector.java");

        // 2. 修复package声明
        fixPackage("adapter/parser/detector/language/CppLanguageDetector.java", "top.yumbo.ai.reviewer.adapter.parser.detector.language");
        fixPackage("adapter/parser/detector/language/GoLanguageDetector.java", "top.yumbo.ai.reviewer.adapter.parser.detector.language");
        fixPackage("adapter/parser/detector/language/RustLanguageDetector.java", "top.yumbo.ai.reviewer.adapter.parser.detector.language");
        fixPackage("adapter/ai/config/AIServiceConfig.java", "top.yumbo.ai.reviewer.adapter.ai.config");
        fixPackage("adapter/ai/bedrock/BedrockAdapter.java", "top.yumbo.ai.reviewer.adapter.ai.bedrock");
        fixPackage("adapter/ai/http/HttpBasedAIAdapter.java", "top.yumbo.ai.reviewer.adapter.ai.http");
        fixPackage("adapter/ai/decorator/LoggingAIServiceDecorator.java", "top.yumbo.ai.reviewer.adapter.ai.decorator");

        // 3. 更新AIServiceFactory中的引用
        updateAIServiceFactory();

        System.out.println("修复完成！");
    }

    private static void deleteIfDuplicate(String oldPath, String newPath) {
        Path old = Paths.get(BASE, oldPath.replace("/", "\\"));
        Path newFile = Paths.get(BASE, newPath.replace("/", "\\"));

        if (Files.exists(newFile) && Files.exists(old)) {
            try {
                Files.delete(old);
                System.out.println("删除重复文件: " + oldPath);
            } catch (IOException e) {
                System.err.println("删除失败: " + oldPath + " - " + e.getMessage());
            }
        }
    }

    private static void fixPackage(String filePath, String correctPackage) throws IOException {
        Path file = Paths.get(BASE, filePath.replace("/", "\\"));

        if (!Files.exists(file)) {
            System.out.println("文件不存在: " + filePath);
            return;
        }

        String content = Files.readString(file);
        String[] lines = content.split("\\r?\\n");
        StringBuilder result = new StringBuilder();

        boolean packageFixed = false;
        for (String line : lines) {
            if (!packageFixed && line.trim().startsWith("package ")) {
                result.append("package ").append(correctPackage).append(";\n");
                packageFixed = true;
            } else {
                result.append(line).append("\n");
            }
        }

        if (packageFixed) {
            Files.writeString(file, result.toString());
            System.out.println("修复package: " + filePath + " -> " + correctPackage);
        }
    }

    private static void updateAIServiceFactory() throws IOException {
        Path file = Paths.get(BASE, "infrastructure\\factory\\AIServiceFactory.java");

        if (!Files.exists(file)) {
            return;
        }

        String content = Files.readString(file);

        // 更新import语句
        content = content.replaceAll(
            "new top\\.yumbo\\.ai\\.reviewer\\.adapter\\.output\\.ai\\.BedrockAdapter",
            "new top.yumbo.ai.reviewer.adapter.ai.bedrock.BedrockAdapter");
        content = content.replaceAll(
            "new top\\.yumbo\\.ai\\.reviewer\\.adapter\\.output\\.ai\\.HttpBasedAIAdapter",
            "new top.yumbo.ai.reviewer.adapter.ai.http.HttpBasedAIAdapter");
        content = content.replaceAll(
            "new top\\.yumbo\\.ai\\.reviewer\\.adapter\\.output\\.ai\\.LoggingAIServiceDecorator",
            "new top.yumbo.ai.reviewer.adapter.ai.decorator.LoggingAIServiceDecorator");

        Files.writeString(file, content);
        System.out.println("更新AIServiceFactory");
    }
}

