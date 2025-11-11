package top.yumbo.ai.reviewer.analyzer;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.reviewer.config.Config;
import top.yumbo.ai.reviewer.entity.FileChunk;
import top.yumbo.ai.reviewer.entity.SourceFile;
import top.yumbo.ai.reviewer.exception.AnalysisException;
import top.yumbo.ai.reviewer.util.FileUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 代码分块器
 * 
 * 负责将源文件智能分块，以便AI分析
 */
@Slf4j
public class ChunkSplitter {

    private final Config config;

    public ChunkSplitter(Config config) {
        this.config = config;
    }

    /**
     * 将源文件分块
     * 
     * @param sourceFiles 源文件列表
     * @return 文件块列表
     * @throws AnalysisException 如果分块过程中发生错误
     */
    public List<FileChunk> split(List<SourceFile> sourceFiles) throws AnalysisException {
        List<FileChunk> chunks = new ArrayList<>();

        for (SourceFile sourceFile : sourceFiles) {
            if (sourceFile.getTokenCount() <= config.getChunkSize()) {
                // 小文件直接作为一个块
                chunks.add(createChunk(sourceFile, sourceFile.getContent(), 0));
            } else {
                // 大文件需要拆分
                chunks.addAll(splitLargeFile(sourceFile));
            }
        }

        log.info("文件分块完成，共生成 {} 个块", chunks.size());
        return chunks;
    }

    /**
     * 拆分大文件
     * 
     * @param sourceFile 源文件
     * @return 文件块列表
     */
    private List<FileChunk> splitLargeFile(SourceFile sourceFile) {
        List<FileChunk> chunks = new ArrayList<>();

        // 根据文件类型选择不同的拆分策略
        switch (sourceFile.getFileType()) {
            case JAVA:
                chunks.addAll(splitJavaFile(sourceFile));
                break;
            case PYTHON:
                chunks.addAll(splitPythonFile(sourceFile));
                break;
            case JAVASCRIPT:
            case TYPESCRIPT:
                chunks.addAll(splitJavaScriptFile(sourceFile));
                break;
            default:
                // 默认按行拆分
                chunks.addAll(splitByLines(sourceFile));
                break;
        }

        return chunks;
    }

    /**
     * 拆分Java文件
     * 
     * @param sourceFile 源文件
     * @return 文件块列表
     */
    private List<FileChunk> splitJavaFile(SourceFile sourceFile) {
        List<FileChunk> chunks = new ArrayList<>();

        // Java文件按类/方法拆分
        String content = sourceFile.getContent();

        // 匹配类定义（启用 DOTALL风格以允许跨行匹配）
        Pattern classPattern = Pattern.compile("(?s)(\\s*(?:public|private|protected)?\\s*(?:abstract|final)?\\s*class\\s+\\w+[^\\{]*\\{)");
        Matcher classMatcher = classPattern.matcher(content);

        int lastEnd = 0;
        int chunkIndex = 0;

        while (classMatcher.find()) {
            // 添加类定义之前的内容
            if (classMatcher.start() > lastEnd) {
                String beforeClass = content.substring(lastEnd, classMatcher.start());
                if (!beforeClass.trim().isEmpty()) {
                    chunks.add(createChunk(sourceFile, beforeClass, chunkIndex++));
                }
            }

            // 找到类的结束位置
            int classStart = classMatcher.start();
            int classEnd = findMatchingBrace(content, classMatcher.start());

            if (classEnd > classStart) {
                String classContent = content.substring(classStart, classEnd);

                // 如果类内容仍然太大，按方法拆分
                if (estimateTokens(classContent) > config.getChunkSize()) {
                    chunks.addAll(splitJavaClassByMethods(sourceFile, classContent, chunkIndex));
                    // update chunkIndex by number of added chunks
                    chunkIndex += Math.max(1, estimateTokens(classContent) / Math.max(1, config.getChunkSize()));
                } else {
                    chunks.add(createChunk(sourceFile, classContent, chunkIndex++));
                }

                lastEnd = classEnd;
            }
        }

        // 添加剩余内容
        if (lastEnd < content.length()) {
            String remaining = content.substring(lastEnd);
            if (!remaining.trim().isEmpty()) {
                chunks.add(createChunk(sourceFile, remaining, chunkIndex));
            }
        }

        return chunks;
    }

    /**
     * 按方法拆分Java类
     * 
     * @param sourceFile 源文件
     * @param classContent 类内容
     * @param startChunkIndex 起始块索引
     * @return 文件块列表
     */
    private List<FileChunk> splitJavaClassByMethods(SourceFile sourceFile, String classContent, int startChunkIndex) {
        List<FileChunk> chunks = new ArrayList<>();

        // 匹配方法定义（启用 DOTALL）
        Pattern methodPattern = Pattern.compile("(?s)(\\s*(?:public|private|protected)?\\s*(?:static)?\\s*(?:final)?\\s*(?:abstract)?\\s*\\w+\\s+\\w+\\s*\\([^)]*\\)\\s*(?:throws\\s+[^\\{]+)?\\s*\\{)");
        Matcher methodMatcher = methodPattern.matcher(classContent);

        int lastEnd = 0;
        int chunkIndex = startChunkIndex;

        while (methodMatcher.find()) {
            // 添加方法定义之前的内容
            if (methodMatcher.start() > lastEnd) {
                String beforeMethod = classContent.substring(lastEnd, methodMatcher.start());
                if (!beforeMethod.trim().isEmpty()) {
                    chunks.add(createChunk(sourceFile, beforeMethod, chunkIndex++));
                }
            }

            // 找到方法的结束位置
            int methodStart = methodMatcher.start();
            int methodEnd = findMatchingBrace(classContent, methodMatcher.start());

            if (methodEnd > methodStart) {
                String methodContent = classContent.substring(methodStart, methodEnd);
                chunks.add(createChunk(sourceFile, methodContent, chunkIndex++));
                lastEnd = methodEnd;
            }
        }

        // 添加剩余内容
        if (lastEnd < classContent.length()) {
            String remaining = classContent.substring(lastEnd);
            if (!remaining.trim().isEmpty()) {
                chunks.add(createChunk(sourceFile, remaining, chunkIndex));
            }
        }

        return chunks;
    }

