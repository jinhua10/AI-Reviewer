package top.yumbo.ai.rag.example.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import top.yumbo.ai.rag.example.application.config.KnowledgeQAProperties;
import top.yumbo.ai.rag.example.application.controller.DocumentManagementController.DocumentInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 文档管理服务
 * 负责文档的上传、删除、列表管理等功能
 *
 * @author AI Reviewer Team
 * @since 2025-11-23
 */
@Slf4j
@Service
public class DocumentManagementService {

    private final KnowledgeQAProperties properties;
    private final Path documentsPath;

    // 支持的文件格式
    private static final List<String> SUPPORTED_EXTENSIONS = Arrays.asList(
        "xlsx", "xls", "docx", "doc", "pptx", "ppt", "pdf", "txt", "md", "html", "xml"
    );

    public DocumentManagementService(KnowledgeQAProperties properties) {
        this.properties = properties;

        // 获取文档路径
        String sourcePath = properties.getKnowledgeBase().getSourcePath();

        // 处理 classpath 路径
        if (sourcePath.startsWith("classpath:")) {
            // classpath 路径不支持上传，使用默认路径
            this.documentsPath = Paths.get("./data/documents");
            log.warn("⚠️  源路径是 classpath，上传文档将保存到: {}", this.documentsPath.toAbsolutePath());
        } else {
            this.documentsPath = Paths.get(sourcePath);
        }

        // 确保目录存在
        try {
            Files.createDirectories(this.documentsPath);
            log.info("✅ 文档目录已就绪: {}", this.documentsPath.toAbsolutePath());
        } catch (IOException e) {
            log.error("❌ 创建文档目录失败", e);
            throw new RuntimeException("无法创建文档目录", e);
        }
    }

    /**
     * 上传文档
     *
     * @param file 上传的文件
     * @return 文档ID
     */
    public String uploadDocument(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("文件名为空");
        }

        // 验证文件格式
        String extension = getFileExtension(originalFilename);
        if (!SUPPORTED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("不支持的文件格式: " + extension);
        }

        // 验证文件大小
        long maxSize = properties.getDocument().getMaxFileSizeMb() * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException(
                String.format("文件过大: %.2f MB (最大: %d MB)",
                    file.getSize() / 1024.0 / 1024.0,
                    properties.getDocument().getMaxFileSizeMb())
            );
        }

        // 保存文件
        Path targetPath = documentsPath.resolve(originalFilename);

        // 如果文件已存在，添加时间戳
        if (Files.exists(targetPath)) {
            String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String nameWithoutExt = originalFilename.substring(0, originalFilename.lastIndexOf('.'));
            String newFilename = nameWithoutExt + "_" + timestamp + "." + extension;
            targetPath = documentsPath.resolve(newFilename);
            log.info("文件已存在，重命名为: {}", newFilename);
        }

        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        log.info("✅ 文档已保存: {}", targetPath.getFileName());

        return targetPath.getFileName().toString();
    }

    /**
     * 删除文档
     *
     * @param fileName 文件名
     * @return 是否删除成功
     */
    public boolean deleteDocument(String fileName) throws IOException {
        Path filePath = documentsPath.resolve(fileName);

        if (!Files.exists(filePath)) {
            log.warn("文档不存在: {}", fileName);
            return false;
        }

        // 安全检查：确保文件在文档目录内
        if (!filePath.normalize().startsWith(documentsPath.normalize())) {
            throw new SecurityException("非法的文件路径");
        }

        Files.delete(filePath);
        log.info("✅ 文档已删除: {}", fileName);

        return true;
    }

    /**
     * 获取文档列表
     *
     * @return 文档列表
     */
    public List<DocumentInfo> listDocuments() throws IOException {
        List<DocumentInfo> documents = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(documentsPath, 1)) {
            List<Path> files = paths
                .filter(Files::isRegularFile)
                .filter(path -> {
                    String extension = getFileExtension(path.getFileName().toString());
                    return SUPPORTED_EXTENSIONS.contains(extension.toLowerCase());
                })
                .collect(Collectors.toList());

            for (Path path : files) {
                DocumentInfo info = new DocumentInfo();
                info.setFileName(path.getFileName().toString());
                info.setFileSize(Files.size(path));
                info.setFileType(getFileExtension(path.getFileName().toString()));

                // 获取创建时间
                BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                info.setUploadTime(sdf.format(new Date(attrs.creationTime().toMillis())));

                // TODO: 检查是否已索引
                info.setIndexed(true);

                documents.add(info);
            }
        }

        // 按上传时间倒序排列
        documents.sort((a, b) -> b.getUploadTime().compareTo(a.getUploadTime()));

        return documents;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) {
            return "";
        }
        return filename.substring(lastDot + 1);
    }

    /**
     * 获取文档目录路径
     */
    public String getDocumentsPath() {
        return documentsPath.toAbsolutePath().toString();
    }
}

