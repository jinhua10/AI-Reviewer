# AI-Reviewer 多文件类型扩展架构设计（第4部分）

**生成时间**: 2025-11-14 23:31:44  
**分析人员**: 世界顶级架构师  
**文档类型**: 架构设计文档

---

## 📋 概述

根据项目目标："设计AI引擎，计划像黑客松一样扩展并读取各类文件类型（媒体、文档等），后期期望利用各类AI模型对各类文件进行用户想要做的事情，例如数据分析、总结等行为"。

本报告详细设计多文件类型支持的完整架构方案。

---

## 🎯 多文件类型支持总体架构

### 架构全景图

```
┌────────────────────────────────────────────────────────────────────┐
│                         AI-Reviewer 引擎                              │
│                   (Multi-Modal File Processing)                    │
└────────────────────────────────────────────────────────────────────┘
                                   │
         ┌─────────────────────────┼─────────────────────────┐
         │                         │                         │
         ▼                         ▼                         ▼
┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐
│  Input Adapters  │      │  Core Engine     │      │ Output Adapters │
│                 │      │                 │      │                 │
│ • File System   │      │ • Orchestrator   │      │ • Report Gen    │
│ • Git Repo      │      │ • Strategy Mgr   │      │ • Visualization │
│ • S3 Storage    │      │ • AI Service     │      │ • Export        │
│ • ZIP Archive   │      │ • Cache          │      │ • Notification  │
│ • URL Download  │      │ • Metrics        │      │                 │
└─────────────────┘      └─────────────────┘      └─────────────────┘
                                   │
         ┌─────────────────────────┼─────────────────────────┐
         │                         │                         │
         ▼                         ▼                         ▼
┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐
│  Code Processor  │      │  Media Processor │      │  Doc Processor   │
│                 │      │                 │      │                 │
│ • Java Parser   │      │ • Image         │      │ • PDF           │
│ • Python Parser │      │ • Video         │      │ • Word/Excel    │
│ • JS/TS Parser  │      │ • Audio         │      │ • Markdown      │
│ • Go Parser     │      │ • 3D Model      │      │ • Text          │
│ • C++ Parser    │      │                 │      │                 │
└─────────────────┘      └─────────────────┘      └─────────────────┘
         │                         │                         │
         └─────────────────────────┼─────────────────────────┘
                                   │
                                   ▼
                    ┌──────────────────────────┐
                    │   AI Model Integration    │
                    │                          │
                    │ • Text Models (GPT, etc) │
                    │ • Vision Models          │
                    │ • Multi-Modal Models     │
                    │ • Custom Models          │
                    └──────────────────────────┘
```

---

## 🎨 核心领域模型扩展

### 1. 增强的 SourceFile 模型

