package top.yumbo.ai.rag.example;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.LocalFileRAG;
import top.yumbo.ai.rag.model.Document;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Excel知识库构建工具
 * 用于批量处理Excel文件并构建可持久化的知识库
 *
 * 功能：
 * 1. 递归扫描文件夹中的所有Excel文件
 * 2. 自动解析Excel内容
 * 3. 构建Lucene索引（持久化到磁盘）
 * 4. 支持增量更新
 * 5. 重启后自动加载已有索引
 */
@Slf4j
public class ExcelKnowledgeBuilder {

    private final LocalFileRAG rag;
    private final String excelFolderPath;
    private final Set<String> processedFiles = new HashSet<>();

    /**
     * 构造函数
     *
     * @param storagePath 知识库存储路径（持久化路径，重启后数据保留）
     * @param excelFolderPath Excel文件夹路径
     */
    public ExcelKnowledgeBuilder(String storagePath, String excelFolderPath) {
        this.excelFolderPath = excelFolderPath;

        // 创建LocalFileRAG实例
        // 注意：storagePath会持久化存储，重启后数据仍然存在
        this.rag = LocalFileRAG.builder()
            .storagePath(storagePath)  // 文档和索引的存储路径
            .enableCache(true)         // 启用缓存提升性能
            .enableCompression(true)   // 启用压缩节省空间
            .build();

        log.info("Excel知识库构建器已初始化");
        log.info("存储路径: {}", storagePath);
        log.info("Excel文件夹: {}", excelFolderPath);

        // 检查是否有已存在的索引
        checkExistingIndex();
    }

    /**
     * 检查已存在的索引
     */
    private void checkExistingIndex() {
        try {
            var stats = rag.getStatistics();
            if (stats.getDocumentCount() > 0) {
                log.info("发现已有知识库，文档数: {}, 索引数: {}",
                    stats.getDocumentCount(),
                    stats.getIndexedDocumentCount());
                log.info("知识库将在现有基础上增量更新");
            } else {
                log.info("这是一个新的知识库，将从零开始构建");
            }
        } catch (Exception e) {
            log.warn("无法获取统计信息: {}", e.getMessage());
        }
    }

    /**
     * 构建知识库（主方法）
     * 扫描Excel文件夹，解析所有Excel文件并建立索引
     */
    public BuildResult buildKnowledgeBase() {
        log.info("开始构建Excel知识库...");
        long startTime = System.currentTimeMillis();

        BuildResult result = new BuildResult();

        try {
            // 1. 扫描Excel文件
            List<File> excelFiles = scanExcelFiles();
            log.info("找到 {} 个Excel文件", excelFiles.size());
            result.totalFiles = excelFiles.size();

            // 2. 批量处理Excel文件
            for (File file : excelFiles) {
                try {
                    if (processExcelFile(file)) {
                        result.successCount++;
                    } else {
                        result.failedCount++;
                        result.failedFiles.add(file.getName());
                    }
                } catch (Exception e) {
                    log.error("处理文件失败: {}", file.getName(), e);
                    result.failedCount++;
                    result.failedFiles.add(file.getName() + " (错误: " + e.getMessage() + ")");
                }

                // 每处理10个文件提交一次
                if ((result.successCount + result.failedCount) % 10 == 0) {
                    rag.commit();
                    log.info("进度: {}/{}", result.successCount + result.failedCount, result.totalFiles);
                }
            }

            // 3. 最终提交
            rag.commit();

            // 4. 优化索引（提升查询性能）
            log.info("优化索引中...");
            rag.optimizeIndex();

            result.buildTimeMs = System.currentTimeMillis() - startTime;

            log.info("知识库构建完成！");
            log.info("总文件数: {}", result.totalFiles);
            log.info("成功: {}", result.successCount);
            log.info("失败: {}", result.failedCount);
            log.info("耗时: {} 秒", result.buildTimeMs / 1000.0);

        } catch (Exception e) {
            log.error("构建知识库失败", e);
            result.error = e.getMessage();
        }

        return result;
    }

    /**
     * 扫描Excel文件
     */
    private List<File> scanExcelFiles() throws IOException {
        List<File> excelFiles = new ArrayList<>();
        Path startPath = Paths.get(excelFolderPath);

        if (!Files.exists(startPath)) {
            log.warn("Excel文件夹不存在: {}", excelFolderPath);
            return excelFiles;
        }

        Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String fileName = file.getFileName().toString().toLowerCase();
                if (fileName.endsWith(".xls") || fileName.endsWith(".xlsx")) {
                    // 排除临时文件（以~$开头）
                    if (!fileName.startsWith("~$")) {
                        excelFiles.add(file.toFile());
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                log.warn("无法访问文件: {}", file, exc);
                return FileVisitResult.CONTINUE;
            }
        });

