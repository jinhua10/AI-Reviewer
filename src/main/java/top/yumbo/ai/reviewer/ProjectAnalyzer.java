package top.yumbo.ai.reviewer;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.reviewer.config.Config;
import top.yumbo.ai.reviewer.entity.SourceFile;
import top.yumbo.ai.reviewer.scanner.FileScanner;
import top.yumbo.ai.reviewer.util.FileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 项目预处理工具
 * 
 * 负责在分析前对项目进行预处理，生成项目结构、文件清单等
 */
@Slf4j
public class ProjectAnalyzer {

    /**
     * 主入口方法
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 解析命令行参数
        Arguments arguments = parseArguments(args);

        // 创建配置
        Config config = Config.builder()
                .projectPath(arguments.rootPath)
                .outputDir(arguments.outDir)
                .treeDepth(arguments.treeDepth)
                .includeTests(arguments.includeTests)
                .topK(arguments.topK)
                .maxCharsPerBatch(arguments.maxCharsPerBatch)
                .snippetMaxLines(arguments.snippetMaxLines)
                .build();

        try {
            // 执行预处理
            preprocess(config, arguments.selectionFile);

            log.info("项目预处理完成");
        } catch (Exception e) {
            log.error("项目预处理失败", e);
            System.exit(1);
        }
    }

    /**
     * 执行预处理
     * 
     * @param config 配置
     * @param selectionFile 选择文件路径（可选）
     * @throws IOException 如果预处理过程中发生IO错误
     */
    private static void preprocess(Config config, String selectionFile) throws IOException {
        // 确保输出目录存在
        Path outputDir = config.getOutputPath();
        Files.createDirectories(outputDir);

        // 扫描项目文件
        FileScanner scanner = new FileScanner(config);
        List<SourceFile> sourceFiles = scanner.scan();

        // 如果提供了选择文件，只分析选定的文件
        if (selectionFile != null && !selectionFile.isEmpty()) {
            sourceFiles = filterBySelectionFile(sourceFiles, selectionFile);
        }

        // 生成文件清单
        generateFileList(outputDir, sourceFiles);

        // 生成Top K文件清单
        if (config.getTopK() > 0 && sourceFiles.size() > config.getTopK()) {
            generateTopKFileList(outputDir, sourceFiles, config.getTopK());
        }

        // 生成代码片段
        generateCodeSnippets(outputDir, sourceFiles, config.getSnippetMaxLines());

        // 生成分块
        generateBatches(outputDir, sourceFiles, config.getMaxCharsPerBatch());
    }

    /**
     * 根据选择文件过滤源文件
     * 
     * @param sourceFiles 源文件列表
     * @param selectionFile 选择文件路径
     * @return 过滤后的源文件列表
     * @throws IOException 如果读取选择文件时发生错误
     */
    private static List<SourceFile> filterBySelectionFile(List<SourceFile> sourceFiles, String selectionFile) throws IOException {
        List<String> selectedPaths = Files.readAllLines(Paths.get(selectionFile));

        return sourceFiles.stream()
                .filter(file -> selectedPaths.contains(file.getPath()))
                .collect(Collectors.toList());
    }

