package top.yumbo.ai.rag.example.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.LocalFileRAG;
import top.yumbo.ai.rag.example.application.config.KnowledgeQAProperties;
import top.yumbo.ai.rag.impl.embedding.LocalEmbeddingEngine;
import top.yumbo.ai.rag.impl.index.SimpleVectorIndexEngine;
import top.yumbo.ai.rag.impl.parser.TikaDocumentParser;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.optimization.DocumentChunker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 知识库构建服务
 * 支持多种文件格式：Excel, Word, PowerPoint, PDF, TXT等
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
@Service
public class KnowledgeBaseService {

    private final KnowledgeQAProperties properties;
    private final TikaDocumentParser documentParser;
    private final DocumentChunker documentChunker;
    private final DocumentProcessingOptimizer optimizer;

    public KnowledgeBaseService(KnowledgeQAProperties properties,
                                DocumentProcessingOptimizer optimizer) {
        this.properties = properties;
        this.optimizer = optimizer;
        this.documentParser = new TikaDocumentParser();
        this.documentChunker = optimizer.createChunker();
    }

    /**
     * 构建知识库
     *
     * @param sourcePath 文档源路径
     * @param storagePath 知识库存储路径
     * @param rebuild 是否重建
     * @return 构建结果
     */
    public top.yumbo.ai.rag.example.application.model.BuildResult buildKnowledgeBase(
            String sourcePath, String storagePath, boolean rebuild) {

        log.info("📂 扫描文档: {}", sourcePath);

        top.yumbo.ai.rag.example.application.model.BuildResult result =
            new top.yumbo.ai.rag.example.application.model.BuildResult();

        long startTime = System.currentTimeMillis();

        try {
            // 1. 扫描文件
            List<File> files = scanDocuments(sourcePath);
            result.setTotalFiles(files.size());

            if (files.isEmpty()) {
                log.warn("⚠️  未找到支持的文档文件");
                log.info("💡 提示: 请将文档放到 {} 目录", sourcePath);
                log.info("      支持格式: {}", properties.getDocument().getSupportedFormats());

                result.setBuildTimeMs(System.currentTimeMillis() - startTime);
                return result;
            }

            log.info("✅ 找到 {} 个文档文件", files.size());

            // 2. 检查是否需要构建
            LocalFileRAG rag = LocalFileRAG.builder()
                .storagePath(storagePath)
                .build();

            var stats = rag.getStatistics();
            boolean knowledgeBaseExists = stats.getDocumentCount() > 0;

            if (knowledgeBaseExists && !rebuild) {
                log.info("📚 检测到已有知识库 ({} 个文档)", stats.getDocumentCount());
                log.info("✅ 跳过构建，使用已有知识库");

                result.setSuccessCount(0);
                result.setFailedCount(0);
                result.setTotalDocuments((int) stats.getDocumentCount());
                result.setBuildTimeMs(System.currentTimeMillis() - startTime);

                rag.close();
                return result;
            }

            if (knowledgeBaseExists && rebuild) {
                log.info("🔄 检测到已有知识库，准备重建...");
                // 清空知识库
                rag.deleteAllDocuments();
                log.info("✓ 已清空旧知识库");
            }

            // 3. 处理文档
            log.info("\n📝 开始处理文档...");
            long processStartTime = System.currentTimeMillis();

            int successCount = 0;
            int failedCount = 0;
            List<Document> batchDocuments = new ArrayList<>();

            // 初始化向量检索引擎（如果启用）
            LocalEmbeddingEngine embeddingEngine = null;
            SimpleVectorIndexEngine vectorIndexEngine = null;

            if (properties.getVectorSearch().isEnabled()) {
                try {
                    embeddingEngine = new LocalEmbeddingEngine();
                    vectorIndexEngine = new SimpleVectorIndexEngine(
                        properties.getVectorSearch().getIndexPath(),
                        embeddingEngine.getEmbeddingDim()
                    );
                    log.info("✅ 向量检索引擎已启用");
                } catch (Exception e) {
                    log.warn("⚠️  向量检索引擎初始化失败，将只使用关键词索引", e);
                }
            }

            // 记录初始内存
            optimizer.logMemoryUsage("开始处理前");

            for (int i = 0; i < files.size(); i++) {
                File file = files.get(i);

                try {
                    // 处理文档并收集到批次
                    List<Document> docs = processDocumentOptimized(
                        file, rag, embeddingEngine, vectorIndexEngine);

                    if (docs != null && !docs.isEmpty()) {
                        batchDocuments.addAll(docs);
                        successCount++;

                        // 估算内存使用
                        long estimatedMemory = docs.stream()
                            .mapToLong(d -> optimizer.estimateMemoryUsage(d.getContent().length()))
                            .sum();
                        optimizer.addBatchMemory(estimatedMemory);

                        // 检查是否需要批处理或GC
                        if (optimizer.shouldBatch(estimatedMemory) || (i + 1) % 10 == 0) {
                            log.info("📦 批处理: {} 个文档 ({} / {})",
                                batchDocuments.size(), i + 1, files.size());

                            rag.commit();
                            batchDocuments.clear();
                            optimizer.resetBatchMemory();
                            optimizer.checkAndTriggerGC();
                        }
                    }

                } catch (Exception e) {
                    log.error("❌ 处理文件失败: {}", file.getName(), e);
                    failedCount++;
                }

                // 定期打印进度和内存状态
                if ((i + 1) % 5 == 0 || i == files.size() - 1) {
                    optimizer.logMemoryUsage(
                        String.format("进度 %d/%d", i + 1, files.size()));
                }
            }

            // 处理剩余的批次
            if (!batchDocuments.isEmpty()) {
                log.info("📦 处理最后一批: {} 个文档", batchDocuments.size());
                rag.commit();
            }

            long processEndTime = System.currentTimeMillis();

            // 4. 填充构建结果
            result.setSuccessCount(successCount);
            result.setFailedCount(failedCount);
            result.setTotalDocuments((int) rag.getStatistics().getDocumentCount());
            result.setBuildTimeMs(processEndTime - processStartTime);

            // 获取峰值内存使用
            long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            result.setPeakMemoryMB(usedMemory / 1024 / 1024);

            // 5. 显示结果
            log.info("\n" + "=".repeat(80));
            log.info("✅ 知识库构建完成");
            log.info("=".repeat(80));
            log.info("   - 成功: {} 个文件", result.getSuccessCount());
            log.info("   - 失败: {} 个文件", result.getFailedCount());
            log.info("   - 总文档: {} 个", result.getTotalDocuments());
            log.info("   - 耗时: {} 秒", String.format("%.2f", result.getBuildTimeMs() / 1000.0));
            log.info("   - 峰值内存: {} MB", result.getPeakMemoryMB());
            log.info("=".repeat(80));

            // 6. 优化和提交
            optimizer.commitAndOptimize(rag);

            // 7. 保存向量索引
            optimizer.saveVectorIndex(vectorIndexEngine);

            // 8. 清理资源
            optimizer.closeEmbeddingEngine(embeddingEngine);

            // 9. 最终内存状态
            optimizer.logMemoryUsage("构建完成");

            rag.close();

            return result;

        } catch (Exception e) {
            log.error("❌ 知识库构建失败", e);

            result.setError(e.getMessage());
            result.setBuildTimeMs(System.currentTimeMillis() - startTime);

            return result;
        }
    }

