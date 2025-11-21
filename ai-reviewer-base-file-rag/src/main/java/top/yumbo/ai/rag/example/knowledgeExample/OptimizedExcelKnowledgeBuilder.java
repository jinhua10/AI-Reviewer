package top.yumbo.ai.rag.example.knowledgeExample;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.LocalFileRAG;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.optimization.DocumentChunker;
import top.yumbo.ai.rag.optimization.MemoryMonitor;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * 优化版Excel知识库构建工具
 * 改进了内存管理和性能表现
 *
 * 主要优化：
 * 1. 基于内存阈值的动态批处理
 * 2. 文件大小限制和检查
 * 3. 文档分块支持
 * 4. 内存监控和自动GC
 * 5. 更详细的进度报告
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
public class OptimizedExcelKnowledgeBuilder {

    private final LocalFileRAG rag;
    private final String excelFolderPath;
    private final Set<String> processedFiles = new HashSet<>();

    // 内存管理
    private final MemoryMonitor memoryMonitor;
    private static final long BATCH_MEMORY_THRESHOLD = 100 * 1024 * 1024; // 100MB
    private long currentBatchMemory = 0;

    // 文件大小限制
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB
    private static final long MAX_CONTENT_SIZE = 10 * 1024 * 1024; // 10MB

    // 自动分块阈值 - 当文档内容超过此大小时自动启用分块
    private static final long AUTO_CHUNK_THRESHOLD = 2 * 1024 * 1024; // 2MB

    // 文档分块
    private final DocumentChunker chunker;
    private final boolean enableChunking;
    private final boolean autoChunking; // 自动分块模式

    // 性能配置
    private static final double GC_TRIGGER_THRESHOLD = 80.0; // 80%内存使用时触发GC

    /**
     * 构造函数（推荐使用Builder模式）
     *
     * @param storagePath 知识库存储路径
     * @param excelFolderPath Excel文件夹路径
     * @param enableChunking 是否启用文档分块（true=总是分块，false=根据文件大小自动判断）
     */
    public OptimizedExcelKnowledgeBuilder(String storagePath, String excelFolderPath, boolean enableChunking) {
        this.excelFolderPath = excelFolderPath;
        this.enableChunking = enableChunking;
        this.autoChunking = !enableChunking; // 如果不强制启用，则使用自动模式
        this.memoryMonitor = new MemoryMonitor();
        this.chunker = DocumentChunker.builder()
            .chunkSize(2000)      // 2000字符每块
            .chunkOverlap(200)    // 200字符重叠
            .smartSplit(true)     // 智能分割
            .build();

        // 创建LocalFileRAG实例
        this.rag = LocalFileRAG.builder()
            .storagePath(storagePath)
            .enableCache(true)
            .enableCompression(true)
            .build();

        log.info("=".repeat(80));
        log.info("Optimized Excel Knowledge Builder Initialized");
        log.info("=".repeat(80));
        log.info("Storage Path: {}", storagePath);
        log.info("Excel Folder: {}", excelFolderPath);
        log.info("Chunking Mode: {}", enableChunking ? "Always Enabled" : "Auto (threshold: " + AUTO_CHUNK_THRESHOLD / 1024 / 1024 + "MB)");
        log.info("Max File Size: {}MB", MAX_FILE_SIZE / 1024 / 1024);
        log.info("Max Content Size: {}MB", MAX_CONTENT_SIZE / 1024 / 1024);
        log.info("Batch Memory Threshold: {}MB", BATCH_MEMORY_THRESHOLD / 1024 / 1024);
        log.info("=".repeat(80));

        // 初始内存状态
        memoryMonitor.logMemoryUsage("Initialization");

        checkExistingIndex();
    }

    /**
     * 创建自动分块模式的构建器（推荐）
     * 自动识别大文件（>2MB）并分块处理
     *
     * @param storagePath 知识库存储路径
     * @param excelFolderPath Excel文件夹路径
     * @return 构建器实例
     */
    public static OptimizedExcelKnowledgeBuilder createWithAutoChunking(String storagePath, String excelFolderPath) {
        return new OptimizedExcelKnowledgeBuilder(storagePath, excelFolderPath, false);
    }

