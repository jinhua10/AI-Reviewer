package top.yumbo.ai.reviewer.analyzer;

import top.yumbo.ai.reviewer.entity.FileChunk;
import top.yumbo.ai.reviewer.entity.SourceFile;

import java.util.ArrayList;
import java.util.List;

/**
 * 代码分块器（简化版）
 * 职责：
 * 1. 将小文件合并成大块（提升效率）
 * 2. 将大文件拆分成小块（避免超过 Token 限制）
 */
public class ChunkSplitter {

    private final int maxChunkSize;  // 单个块的最大 Token 数

    public ChunkSplitter(int maxChunkSize) {
        this.maxChunkSize = maxChunkSize;
    }

    /**
     * 将文件列表分块
     */
    public List<FileChunk> split(List<SourceFile> files) {
        List<FileChunk> chunks = new ArrayList<>();
        List<SourceFile> currentBatch = new ArrayList<>();
        int currentTokens = 0;
        int chunkIndex = 0;

        for (SourceFile file : files) {
            int fileTokens = file.getEstimatedTokens();

            // 如果单个文件就超过限制，单独处理
            if (fileTokens > maxChunkSize) {
                // 先保存当前批次
                if (!currentBatch.isEmpty()) {
                    chunks.add(createChunk(currentBatch, chunkIndex++));
                    currentBatch = new ArrayList<>();
                    currentTokens = 0;
                }

                // 大文件分块（简化版：直接拆分内容）
                chunks.addAll(splitLargeFile(file, chunkIndex));
                chunkIndex += chunks.size();
            }
            // 如果加入当前文件会超过限制，先保存当前批次
            else if (currentTokens + fileTokens > maxChunkSize) {
                if (!currentBatch.isEmpty()) {
                    chunks.add(createChunk(currentBatch, chunkIndex++));
                }
                currentBatch = new ArrayList<>();
                currentBatch.add(file);
                currentTokens = fileTokens;
            }
            // 否则加入当前批次
            else {
                currentBatch.add(file);
                currentTokens += fileTokens;
            }
        }

        // 保存最后一个批次
        if (!currentBatch.isEmpty()) {
            chunks.add(createChunk(currentBatch, chunkIndex));
        }

        return chunks;
    }

    /**
     * 创建文件块
     */
    private FileChunk createChunk(List<SourceFile> files, int index) {
        StringBuilder content = new StringBuilder();
        int totalTokens = 0;

        for (SourceFile file : files) {
            content.append("=== ").append(file.getRelativePath(file.getPath().getParent()))
                   .append(" ===\n");
            content.append(file.getContent()).append("\n\n");
            totalTokens += file.getEstimatedTokens();
        }

        return new FileChunk(new ArrayList<>(files), content.toString(), totalTokens, index);
    }

    /**
     * 拆分大文件（简化版）
     * TODO: 实现更智能的按语义边界拆分
     */
    private List<FileChunk> splitLargeFile(SourceFile file, int startIndex) {
        List<FileChunk> chunks = new ArrayList<>();
        String content = file.getContent();
        String[] lines = content.split("\n");

        List<String> currentLines = new ArrayList<>();
        int currentTokens = 0;
        int chunkIndex = startIndex;

        for (String line : lines) {
            int lineTokens = line.length() / 4; // 简单估算

            if (currentTokens + lineTokens > maxChunkSize && !currentLines.isEmpty()) {
                // 创建一个块
                String chunkContent = String.join("\n", currentLines);
                FileChunk chunk = new FileChunk(
                    List.of(file),
                    chunkContent,
                    currentTokens,
                    chunkIndex++
                );
                chunks.add(chunk);

                currentLines = new ArrayList<>();
                currentTokens = 0;
            }

            currentLines.add(line);
            currentTokens += lineTokens;
        }

        // 保存最后一个块
        if (!currentLines.isEmpty()) {
            String chunkContent = String.join("\n", currentLines);
            FileChunk chunk = new FileChunk(
                List.of(file),
                chunkContent,
                currentTokens,
                chunkIndex
            );
            chunks.add(chunk);
        }

        return chunks;
    }
}

