package top.yumbo.ai.reviewer.domain.model;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 源文件领域模型（增强版）
 * 支持多种文件类型：代码、图片、视频、文档等
 *
 * @author AI-Reviewer Team
 * @version 2.0
 */
@Data
@Builder
public class SourceFile {

    // 基础信息
    private final Path path;
    private final String relativePath;
    private final String fileName;
    private final String extension;

    private String content;
    private int lineCount;
    private long sizeInBytes;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime modifiedAt = LocalDateTime.now();

    // 文件分类（增强版）
    @Builder.Default
    private FileMainCategory mainCategory = FileMainCategory.UNKNOWN;

    @Builder.Default
    private FileType fileType = FileType.UNKNOWN;

    @Builder.Default
    private String mimeType = "application/octet-stream";

    // 兼容旧版本的分类
    @Builder.Default
    private FileCategory category = FileCategory.UNKNOWN;

    @Builder.Default
    private boolean isCore = false;

    @Builder.Default
    private int priority = 0;

    // 二进制内容（用于媒体文件）
    private byte[] binaryContent;

    // 元数据（可扩展）
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    // 分析结果（按需加载）
    private transient Object analysisResult;

    /**
     * 获取文件类型
     */
    public ProjectType getProjectType() {
        return ProjectType.fromExtension(extension);
    }

    /**
     * 判断是否为配置文件
     */
    public boolean isConfigFile() {
        return category == FileCategory.CONFIG ||
               mainCategory == FileMainCategory.CONFIG;
    }

    /**
     * 判断是否为入口文件
     */
    public boolean isEntryPoint() {
        return category == FileCategory.ENTRY_POINT;
    }

    /**
     * 判断是否为测试文件
     */
    public boolean isTestFile() {
        return category == FileCategory.TEST;
    }

    /**
     * 判断是否为文本型文件
     */
    public boolean isTextBased() {
        return mainCategory != null && mainCategory.isTextBased();
    }

    /**
     * 判断是否为媒体文件
     */
    public boolean isMediaFile() {
        return mainCategory == FileMainCategory.IMAGE ||
               mainCategory == FileMainCategory.VIDEO ||
               mainCategory == FileMainCategory.AUDIO;
    }

    /**
     * 判断是否为文档文件
     */
    public boolean isDocumentFile() {
        return mainCategory == FileMainCategory.DOCUMENT;
    }

    /**
     * 文件主类别（一级分类）
     */
    @Getter
    public enum FileMainCategory {
        CODE("代码文件", true),
        DOCUMENT("文档文件", true),
        IMAGE("图片文件", false),
        VIDEO("视频文件", false),
        AUDIO("音频文件", false),
        DATA("数据文件", true),
        CONFIG("配置文件", true),
        ARCHIVE("压缩文件", false),
        OTHER("其他文件", false),
        UNKNOWN("未知类型", false);

        private final String displayName;
        private final boolean textBased;

        FileMainCategory(String displayName, boolean textBased) {
            this.displayName = displayName;
            this.textBased = textBased;
        }
    }

    /**
     * 文件类型（二级分类 - 详细）
     */
    @Getter
    public enum FileType {
        // 代码类
        JAVA("java", FileMainCategory.CODE, "text/x-java"),
        PYTHON("python", FileMainCategory.CODE, "text/x-python"),
        JAVASCRIPT("javascript", FileMainCategory.CODE, "text/javascript"),
        TYPESCRIPT("typescript", FileMainCategory.CODE, "text/typescript"),
        GO("go", FileMainCategory.CODE, "text/x-go"),
        RUST("rust", FileMainCategory.CODE, "text/x-rust"),
        CPP("c++", FileMainCategory.CODE, "text/x-c++src"),
        C("c", FileMainCategory.CODE, "text/x-csrc"),
        CSHARP("c#", FileMainCategory.CODE, "text/x-csharp"),

