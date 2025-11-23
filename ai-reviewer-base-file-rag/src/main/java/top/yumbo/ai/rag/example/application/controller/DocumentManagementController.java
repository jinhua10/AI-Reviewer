package top.yumbo.ai.rag.example.application.controller;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.yumbo.ai.rag.example.application.service.DocumentManagementService;

import java.util.List;

/**
 * 文档管理 REST API 控制器
 * 支持文档的上传、删除、列表查询等操作
 *
 * @author AI Reviewer Team
 * @since 2025-11-23
 */
@Slf4j
@RestController
@RequestMapping("/api/documents")
public class DocumentManagementController {

    private final DocumentManagementService documentService;

    public DocumentManagementController(DocumentManagementService documentService) {
        this.documentService = documentService;
    }

    /**
     * 上传文档
     */
    @PostMapping("/upload")
    public UploadResponse uploadDocument(@RequestParam("file") MultipartFile file) {
        log.info("收到文档上传请求: {}", file.getOriginalFilename());

        UploadResponse response = new UploadResponse();

        try {
            if (file.isEmpty()) {
                response.setSuccess(false);
                response.setMessage("文件为空");
                return response;
            }

            String result = documentService.uploadDocument(file);

            response.setSuccess(true);
            response.setMessage("文档上传成功");
            response.setFileName(file.getOriginalFilename());
            response.setFileSize(file.getSize());
            response.setDocumentId(result);

            log.info("文档上传成功: {}", file.getOriginalFilename());
            return response;

        } catch (Exception e) {
            log.error("文档上传失败", e);
            response.setSuccess(false);
            response.setMessage("上传失败: " + e.getMessage());
            return response;
        }
    }

    /**
     * 批量上传文档
     */
    @PostMapping("/upload-batch")
    public BatchUploadResponse uploadBatch(@RequestParam("files") MultipartFile[] files) {
        log.info("收到批量上传请求: {} 个文件", files.length);

        BatchUploadResponse response = new BatchUploadResponse();
        response.setTotal(files.length);

        int successCount = 0;
        int failureCount = 0;

        for (MultipartFile file : files) {
            try {
                if (!file.isEmpty()) {
                    documentService.uploadDocument(file);
                    successCount++;
                    response.getSuccessFiles().add(file.getOriginalFilename());
                }
            } catch (Exception e) {
                log.error("文件上传失败: {}", file.getOriginalFilename(), e);
                failureCount++;
                response.getFailedFiles().add(file.getOriginalFilename());
            }
        }

        response.setSuccessCount(successCount);
        response.setFailureCount(failureCount);
        response.setMessage(String.format("成功: %d, 失败: %d", successCount, failureCount));

        return response;
    }

    /**
     * 获取文档列表
     */
    @GetMapping("/list")
    public ListResponse listDocuments() {
        log.info("获取文档列表");

        try {
            List<DocumentInfo> documents = documentService.listDocuments();

            ListResponse response = new ListResponse();
            response.setSuccess(true);
            response.setTotal(documents.size());
            response.setDocuments(documents);

            return response;
        } catch (Exception e) {
            log.error("获取文档列表失败", e);

            ListResponse response = new ListResponse();
            response.setSuccess(false);
            response.setMessage("获取列表失败: " + e.getMessage());

            return response;
        }
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/{fileName}")
    public DeleteResponse deleteDocument(@PathVariable String fileName) {
        log.info("删除文档: {}", fileName);

        DeleteResponse response = new DeleteResponse();

        try {
            boolean deleted = documentService.deleteDocument(fileName);

            if (deleted) {
                response.setSuccess(true);
                response.setMessage("文档删除成功");
                response.setFileName(fileName);
            } else {
                response.setSuccess(false);
                response.setMessage("文档不存在");
            }

            return response;
        } catch (Exception e) {
            log.error("删除文档失败", e);
            response.setSuccess(false);
            response.setMessage("删除失败: " + e.getMessage());
            return response;
        }
    }

    /**
     * 批量删除文档
     */
    @DeleteMapping("/batch")
    public BatchDeleteResponse deleteBatch(@RequestBody List<String> fileNames) {
        log.info("批量删除文档: {} 个", fileNames.size());

        BatchDeleteResponse response = new BatchDeleteResponse();
        response.setTotal(fileNames.size());

        int successCount = 0;
        int failureCount = 0;

        for (String fileName : fileNames) {
            try {
                if (documentService.deleteDocument(fileName)) {
                    successCount++;
                    response.getSuccessFiles().add(fileName);
                } else {
                    failureCount++;
                    response.getFailedFiles().add(fileName);
                }
            } catch (Exception e) {
                log.error("删除文档失败: {}", fileName, e);
                failureCount++;
                response.getFailedFiles().add(fileName);
            }
        }

        response.setSuccessCount(successCount);
        response.setFailureCount(failureCount);
        response.setMessage(String.format("成功: %d, 失败: %d", successCount, failureCount));

        return response;
    }

    // ========== DTO 类 ==========

    @Data
    public static class UploadResponse {
        private boolean success;
        private String message;
        private String fileName;
        private long fileSize;
        private String documentId;
    }

    @Data
    public static class BatchUploadResponse {
        private int total;
        private int successCount;
        private int failureCount;
        private String message;
        private List<String> successFiles = new java.util.ArrayList<>();
        private List<String> failedFiles = new java.util.ArrayList<>();
    }

    @Data
    public static class ListResponse {
        private boolean success = true;
        private String message;
        private int total;
        private List<DocumentInfo> documents;
    }

    @Data
    public static class DocumentInfo {
        private String fileName;
        private long fileSize;
        private String fileType;
        private String uploadTime;
        private boolean indexed;
    }

    @Data
    public static class DeleteResponse {
        private boolean success;
        private String message;
        private String fileName;
    }

    @Data
    public static class BatchDeleteResponse {
        private int total;
        private int successCount;
        private int failureCount;
        private String message;
        private List<String> successFiles = new java.util.ArrayList<>();
        private List<String> failedFiles = new java.util.ArrayList<>();
    }
}

