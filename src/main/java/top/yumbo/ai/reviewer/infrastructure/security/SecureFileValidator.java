package top.yumbo.ai.reviewer.infrastructure.security;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.reviewer.domain.model.SourceFile;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/**
 * 安全文件验证器
 * 防止路径遍历攻击、恶意文件上传等安全问题
 *
 * @author AI-Reviewer Team
 * @version 2.0
 */
@Slf4j
public class SecureFileValidator {

    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB
    private static final long MAX_TOTAL_SIZE = 1024 * 1024 * 1024; // 1GB
    private static final int MAX_FILE_COUNT = 10000;

    // 危险文件扩展名黑名单
    private static final Set<String> DANGEROUS_EXTENSIONS = Set.of(
        "exe", "dll", "bat", "cmd", "sh", "bash",
        "ps1", "vbs", "scr", "com", "pif"
    );

    // 允许的文件类型（白名单）
    private static final Map<SourceFile.FileMainCategory, Set<String>> ALLOWED_EXTENSIONS = Map.of(
        SourceFile.FileMainCategory.CODE, Set.of("java", "py", "js", "ts", "go", "rs", "cpp", "c", "h", "cs"),
        SourceFile.FileMainCategory.DOCUMENT, Set.of("pdf", "doc", "docx", "md", "txt", "rtf"),
        SourceFile.FileMainCategory.IMAGE, Set.of("jpg", "jpeg", "png", "gif", "svg", "webp"),
        SourceFile.FileMainCategory.VIDEO, Set.of("mp4", "avi", "mov", "mkv", "webm"),
        SourceFile.FileMainCategory.AUDIO, Set.of("mp3", "wav", "flac", "ogg", "m4a"),
        SourceFile.FileMainCategory.CONFIG, Set.of("json", "yaml", "yml", "xml", "properties", "toml")
    );

    /**
     * 验证文件路径安全性（防止路径遍历攻击）
     */
    public static ValidationResult validatePath(Path basePath, Path targetPath) {
        try {
            Path normalizedBase = basePath.normalize().toAbsolutePath();
            Path normalizedTarget = targetPath.normalize().toAbsolutePath();

            if (!normalizedTarget.startsWith(normalizedBase)) {
                log.warn("检测到路径遍历攻击: {} 不在 {} 范围内", targetPath, basePath);
                return ValidationResult.fail("不安全的文件路径");
            }

            return ValidationResult.success();

        } catch (Exception e) {
            log.error("路径验证失败", e);
            return ValidationResult.fail("路径验证异常: " + e.getMessage());
        }
    }

    /**
     * 验证文件扩展名
     */
    public static ValidationResult validateExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return ValidationResult.fail("文件名为空");
        }

        String extension = getExtension(fileName).toLowerCase();

        // 检查是否为危险扩展名
        if (DANGEROUS_EXTENSIONS.contains(extension)) {
            log.warn("检测到危险文件扩展名: {}", fileName);
            return ValidationResult.fail("不允许的文件类型: " + extension);
        }

        return ValidationResult.success();
    }

    /**
     * 验证文件大小
     */
    public static ValidationResult validateFileSize(long size) {
        if (size <= 0) {
            return ValidationResult.fail("文件大小无效");
        }

        if (size > MAX_FILE_SIZE) {
            log.warn("文件大小超过限制: {} bytes (最大: {} bytes)", size, MAX_FILE_SIZE);
            return ValidationResult.fail(
                String.format("文件大小超过限制 (%d MB)", MAX_FILE_SIZE / 1024 / 1024)
            );
        }

        return ValidationResult.success();
    }

    /**
     * 验证总大小
     */
    public static ValidationResult validateTotalSize(long totalSize) {
        if (totalSize > MAX_TOTAL_SIZE) {
            log.warn("总文件大小超过限制: {} bytes", totalSize);
            return ValidationResult.fail(
                String.format("总文件大小超过限制 (%d MB)", MAX_TOTAL_SIZE / 1024 / 1024)
            );
        }

        return ValidationResult.success();
    }

    /**
     * 验证文件数量
     */
    public static ValidationResult validateFileCount(int count) {
        if (count > MAX_FILE_COUNT) {
            log.warn("文件数量超过限制: {}", count);
            return ValidationResult.fail("文件数量超过限制: " + MAX_FILE_COUNT);
        }

        return ValidationResult.success();
    }

    /**
     * 综合验证
     */
    public static ValidationResult validateFile(SourceFile file, Path basePath) {
        // 1. 路径验证
        ValidationResult pathResult = validatePath(basePath, file.getPath());
        if (!pathResult.isValid()) {
            return pathResult;
        }

        // 2. 扩展名验证
        ValidationResult extensionResult = validateExtension(file.getFileName());
        if (!extensionResult.isValid()) {
            return extensionResult;
        }

        // 3. 大小验证
        ValidationResult sizeResult = validateFileSize(file.getSizeInBytes());
        if (!sizeResult.isValid()) {
            return sizeResult;
        }

        return ValidationResult.success();
    }

    private static String getExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1) {
            return "";
        }
        return fileName.substring(lastDot + 1);
    }

    /**
     * 验证结果
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, "验证通过");
        }

        public static ValidationResult fail(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
}