```java
package top.yumbo.ai.reviewer.domain.model;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 源文件领域模型（增强版）
 * 支持多种文件类型：代码、图片、视频、文档等
 */
@Data
@Builder
@Slf4j
public class SourceFile {
    
    // 基础信息
    private String fileName;
    private Path path;
    private Path relativePath;
    private long size;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    
    // 文件分类
    private FileCategory category;
    private FileType fileType;
    private String mimeType;
    private ProjectType projectType; // 仅代码文件有效
    
    // 内容信息
    private String content;          // 文本内容（代码、文档）
    private byte[] binaryContent;    // 二进制内容（图片、视频）
    
    // 元数据
    private FileMetadata metadata;
    
    // 分析结果（按需加载）
    private transient Object analysisResult; // 多态：CodeInsight, ImageAnalysis, etc.
    
    /**
     * 文件类别（一级分类）
     */
    public enum FileCategory {
        CODE("代码文件", true),
        DOCUMENT("文档文件", true),
        IMAGE("图片文件", false),
        VIDEO("视频文件", false),
        AUDIO("音频文件", false),
        DATA("数据文件", true),
        CONFIG("配置文件", true),
        ARCHIVE("压缩文件", false),
        OTHER("其他文件", false);
        
        private final String displayName;
        private final boolean textBased;
        
        FileCategory(String displayName, boolean textBased) {
            this.displayName = displayName;
            this.textBased = textBased;
        }
        
        public boolean isTextBased() {
            return textBased;
        }
    }
    
    /**
     * 文件类型（二级分类 - 详细）
     */
    @lombok.Getter
    public enum FileType {
        // 代码类
        JAVA("java", FileCategory.CODE, "text/x-java"),
        PYTHON("python", FileCategory.CODE, "text/x-python"),
        JAVASCRIPT("javascript", FileCategory.CODE, "text/javascript"),
        TYPESCRIPT("typescript", FileCategory.CODE, "text/typescript"),
        GO("go", FileCategory.CODE, "text/x-go"),
        RUST("rust", FileCategory.CODE, "text/x-rust"),
        CPP("c++", FileCategory.CODE, "text/x-c++src"),
        C("c", FileCategory.CODE, "text/x-csrc"),
        CSHARP("c#", FileCategory.CODE, "text/x-csharp"),
        
        // 文档类
        PDF("pdf", FileCategory.DOCUMENT, "application/pdf"),
        WORD("word", FileCategory.DOCUMENT, "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
        EXCEL("excel", FileCategory.DOCUMENT, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        POWERPOINT("powerpoint", FileCategory.DOCUMENT, "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
        MARKDOWN("markdown", FileCategory.DOCUMENT, "text/markdown"),
        TEXT("text", FileCategory.DOCUMENT, "text/plain"),
        RTF("rtf", FileCategory.DOCUMENT, "application/rtf"),
        
        // 图片类
        JPEG("jpeg", FileCategory.IMAGE, "image/jpeg"),
        PNG("png", FileCategory.IMAGE, "image/png"),
        GIF("gif", FileCategory.IMAGE, "image/gif"),
        SVG("svg", FileCategory.IMAGE, "image/svg+xml"),
        WEBP("webp", FileCategory.IMAGE, "image/webp"),
        BMP("bmp", FileCategory.IMAGE, "image/bmp"),
        TIFF("tiff", FileCategory.IMAGE, "image/tiff"),
        
        // 视频类
        MP4("mp4", FileCategory.VIDEO, "video/mp4"),
        AVI("avi", FileCategory.VIDEO, "video/x-msvideo"),
        MOV("mov", FileCategory.VIDEO, "video/quicktime"),
        MKV("mkv", FileCategory.VIDEO, "video/x-matroska"),
        WEBM("webm", FileCategory.VIDEO, "video/webm"),
        FLV("flv", FileCategory.VIDEO, "video/x-flv"),
        
        // 音频类
        MP3("mp3", FileCategory.AUDIO, "audio/mpeg"),
        WAV("wav", FileCategory.AUDIO, "audio/wav"),
        FLAC("flac", FileCategory.AUDIO, "audio/flac"),
        OGG("ogg", FileCategory.AUDIO, "audio/ogg"),
        M4A("m4a", FileCategory.AUDIO, "audio/mp4"),
        
        // 数据类
        JSON("json", FileCategory.DATA, "application/json"),
        XML("xml", FileCategory.DATA, "application/xml"),
        CSV("csv", FileCategory.DATA, "text/csv"),
        YAML("yaml", FileCategory.DATA, "application/x-yaml"),
        SQL("sql", FileCategory.DATA, "application/sql"),
        
        // 配置类
        PROPERTIES("properties", FileCategory.CONFIG, "text/x-java-properties"),
        INI("ini", FileCategory.CONFIG, "text/plain"),
        TOML("toml", FileCategory.CONFIG, "application/toml"),
        ENV("env", FileCategory.CONFIG, "text/plain"),
        
        // 压缩类
        ZIP("zip", FileCategory.ARCHIVE, "application/zip"),
        TAR("tar", FileCategory.ARCHIVE, "application/x-tar"),
        GZ("gzip", FileCategory.ARCHIVE, "application/gzip"),
        RAR("rar", FileCategory.ARCHIVE, "application/vnd.rar"),
        SEVEN_Z("7z", FileCategory.ARCHIVE, "application/x-7z-compressed"),
        
        // 其他
        UNKNOWN("unknown", FileCategory.OTHER, "application/octet-stream");
        
        private final String name;
        private final FileCategory category;
        private final String mimeType;
        
        FileType(String name, FileCategory category, String mimeType) {
            this.name = name;
            this.category = category;
            this.mimeType = mimeType;
        }
        
        public static FileType fromExtension(String extension) {
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
                case "ppt", "pptx" -> POWERPOINT;
                case "md", "markdown" -> MARKDOWN;
                case "txt" -> TEXT;
                case "rtf" -> RTF;
                
                // 图片
                case "jpg", "jpeg" -> JPEG;
                case "png" -> PNG;
                case "gif" -> GIF;
                case "svg" -> SVG;
                case "webp" -> WEBP;
                case "bmp" -> BMP;
                case "tif", "tiff" -> TIFF;
                
                // 视频
                case "mp4" -> MP4;
                case "avi" -> AVI;
                case "mov" -> MOV;
                case "mkv" -> MKV;
                case "webm" -> WEBM;
                case "flv" -> FLV;
                
                // 音频
                case "mp3" -> MP3;
                case "wav" -> WAV;
                case "flac" -> FLAC;
                case "ogg" -> OGG;
                case "m4a" -> M4A;
                
                // 数据
                case "json" -> JSON;
                case "xml" -> XML;
                case "csv" -> CSV;
                case "yaml", "yml" -> YAML;
                case "sql" -> SQL;
                
                // 配置
                case "properties" -> PROPERTIES;
                case "ini" -> INI;
                case "toml" -> TOML;
                case "env" -> ENV;
                
                // 压缩
                case "zip" -> ZIP;
                case "tar" -> TAR;
                case "gz", "gzip" -> GZ;
                case "rar" -> RAR;
                case "7z" -> SEVEN_Z;
                
                default -> UNKNOWN;
            };
        }
    }
    
    /**
     * 文件元数据（可扩展）
     */
    @Data
    @Builder
    public static class FileMetadata {
        // 通用元数据
        private String encoding;
        private Integer lineCount;
        private String checksum;
        
        // 代码文件元数据
        private Integer codeLines;
        private Integer commentLines;
        private Integer blankLines;
        private Double complexity;
        
        // 图片元数据
        private ImageMetadata imageMetadata;
        
        // 视频元数据
        private VideoMetadata videoMetadata;
        
        // 文档元数据
        private DocumentMetadata documentMetadata;
        
        // 自定义元数据
        private Map<String, Object> customMetadata;
    }
    
    /**
     * 图片元数据
     */
    @Data
    @Builder
    public static class ImageMetadata {
        private Integer width;
        private Integer height;
        private String format;
        private Integer bitDepth;
        private String colorSpace;
        private Boolean hasAlpha;
        private Long fileSize;
        private Double aspectRatio;
        private String cameraMake;
        private String cameraModel;
        private LocalDateTime captureTime;
        private String gpsLocation;
    }
    
    /**
     * 视频元数据
     */
    @Data
    @Builder
    public static class VideoMetadata {
        private Integer width;
        private Integer height;
        private Double duration;        // 秒
        private Integer frameRate;      // FPS
        private Long bitrate;           // bps
        private String codec;
        private String audioCodec;
        private Integer audioChannels;
        private Integer audioSampleRate;
        private Long fileSize;
    }
    
    /**
     * 文档元数据
     */
    @Data
    @Builder
    public static class DocumentMetadata {
        private String title;
        private String author;
        private String subject;
        private String keywords;
        private LocalDateTime createdDate;
        private LocalDateTime modifiedDate;
        private Integer pageCount;
        private Integer wordCount;
        private Integer characterCount;
        private String language;
        private String application;  // 创建应用
    }
    
    // 便捷方法
    
    public boolean isCode() {
        return category == FileCategory.CODE;
    }
    
    public boolean isImage() {
        return category == FileCategory.IMAGE;
    }
    
    public boolean isVideo() {
        return category == FileCategory.VIDEO;
    }
    
    public boolean isDocument() {
        return category == FileCategory.DOCUMENT;
    }
    
    public boolean isTextBased() {
        return category != null && category.isTextBased();
    }
    
    public boolean isBinary() {
        return !isTextBased();
    }
    
    /**
     * 自动检测文件类型
     */
    public static FileType detectFileType(Path filePath) {
        String fileName = filePath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            String extension = fileName.substring(dotIndex + 1);
            return FileType.fromExtension(extension);
        }
        
        return FileType.UNKNOWN;
    }
}
```

