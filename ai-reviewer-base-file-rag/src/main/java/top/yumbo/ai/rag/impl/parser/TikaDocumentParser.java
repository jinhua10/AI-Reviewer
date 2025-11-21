package top.yumbo.ai.rag.impl.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import top.yumbo.ai.rag.core.DocumentParser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Apache Tika文档解析器实现
 * 支持多种文档格式的解析
 *
 * @author AI Reviewer Team
 * @since 2025-11-21
 */
@Slf4j
public class TikaDocumentParser implements DocumentParser {

    private final Tika tika;

    // 支持的MIME类型
    private static final Set<String> SUPPORTED_MIME_TYPES = new HashSet<>(Arrays.asList(
            // 文本
            "text/plain",
            "text/html",
            "text/xml",
            "text/markdown",

            // 文档
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",

            // 代码
            "text/x-java-source",
            "text/x-python",
            "text/x-c",
            "application/javascript",
            "application/json",
            "application/xml"
    ));

    // 支持的文件扩展名
    private static final Set<String> SUPPORTED_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".txt", ".md", ".html", ".xml", ".json",
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".java", ".py", ".js", ".ts", ".c", ".cpp", ".h", ".go", ".rs"
    ));

    public TikaDocumentParser() {
        this.tika = new Tika();
        log.info("TikaDocumentParser initialized");
    }

    @Override
    public String parse(File file) {
        if (file == null || !file.exists()) {
            log.warn("File does not exist: {}", file);
            return "";
        }

        try {
            // 检测MIME类型
            String mimeType = tika.detect(file);
            log.debug("Detected MIME type: {} for file: {}", mimeType, file.getName());

            // 解析文件
            String content = tika.parseToString(file);

            log.debug("Parsed file: {}, content length: {}", file.getName(), content.length());
            return content;

        } catch (IOException | TikaException e) {
            log.error("Failed to parse file: {}", file.getAbsolutePath(), e);
            return "";
        }
    }

    @Override
    public String parse(byte[] bytes, String mimeType) {
        if (bytes == null || bytes.length == 0) {
            log.warn("Empty byte array provided");
            return "";
        }

        try {
            // 使用Tika解析
            String content = tika.parseToString(new java.io.ByteArrayInputStream(bytes));

            log.debug("Parsed bytes: mimeType={}, content length: {}", mimeType, content.length());
            return content;

        } catch (IOException | TikaException e) {
            log.error("Failed to parse bytes: mimeType={}", mimeType, e);
            return "";
        }
    }

    @Override
    public boolean supports(String mimeType) {
        if (mimeType == null) {
            return false;
        }

        // 检查是否在支持列表中
        if (SUPPORTED_MIME_TYPES.contains(mimeType)) {
            return true;
        }

        // 检查通配符匹配
        if (mimeType.startsWith("text/")) {
            return true;
        }

        return false;
    }

    @Override
    public boolean supportsExtension(String extension) {
        if (extension == null) {
            return false;
        }

        // 确保扩展名以点开头
        String ext = extension.startsWith(".") ? extension : "." + extension;
        return SUPPORTED_EXTENSIONS.contains(ext.toLowerCase());
    }

    /**
     * 检测文件的MIME类型
     */
    public String detectMimeType(File file) {
        try {
            return tika.detect(file);
        } catch (IOException e) {
            log.error("Failed to detect MIME type: {}", file.getAbsolutePath(), e);
            return "application/octet-stream";
        }
    }

    /**
     * 检测字节数组的MIME类型
     */
    public String detectMimeType(byte[] bytes) {
        return tika.detect(bytes);
    }

    /**
     * 根据文件扩展名检测MIME类型
     */
    public String detectMimeType(String filename) {
        return tika.detect(filename);
    }
}

