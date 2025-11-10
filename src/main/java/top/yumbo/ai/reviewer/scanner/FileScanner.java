package top.yumbo.ai.reviewer.scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.yumbo.ai.reviewer.config.Config;
import top.yumbo.ai.reviewer.entity.SourceFile;
import top.yumbo.ai.reviewer.exception.AnalysisException;
import top.yumbo.ai.reviewer.util.FileUtil;
import top.yumbo.ai.reviewer.util.TokenEstimator;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件扫描器（简化版）
 * 职责：
 * 1. 递归扫描项目目录
 * 2. 过滤不需要分析的文件
 * 3. 识别文件类型
 * 4. 读取文件内容
 * 5. 估算 Token 数量
 */
public class FileScanner {

    private static final Logger log = LoggerFactory.getLogger(FileScanner.class);

    private final Config config;

    public FileScanner(Config config) {
        this.config = config;
    }

    /**
     * 扫描项目目录，返回需要分析的文件列表
     */
    public List<SourceFile> scan() throws AnalysisException {
        Path projectPath = config.getProjectPath();
        log.info("开始扫描项目: {}", projectPath);

        // 1. 递归扫描所有文件
        List<Path> allFiles = FileUtil.scanDirectory(projectPath);
        log.debug("扫描到 {} 个文件", allFiles.size());

        // 2. 过滤文件
        List<Path> filteredFiles = allFiles.stream()
            .filter(path -> !shouldIgnore(path))
            .filter(path -> shouldInclude(path))
            .collect(Collectors.toList());

        log.info("过滤后剩余 {} 个文件需要分析", filteredFiles.size());

        // 3. 转换为 SourceFile 对象
        List<SourceFile> sourceFiles = new ArrayList<>();
        for (Path path : filteredFiles) {
            try {
                SourceFile sourceFile = createSourceFile(path);
                if (sourceFile != null) {
                    sourceFiles.add(sourceFile);
                }
            } catch (Exception e) {
                log.warn("无法处理文件 {}: {}", path, e.getMessage());
            }
        }

        log.info("成功加载 {} 个源文件", sourceFiles.size());
        return sourceFiles;
    }

    /**
     * 判断文件是否应该被忽略
     */
    private boolean shouldIgnore(Path path) {
        // 检查是否是二进制文件
        if (FileUtil.isBinaryFile(path)) {
            return true;
        }

        // 检查是否在排除列表中
        if (FileUtil.shouldIgnore(path, config.getExcludePatterns())) {
            return true;
        }

        // 检查文件类型
        SourceFile.FileType fileType = FileUtil.detectFileType(path);
        if (fileType == SourceFile.FileType.BINARY ||
            fileType == SourceFile.FileType.UNKNOWN) {
            return true;
        }

        return false;
    }

    /**
     * 判断文件是否应该被包含
     */
    private boolean shouldInclude(Path path) {
        List<String> includePatterns = config.getIncludePatterns();

        // 如果没有指定包含规则，默认包含所有
        if (includePatterns == null || includePatterns.isEmpty()) {
            return true;
        }

        // 检查是否匹配包含规则
        String pathStr = path.toString();
        for (String pattern : includePatterns) {
            if (pathStr.contains(pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 创建 SourceFile 对象
     */
    private SourceFile createSourceFile(Path path) throws AnalysisException {
        // 读取文件内容
        String content = FileUtil.readFile(path);

        // 如果文件为空，跳过
        if (content == null || content.trim().isEmpty()) {
            return null;
        }

        // 识别文件类型
        SourceFile.FileType fileType = FileUtil.detectFileType(path);

        // 获取文件大小
        long size = FileUtil.getFileSize(path);

        // 估算 Token 数量
        int estimatedTokens = TokenEstimator.estimateForFile(content, path.toString());

        return new SourceFile(path, content, fileType, size, estimatedTokens);
    }

    /**
     * 获取扫描统计信息
     */
    public String getScanSummary(List<SourceFile> files) {
        int totalFiles = files.size();
        long totalSize = files.stream().mapToLong(SourceFile::getSize).sum();
        int totalTokens = files.stream().mapToInt(SourceFile::getEstimatedTokens).sum();

        return String.format(
            "扫描完成: %d 个文件, 总大小 %d KB, 估算 Token %d",
            totalFiles, totalSize / 1024, totalTokens
        );
    }
}

