package top.yumbo.ai.reviewer.adapter.storage.s3;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.reviewer.application.service.S3StorageService;
import top.yumbo.ai.reviewer.domain.model.S3DownloadResult;
import top.yumbo.ai.reviewer.domain.model.S3File;

import java.nio.file.Paths;
import java.util.List;

/**
 * S3 存储服务使用示例
 * 演示如何使用 IAM 角色访问 S3
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-14
 */
@Slf4j
public class S3StorageExample {

    public static void main(String[] args) {
        // 示例 1: 使用 IAM 角色（推荐用于 AWS 环境）
        exampleWithIAMRole();

        // 示例 2: 使用显式凭证（用于开发环境）
        // exampleWithExplicitCredentials();
    }

    /**
     * 示例 1: 使用 IAM 角色访问 S3
     * 适用于在 AWS EC2/ECS/Lambda 上运行
     */
    public static void exampleWithIAMRole() {
        log.info("=== 示例 1: 使用 IAM 角色访问 S3 ===");

        // 创建配置（不提供 accessKeyId，将使用 IAM 角色）
        S3StorageConfig config = S3StorageConfig.builder()
                .region("us-east-1")
                .bucketName("my-project-bucket")
                .maxConcurrency(10)
                .connectTimeout(30000)
                .readTimeout(60000)
                .maxRetries(3)
                .retryDelay(1000)
                .build();

        // 创建适配器和服务
        S3StorageAdapter adapter = new S3StorageAdapter(config);
        S3StorageService service = new S3StorageService(adapter);

        try {
            String bucketName = "my-project-bucket";
            String projectPrefix = "projects/my-app/";

            // 1. 获取项目统计信息
            log.info("\n--- 1. 获取项目统计信息 ---");
            String statistics = service.getProjectStatistics(bucketName, projectPrefix);
            log.info(statistics);

            // 2. 列出源代码文件
            log.info("\n--- 2. 列出源代码文件 ---");
            List<S3File> sourceFiles = service.listSourceCodeFiles(bucketName, projectPrefix);
            log.info("找到 {} 个源代码文件", sourceFiles.size());
            sourceFiles.stream().limit(5).forEach(file ->
                    log.info("  - {} ({} KB)", file.getFileName(), String.format("%.2f", file.getSizeInKB()))
            );

            // 3. 下载项目到本地进行审查
            log.info("\n--- 3. 下载项目到本地 ---");
            S3DownloadResult result = service.downloadProjectForReview(
                    bucketName,
                    projectPrefix,
                    Paths.get("./temp-projects/my-app")
            );
            log.info("下载结果: {}", result);
            log.info("成功率: {:.2f}%, 速度: {:.2f} MB/s",
                    result.getSuccessRate(), result.getDownloadSpeedMBps());

            // 4. 下载项目到内存（小项目）
            log.info("\n--- 4. 下载项目到内存 ---");
            S3DownloadResult memoryResult = service.downloadProjectToMemory(bucketName, projectPrefix);
            log.info("下载到内存: {} 个文件, {:.2f} MB",
                    memoryResult.getSuccessCount(), memoryResult.getTotalSizeInMB());

            // 5. 上传审查报告
            log.info("\n--- 5. 上传审查报告 ---");
            String reportContent = generateSampleReport();
            service.uploadReviewReport(
                    bucketName,
                    "reports/my-app-review-" + System.currentTimeMillis() + ".json",
                    reportContent.getBytes()
            );
            log.info("审查报告上传成功");

        } catch (Exception e) {
            log.error("操作失败", e);
        } finally {
            service.shutdown();
        }
    }

    /**
     * 示例 2: 使用显式凭证访问 S3
     * 适用于开发环境或需要使用特定凭证的场景
     */
    public static void exampleWithExplicitCredentials() {
        log.info("=== 示例 2: 使用显式凭证访问 S3 ===");

        // 创建配置（提供 accessKeyId 和 secretAccessKey）
        S3StorageConfig config = S3StorageConfig.builder()
                .region("us-east-1")
                .bucketName("my-project-bucket")
                .accessKeyId("YOUR_ACCESS_KEY_ID")
                .secretAccessKey("YOUR_SECRET_ACCESS_KEY")
                .maxConcurrency(10)
                .build();

        S3StorageAdapter adapter = new S3StorageAdapter(config);
        S3StorageService service = new S3StorageService(adapter);

        try {
            // 使用服务...
            log.info("使用显式凭证的 S3 服务已就绪");

        } finally {
            service.shutdown();
        }
    }

    /**
     * 示例 3: 异步下载
     */
    public static void exampleAsyncDownload() {
        log.info("=== 示例 3: 异步下载 ===");

        S3StorageConfig config = S3StorageConfig.builder()
                .region("us-east-1")
                .bucketName("my-project-bucket")
                .maxConcurrency(20)
                .build();

        S3StorageAdapter adapter = new S3StorageAdapter(config);
        S3StorageService service = new S3StorageService(adapter);

        try {
            // 异步下载多个项目
            log.info("开始异步下载多个项目...");

            service.downloadProjectForReviewAsync(
                    "my-project-bucket",
                    "projects/project-1/",
                    Paths.get("./temp-projects/project-1")
            ).thenAccept(result -> {
                log.info("项目 1 下载完成: {}", result);
            });

            service.downloadProjectForReviewAsync(
                    "my-project-bucket",
                    "projects/project-2/",
                    Paths.get("./temp-projects/project-2")
            ).thenAccept(result -> {
                log.info("项目 2 下载完成: {}", result);
            });

            // 等待所有任务完成
            Thread.sleep(10000);

        } catch (Exception e) {
            log.error("异步下载失败", e);
        } finally {
            service.shutdown();
        }
    }

    /**
     * 示例 4: 直接使用适配器（低级 API）
     */
    public static void exampleDirectAdapterUsage() {
        log.info("=== 示例 4: 直接使用适配器 ===");

        S3StorageConfig config = S3StorageConfig.builder()
                .region("us-east-1")
                .build();

        S3StorageAdapter adapter = new S3StorageAdapter(config);

        try {
            String bucketName = "my-bucket";
            String key = "projects/my-app/README.md";

            // 检查文件是否存在
            if (adapter.fileExists(bucketName, key)) {
                log.info("文件存在: {}", key);

                // 获取元数据
                S3File metadata = adapter.getFileMetadata(bucketName, key);
                log.info("文件大小: {} KB", metadata.getSizeInKB());
                log.info("最后修改: {}", metadata.getLastModified());

                // 下载文件
                S3File file = adapter.downloadFile(bucketName, key);
                String content = new String(file.getContent());
                log.info("文件内容:\n{}", content);
            } else {
                log.warn("文件不存在: {}", key);
            }

        } finally {
            adapter.shutdown();
        }
    }

    /**
     * 生成示例报告
     */
    private static String generateSampleReport() {
        return """
                {
                  "projectName": "my-app",
                  "reviewDate": "%s",
                  "totalFiles": 150,
                  "score": 85.5,
                  "issues": [
                    {"type": "security", "severity": "high", "count": 2},
                    {"type": "performance", "severity": "medium", "count": 5}
                  ]
                }
                """.formatted(java.time.Instant.now());
    }
}