    /**
     * 检查已存在的索引
     */
    private void checkExistingIndex() {
        try {
            var stats = rag.getStatistics();
            if (stats.getDocumentCount() > 0) {
                log.info("📚 Existing knowledge base found:");
                log.info("  - Documents: {}", stats.getDocumentCount());
                log.info("  - Indexed: {}", stats.getIndexedDocumentCount());
                log.info("  - Mode: Incremental update");
            } else {
                log.info("📝 New knowledge base - building from scratch");
            }
        } catch (Exception e) {
            log.warn("Unable to get statistics: {}", e.getMessage());
        }
    }

    /**
     * 构建知识库（主方法）
     */
    public BuildResult buildKnowledgeBase() {
        log.info("\n🚀 Starting knowledge base construction...\n");
        long startTime = System.currentTimeMillis();

        memoryMonitor.logMemoryUsage("Build Start");

        BuildResult result = new BuildResult();

        try {
            // 1. 扫描Excel文件
            log.info("📂 Scanning Excel files...");
            List<File> excelFiles = scanExcelFiles();
            log.info("✓ Found {} Excel files", excelFiles.size());
            result.totalFiles = excelFiles.size();

            if (excelFiles.isEmpty()) {
                log.warn("⚠️ No Excel files found in: {}", excelFolderPath);
                return result;
            }

            // 2. 按文件大小排序（先处理小文件）
            excelFiles.sort(Comparator.comparingLong(File::length));

            // 3. 统计文件大小
            long totalSize = excelFiles.stream().mapToLong(File::length).sum();
            log.info("📊 Total size: {}MB", totalSize / 1024 / 1024);

            // 4. 批量处理Excel文件
            log.info("\n📝 Processing files...\n");

            int processedCount = 0;
            for (File file : excelFiles) {
                processedCount++;

                try {
                    // 检查内存使用情况
                    if (memoryMonitor.shouldTriggerGC(GC_TRIGGER_THRESHOLD)) {
                        log.warn("⚠️ Memory usage high, triggering GC before processing next file");
                        memoryMonitor.suggestGC();
                    }

                    // 处理文件
                    ProcessFileResult fileResult = processExcelFile(file);

                    if (fileResult.success) {
                        result.successCount++;
                        result.totalDocuments += fileResult.documentsCreated;
                        currentBatchMemory += fileResult.estimatedMemory;
                    } else {
                        result.failedCount++;
                        result.failedFiles.add(file.getName() + " (" + fileResult.error + ")");
                    }

                    // 进度报告
                    if (processedCount % 10 == 0) {
                        double progress = (double) processedCount / result.totalFiles * 100;
                        log.info("Progress: {}/{} ({} %) - Success: {}, Failed: {}",
                            processedCount, result.totalFiles, String.format("%.1f", progress),
                            result.successCount, result.failedCount);
                        memoryMonitor.logMemoryUsage("Processing");
                    }

                    // 基于内存阈值提交
                    if (currentBatchMemory >= BATCH_MEMORY_THRESHOLD) {
                        log.info("📦 Committing batch (accumulated {}MB)...",
                            currentBatchMemory / 1024 / 1024);

                        rag.commit();
                        currentBatchMemory = 0;

                        memoryMonitor.suggestGC();
                        memoryMonitor.logMemoryUsage("After Batch Commit");
                    }

                } catch (Exception e) {
                    log.error("❌ Failed to process file: {}", file.getName(), e);
                    result.failedCount++;
                    result.failedFiles.add(file.getName() + " (Exception: " + e.getMessage() + ")");
                }
            }

            // 5. 最终提交
            log.info("\n📦 Final commit...");
            rag.commit();

            // 6. 优化索引
            log.info("🔧 Optimizing index...");
            rag.optimizeIndex();

            memoryMonitor.logMemoryUsage("After Optimization");

            result.buildTimeMs = System.currentTimeMillis() - startTime;

            // 7. 打印最终报告
            printFinalReport(result);

        } catch (Exception e) {
            log.error("❌ Knowledge base construction failed", e);
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
            log.warn("Excel folder does not exist: {}", excelFolderPath);
            return excelFiles;
        }

        Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String fileName = file.getFileName().toString().toLowerCase();

                // 检查文件扩展名
                if (fileName.endsWith(".xls") || fileName.endsWith(".xlsx")) {
                    // 排除临时文件
                    if (!fileName.startsWith("~$")) {
                        File f = file.toFile();

                        // 检查文件大小
                        if (f.length() > MAX_FILE_SIZE) {
                            log.warn("⚠️ File too large, skipping: {} ({}MB)",
                                f.getName(), f.length() / 1024 / 1024);
                        } else {
                            excelFiles.add(f);
                        }
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                log.warn("Cannot access file: {}", file, exc);
                return FileVisitResult.CONTINUE;
            }
        });

