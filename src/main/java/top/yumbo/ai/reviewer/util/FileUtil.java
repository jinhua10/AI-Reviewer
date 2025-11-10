package top.yumbo.ai.reviewer.util;

import top.yumbo.ai.reviewer.entity.SourceFile;
import top.yumbo.ai.reviewer.exception.AnalysisException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 文件工具类（简化版）
 * 提供文件读取、类型检测等基础功能
 */
public class FileUtil {

    /**
     * 递归扫描目录，返回所有文件
     */
    public static List<Path> scanDirectory(Path directory) throws AnalysisException {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(directory)) {
            stream.filter(Files::isRegularFile)
                  .forEach(files::add);
        } catch (IOException e) {
            throw AnalysisException.fileError("Failed to scan directory: " + directory, e);
        }
        return files;
    }

    /**
     * 读取文件内容
     */
    public static String readFile(Path path) throws AnalysisException {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw AnalysisException.fileError("Failed to read file: " + path, e);
        }
    }

    /**
     * 检测文件类型
     */
    public static SourceFile.FileType detectFileType(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();

        // 编程语言
        if (fileName.endsWith(".java")) return SourceFile.FileType.JAVA;
        if (fileName.endsWith(".py")) return SourceFile.FileType.PYTHON;
        if (fileName.endsWith(".js")) return SourceFile.FileType.JAVASCRIPT;
        if (fileName.endsWith(".ts")) return SourceFile.FileType.TYPESCRIPT;
        if (fileName.endsWith(".go")) return SourceFile.FileType.GO;
        if (fileName.endsWith(".rs")) return SourceFile.FileType.RUST;

        // 文档
        if (fileName.endsWith(".md")) return SourceFile.FileType.MARKDOWN;
        if (fileName.endsWith(".txt")) return SourceFile.FileType.TEXT;
        if (fileName.endsWith(".json")) return SourceFile.FileType.JSON;
        if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) return SourceFile.FileType.YAML;
        if (fileName.endsWith(".xml")) return SourceFile.FileType.XML;

        // 图片
        if (fileName.matches(".*\\.(jpg|jpeg|png|gif|bmp|svg)")) return SourceFile.FileType.IMAGE;

        // 其他
        if (fileName.endsWith(".pdf")) return SourceFile.FileType.PDF;
        if (fileName.matches(".*\\.(doc|docx)")) return SourceFile.FileType.WORD;

        // 二进制文件检测
        if (isBinaryFile(path)) return SourceFile.FileType.BINARY;

        return SourceFile.FileType.UNKNOWN;
    }

    /**
     * 检查是否为二进制文件
     */
    public static boolean isBinaryFile(Path path) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            if (bytes.length == 0) return false;

            // 检查前 1024 字节中是否有非文本字符
            int checkLength = Math.min(bytes.length, 1024);
            int nullCount = 0;

            for (int i = 0; i < checkLength; i++) {
                if (bytes[i] == 0) {
                    nullCount++;
                }
            }

            // 如果有超过 1% 的空字节，认为是二进制文件
            return (nullCount * 100.0 / checkLength) > 1.0;

        } catch (IOException e) {
            return true; // 无法读取，当作二进制处理
        }
    }

    /**
     * 检查文件是否应该被忽略
     */
    public static boolean shouldIgnore(Path path, List<String> ignorePatterns) {
        String pathStr = path.toString();

        // 默认忽略规则
        if (pathStr.contains(".git") ||
            pathStr.contains("node_modules") ||
            pathStr.contains("target") ||
            pathStr.contains("build") ||
            pathStr.contains(".idea")) {
            return true;
        }

        // 自定义忽略规则
        if (ignorePatterns != null) {
            for (String pattern : ignorePatterns) {
                if (pathStr.contains(pattern)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 获取文件大小
     */
    public static long getFileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return 0;
        }
    }
}