        // 文档类
        PDF("pdf", FileMainCategory.DOCUMENT, "application/pdf"),
        WORD("word", FileMainCategory.DOCUMENT, "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
        EXCEL("excel", FileMainCategory.DOCUMENT, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        MARKDOWN("markdown", FileMainCategory.DOCUMENT, "text/markdown"),
        TEXT("text", FileMainCategory.DOCUMENT, "text/plain"),

        // 图片类
        JPEG("jpeg", FileMainCategory.IMAGE, "image/jpeg"),
        PNG("png", FileMainCategory.IMAGE, "image/png"),
        GIF("gif", FileMainCategory.IMAGE, "image/gif"),
        SVG("svg", FileMainCategory.IMAGE, "image/svg+xml"),

        // 视频类
        MP4("mp4", FileMainCategory.VIDEO, "video/mp4"),
        AVI("avi", FileMainCategory.VIDEO, "video/x-msvideo"),
        MOV("mov", FileMainCategory.VIDEO, "video/quicktime"),

        // 音频类
        MP3("mp3", FileMainCategory.AUDIO, "audio/mpeg"),
        WAV("wav", FileMainCategory.AUDIO, "audio/wav"),

        // 数据类
        JSON("json", FileMainCategory.DATA, "application/json"),
        XML("xml", FileMainCategory.DATA, "application/xml"),
        CSV("csv", FileMainCategory.DATA, "text/csv"),
        YAML("yaml", FileMainCategory.DATA, "application/x-yaml"),

        // 配置类
        PROPERTIES("properties", FileMainCategory.CONFIG, "text/x-java-properties"),

        // 压缩类
        ZIP("zip", FileMainCategory.ARCHIVE, "application/zip"),

        // 其他
        UNKNOWN("unknown", FileMainCategory.UNKNOWN, "application/octet-stream");

        private final String name;
        private final FileMainCategory mainCategory;
        private final String mimeType;

        FileType(String name, FileMainCategory mainCategory, String mimeType) {
            this.name = name;
            this.mainCategory = mainCategory;
            this.mimeType = mimeType;
        }

        /**
         * 根据文件扩展名识别文件类型
         */
        public static FileType fromExtension(String extension) {
            if (extension == null) return UNKNOWN;
            String ext = extension.toLowerCase().replaceFirst("^\\.", "");

            return switch (ext) {
                // 代码
                case "java" -> JAVA;
                case "py" -> PYTHON;
                case "js", "jsx", "mjs" -> JAVASCRIPT;
                case "ts", "tsx" -> TYPESCRIPT;
                case "go" -> GO;
                case "rs" -> RUST;
                case "cpp", "cc", "cxx", "c++" -> CPP;
                case "c" -> C;
                case "cs" -> CSHARP;

                // 文档
                case "pdf" -> PDF;
                case "doc", "docx" -> WORD;
                case "xls", "xlsx" -> EXCEL;
                case "md", "markdown" -> MARKDOWN;
                case "txt" -> TEXT;

                // 图片
                case "jpg", "jpeg" -> JPEG;
                case "png" -> PNG;
                case "gif" -> GIF;
                case "svg" -> SVG;

                // 视频
                case "mp4" -> MP4;
                case "avi" -> AVI;
                case "mov" -> MOV;

                // 音频
                case "mp3" -> MP3;
                case "wav" -> WAV;

                // 数据
                case "json" -> JSON;
                case "xml" -> XML;
                case "csv" -> CSV;
                case "yaml", "yml" -> YAML;

                // 配置
                case "properties" -> PROPERTIES;

                // 压缩
                case "zip" -> ZIP;

                default -> UNKNOWN;
            };
        }
    }

    /**
     * 文件类别（兼容旧版）
     */
    public enum FileCategory {
        ENTRY_POINT,    // 入口文件
        CONFIG,         // 配置文件
        CORE_BUSINESS,  // 核心业务
        UTIL,           // 工具类
        TEST,           // 测试文件
        DOCUMENTATION,  // 文档文件
        SOURCE_CODE,    // 源码文件
        UNKNOWN         // 未知类型
    }
}