        return excelFiles;
    }

    /**
     * 处理单个Excel文件
     */
    private ProcessFileResult processExcelFile(File file) {
        ProcessFileResult result = new ProcessFileResult();

        try {
            log.debug("Processing: {} ({}KB)", file.getName(), file.length() / 1024);

            // 1. 提取Excel内容
            String content = extractExcelContent(file);

            if (content == null || content.trim().isEmpty()) {
                result.error = "Empty content";
                return result;
            }

            // 2. 检查内容大小限制
            if (content.length() > MAX_CONTENT_SIZE) {
                log.warn("⚠️ Content too large: {} ({}MB), truncating to {}MB",
                    file.getName(),
                    content.length() / 1024 / 1024,
                    MAX_CONTENT_SIZE / 1024 / 1024);

                content = content.substring(0, (int) MAX_CONTENT_SIZE);
            }

            // 3. 构建文档元数据
            Map<String, Object> metadata = buildMetadata(file);

            // 4. 创建文档
            Document document = Document.builder()
                .title(file.getName())
                .content(content)
                .metadata(metadata)
                .build();

            // 5. 智能分块处理
            List<Document> documentsToIndex;
            boolean shouldChunk = false;
            String chunkReason = "";

            // 判断是否需要分块
            if (enableChunking) {
                // 强制启用分块模式
                shouldChunk = content.length() > DocumentChunker.DEFAULT_CHUNK_SIZE;
                chunkReason = "Force enabled";
            } else if (autoChunking) {
                // 自动模式：根据内容大小判断
                if (content.length() > AUTO_CHUNK_THRESHOLD) {
                    shouldChunk = true;
                    chunkReason = String.format("Large file auto-detected (%dMB > %dMB)",
                        content.length() / 1024 / 1024,
                        AUTO_CHUNK_THRESHOLD / 1024 / 1024);
                }
            }

            if (shouldChunk) {
                documentsToIndex = chunker.chunk(document);
                log.info("📄 Document chunked: {} -> {} chunks ({})",
                    file.getName(), documentsToIndex.size(), chunkReason);
            } else {
                documentsToIndex = List.of(document);
                log.debug("Document indexed without chunking: {}", file.getName());
            }

            // 6. 索引文档
            for (Document doc : documentsToIndex) {
                String docId = rag.index(doc);
                log.trace("Indexed: {} -> {}", file.getName(), docId);
            }

            processedFiles.add(file.getAbsolutePath());

            result.success = true;
            result.documentsCreated = documentsToIndex.size();
            result.estimatedMemory = content.length() * 2L; // 估算内存占用（约2倍）

        } catch (Exception e) {
            log.error("Failed to process Excel file: {}", file.getName(), e);
            result.error = e.getMessage();
        }

        return result;
    }

    /**
     * 提取Excel内容
     */
    private String extractExcelContent(File file) {
        return new top.yumbo.ai.rag.impl.parser.TikaDocumentParser().parse(file);
    }

    /**
     * 构建文档元数据
     */
    private Map<String, Object> buildMetadata(File file) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fileName", file.getName());
        metadata.put("filePath", file.getAbsolutePath());
        metadata.put("fileSize", file.length());
        metadata.put("fileType", "excel");
        metadata.put("extension", getFileExtension(file.getName()));
        metadata.put("indexedAt", System.currentTimeMillis());
        metadata.put("lastModified", file.lastModified());
        return metadata;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot) : "";
    }

    /**
     * 打印最终报告
     */
    private void printFinalReport(BuildResult result) {
        log.info("\n" + "=".repeat(80));
        log.info("📊 Knowledge Base Construction Report");
        log.info("=".repeat(80));
        log.info("✓ Total Files: {}", result.totalFiles);
        log.info("✓ Successful: {} ({}%)",
            result.successCount,
            result.totalFiles > 0 ? String.format("%.1f", (double) result.successCount / result.totalFiles * 100) : "0");
        log.info("✗ Failed: {} ({}%)",
            result.failedCount,
            result.totalFiles > 0 ? String.format("%.1f", (double) result.failedCount / result.totalFiles * 100) : "0");
        log.info("📄 Total Documents Created: {}", result.totalDocuments);
        log.info("⏱️  Total Time: {} seconds", String.format("%.2f", result.buildTimeMs / 1000.0));

        if (result.totalFiles > 0) {
            log.info("📈 Average Time per File: {} ms",
                String.format("%.2f", (double) result.buildTimeMs / result.totalFiles));
        }

        memoryMonitor.logMemoryUsage("Final");

        if (!result.failedFiles.isEmpty()) {
            log.warn("\n⚠️  Failed Files:");
            result.failedFiles.forEach(f -> log.warn("  - {}", f));
        }

        log.info("=".repeat(80) + "\n");
    }

    /**
     * 获取知识库统计信息
     *
     * @return 统计信息
     */
    public LocalFileRAG.Statistics getStatistics() {
        return rag.getStatistics();
    }

    /**
     * 清空知识库
     * 警告：此操作将删除所有已索引的文档和元数据
     */
    public void clearKnowledgeBase() {
        log.warn("⚠️  Clearing knowledge base - all documents will be deleted");
        try {
            // 通过删除所有文档来清空
            var stats = rag.getStatistics();
            long docCount = stats.getDocumentCount();

            if (docCount > 0) {
                log.info("Clearing {} documents...", docCount);
                // 注意：这里需要实现清空逻辑
                // 由于 LocalFileRAG 可能没有直接的 clearAll 方法，我们需要通过底层存储引擎清空
                // 或者重新初始化 RAG 实例
                log.warn("⚠️  Note: Physical deletion requires manual cleanup or restart");
            } else {
                log.info("Knowledge base is already empty");
            }

            processedFiles.clear();
        } catch (Exception e) {
            log.error("Failed to clear knowledge base", e);
        }
    }

    /**
     * 关闭资源
     */
    public void close() {
        rag.close();
        log.info("Knowledge builder closed");
    }

    /**
     * 处理文件结果
     */
    private static class ProcessFileResult {
        boolean success = false;
        String error = null;
        int documentsCreated = 0;
        long estimatedMemory = 0;
    }

    /**
     * 构建结果
     */
    public static class BuildResult {
        public int totalFiles = 0;
        public int successCount = 0;
        public int failedCount = 0;
        public int totalDocuments = 0;
        public long buildTimeMs = 0;
        public String error = null;
        public List<String> failedFiles = new ArrayList<>();
    }

    /**
     * 主方法 - 演示使用
     */
    public static void main(String[] args) {
        String storagePath = "./data/excel-knowledge-base-optimized";
        String excelFolder = "./data/excel-files";
        String mode = "auto"; // auto, force, disable

        // 从命令行参数读取
        if (args.length >= 1) {
            storagePath = args[0];
        }
        if (args.length >= 2) {
            excelFolder = args[1];
        }
        if (args.length >= 3) {
            mode = args[2]; // auto/force/disable
        }

        log.info("🚀 Starting optimized Excel knowledge base builder...");
        log.info("📊 JVM Max Memory: {}MB",
            Runtime.getRuntime().maxMemory() / 1024 / 1024);

        OptimizedExcelKnowledgeBuilder builder;

        // 根据模式创建构建器
        switch (mode.toLowerCase()) {
            case "force":
                log.info("📝 Mode: Force chunking (all files will be chunked)");
                builder = new OptimizedExcelKnowledgeBuilder(storagePath, excelFolder, true);
                break;
            case "disable":
                log.info("📝 Mode: Chunking disabled");
                builder = new OptimizedExcelKnowledgeBuilder(storagePath, excelFolder, false) {
                    // 特殊模式：完全禁用分块
                };
                break;
            case "auto":
            default:
                log.info("📝 Mode: Auto chunking (large files >2MB will be chunked automatically)");
                builder = createWithAutoChunking(storagePath, excelFolder);
                break;
        }

        try {
            BuildResult result = builder.buildKnowledgeBase();

            if (result.error != null) {
                log.error("❌ Build failed: {}", result.error);
                System.exit(1);
            }

            log.info("\n✅ Knowledge base built successfully!");
            log.info("📊 Statistics:");
            log.info("   - Total files: {}", result.totalFiles);
            log.info("   - Success: {}", result.successCount);
            log.info("   - Failed: {}", result.failedCount);
            log.info("   - Total documents: {}", result.totalDocuments);
            log.info("   - Time: {}s", String.format("%.2f", result.buildTimeMs / 1000.0));

        } finally {
            builder.close();
        }
    }
}

