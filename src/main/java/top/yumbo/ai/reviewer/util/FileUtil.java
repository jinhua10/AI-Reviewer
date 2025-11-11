package top.yumbo.ai.reviewer.util;

import top.yumbo.ai.reviewer.entity.SourceFile;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件工具类
 * 
 * 提供文件相关的实用方法
 */
public class FileUtil {

    /**
     * 获取文件类型
     * 
     * @param path 文件路径
     * @return 文件类型
     */
    public static SourceFile.FileType getFileType(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();

        if (fileName.endsWith(".java")) {
            return SourceFile.FileType.JAVA;
        } else if (fileName.endsWith(".py")) {
            return SourceFile.FileType.PYTHON;
        } else if (fileName.endsWith(".js")) {
            return SourceFile.FileType.JAVASCRIPT;
        } else if (fileName.endsWith(".ts")) {
            return SourceFile.FileType.TYPESCRIPT;
        } else if (fileName.endsWith(".go")) {
            return SourceFile.FileType.GO;
        } else if (fileName.endsWith(".cpp") || fileName.endsWith(".cxx") || fileName.endsWith(".cc")) {
            return SourceFile.FileType.CPP;
        } else if (fileName.endsWith(".c")) {
            return SourceFile.FileType.C;
        } else if (fileName.endsWith(".h") || fileName.endsWith(".hpp")) {
            return SourceFile.FileType.H;
        } else {
            return SourceFile.FileType.OTHER;
        }
    }

    /**
     * 检查文件名是否匹配模式
     * 
     * @param fileName 文件名
     * @param pattern 模式（支持通配符）
     * @return 如果匹配返回true，否则返回false
     */
    public static boolean matchesPattern(String fileName, String pattern) {
        // 简单实现，支持*通配符
        String regex = pattern.replace("*", ".*").replace("?", ".");
        return fileName.matches(regex);
    }

    /**
     * 获取相对路径
     * 
     * @param basePath 基础路径
     * @param fullPath 完整路径
     * @return 相对路径
     */
    public static String getRelativePath(String basePath, String fullPath) {
        Path base = Paths.get(basePath).normalize();
        Path full = Paths.get(fullPath).normalize();

        if (full.startsWith(base)) {
            return base.relativize(full).toString();
        } else {
            return fullPath;
        }
    }
}
