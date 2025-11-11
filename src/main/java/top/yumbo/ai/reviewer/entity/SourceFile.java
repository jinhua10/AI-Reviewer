package top.yumbo.ai.reviewer.entity;

import lombok.Builder;
import lombok.Data;

/**
 * 源文件实体
 * 
 * 表示一个源代码文件及其相关信息
 */
@Data
@Builder
public class SourceFile {

    /**
     * 相对路径
     */
    private String path;

    /**
     * 绝对路径
     */
    private String absolutePath;

    /**
     * 文件内容
     */
    private String content;

    /**
     * Token数量
     */
    private int tokenCount;

    /**
     * 文件类型
     */
    private FileType fileType;

    /**
     * 文件类型枚举
     */
    public enum FileType {
        JAVA,
        PYTHON,
        JAVASCRIPT,
        TYPESCRIPT,
        GO,
        CPP,
        C,
        H,
        OTHER
    }
}
