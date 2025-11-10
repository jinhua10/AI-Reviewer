package top.yumbo.ai.reviewer.entity;

import java.nio.file.Path;

/**
 * 源文件数据模型
 * 简化版：只保留核心字段
 */
public class SourceFile {
    private final Path path;              // 文件路径
    private final String content;         // 文件内容
    private final FileType fileType;      // 文件类型
    private final long size;              // 文件大小（字节）
    private final int estimatedTokens;    // 估算的 Token 数

    public enum FileType {
        JAVA, PYTHON, JAVASCRIPT, TYPESCRIPT, GO, RUST,
        MARKDOWN, TEXT, JSON, YAML, XML,
        IMAGE, PDF, WORD,
        BINARY, UNKNOWN
    }

    public SourceFile(Path path, String content, FileType fileType, long size, int estimatedTokens) {
        this.path = path;
        this.content = content;
        this.fileType = fileType;
        this.size = size;
        this.estimatedTokens = estimatedTokens;
    }

    // Getters
    public Path getPath() { return path; }
    public String getContent() { return content; }
    public FileType getFileType() { return fileType; }
    public long getSize() { return size; }
    public int getEstimatedTokens() { return estimatedTokens; }

    public String getFileName() {
        return path.getFileName().toString();
    }

    public String getRelativePath(Path basePath) {
        return basePath.relativize(path).toString();
    }

    @Override
    public String toString() {
        return String.format("SourceFile{path=%s, type=%s, size=%d, tokens=%d}",
            path.getFileName(), fileType, size, estimatedTokens);
    }
}