    /**
     * 扫描文档文件
     */
    private List<File> scanDocuments(String sourcePath) throws IOException {
        File sourceFile = new File(sourcePath);

        if (!sourceFile.exists()) {
            log.warn("⚠️  路径不存在: {}", sourcePath);
            return Collections.emptyList();
        }

        List<File> files = new ArrayList<>();

        if (sourceFile.isFile()) {
            // 单个文件
            if (isSupportedFile(sourceFile)) {
                files.add(sourceFile);
            }
        } else if (sourceFile.isDirectory()) {
            // 文件夹 - 递归扫描
            try (var stream = Files.walk(Paths.get(sourcePath))) {
                stream.filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(this::isSupportedFile)
                    .forEach(files::add);
            }
        }

        return files;
    }

    /**
     * 判断是否支持的文件格式
     */
    private boolean isSupportedFile(File file) {
        String fileName = file.getName().toLowerCase();
        List<String> supportedFormats = properties.getDocument().getSupportedFormats();

        return supportedFormats.stream()
            .anyMatch(format -> fileName.endsWith("." + format));
    }

    /**
     * 处理单个文档（优化版，返回文档列表以支持批处理）
     */
    private List<Document> processDocumentOptimized(File file, LocalFileRAG rag,
                                                     LocalEmbeddingEngine embeddingEngine,
                                                     SimpleVectorIndexEngine vectorIndexEngine) {

        log.info("📄 处理: {} ({} KB)", file.getName(), file.length() / 1024);
        List<Document> createdDocuments = new ArrayList<>();

        try {
            // 1. 检查文件大小
            if (!optimizer.checkFileSize(file.length())) {
                log.warn("   ⚠️  文件过大，跳过: {} MB > {} MB",
                    file.length() / 1024 / 1024,
                    properties.getDocument().getMaxFileSizeMb());
                return createdDocuments;
            }

            // 2. 解析文档内容
            String content = documentParser.parse(file);

            if (content == null || content.trim().isEmpty()) {
                log.warn("   ⚠️  解析内容为空，跳过");
                return createdDocuments;
            }

            log.info("   ✓ 提取 {} 字符", content.length());

            // 3. 检查内容大小并判断分块策略
            boolean forceChunk = optimizer.needsForceChunking(content.length());
            boolean autoChunk = optimizer.shouldAutoChunk(content.length());

            if (forceChunk) {
                log.warn("   ⚠️  内容过大 ({} MB)，强制分块",
                    content.length() / 1024 / 1024);
            } else if (autoChunk) {
                log.info("   📝 内容较大 ({} KB)，自动分块",
                    content.length() / 1024);
            }

            // 4. 创建文档
            Document document = Document.builder()
                .title(file.getName())
                .content(content)
                .metadata(buildMetadata(file))
                .build();

            // 5. 判断是否需要分块
            List<Document> documentsToIndex;

            if (forceChunk || autoChunk) {
                documentsToIndex = documentChunker.chunk(document);
                log.info("   ✓ 分块: {} 个", documentsToIndex.size());
            } else {
                documentsToIndex = List.of(document);
            }

            // 6. 索引文档
            for (Document doc : documentsToIndex) {
                String docId = rag.index(doc);
                doc.setId(docId);
                createdDocuments.add(doc);

                // 7. 生成向量索引（如果启用）
                if (embeddingEngine != null && vectorIndexEngine != null) {
                    try {
                        float[] vector = embeddingEngine.embed(doc.getContent());
                        vectorIndexEngine.addDocument(docId, vector);
                    } catch (Exception e) {
                        log.debug("向量生成失败: {}", e.getMessage());
                    }
                }
            }

            log.info("   ✅ 索引完成 ({} 个文档)", createdDocuments.size());

            return createdDocuments;

        } catch (Exception e) {
            log.error("   ❌ 处理失败", e);
            throw new RuntimeException("文档处理失败: " + file.getName(), e);
        }
    }

    /**
     * 构建文档元数据
     */
    private Map<String, Object> buildMetadata(File file) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fileName", file.getName());
        metadata.put("fileSize", file.length());
        metadata.put("filePath", file.getAbsolutePath());
        metadata.put("fileExtension", getFileExtension(file));
        metadata.put("lastModified", file.lastModified());
        metadata.put("indexTime", System.currentTimeMillis());
        return metadata;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(File file) {
        String fileName = file.getName();
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1).toLowerCase() : "";
    }
}