    /**
     * 拆分Python文件
     * 
     * @param sourceFile 源文件
     * @return 文件块列表
     */
    private List<FileChunk> splitPythonFile(SourceFile sourceFile) {
        List<FileChunk> chunks = new ArrayList<>();

        // Python文件按函数/类拆分
        String content = sourceFile.getContent();
        String[] lines = content.split("\\R");

        StringBuilder currentChunk = new StringBuilder();
        int currentTokens = 0;
        int chunkIndex = 0;

        for (String line : lines) {
            // 检查是否是函数或类定义
            if (line.trim().matches("^(def |class )")) {
                // 如果当前块不为空，保存它
                if (currentChunk.length() > 0) {
                    chunks.add(createChunk(sourceFile, currentChunk.toString(), chunkIndex++));
                    currentChunk = new StringBuilder();
                    currentTokens = 0;
                }
            }

            // 添加当前行到块中
            currentChunk.append(line).append(System.lineSeparator());
            currentTokens += estimateTokens(line);

            // 如果块大小超过限制，保存它
            if (currentTokens >= config.getChunkSize()) {
                chunks.add(createChunk(sourceFile, currentChunk.toString(), chunkIndex++));
                currentChunk = new StringBuilder();
                currentTokens = 0;
            }
        }

        // 添加最后一块
        if (currentChunk.length() > 0) {
            chunks.add(createChunk(sourceFile, currentChunk.toString(), chunkIndex));
        }

        return chunks;
    }

    /**
     * 拆分JavaScript/TypeScript文件
     * 
     * @param sourceFile 源文件
     * @return 文件块列表
     */
    private List<FileChunk> splitJavaScriptFile(SourceFile sourceFile) {
        // JavaScript/TypeScript文件按函数/类拆分，与Python类似
        return splitPythonFile(sourceFile);
    }

    /**
     * 按行拆分文件
     * 
     * @param sourceFile 源文件
     * @return 文件块列表
     */
    private List<FileChunk> splitByLines(SourceFile sourceFile) {
        List<FileChunk> chunks = new ArrayList<>();

        String content = sourceFile.getContent();
        String[] lines = content.split("\\R");

        StringBuilder currentChunk = new StringBuilder();
        int currentTokens = 0;
        int chunkIndex = 0;

        for (String line : lines) {
            currentChunk.append(line).append(System.lineSeparator());
            currentTokens += estimateTokens(line);

            // 如果块大小超过限制，保存它
            if (currentTokens >= config.getChunkSize()) {
                chunks.add(createChunk(sourceFile, currentChunk.toString(), chunkIndex++));
                currentChunk = new StringBuilder();
                currentTokens = 0;
            }
        }

        // 添加最后一块
        if (currentChunk.length() > 0) {
            chunks.add(createChunk(sourceFile, currentChunk.toString(), chunkIndex));
        }

        return chunks;
    }

    /**
     * 创建文件块
     * 
     * @param sourceFile 源文件
     * @param content 块内容
     * @param index 块索引
     * @return 文件块
     */
    private FileChunk createChunk(SourceFile sourceFile, String content, int index) {
        return FileChunk.builder()
                .sourceFile(sourceFile)
                .content(content)
                .index(index)
                .tokenCount(estimateTokens(content))
                .build();
    }

    /**
     * 查找匹配的大括号
     * 
     * @param content 内容
     * @param start 起始位置
     * @return 匹配的大括号位置
     */
    private int findMatchingBrace(String content, int start) {
        int braceCount = 0;
        boolean inString = false;
        boolean inChar = false;
        boolean escape = false;

        for (int i = start; i < content.length(); i++) {
            char c = content.charAt(i);

            if (escape) {
                escape = false;
                continue;
            }

            if (c == '\\') {
                escape = true;
                continue;
            }

            if (c == '"' && !inChar) {
                inString = !inString;
                continue;
            }

            if (c == '\'' && !inString) {
                inChar = !inChar;
                continue;
            }

            if (!inString && !inChar) {
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        return i + 1; // 包含结束大括号
                    }
                }
            }
        }

        // 如果没有找到匹配的大括号，返回内容末尾
        return content.length();
    }

    /**
     * 估算内容的token数量
     * 
     * @param content 内容
     * @return 估算的token数量
     */
    private int estimateTokens(String content) {
        // 简单估算：1个token约等于4个字符
        return content.length() / 4;
    }
}