---

## 🖼️ 图片处理模块设计

### ImageProcessingStrategy 实现

```java
package top.yumbo.ai.reviewer.adapter.input.media;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.reviewer.domain.model.SourceFile;
import top.yumbo.ai.reviewer.application.service.FileProcessingStrategy;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 图片文件处理策略
 */
@Slf4j
public class ImageProcessingStrategy implements FileProcessingStrategy {
    
    private final AIService aiService;
    private final ImageQualityConfig config;
    
    @Override
    public boolean supports(SourceFile file) {
        return file.getCategory() == SourceFile.FileCategory.IMAGE;
    }
    
    @Override
    public ProcessingResult process(SourceFile file) {
        log.info("处理图片文件: {}", file.getFileName());
        
        try {
            // 1. 提取基础元数据
            ImageMetadata metadata = extractMetadata(file);
            
            // 2. 质量评估
            ImageQuality quality = assessQuality(file, metadata);
            
            // 3. AI 图片理解（如果启用）
            ImageUnderstanding understanding = null;
            if (aiService.supportsVision()) {
                understanding = analyzeWithAI(file, metadata);
            }
            
            // 4. 生成处理结果
            return ProcessingResult.builder()
                .file(file)
                .metadata(metadata)
                .quality(quality)
                .understanding(understanding)
                .success(true)
                .build();
                
        } catch (Exception e) {
            log.error("图片处理失败: {}", file.getFileName(), e);
            return ProcessingResult.failure(file, e.getMessage());
        }
    }
    
    /**
     * 提取图片元数据
     */
    private ImageMetadata extractMetadata(SourceFile file) throws IOException {
        Path path = file.getPath();
        
        try (ImageInputStream iis = ImageIO.createImageInputStream(Files.newInputStream(path))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            
            if (!readers.hasNext()) {
                throw new IOException("无法读取图片: " + file.getFileName());
            }
            
            ImageReader reader = readers.next();
            reader.setInput(iis);
            
            // 基础信息
            int width = reader.getWidth(0);
            int height = reader.getHeight(0);
            String format = reader.getFormatName();
            
            // EXIF 信息（JPEG）
            IIOMetadata imageMetadata = reader.getImageMetadata(0);
            Map<String, String> exifData = extractExifData(imageMetadata);
            
            // 读取图片
            BufferedImage image = reader.read(0);
            
            return ImageMetadata.builder()
                .width(width)
                .height(height)
                .format(format)
                .aspectRatio((double) width / height)
                .colorDepth(image.getColorModel().getPixelSize())
                .hasAlpha(image.getColorModel().hasAlpha())
                .fileSize(Files.size(path))
                .exifData(exifData)
                .build();
                
        }
    }
    
    /**
     * 图片质量评估
     */
    private ImageQuality assessQuality(SourceFile file, ImageMetadata metadata) {
        ImageQuality.ImageQualityBuilder builder = ImageQuality.builder();
        
        int score = 100;
        List<String> issues = new ArrayList<>();
        
        // 1. 分辨率检查
        if (metadata.getWidth() < config.getMinWidth() || 
            metadata.getHeight() < config.getMinHeight()) {
            score -= 20;
            issues.add(String.format("分辨率过低 (%dx%d)", metadata.getWidth(), metadata.getHeight()));
        }
        
        if (metadata.getWidth() > config.getMaxWidth() || 
            metadata.getHeight() > config.getMaxHeight()) {
            score -= 10;
            issues.add(String.format("分辨率过高 (%dx%d)", metadata.getWidth(), metadata.getHeight()));
        }
        
        // 2. 文件大小检查
        long sizeMB = metadata.getFileSize() / 1024 / 1024;
        if (sizeMB > config.getMaxSizeMB()) {
            score -= 15;
            issues.add(String.format("文件过大 (%dMB)", sizeMB));
        }
        
        // 3. 格式检查
        if (!config.getSupportedFormats().contains(metadata.getFormat().toLowerCase())) {
            score -= 10;
            issues.add("不推荐的图片格式: " + metadata.getFormat());
        }
        
        // 4. 宽高比检查
        double ratio = metadata.getAspectRatio();
        if (ratio < 0.5 || ratio > 2.0) {
            score -= 5;
            issues.add(String.format("异常的宽高比: %.2f", ratio));
        }
        
        // 计算等级
        String grade = calculateGrade(score);
        
        return builder
            .score(score)
            .grade(grade)
            .issues(issues)
            .isValid(score >= config.getMinAcceptableScore())
            .build();
    }
    
    /**
     * AI 图片理解
     */
    private ImageUnderstanding analyzeWithAI(SourceFile file, ImageMetadata metadata) {
        log.info("使用 AI 分析图片: {}", file.getFileName());
        
        try {
            // 构建提示词
            String prompt = buildImageAnalysisPrompt(file, metadata);
            
            // 调用 AI 服务（Vision API）
            String analysis = aiService.analyzeImage(file.getPath(), prompt);
            
            // 解析 AI 响应
            return parseAIResponse(analysis);
            
        } catch (Exception e) {
            log.warn("AI 图片分析失败: {}", file.getFileName(), e);
            return ImageUnderstanding.empty();
        }
    }
    
    /**
     * 构建图片分析提示词
     */
    private String buildImageAnalysisPrompt(SourceFile file, ImageMetadata metadata) {
        return String.format("""
            请分析这张图片并提供以下信息：
            
            1. **内容描述**: 图片中包含什么内容？
            2. **主题**: 图片的主要主题是什么？
            3. **质量评估**: 图片的清晰度、构图、色彩如何？
            4. **用途建议**: 这张图片适合用于什么场景？
            5. **改进建议**: 如何改进这张图片？
            
            图片信息:
            - 文件名: %s
            - 分辨率: %dx%d
            - 格式: %s
            - 大小: %.2f MB
            
            请以 JSON 格式返回分析结果。
            """,
            file.getFileName(),
            metadata.getWidth(),
            metadata.getHeight(),
            metadata.getFormat(),
            metadata.getFileSize() / 1024.0 / 1024.0
        );
    }
    
    private String calculateGrade(int score) {
        if (score >= 90) return "A+";
        if (score >= 80) return "A";
        if (score >= 70) return "B";
        if (score >= 60) return "C";
        return "D";
    }
    
    @Data
    @Builder
    public static class ImageMetadata {
        private Integer width;
        private Integer height;
        private String format;
        private Double aspectRatio;
        private Integer colorDepth;
        private Boolean hasAlpha;
        private Long fileSize;
        private Map<String, String> exifData;
    }
    
    @Data
    @Builder
    public static class ImageQuality {
        private Integer score;
        private String grade;
        private Boolean isValid;
        private List<String> issues;
        private Map<String, Object> details;
    }
    
    @Data
    @Builder
    public static class ImageUnderstanding {
        private String description;
        private String theme;
        private List<String> objects;      // 检测到的对象
        private List<String> colors;       // 主要颜色
        private String qualityAssessment;
        private String usageSuggestion;
        private List<String> improvements;
        private Double confidenceScore;
        
        public static ImageUnderstanding empty() {
            return ImageUnderstanding.builder()
                .description("N/A")
                .confidenceScore(0.0)
                .build();
        }
    }
    
    @Data
    @Builder
    public static class ImageQualityConfig {
        @Builder.Default
        private Integer minWidth = 800;
        
        @Builder.Default
        private Integer minHeight = 600;
        
        @Builder.Default
        private Integer maxWidth = 4096;
        
        @Builder.Default
        private Integer maxHeight = 4096;
        
        @Builder.Default
        private Integer maxSizeMB = 10;
        
        @Builder.Default
        private Integer minAcceptableScore = 60;
        
        @Builder.Default
        private Set<String> supportedFormats = Set.of("jpg", "jpeg", "png", "gif", "webp");
    }
}
```