        return excelFiles;
    }

    /**
     * 处理单个Excel文件
     */
    private boolean processExcelFile(File file) {
        try {
            log.debug("处理文件: {}", file.getName());

            // 1. 读取文件内容
            byte[] bytes = Files.readAllBytes(file.toPath());

            // 2. 使用Tika解析Excel（框架已内置支持）
            // LocalFileRAG会自动调用TikaDocumentParser解析Excel
            String content = extractExcelContent(file);

            if (content == null || content.trim().isEmpty()) {
                log.warn("文件内容为空: {}", file.getName());
                return false;
            }

            // 3. 构建文档元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("fileName", file.getName());
            metadata.put("filePath", file.getAbsolutePath());
            metadata.put("fileSize", file.length());
            metadata.put("fileType", "excel");
            metadata.put("extension", getFileExtension(file.getName()));
            metadata.put("indexedAt", System.currentTimeMillis());

            // 可以添加更多自定义元数据
            // metadata.put("category", extractCategory(file));
            // metadata.put("department", extractDepartment(file));

            // 4. 创建文档并索引
            Document document = Document.builder()
                .title(file.getName())  // 使用文件名作为标题
                .content(content)       // Excel解析后的文本内容
                .metadata(metadata)     // 元数据
                .build();

            String docId = rag.index(document);

            processedFiles.add(file.getAbsolutePath());
            log.debug("文件已索引: {} -> {}", file.getName(), docId);

            return true;

        } catch (Exception e) {
            log.error("处理Excel文件失败: {}", file.getName(), e);
            return false;
        }
    }

    /**
     * 提取Excel内容
     * 使用框架内置的Tika解析器
     */
    private String extractExcelContent(File file) throws IOException {
        // LocalFileRAG使用Apache Tika自动解析Excel
        // Tika会将Excel的所有sheet内容提取为纯文本
        return new top.yumbo.ai.rag.impl.parser.TikaDocumentParser().parse(file);
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot) : "";
    }

    /**
     * 增量更新知识库
     * 只处理新增或修改的文件
     */
    public BuildResult incrementalUpdate() {
        log.info("开始增量更新知识库...");
        long startTime = System.currentTimeMillis();

        BuildResult result = new BuildResult();

        try {
            List<File> excelFiles = scanExcelFiles();
            result.totalFiles = excelFiles.size();

            for (File file : excelFiles) {
                // 检查文件是否已处理过
                if (!processedFiles.contains(file.getAbsolutePath())) {
                    try {
                        if (processExcelFile(file)) {
                            result.successCount++;
                        } else {
                            result.failedCount++;
                        }
                    } catch (Exception e) {
                        log.error("处理文件失败: {}", file.getName(), e);
                        result.failedCount++;
                    }
                }
            }

            rag.commit();
            result.buildTimeMs = System.currentTimeMillis() - startTime;

            log.info("增量更新完成，新增 {} 个文档", result.successCount);

        } catch (Exception e) {
            log.error("增量更新失败", e);
            result.error = e.getMessage();
        }

        return result;
    }

    /**
     * 获取知识库统计信息
     */
    public KnowledgeBaseStats getStats() {
        var stats = rag.getStatistics();

        KnowledgeBaseStats kbStats = new KnowledgeBaseStats();
        kbStats.totalDocuments = stats.getDocumentCount();
        kbStats.indexedDocuments = stats.getIndexedDocumentCount();
        kbStats.processedFiles = processedFiles.size();

        return kbStats;
    }

    /**
     * 关闭知识库
     * 注意：关闭后数据仍然保存在磁盘，下次启动会自动加载
     */
    public void close() {
        rag.close();
        log.info("知识库已关闭（数据已持久化到磁盘）");
    }

    /**
     * 主方法 - 演示如何使用
     */
    public static void main(String[] args) {
        // 配置路径
        String storagePath = "./data/excel-knowledge-base";  // 知识库存储路径（持久化）
        String excelFolder = "./excel-files";                // Excel文件夹路径

        // 从命令行参数读取路径（如果提供）
        if (args.length >= 2) {
            storagePath = args[0];
            excelFolder = args[1];
        }

        System.out.println("=".repeat(80));
        System.out.println("Excel知识库构建工具");
        System.out.println("=".repeat(80));
        System.out.println("知识库路径: " + storagePath);
        System.out.println("Excel文件夹: " + excelFolder);
        System.out.println("=".repeat(80));

        // 创建构建器
        ExcelKnowledgeBuilder builder = new ExcelKnowledgeBuilder(storagePath, excelFolder);

        try {
            // 构建知识库
            BuildResult result = builder.buildKnowledgeBase();

            // 显示结果
            System.out.println("\n" + "=".repeat(80));
            System.out.println("构建结果");
            System.out.println("=".repeat(80));
            System.out.println("总文件数: " + result.totalFiles);
            System.out.println("成功: " + result.successCount);
            System.out.println("失败: " + result.failedCount);
            System.out.println("耗时: " + result.buildTimeMs / 1000.0 + " 秒");

            if (!result.failedFiles.isEmpty()) {
                System.out.println("\n失败的文件:");
                result.failedFiles.forEach(f -> System.out.println("  - " + f));
            }

            // 显示统计
            KnowledgeBaseStats stats = builder.getStats();
            System.out.println("\n知识库统计:");
            System.out.println("  总文档数: " + stats.totalDocuments);
            System.out.println("  索引文档数: " + stats.indexedDocuments);
            System.out.println("  处理文件数: " + stats.processedFiles);

            System.out.println("\n知识库已持久化到: " + storagePath);
            System.out.println("重启程序后，知识库数据仍然可用！");
            System.out.println("=".repeat(80));

        } finally {
            // 关闭（数据已保存）
            builder.close();
        }
    }
}

/**
 * 构建结果
 */
class BuildResult {
    int totalFiles = 0;
    int successCount = 0;
    int failedCount = 0;
    long buildTimeMs = 0;
    List<String> failedFiles = new ArrayList<>();
    String error = null;
}

/**
 * 知识库统计
 */
class KnowledgeBaseStats {
    long totalDocuments = 0;
    long indexedDocuments = 0;
    int processedFiles = 0;
}

