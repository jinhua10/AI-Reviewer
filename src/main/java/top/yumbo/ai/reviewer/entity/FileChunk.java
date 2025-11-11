package top.yumbo.ai.reviewer.entity;

import lombok.Builder;
import lombok.Data;

/**
 * 文件块实体
 * 
 * 表示源文件的一个分块
 */
@Data
@Builder
public class FileChunk {

    /**
     * 源文件
     */
    private SourceFile sourceFile;

    /**
     * 块内容
     */
    private String content;

    /**
     * 块索引
     */
    private int index;

    /**
     * Token数量
     */
    private int tokenCount;
}