    /**
     * 生成文件清单
     * 
     * @param outputDir 输出目录
     * @param sourceFiles 源文件列表
     * @throws IOException 如果生成文件清单时发生错误
     */
    private static void generateFileList(Path outputDir, List<SourceFile> sourceFiles) throws IOException {
        Path fileList = outputDir.resolve("selected_files.txt");

        StringBuilder content = new StringBuilder();
        content.append("# 文件清单\n");
        content.append("# 格式: [优先级分数] [Token数量] [相对路径]\n\n");

        for (SourceFile file : sourceFiles) {
            int priority = calculatePriority(file);
            content.append(String.format("[%d] [%d] %s%n", priority, file.getTokenCount(), file.getPath()));
        }

        Files.writeString(fileList, content.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        log.info("文件清单已生成: {}", fileList);
    }

    /**
     * 生成Top K文件清单
     * 
     * @param outputDir 输出目录
     * @param sourceFiles 源文件列表
     * @param topK Top K数量
     * @throws IOException 如果生成Top K文件清单时发生错误
     */
    private static void generateTopKFileList(Path outputDir, List<SourceFile> sourceFiles, int topK) throws IOException {
        Path topKFileList = outputDir.resolve("top_k_selected.txt");

        // 按优先级排序
        List<SourceFile> topKFiles = sourceFiles.stream()
                .sorted((a, b) -> {
                    int aPriority = calculatePriority(a);
                    int bPriority = calculatePriority(b);

                    if (aPriority != bPriority) {
                        return Integer.compare(bPriority, aPriority); // 高优先级在前
                    }

                    return Integer.compare(b.getTokenCount(), a.getTokenCount()); // 大文件在前
                })
                .limit(topK)
                .collect(Collectors.toList());

        StringBuilder content = new StringBuilder();
        content.append("# Top K 文件清单\n");
        content.append("# 格式: [优先级分数] [Token数量] [相对路径]\n\n");

        for (SourceFile file : topKFiles) {
            int priority = calculatePriority(file);
            content.append(String.format("[%d] [%d] %s%n", priority, file.getTokenCount(), file.getPath()));
        }

        Files.writeString(topKFileList, content.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        log.info("Top K文件清单已生成: {}", topKFileList);
    }

    /**
     * 生成代码片段
     * 
     * @param outputDir 输出目录
     * @param sourceFiles 源文件列表
     * @param snippetMaxLines 代码片段最大行数
     * @throws IOException 如果生成代码片段时发生错误
     */
    private static void generateCodeSnippets(Path outputDir, List<SourceFile> sourceFiles, int snippetMaxLines) throws IOException {
        // 创建代码片段目录
        Path snippetsDir = outputDir.resolve("snippets");
        Files.createDirectories(snippetsDir);

        for (SourceFile file : sourceFiles) {
            // 生成代码片段文件名
            String fileName = file.getPath().replaceAll("[/\\\\]", "_") + ".txt";
            Path snippetFile = snippetsDir.resolve(fileName);

            // 提取代码片段
            String snippet = extractSnippet(file.getContent(), snippetMaxLines);

            // 写入代码片段
            Files.writeString(snippetFile, snippet, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }

        log.info("代码片段已生成到目录: {}", snippetsDir);
    }

    /**
     * 生成分块
     * 
     * @param outputDir 输出目录
     * @param sourceFiles 源文件列表
     * @param maxCharsPerBatch 每批最大字符数
     * @throws IOException 如果生成分块时发生错误
     */
    private static void generateBatches(Path outputDir, List<SourceFile> sourceFiles, int maxCharsPerBatch) throws IOException {
        // 创建分块目录
        Path batchesDir = outputDir.resolve("batches");
        Files.createDirectories(batchesDir);

        // 创建分块索引文件
        Path batchIndexPath = outputDir.resolve("batch_index.txt");
        StringBuilder indexContent = new StringBuilder();
        indexContent.append("# 分块索引\n");
        indexContent.append("# 格式: [批次号] [字符数] [估算Token数] [文件列表]\n\n");

        int batchIndex = 0;
        int currentSize = 0;
        StringBuilder batchContent = new StringBuilder();
        StringBuilder fileList = new StringBuilder();

        for (SourceFile file : sourceFiles) {
            String content = file.getContent();
            int fileSize = content.length();

            // 如果当前批次加上这个文件会超过限制，保存当前批次
            if (currentSize + fileSize > maxCharsPerBatch && currentSize > 0) {
                // 保存当前批次
                String batchFileName = String.format("batch_%03d.txt", batchIndex);
                Path batchFile = batchesDir.resolve(batchFileName);
                Files.writeString(batchFile, batchContent.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                // 添加到索引
                int estimatedTokens = currentSize / 4; // 简单估算
                indexContent.append(String.format("[%03d] [%d] [%d] %s%n", 
                        batchIndex, currentSize, estimatedTokens, fileList.toString()));

                // 重置
                batchContent = new StringBuilder();
                fileList = new StringBuilder();
                currentSize = 0;
                batchIndex++;
            }

            // 添加文件到当前批次
            batchContent.append("===== ").append(file.getPath()).append(" =====\n");
            batchContent.append(content).append("\n\n");

            if (fileList.length() > 0) {
                fileList.append(", ");
            }
            fileList.append(file.getPath());

            currentSize += fileSize;
        }

        // 保存最后一个批次
        if (currentSize > 0) {
            String batchFileName = String.format("batch_%03d.txt", batchIndex);
            Path batchFile = batchesDir.resolve(batchFileName);
            Files.writeString(batchFile, batchContent.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // 添加到索引
            int estimatedTokens = currentSize / 4; // 简单估算
            indexContent.append(String.format("[%03d] [%d] [%d] %s%n", 
                    batchIndex, currentSize, estimatedTokens, fileList.toString()));
        }

        // 写入索引文件
        Files.writeString(batchIndexPath, indexContent.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        log.info("分块已生成到目录: {}", batchesDir);
        log.info("分块索引已生成: {}", batchIndexPath);
    }

    /**
     * 提取代码片段
     * 
     * @param content 文件内容
     * @param maxLines 最大行数
     * @return 代码片段
     */
    private static String extractSnippet(String content, int maxLines) {
        String[] lines = content.split("\\n");

        if (lines.length <= maxLines) {
            return content;
        }

        StringBuilder snippet = new StringBuilder();
        for (int i = 0; i < maxLines; i++) {
            snippet.append(lines[i]).append("\n");
        }

        snippet.append("\n... (").append(lines.length - maxLines).append(" 行省略) ...\n");
        return snippet.toString();
    }

    /**
     * 计算文件优先级
     * 
     * @param file 源文件
     * @return 优先级分数
     */
    private static int calculatePriority(SourceFile file) {
        String path = file.getPath().toLowerCase();

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

    /**
     * 解析命令行参数
     * 
     * @param args 命令行参数
     * @return 解析后的参数对象
     */
    private static Arguments parseArguments(String[] args) {
        Arguments arguments = new Arguments();

        // 设置默认值
        arguments.rootPath = ".";
        arguments.outDir = "llm_output";
        arguments.maxCharsPerBatch = 100000;
        arguments.snippetMaxLines = 200;
        arguments.treeDepth = 4;
        arguments.includeTests = false;
        arguments.topK = -1;
        arguments.selectionFile = null;

        // 解析参数
        if (args.length > 0) {
            arguments.rootPath = args[0];
        }

        if (args.length > 1) {
            arguments.outDir = args[1];
        }

        if (args.length > 2) {
            arguments.maxCharsPerBatch = Integer.parseInt(args[2]);
        }

        if (args.length > 3) {
            arguments.snippetMaxLines = Integer.parseInt(args[3]);
        }

        if (args.length > 4) {
            arguments.treeDepth = Integer.parseInt(args[4]);
        }

        if (args.length > 5) {
            arguments.includeTests = Boolean.parseBoolean(args[5]);
        }

        if (args.length > 6) {
            arguments.topK = Integer.parseInt(args[6]);
        }

        if (args.length > 7) {
            arguments.selectionFile = args[7];
        }

        return arguments;
    }

    /**
     * 命令行参数封装类
     */
    private static class Arguments {
        String rootPath;
        String outDir;
        int maxCharsPerBatch;
        int snippetMaxLines;
        int treeDepth;
        boolean includeTests;
        int topK;
        String selectionFile;
    }
}
