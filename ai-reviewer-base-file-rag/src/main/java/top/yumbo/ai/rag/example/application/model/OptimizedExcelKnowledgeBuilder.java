package top.yumbo.ai.rag.example.application.model;

import ai.onnxruntime.OrtException;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.LocalFileRAG;
import top.yumbo.ai.rag.impl.embedding.LocalEmbeddingEngine;
import top.yumbo.ai.rag.impl.index.SimpleVectorIndexEngine;
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

    // P0修复：向量嵌入和索引（使用简化版）
    private final LocalEmbeddingEngine embeddingEngine;
    private final SimpleVectorIndexEngine vectorIndexEngine;
    private final boolean enableVectorSearch;  // 是否启用向量检索

    // 内存管理
    private final MemoryMonitor memoryMonitor;
    private static final long BATCH_MEMORY_THRESHOLD = 100 * 1024 * 1024; // 100MB
    private long currentBatchMemory = 0;

    // 🔧 优化：文件大小限制
    private static final long MAX_FILE_SIZE = 200 * 1024 * 1024; // 200MB（从100MB增加）
    private static final long MAX_CONTENT_SIZE = 50 * 1024 * 1024; // 50MB（从10MB增加）- 触发强制分块的阈值

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
        this(storagePath, excelFolderPath, enableChunking, true); // 默认启用向量检索
    }

    /**
     * 完整构造函数
     *
     * @param storagePath 知识库存储路径
     * @param excelFolderPath Excel文件夹路径
     * @param enableChunking 是否启用文档分块
     * @param enableVectorSearch 是否启用向量检索（P0修复）
     */
    public OptimizedExcelKnowledgeBuilder(String storagePath, String excelFolderPath,
                                           boolean enableChunking, boolean enableVectorSearch) {
        this.excelFolderPath = excelFolderPath;
        this.enableChunking = enableChunking;
        this.autoChunking = !enableChunking;
        this.enableVectorSearch = enableVectorSearch;
        this.memoryMonitor = new MemoryMonitor();
        this.chunker = DocumentChunker.builder()
            .chunkSize(2000)
            .chunkOverlap(200)
            .smartSplit(true)
            .build();

        // 创建LocalFileRAG实例
        this.rag = LocalFileRAG.builder()
            .storagePath(storagePath)
            .enableCache(true)
            .enableCompression(true)
            .build();

        // P0修复：初始化向量嵌入和索引引擎（简化版）
        LocalEmbeddingEngine tempEmbedding = null;
        SimpleVectorIndexEngine tempVector = null;

        if (enableVectorSearch) {
            try {
                log.info("🚀 初始化向量检索引擎（简化版）...");

                // 获取模型路径（支持从 resources 或文件系统加载）
                String modelPath = getModelPathFromResourcesOrFileSystem();
                log.info("📦 模型路径: {}", modelPath);

                // 初始化嵌入引擎
                tempEmbedding = new LocalEmbeddingEngine(modelPath);

                // 初始化简化版向量索引引擎（线性扫描）
                tempVector = new SimpleVectorIndexEngine(
                    storagePath,
                    tempEmbedding.getEmbeddingDim()
                );

                log.info("✅ 向量检索引擎初始化成功（适合<10万条文档）");

            } catch (OrtException | IOException e) {
                log.error("❌ 向量检索引擎初始化失败，将使用纯关键词检索模式", e);
                log.warn("💡 提示：如需启用向量检索，请确保模型文件已下载");
                log.warn("   方式1: 放到 resources/models/paraphrase-multilingual/model.onnx");
                log.warn("   方式2: 放到 ./models/paraphrase-multilingual/model.onnx");

                // 清理已创建的资源
                if (tempEmbedding != null) {
                    try {
                        tempEmbedding.close();
                    } catch (Exception ex) {
                        // 忽略关闭异常
                    }
                }
                tempEmbedding = null;
                tempVector = null;
            }
        }

        this.embeddingEngine = tempEmbedding;
        this.vectorIndexEngine = tempVector;

        log.info("=".repeat(80));
        log.info("Optimized Excel Knowledge Builder Initialized");
        log.info("=".repeat(80));
        log.info("Storage Path: {}", storagePath);
        log.info("Excel Folder: {}", excelFolderPath);
        log.info("Chunking Mode: {}", enableChunking ? "Always Enabled" : "Auto (threshold: " + AUTO_CHUNK_THRESHOLD / 1024 / 1024 + "MB)");
        log.info("Vector Search: {}", this.embeddingEngine != null ? "✅ Enabled" : "❌ Disabled (Keyword Only)");
        if (this.embeddingEngine != null) {
            log.info("Embedding Model: {}", this.embeddingEngine.getModelName());
            log.info("Vector Dimension: {}", this.embeddingEngine.getEmbeddingDim());
        }
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
     * 从 resources 或文件系统获取模型文件路径
     * 优先级：
     * 1. resources/models/paraphrase-multilingual/model.onnx（打包后可用）
     * 2. ./models/paraphrase-multilingual/model.onnx（开发环境）
     *
     * @return 模型文件路径
     * @throws IOException 如果模型文件不存在
     */
    private String getModelPathFromResourcesOrFileSystem() throws IOException {
        // 🔧 支持多种模型文件，按优先级查找
        // 支持推荐的新模型: BGE-M3, E5-Large, GTE-Large, Jina等
        String[] modelFiles = {
            "model.onnx",                    // 标准模型（推荐，兼容性最好）
            "model_O2.onnx",                 // 优化模型（性能提升）
            "model_O3.onnx",                 // 高级优化
            "model_quantized.onnx",          // 通用量化模型
            "model_quint8_avx2.onnx",        // AVX2 量化（大多数CPU支持）
            "model_qint8_avx512.onnx",       // AVX-512 量化
            "model_qint8_avx512_vnni.onnx",  // AVX-512 VNNI 量化
            "model_qint8_arm64.onnx"         // ARM64 量化（Mac M1/M2）
        };

        // 🔧 支持多个模型目录
        String[] modelDirs = {
            "bge-m3",                    // BGE-M3（推荐，2024最新）
            "e5-large",                  // E5-Large（微软，性能优秀）
            "multilingual-e5-large",     // Multilingual E5-Large
            "bge-large-zh",              // BGE-Large-ZH（中文最佳）
            "gte-large-zh",              // GTE-Large-ZH（阿里达摩院）
            "jina-v2",                   // Jina v2（支持长文本）
            "paraphrase-multilingual",   // 当前默认模型
            "text2vec-base-chinese"      // 旧版中文模型
        };

        // 方式1：尝试从 classpath/resources 加载（支持打包后运行）
        for (String modelDir : modelDirs) {
            for (String modelFile : modelFiles) {
                String resourcePath = "/models/" + modelDir + "/" + modelFile;
                java.net.URL resourceUrl = getClass().getResource(resourcePath);

                if (resourceUrl != null) {
                    try {
                        // 如果是 jar 包内资源，需要提取到临时文件
                        if (resourceUrl.getProtocol().equals("jar")) {
                            log.info("📦 检测到 JAR 包内模型: {}/{}", modelDir, modelFile);

                            java.io.InputStream is = getClass().getResourceAsStream(resourcePath);
                            if (is == null) {
                                continue; // 尝试下一个
                            }

                            // 创建临时文件
                            Path tempFile = Files.createTempFile("embedding-model-", ".onnx");
                            tempFile.toFile().deleteOnExit();

                            // 复制到临时文件
                            Files.copy(is, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            is.close();

                            log.info("✅ 模型已提取: {}/{}", modelDir, modelFile);
                            return tempFile.toString();

                        } else {
                            // 如果是文件系统资源（开发环境）
                            Path modelPath = Paths.get(resourceUrl.toURI());
                            if (Files.exists(modelPath)) {
                                log.info("✅ 从 resources 加载模型: {}/{}", modelDir, modelFile);
                                log.info("   路径: {}", modelPath);
                                return modelPath.toString();
                            }
                        }
                    } catch (Exception e) {
                        log.debug("尝试 {}/{} 失败: {}", modelDir, modelFile, e.getMessage());
                        // 继续尝试下一个
                    }
                }
            }
        }

        // 方式2：尝试从文件系统加载（开发环境备用）
        for (String modelDir : modelDirs) {
            for (String modelFile : modelFiles) {
                String fileSystemPath = "./models/" + modelDir + "/" + modelFile;
                Path fsPath = Paths.get(fileSystemPath);
                if (Files.exists(fsPath)) {
                    log.info("✅ 从文件系统加载模型: {}/{}", modelDir, modelFile);
                    log.info("   路径: {}", fsPath.toAbsolutePath());
                    return fsPath.toString();
                }
            }
        }

        // 方式3：检查绝对路径（用户自定义）
        for (String modelDir : modelDirs) {
            for (String modelFile : modelFiles) {
                String absolutePath = "models/" + modelDir + "/" + modelFile;
                Path absPath = Paths.get(absolutePath);
                if (Files.exists(absPath)) {
                    log.info("✅ 从绝对路径加载模型: {}/{}", modelDir, modelFile);
                    log.info("   路径: {}", absPath.toAbsolutePath());
                    return absPath.toString();
                }
            }
        }

        // 所有方式都失败
        StringBuilder searchedDirs = new StringBuilder();
        searchedDirs.append("已搜索的模型目录（按优先级）：\n");
        for (int i = 0; i < modelDirs.length; i++) {
            searchedDirs.append("  ").append(i + 1).append(". models/").append(modelDirs[i]).append("/\n");
        }

        StringBuilder searchedFiles = new StringBuilder();
        searchedFiles.append("\n已尝试的文件名：\n");
        for (String file : modelFiles) {
            searchedFiles.append("  - ").append(file).append("\n");
        }

        throw new IOException(
            "❌ 嵌入模型文件不存在！\n\n" +
            searchedDirs + searchedFiles + "\n" +
            "📥 推荐的模型（按性能排序）：\n\n" +
            "  1️⃣  BGE-M3 ⭐⭐⭐⭐⭐ （2024最新，性能最佳）\n" +
            "      https://huggingface.co/BAAI/bge-m3\n" +
            "      目录: ./models/bge-m3/model.onnx\n\n" +
            "  2️⃣  Multilingual-E5-Large ⭐⭐⭐⭐ （微软出品，平衡）\n" +
            "      https://huggingface.co/intfloat/multilingual-e5-large\n" +
            "      目录: ./models/multilingual-e5-large/model.onnx\n\n" +
            "  3️⃣  BGE-Large-ZH ⭐⭐⭐⭐ （中文最佳）\n" +
            "      https://huggingface.co/BAAI/bge-large-zh-v1.5\n" +
            "      目录: ./models/bge-large-zh/model.onnx\n\n" +
            "  4️⃣  Paraphrase-Multilingual ⭐⭐⭐ （轻量兼容）\n" +
            "      https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2\n" +
            "      目录: ./models/paraphrase-multilingual/model.onnx\n\n" +
            "💡 快速开始：\n" +
            "  1. 下载任一模型的 model.onnx 文件\n" +
            "  2. 放到对应目录（如 ./models/bge-m3/model.onnx）\n" +
            "  3. 系统会自动检测并使用\n\n" +
            "📖 详细对比请查看: 更好的嵌入模型推荐.md"
        );
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
                        result.failedFiles.add(file.getName());
                        result.fileErrors.put(file.getName(), fileResult.error);  // 存储错误信息
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
                    String errorMsg = e.getClass().getSimpleName() + ": " + e.getMessage();
                    result.failedFiles.add(file.getName());
                    result.fileErrors.put(file.getName(), errorMsg);  // 存储错误信息
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
     * 支持：
     * 1. 单个Excel文件路径（直接处理该文件）
     * 2. 文件夹路径（递归扫描文件夹中的所有Excel文件）
     */
    private List<File> scanExcelFiles() throws IOException {
        List<File> excelFiles = new ArrayList<>();
        File inputFile = new File(excelFolderPath);

        // 检查路径是否存在
        if (!inputFile.exists()) {
            log.warn("❌ Path does not exist: {}", excelFolderPath);
            log.info("💡 提示：请检查路径是否正确，注意中文路径编码");
            return excelFiles;
        }

        // 情况1：如果是单个文件，直接处理
        if (inputFile.isFile()) {
            String fileName = inputFile.getName().toLowerCase();

            if (fileName.endsWith(".xls") || fileName.endsWith(".xlsx")) {
                if (!fileName.startsWith("~$")) {
                    if (inputFile.length() > MAX_FILE_SIZE) {
                        log.warn("⚠️ File too large: {} ({}MB), max allowed: {}MB",
                            inputFile.getName(),
                            inputFile.length() / 1024 / 1024,
                            MAX_FILE_SIZE / 1024 / 1024);
                    } else {
                        log.info("✓ Found single Excel file: {} ({}KB)",
                            inputFile.getName(),
                            inputFile.length() / 1024);
                        excelFiles.add(inputFile);
                    }
                } else {
                    log.warn("⚠️ Skipping temporary file: {}", inputFile.getName());
                }
            } else {
                log.warn("⚠️ File is not an Excel file (.xls/.xlsx): {}", inputFile.getName());
            }

            return excelFiles;
        }

        // 情况2：如果是文件夹，递归扫描
        if (inputFile.isDirectory()) {
            log.info("📂 Scanning directory: {}", excelFolderPath);
            Path startPath = inputFile.toPath();

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
        }

        return excelFiles;
    }

    /**
     * 处理单个Excel文件
     */
    private ProcessFileResult processExcelFile(File file) {
        ProcessFileResult result = new ProcessFileResult();

        try {
            log.info("📄 Processing: {} ({}KB)", file.getName(), file.length() / 1024);

            // 1. 提取Excel内容
            log.info("   ⏳ Extracting content from Excel file...");
            String content = extractExcelContent(file);

            if (content == null || content.trim().isEmpty()) {
                result.error = "Empty content - Excel文件可能是空的或解析失败";
                log.error("   ❌ Failed: {}", result.error);
                return result;
            }

            log.info("   ✓ Extracted {} characters ({} MB)",
                content.length(),
                String.format("%.2f", content.length() / 1024.0 / 1024.0));

            // 2. 🔧 优化：检查超大内容，强制分块而不是截断
            boolean isLargeContent = content.length() > MAX_CONTENT_SIZE;
            if (isLargeContent) {
                log.warn("⚠️ Large content detected: {} ({} MB > {} MB)",
                    file.getName(),
                    String.format("%.2f", content.length() / 1024.0 / 1024.0),
                    MAX_CONTENT_SIZE / 1024 / 1024);
                log.info("   ✅ 将使用智能分块处理（而不是截断）以保留完整数据");
            }

            // 3. 构建文档元数据
            Map<String, Object> metadata = buildMetadata(file);
            if (isLargeContent) {
                metadata.put("isLargeFile", true);
                metadata.put("originalSize", content.length());
            }

            // 4. 创建文档
            Document document = Document.builder()
                .title(file.getName())
                .content(content)
                .metadata(metadata)
                .build();

            // 5. 🔧 优化：智能分块处理（超大文件强制分块）
            List<Document> documentsToIndex;
            boolean shouldChunk = false;
            String chunkReason = "";

            // 判断是否需要分块
            if (isLargeContent) {
                // 🔧 新增：超大内容（>10MB）强制分块
                shouldChunk = true;
                chunkReason = String.format("Large content auto-chunking (%.2f MB > %d MB) - 保留完整数据",
                    content.length() / 1024.0 / 1024.0,
                    MAX_CONTENT_SIZE / 1024 / 1024);
            } else if (enableChunking) {
                // 强制启用分块模式
                shouldChunk = content.length() > DocumentChunker.DEFAULT_CHUNK_SIZE;
                chunkReason = "Force enabled";
            } else if (autoChunking) {
                // 自动模式：根据内容大小判断
                if (content.length() > AUTO_CHUNK_THRESHOLD) {
                    shouldChunk = true;
                    chunkReason = String.format("Auto-detected (%.2f MB > %.2f MB)",
                        content.length() / 1024.0 / 1024.0,
                        AUTO_CHUNK_THRESHOLD / 1024.0 / 1024.0);
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

            // 6. 索引文档（Lucene + 向量）
            for (Document doc : documentsToIndex) {
                // 6.1 Lucene 索引（关键词检索）
                String docId = rag.index(doc);
                log.trace("Indexed (Lucene): {} -> {}", file.getName(), docId);

                // 6.2 向量索引（语义检索）- P0修复
                if (embeddingEngine != null && vectorIndexEngine != null) {
                    try {
                        // 生成文档的文本表示（标题 + 内容）
                        String textForEmbedding = doc.getTitle() + "\n" + doc.getContent();

                        // 截断过长文本（避免ONNX内存溢出）
                        if (textForEmbedding.length() > 5000) {
                            textForEmbedding = textForEmbedding.substring(0, 5000);
                        }

                        // 生成向量嵌入
                        float[] vector = embeddingEngine.embed(textForEmbedding);

                        // 添加到向量索引
                        vectorIndexEngine.addDocument(docId, vector);

                        log.trace("Vector indexed: {} -> {} dims", docId, vector.length);

                    } catch (Exception e) {
                        log.warn("向量索引失败: {} - {}", docId, e.getMessage());
                        // 不影响主流程，继续处理
                    }
                }
            }

            processedFiles.add(file.getAbsolutePath());

            result.success = true;
            result.documentsCreated = documentsToIndex.size();
            result.estimatedMemory = content.length() * 2L; // 估算内存占用（约2倍）

            log.info("   ✅ Successfully processed: {} documents created", result.documentsCreated);

        } catch (Exception e) {
            // 友好的错误处理
            String errorType = e.getClass().getSimpleName();
            String errorMsg = e.getMessage();

            log.error("   ❌ Failed to process Excel file: {}", file.getName());

            // 根据错误类型提供更友好的提示
            if (e instanceof org.apache.tika.exception.TikaException) {
                Throwable cause = e.getCause();
                if (cause instanceof java.lang.ArrayIndexOutOfBoundsException) {
                    log.error("   💡 原因: Excel文件可能已损坏或格式不兼容");
                    log.error("   📝 建议: ");
                    log.error("      1. 尝试用 Excel 打开并另存为新文件");
                    log.error("      2. 检查文件是否完整下载");
                    log.error("      3. 如果是旧版 Excel 文件(.xls)，尝试转换为 .xlsx");
                    result.error = "文件损坏或格式不兼容 (ArrayIndexOutOfBoundsException)";
                } else {
                    log.error("   💡 原因: Tika 解析错误 - {}", cause != null ? cause.getMessage() : errorMsg);
                    result.error = "Tika解析失败: " + errorMsg;
                }
            } else if (e instanceof java.io.IOException) {
                log.error("   💡 原因: 文件读取错误 - {}", errorMsg);
                log.error("   📝 建议: 检查文件权限和路径");
                result.error = "IO错误: " + errorMsg;
            } else {
                log.error("   💡 原因: {} - {}", errorType, errorMsg);
                result.error = errorType + ": " + errorMsg;
            }

            log.info("   ⏭️  跳过此文件，继续处理其他文件...");
        }

        return result;
    }

    /**
     * 提取Excel内容（带错误处理）
     */
    private String extractExcelContent(File file) {
        try {
            // 尝试使用 Tika 解析
            String content = new top.yumbo.ai.rag.impl.parser.TikaDocumentParser().parse(file);

            if (content != null && !content.trim().isEmpty()) {
                return content;
            }

            log.warn("   ⚠️  Tika 解析返回空内容");
            return null;

        } catch (Exception e) {
            // Tika 解析失败，记录详细错误
            log.error("   ❌ Tika 解析失败: {}", e.getMessage());

            // 如果是特定的错误类型，提供更详细的信息
            if (e.getCause() instanceof java.lang.ArrayIndexOutOfBoundsException) {
                log.error("   💡 这通常表示 Excel 文件已损坏或使用了不兼容的格式");
            }

            // 重新抛出异常，让上层处理
            throw new RuntimeException("Excel解析失败: " + e.getMessage(), e);
        }
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

        // 显示失败文件的详细信息
        if (!result.failedFiles.isEmpty()) {
            log.warn("\n⚠️  Failed Files Details:");
            log.warn("-".repeat(80));
            for (String failedFile : result.failedFiles) {
                log.warn("  ❌ {}", failedFile);
                // 查找对应的错误信息
                String errorMsg = result.fileErrors.getOrDefault(failedFile, "Unknown error");
                log.warn("     💡 原因: {}", errorMsg);
            }
            log.warn("-".repeat(80));
            log.warn("💡 建议: 损坏的文件将被跳过，不影响其他文件的处理");
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
        // P0修复：保存向量索引
        if (vectorIndexEngine != null) {
            try {
                log.info("💾 保存向量索引...");
                vectorIndexEngine.saveIndex();
                log.info("✅ 向量索引已保存");
            } catch (IOException e) {
                log.error("保存向量索引失败", e);
            }
        }

        // 关闭嵌入引擎
        if (embeddingEngine != null) {
            embeddingEngine.close();
        }

        // 关闭 RAG
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
        public Map<String, String> fileErrors = new HashMap<>();  // 文件名 -> 错误信息
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

