package top.yumbo.ai.reviewer.scanner;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.reviewer.config.Config;
import top.yumbo.ai.reviewer.entity.SourceFile;
import top.yumbo.ai.reviewer.exception.AnalysisException;
import top.yumbo.ai.reviewer.util.FileUtil;
import top.yumbo.ai.reviewer.util.TokenEstimator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 文件扫描器
 * 
 * 负责扫描项目文件，过滤并收集需要分析的源文件
 */
@Slf4j
public class FileScanner {

    private final Config config;
    private final TokenEstimator tokenEstimator;

    public FileScanner(Config config) {
        this.config = config;
        this.tokenEstimator = new TokenEstimator();
    }

    /**
     * 扫描项目文件
     * 
     * @return 源文件列表
     * @throws AnalysisException 如果扫描过程中发生错误
     */
    public List<SourceFile> scan() throws AnalysisException {
        try {
            Path projectPath = config.getProjectPath();
            log.info("开始扫描项目文件: {}", projectPath.toAbsolutePath());

            // 生成项目结构树
            generateProjectTree(projectPath);

            // 扫描源文件
            List<SourceFile> sourceFiles = scanSourceFiles(projectPath);

            // 按优先级排序
            sourceFiles = prioritizeFiles(sourceFiles);

            // 应用 Top K 选择
            if (config.getTopK() > 0 && sourceFiles.size() > config.getTopK()) {
                sourceFiles = sourceFiles.subList(0, config.getTopK());
            }

            log.info("扫描完成，共找到 {} 个源文件", sourceFiles.size());
            return sourceFiles;

        } catch (IOException e) {
            throw new AnalysisException("扫描项目文件时发生错误", e);
        }
    }

    /**
     * 生成项目结构树
     * 
     * @param projectPath 项目路径
     * @throws IOException 如果生成项目结构树时发生错误
     */
    private void generateProjectTree(Path projectPath) throws IOException {
        Path outputPath = config.getOutputPath();
        Files.createDirectories(outputPath);

        Path treeFile = outputPath.resolve("project_structure.txt");

        try (Stream<String> lines = Files.walk(projectPath)
                .filter(path -> {
                    try {
                        return !Files.isHidden(path);
                    } catch (IOException e) {
                        return false;
                    }
                })
                .filter(path -> !shouldExclude(path))
                .sorted()
                .map(path -> formatTreePath(projectPath, path))
                .filter(line -> line != null)) {

            Files.write(treeFile, (Iterable<String>) lines::iterator);
            log.info("项目结构树已生成: {}", treeFile);
        }
    }

    /**
     * 扫描源文件
     * 
     * @param projectPath 项目路径
     * @return 源文件列表
     * @throws IOException 如果扫描源文件时发生错误
     */
    private List<SourceFile> scanSourceFiles(Path projectPath) throws IOException {
        try (Stream<Path> paths = Files.walk(projectPath)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        try {
                            return !Files.isHidden(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .filter(path -> !shouldExclude(path))
                    .filter(this::shouldInclude)
                    .map(path -> createSourceFile(projectPath, path))
                    .collect(Collectors.toList());
        }
    }

    /**
     * 判断路径是否应该被排除
     * 
     * @param path 文件路径
     * @return 如果应该被排除返回 true，否则返回 false
     */
    private boolean shouldExclude(Path path) {
        String pathStr = path.toString().replace("\\", "/");

        return config.getExcludePatterns().stream()
                .anyMatch(pattern -> pathStr.contains(pattern));
    }

    /**
     * 判断文件是否应该被包含
     * 
     * @param path 文件路径
     * @return 如果应该被包含返回 true，否则返回 false
     */
    private boolean shouldInclude(Path path) {
        String fileName = path.getFileName().toString();

        return config.getIncludePatterns().stream()
                .anyMatch(pattern -> FileUtil.matchesPattern(fileName, pattern));
    }

    /**
     * 创建源文件对象
     * 
     * @param projectPath 项目路径
     * @param filePath 文件路径
     * @return 源文件对象
     */
    private SourceFile createSourceFile(Path projectPath, Path filePath) {
        try {
            String relativePath = projectPath.relativize(filePath).toString();
            String content = Files.readString(filePath);
            int tokenCount = tokenEstimator.estimate(content);

            return SourceFile.builder()
                    .path(relativePath)
                    .absolutePath(filePath.toString())
                    .content(content)
                    .tokenCount(tokenCount)
                    .fileType(FileUtil.getFileType(filePath))
                    .build();

        } catch (IOException e) {
            log.warn("无法读取文件: {}", filePath, e);
            return SourceFile.builder()
                    .path(projectPath.relativize(filePath).toString())
                    .absolutePath(filePath.toString())
                    .content("")
                    .tokenCount(0)
                    .fileType(FileUtil.getFileType(filePath))
                    .build();
        }
    }

    /**
     * 格式化树路径
     * 
     * @param projectPath 项目路径
     * @param path 文件路径
     * @return 格式化后的路径字符串
     */
    private String formatTreePath(Path projectPath, Path path) {
        int depth = projectPath.relativize(path).getNameCount();
        if (depth > config.getTreeDepth()) {
            return null;
        }

        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            indent.append("│   ");
        }

        if (Files.isDirectory(path)) {
            return indent + "├── " + path.getFileName() + "/";
        } else {
            return indent + "├── " + path.getFileName();
        }
    }

    /**
     * 按优先级排序文件
     * 
     * @param sourceFiles 源文件列表
     * @return 排序后的源文件列表
     */
    private List<SourceFile> prioritizeFiles(List<SourceFile> sourceFiles) {
        return sourceFiles.stream()
                .sorted((a, b) -> {
                    // 优先级规则：
                    // 1. 入口文件（如 main.java, app.py）
                    // 2. 配置文件（如 pom.xml, package.json）
                    // 3. 核心模块（如 service, controller）
                    // 4. 工具类
                    // 5. 按文件大小（大文件优先）

                    int aPriority = calculatePriority(a);
                    int bPriority = calculatePriority(b);

                    if (aPriority != bPriority) {
                        return Integer.compare(bPriority, aPriority); // 高优先级在前
                    }

                    return Integer.compare(b.getTokenCount(), a.getTokenCount()); // 大文件在前
                })
                .collect(Collectors.toList());
    }

    /**
     * 计算文件优先级
     * 
     * @param sourceFile 源文件
     * @return 优先级分数
     */
    private int calculatePriority(SourceFile sourceFile) {
        String path = sourceFile.getPath().toLowerCase();

        // 入口文件
        if (path.contains("main.") || path.contains("app.") || path.contains("index.")) {
            return 100;
        }

        // 配置文件
        if (path.endsWith("pom.xml") || path.endsWith("package.json") || 
            path.endsWith("config.yml") || path.endsWith("config.yaml") ||
            path.endsWith("application.properties") || path.endsWith("application.yml")) {
            return 90;
        }

        // 核心模块
        if (path.contains("service") || path.contains("controller") || 
            path.contains("api") || path.contains("core")) {
            return 80;
        }

        // 数据模型
        if (path.contains("model") || path.contains("entity") || path.contains("dto")) {
            return 70;
        }

        // 工具类
        if (path.contains("util") || path.contains("helper")) {
            return 60;
        }

        // 默认优先级
        return 50;
    }
}
