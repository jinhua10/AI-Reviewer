package top.yumbo.ai.reviewer.adapter.output.filesystem;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.reviewer.application.port.output.FileSystemPort;
import top.yumbo.ai.reviewer.domain.model.SourceFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 本地文件系统适配器
 * 实现FileSystemPort接口，提供文件系统操作
 */
@Slf4j
public class LocalFileSystemAdapter implements FileSystemPort {

    private final FileSystemConfig config;

    public LocalFileSystemAdapter(FileSystemConfig config) {
        this.config = config;
    }

    @Override
    public List<SourceFile> scanProjectFiles(Path projectRoot) {
        log.info("扫描项目文件: {}", projectRoot);

        List<SourceFile> sourceFiles = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(projectRoot)) {
            paths.filter(Files::isRegularFile)
                    .filter(this::shouldIncludeFile)
                    .forEach(path -> {
                        try {
                            SourceFile sourceFile = buildSourceFile(path, projectRoot);
                            sourceFiles.add(sourceFile);
                        } catch (Exception e) {
                            log.warn("处理文件失败: {}", path, e);
                        }
                    });
        } catch (IOException e) {
            log.error("扫描项目文件失败", e);
            throw new RuntimeException("扫描项目文件失败", e);
        }

        log.info("扫描完成，共找到 {} 个文件", sourceFiles.size());
        return sourceFiles;
    }

    @Override
    public String readFileContent(Path filePath) {
        try {
            return Files.readString(filePath);
        } catch (IOException e) {
            log.error("读取文件失败: {}", filePath, e);
            throw new RuntimeException("读取文件失败: " + filePath, e);
        }
    }

    @Override
    public void writeFileContent(Path filePath, String content) {
        try {
            // 确保父目录存在
            Path parent = filePath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            Files.writeString(filePath, content);
            log.debug("文件写入成功: {}", filePath);
        } catch (IOException e) {
            log.error("写入文件失败: {}", filePath, e);
            throw new RuntimeException("写入文件失败: " + filePath, e);
        }
    }

    @Override
    public String generateProjectStructure(Path projectRoot) {
        log.info("生成项目结构树: {}", projectRoot);

        StringBuilder structure = new StringBuilder();
        structure.append(projectRoot.getFileName()).append("/\n");

        try {
            buildDirectoryTree(projectRoot, structure, "", config.maxDepth());
        } catch (IOException e) {
            log.error("生成项目结构失败", e);
            return "无法生成项目结构";
        }

        return structure.toString();
    }

    @Override
    public boolean fileExists(Path filePath) {
        return Files.exists(filePath) && Files.isRegularFile(filePath);
    }

    @Override
    public boolean directoryExists(Path dirPath) {
        return Files.exists(dirPath) && Files.isDirectory(dirPath);
    }

    @Override
    public void createDirectory(Path dirPath) {
        try {
            Files.createDirectories(dirPath);
            log.debug("目录创建成功: {}", dirPath);
        } catch (IOException e) {
            log.error("创建目录失败: {}", dirPath, e);
            throw new RuntimeException("创建目录失败: " + dirPath, e);
        }
    }

    /**
     * 判断是否应该包含文件
     */
    private boolean shouldIncludeFile(Path path) {
        String fileName = path.getFileName().toString();

        // 检查排除模式
        for (String excludePattern : config.excludePatterns()) {
            if (matchesPattern(fileName, excludePattern)) {
                return false;
            }
        }

        // 检查包含模式
        for (String includePattern : config.includePatterns()) {
            if (matchesPattern(fileName, includePattern)) {
                // 检查文件大小
                try {
                    long sizeKB = Files.size(path) / 1024;
                    if (sizeKB > config.maxFileSizeKB()) {
                        log.debug("跳过大文件: {} ({} KB)", path, sizeKB);
                        return false;
                    }
                    return true;
                } catch (IOException e) {
                    log.warn("无法获取文件大小: {}", path);
                    return false;
                }
            }
        }

        return false;
    }

    /**
     * 模式匹配
     */
    private boolean matchesPattern(String fileName, String pattern) {
        // 简单的通配符匹配
        if (pattern.startsWith("*") && pattern.endsWith("*")) {
            // *something* - contains pattern
            String middle = pattern.substring(1, pattern.length() - 1);
            return fileName.toLowerCase().contains(middle.toLowerCase());
        }
        if (pattern.startsWith("*")) {
            // *.ext - ends with pattern
            return fileName.toLowerCase().endsWith(pattern.substring(1).toLowerCase());
        }
        if (pattern.endsWith("*")) {
            // README* - starts with pattern
            return fileName.toUpperCase().startsWith(pattern.substring(0, pattern.length() - 1).toUpperCase());
        }
        // Exact match (case insensitive for README, Makefile, etc.)
        return fileName.equalsIgnoreCase(pattern);
    }

    /**
     * 构建源文件对象
     */
    private SourceFile buildSourceFile(Path path, Path projectRoot) throws IOException {
        String fileName = path.getFileName().toString();
        String extension = getFileExtension(fileName);
        String relativePath = projectRoot.relativize(path).toString();

        return SourceFile.builder()
                .path(path)
                .fileName(fileName)
                .extension(extension)
                .relativePath(relativePath)
                .lineCount(countLines(path))
                .sizeInBytes(Files.size(path))
                .isCore(isCoreFile(fileName, relativePath))
                .category(determineCategory(fileName, relativePath))
                .build();
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }

    /**
     * 统计文件行数
     */
    private int countLines(Path path) {
        try {
            return (int) Files.lines(path).count();
        } catch (IOException e) {
            log.warn("统计行数失败: {}", path);
            return 0;
        }
    }

    /**
     * 判断是否为核心文件
     */
    private boolean isCoreFile(String fileName, String relativePath) {
        // 入口文件
        if (fileName.toLowerCase().contains("main") ||
            fileName.toLowerCase().contains("application")) {
            return true;
        }

        // 配置文件
        if (fileName.endsWith(".xml") || fileName.endsWith(".yaml") ||
            fileName.endsWith(".yml") || fileName.endsWith(".properties")) {
            return true;
        }

        return false;
    }

    /**
     * 确定文件类别
     */
    private SourceFile.FileCategory determineCategory(String fileName, String relativePath) {
        if (fileName.toLowerCase().contains("main")) {
            return SourceFile.FileCategory.ENTRY_POINT;
        }
        if (fileName.endsWith(".xml") || fileName.endsWith(".yaml")) {
            return SourceFile.FileCategory.CONFIG;
        }
        if (relativePath.contains("test")) {
            return SourceFile.FileCategory.TEST;
        }
        if (fileName.toLowerCase().contains("util")) {
            return SourceFile.FileCategory.UTIL;
        }
        return SourceFile.FileCategory.CORE_BUSINESS;
    }

    /**
     * 构建目录树
     */
    private void buildDirectoryTree(Path dir, StringBuilder structure, String prefix, int maxDepth)
            throws IOException {
        if (maxDepth <= 0) return;

        List<Path> children = Files.list(dir).sorted().toList();

        for (int i = 0; i < children.size(); i++) {
            Path child = children.get(i);
            boolean isLast = (i == children.size() - 1);

            structure.append(prefix)
                    .append(isLast ? "└── " : "├── ")
                    .append(child.getFileName());

            if (Files.isDirectory(child)) {
                structure.append("/\n");
                String newPrefix = prefix + (isLast ? "    " : "│   ");
                buildDirectoryTree(child, structure, newPrefix, maxDepth - 1);
            } else {
                structure.append("\n");
            }
        }
    }

    /**
     * 文件系统配置
     */
    public record FileSystemConfig(
            List<String> includePatterns,
            List<String> excludePatterns,
            int maxFileSizeKB,
            int maxDepth
    ) {
        public FileSystemConfig {
            if (includePatterns == null) {
                includePatterns = List.of(
                    "*.java", "*.py", "*.js", "*.ts", "*.go", "*.rs", "*.cpp", "*.c", "*.h",
                    "*.md", "*.txt", "*.json", "*.yaml", "*.yml", "*.xml", "*.properties",
                    "README*", "Dockerfile", "Makefile", "pom.xml", "build.gradle", "package.json"
                );
            }
            if (excludePatterns == null) {
                excludePatterns = List.of("*test*", "*.class", "*.jar");
            }
            if (maxFileSizeKB <= 0) {
                maxFileSizeKB = 1024; // 默认1MB
            }
            if (maxDepth <= 0) {
                maxDepth = 4; // 默认4层
            }
        }
    }
}