---

## 📹 视频处理模块设计

### VideoProcessingStrategy 实现

```java
package top.yumbo.ai.reviewer.adapter.input.media;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.reviewer.domain.model.SourceFile;
import top.yumbo.ai.reviewer.application.service.FileProcessingStrategy;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.*;

/**
 * 视频文件处理策略
 * 使用 JavaCV (FFmpeg) 进行视频处理
 */
@Slf4j
public class VideoProcessingStrategy implements FileProcessingStrategy {
    
    private final AIService aiService;
    private final VideoQualityConfig config;
    
    @Override
    public boolean supports(SourceFile file) {
        return file.getCategory() == SourceFile.FileCategory.VIDEO;
    }
    
    @Override
    public ProcessingResult process(SourceFile file) {
        log.info("处理视频文件: {}", file.getFileName());
        
        try {
            // 1. 提取元数据
            VideoMetadata metadata = extractMetadata(file);
            
            // 2. 提取关键帧
            List<VideoFrame> keyFrames = extractKeyFrames(file, metadata);
            
            // 3. 质量评估
            VideoQuality quality = assessQuality(metadata, keyFrames);
            
            // 4. AI 视频理解（如果启用）
            VideoUnderstanding understanding = null;
            if (aiService.supportsVideo()) {
                understanding = analyzeWithAI(file, metadata, keyFrames);
            }
            
            return ProcessingResult.builder()
                .file(file)
                .metadata(metadata)
                .keyFrames(keyFrames)
                .quality(quality)
                .understanding(understanding)
                .success(true)
                .build();
                
        } catch (Exception e) {
            log.error("视频处理失败: {}", file.getFileName(), e);
            return ProcessingResult.failure(file, e.getMessage());
        }
    }
    
    /**
     * 提取视频元数据
     */
    private VideoMetadata extractMetadata(SourceFile file) throws Exception {
        Path path = file.getPath();
        
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(path.toFile())) {
            grabber.start();
            
            return VideoMetadata.builder()
                .width(grabber.getImageWidth())
                .height(grabber.getImageHeight())
                .duration(grabber.getLengthInTime() / 1000000.0) // 微秒转秒
                .frameRate(grabber.getFrameRate())
                .bitrate(grabber.getVideoBitrate())
                .videoCodec(grabber.getVideoCodecName())
                .audioCodec(grabber.getAudioCodecName())
                .audioChannels(grabber.getAudioChannels())
                .format(grabber.getFormat())
                .totalFrames(grabber.getLengthInFrames())
                .build();
        }
    }
    
    /**
     * 提取关键帧
     */
    private List<VideoFrame> extractKeyFrames(SourceFile file, VideoMetadata metadata) throws Exception {
        Path path = file.getPath();
        List<VideoFrame> keyFrames = new ArrayList<>();
        
        int numFrames = config.getKeyFrameCount();
        double duration = metadata.getDuration();
        double interval = duration / (numFrames + 1);
        
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(path.toFile())) {
            grabber.start();
            
            Java2DFrameConverter converter = new Java2DFrameConverter();
            
            for (int i = 1; i <= numFrames; i++) {
                double timestamp = interval * i;
                long timestampMicros = (long) (timestamp * 1000000);
                
                grabber.setTimestamp(timestampMicros);
                Frame frame = grabber.grabImage();
                
                if (frame != null) {
                    BufferedImage image = converter.convert(frame);
                    
                    keyFrames.add(VideoFrame.builder()
                        .frameNumber(grabber.getFrameNumber())
                        .timestamp(timestamp)
                        .image(image)
                        .build());
                }
            }
            
            converter.close();
        }
        
        log.info("提取了 {} 个关键帧", keyFrames.size());
        return keyFrames;
    }
    
    /**
     * 视频质量评估
     */
    private VideoQuality assessQuality(VideoMetadata metadata, List<VideoFrame> keyFrames) {
        int score = 100;
        List<String> issues = new ArrayList<>();
        
        // 1. 分辨率检查
        if (metadata.getWidth() < config.getMinWidth() || 
            metadata.getHeight() < config.getMinHeight()) {
            score -= 20;
            issues.add(String.format("分辨率过低 (%dx%d)", metadata.getWidth(), metadata.getHeight()));
        }
        
        // 2. 时长检查
        if (metadata.getDuration() < config.getMinDuration()) {
            score -= 15;
            issues.add(String.format("视频过短 (%.1f秒)", metadata.getDuration()));
        }
        
        if (metadata.getDuration() > config.getMaxDuration()) {
            score -= 10;
            issues.add(String.format("视频过长 (%.1f秒)", metadata.getDuration()));
        }
        
        // 3. 帧率检查
        if (metadata.getFrameRate() < config.getMinFrameRate()) {
            score -= 10;
            issues.add(String.format("帧率过低 (%.1f fps)", metadata.getFrameRate()));
        }
        
        // 4. 编码格式检查
        if (!config.getSupportedCodecs().contains(metadata.getVideoCodec())) {
            score -= 5;
            issues.add("不推荐的编码格式: " + metadata.getVideoCodec());
        }
        
        String grade = calculateGrade(score);
        
        return VideoQuality.builder()
            .score(score)
            .grade(grade)
            .issues(issues)
            .isValid(score >= config.getMinAcceptableScore())
            .build();
    }
    
    /**
     * AI 视频理解
     */
    private VideoUnderstanding analyzeWithAI(SourceFile file, VideoMetadata metadata, List<VideoFrame> keyFrames) {
        log.info("使用 AI 分析视频: {}", file.getFileName());
        
        try {
            String prompt = buildVideoAnalysisPrompt(file, metadata);
            
            // 分析关键帧
            List<String> frameAnalyses = new ArrayList<>();
            for (VideoFrame frame : keyFrames) {
                String frameAnalysis = aiService.analyzeImage(frame.getImage(), 
                    "描述这一帧的内容（时间: " + String.format("%.1f", frame.getTimestamp()) + "秒）");
                frameAnalyses.add(frameAnalysis);
            }
            
            // 综合分析
            String overallAnalysis = aiService.analyzeText(prompt + "\n\n关键帧分析:\n" + 
                String.join("\n", frameAnalyses));
            
            return parseVideoAnalysis(overallAnalysis);
            
        } catch (Exception e) {
            log.warn("AI 视频分析失败: {}", file.getFileName(), e);
            return VideoUnderstanding.empty();
        }
    }
    
    private String buildVideoAnalysisPrompt(SourceFile file, VideoMetadata metadata) {
        return String.format("""
            请分析这个视频并提供以下信息：
            
            1. **内容摘要**: 视频的主要内容是什么？
            2. **场景分析**: 视频包含哪些场景？
            3. **质量评估**: 视频的画质、流畅度如何？
            4. **音频评估**: 音频质量如何？
            5. **用途建议**: 这个视频适合用于什么场景？
            6. **改进建议**: 如何改进这个视频？
            
            视频信息:
            - 文件名: %s
            - 分辨率: %dx%d
            - 时长: %.1f 秒
            - 帧率: %.1f fps
            - 编码: %s
            
            请以 JSON 格式返回分析结果。
            """,
            file.getFileName(),
            metadata.getWidth(),
            metadata.getHeight(),
            metadata.getDuration(),
            metadata.getFrameRate(),
            metadata.getVideoCodec()
        );
    }
    
    @Data
    @Builder
    public static class VideoMetadata {
        private Integer width;
        private Integer height;
        private Double duration;
        private Double frameRate;
        private Long bitrate;
        private String videoCodec;
        private String audioCodec;
        private Integer audioChannels;
        private String format;
        private Integer totalFrames;
    }
    
    @Data
    @Builder
    public static class VideoFrame {
        private Integer frameNumber;
        private Double timestamp;
        private BufferedImage image;
    }
    
    @Data
    @Builder
    public static class VideoQuality {
        private Integer score;
        private String grade;
        private Boolean isValid;
        private List<String> issues;
    }
    
    @Data
    @Builder
    public static class VideoUnderstanding {
        private String summary;
        private List<String> scenes;
        private String videoQuality;
        private String audioQuality;
        private String usageSuggestion;
        private List<String> improvements;
        
        public static VideoUnderstanding empty() {
            return VideoUnderstanding.builder()
                .summary("N/A")
                .build();
        }
    }
    
    @Data
    @Builder
    public static class VideoQualityConfig {
        @Builder.Default
        private Integer minWidth = 720;
        
        @Builder.Default
        private Integer minHeight = 480;
        
        @Builder.Default
        private Double minDuration = 10.0;
        
        @Builder.Default
        private Double maxDuration = 600.0;
        
        @Builder.Default
        private Double minFrameRate = 24.0;
        
        @Builder.Default
        private Integer minAcceptableScore = 60;
        
        @Builder.Default
        private Integer keyFrameCount = 10;
        
        @Builder.Default
        private Set<String> supportedCodecs = Set.of("h264", "h265", "vp8", "vp9");
    }
}
```

---

**报告结束 - 第4部分**

继续阅读：
- 《第5部分：文档处理与 AI 引擎未来演进路线图》

