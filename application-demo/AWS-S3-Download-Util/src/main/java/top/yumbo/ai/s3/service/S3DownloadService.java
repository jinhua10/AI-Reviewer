package top.yumbo.ai.s3.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
public class S3DownloadService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name:hackathon2025-project-artifacts}")
    private String bucketName;

    @Value("${aws.s3.download-path:.}")
    private String downloadBasePath;

    public S3DownloadService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public void downloadAllFiles() {
        log.info("开始从bucket [{}] 下载文件", bucketName);

        // 创建下载目录
        Path downloadDir = Paths.get(downloadBasePath, bucketName);
        File dir = downloadDir.toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try {
            // 列出bucket中的所有对象
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .build();

            ListObjectsV2Response listResponse;
            int fileCount = 0;

            do {
                listResponse = s3Client.listObjectsV2(listRequest);

                for (S3Object s3Object : listResponse.contents()) {
                    String key = s3Object.key();

                    // 跳过目录标记
                    if (key.endsWith("/")) {
                        continue;
                    }

                    Path filePath = downloadDir.resolve(key);
                    File file = filePath.toFile();

                    // 创建父目录
                    file.getParentFile().mkdirs();

                    // 下载文件
                    GetObjectRequest getRequest = GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build();

                    s3Client.getObject(getRequest, ResponseTransformer.toFile(file));
                    fileCount++;
                    log.info("下载文件: {} -> {}", key, filePath);
                }

                // 处理分页
                listRequest = listRequest.toBuilder()
                        .continuationToken(listResponse.nextContinuationToken())
                        .build();

            } while (listResponse.isTruncated());

            log.info("文件下载完成，共下载 {} 个文件到目录: {}", fileCount, downloadDir);

        } catch (S3Exception e) {
            log.error("从S3下载文件失败: {}", e.awsErrorDetails().errorMessage(), e);
            throw new RuntimeException("S3文件下载失败", e);
        } catch (Exception e) {
            log.error("下载文件时发生错误", e);
            throw new RuntimeException("文件下载失败", e);
        }
    }
}

