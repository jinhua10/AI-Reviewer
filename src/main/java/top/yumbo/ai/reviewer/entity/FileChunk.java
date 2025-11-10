package top.yumbo.ai.reviewer.entity;

import java.util.List;

/**
 * 文件分块数据模型
 * 用于将大文件拆分成小块进行 AI 分析
 */
public class FileChunk {
    private final List<SourceFile> files;  // 包含的文件列表（合并小文件）
    private final String content;          // 合并后的内容
    private final int estimatedTokens;     // 估算的 Token 数
    private final int chunkIndex;          // 块索引

    public FileChunk(List<SourceFile> files, String content, int estimatedTokens, int chunkIndex) {
        this.files = files;
        this.content = content;
        this.estimatedTokens = estimatedTokens;
        this.chunkIndex = chunkIndex;
    }

    // Getters
    public List<SourceFile> getFiles() { return files; }
    public String getContent() { return content; }
    public int getEstimatedTokens() { return estimatedTokens; }
    public int getChunkIndex() { return chunkIndex; }

    public int getFileCount() {
        return files.size();
    }

    public boolean isSingleFile() {
        return files.size() == 1;
    }

    @Override
    public String toString() {
        return String.format("FileChunk{index=%d, files=%d, tokens=%d}",
            chunkIndex, files.size(), estimatedTokens);
    }
}

