package top.yumbo.ai.reviewer.adapter.output.storage;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
import top.yumbo.ai.reviewer.application.port.output.S3StoragePort;
import top.yumbo.ai.reviewer.domain.model.S3DownloadResult;
import top.yumbo.ai.reviewer.domain.model.S3File;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * AWS S3 存储适配器
 * 支持使用 IAM 角色默认凭证或显式凭证
 * 实现文件夹下载、批量下载等功能
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-14
 */
@Slf4j
public class S3StorageAdapter implements S3StoragePort {

    private final S3Client s3Client;
    private final S3StorageConfig config;
    private final ExecutorService executorService;
    private final Semaphore concurrencyLimiter;
    private final AtomicInteger activeRequests = new AtomicInteger(0);

    /**
     * 构造函数
     *
     * @param config S3 存储配置
     */
    public S3StorageAdapter(S3StorageConfig config) {
        this.config = config;

        // 初始化 S3 客户端
        S3ClientBuilder clientBuilder = S3Client.builder()
                .region(Region.of(config.region() != null ? config.region() : "us-east-1"));

        // 配置凭证提供者
        if (config.useDefaultCredentials()) {
            log.info("使用默认 AWS 凭证链（IAM 角色）");
        } else {
            log.info("使用提供的 AWS 凭证");
            AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(
                    config.accessKeyId(),
                    config.secretAccessKey()
            );
            clientBuilder.credentialsProvider(StaticCredentialsProvider.create(awsCredentials));
        }

        // 配置自定义端点（用于测试或兼容 S3 的存储）
        if (config.hasCustomEndpoint()) {
            clientBuilder.endpointOverride(URI.create(config.endpoint()));
            log.info("使用自定义 S3 端点: {}", config.endpoint());
        }

        // 配置加速端点
        if (config.isUseAccelerateEndpoint()) {
            clientBuilder.accelerate(true);
            log.info("启用 S3 传输加速");
        }

        // 配置路径样式访问
        if (config.isUsePathStyleAccess()) {
            clientBuilder.forcePathStyle(true);
            log.info("使用路径样式访问");
        }

        this.s3Client = clientBuilder.build();

        // 初始化线程池
        int maxConcurrency = config.getEffectiveMaxConcurrency();
        final AtomicInteger threadNumber = new AtomicInteger(1);
        this.executorService = Executors.newFixedThreadPool(
                maxConcurrency,
                r -> {
                    Thread t = new Thread(r, "s3-downloader-" + threadNumber.getAndIncrement());
                    t.setDaemon(true);
                    return t;
                }
        );

        this.concurrencyLimiter = new Semaphore(maxConcurrency);

        log.info("AWS S3 适配器初始化完成 - 区域: {}, 最大并发: {}, 默认存储桶: {}",
                config.region(), maxConcurrency, config.bucketName());
    }

    @Override
    public List<S3File> listFiles(String bucketName, String prefix) {
        log.info("列出 S3 文件 - Bucket: {}, Prefix: {}", bucketName, prefix);

        List<S3File> files = new ArrayList<>();

        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .build();

            ListObjectsV2Iterable responses = s3Client.listObjectsV2Paginator(request);

            for (ListObjectsV2Response response : responses) {
                for (S3Object s3Object : response.contents()) {
                    // 跳过目录标记
                    if (s3Object.key().endsWith("/")) {
                        continue;
                    }

                    S3File file = buildS3File(bucketName, s3Object);
                    files.add(file);
                }
            }

            log.info("找到 {} 个文件", files.size());
            return files;

        } catch (S3Exception e) {
            log.error("列出 S3 文件失败: {}", e.awsErrorDetails().errorMessage(), e);
            throw new RuntimeException("列出 S3 文件失败: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    @Override
    public CompletableFuture<List<S3File>> listFilesAsync(String bucketName, String prefix) {
        return CompletableFuture.supplyAsync(() -> listFiles(bucketName, prefix), executorService);
    }

    @Override
    public S3File downloadFile(String bucketName, String key) {
        log.debug("下载文件 - Bucket: {}, Key: {}", bucketName, key);

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);
            GetObjectResponse response = objectBytes.response();

            S3File file = S3File.builder()
                    .key(key)
                    .fileName(extractFileName(key))
                    .prefix(extractPrefix(key))
                    .sizeInBytes(response.contentLength())
                    .lastModified(response.lastModified())
                    .etag(response.eTag())
                    .contentType(response.contentType())
                    .storageClass(response.storageClassAsString())
                    .content(objectBytes.asByteArray())
                    .build();

            log.debug("文件下载成功 - Key: {}, Size: {} bytes", key, file.getSizeInBytes());
            return file;

        } catch (S3Exception e) {
            log.error("下载文件失败 - Key: {}, Error: {}", key, e.awsErrorDetails().errorMessage(), e);
            throw new RuntimeException("下载文件失败: " + key, e);
        }
    }

    @Override
    public void downloadFileToPath(String bucketName, String key, Path localPath) {
        log.debug("下载文件到本地 - Key: {}, Path: {}", key, localPath);

        try {
            // 确保父目录存在
            Path parent = localPath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.getObject(getObjectRequest, localPath);
            log.debug("文件下载到本地成功 - Key: {}, Path: {}", key, localPath);

        } catch (S3Exception e) {
            log.error("下载文件到本地失败 - Key: {}, Error: {}", key, e.awsErrorDetails().errorMessage(), e);
            throw new RuntimeException("下载文件到本地失败: " + key, e);
        } catch (IOException e) {
            log.error("创建目录失败: {}", localPath, e);
            throw new RuntimeException("创建目录失败: " + localPath, e);
        }
    }

    @Override
    public S3DownloadResult downloadFolder(String bucketName, String prefix) {
        log.info("开始下载文件夹 - Bucket: {}, Prefix: {}", bucketName, prefix);

        Instant startTime = Instant.now();
        List<S3File> allFiles = listFiles(bucketName, prefix);
        List<S3File> downloadedFiles = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        long totalSize = 0;

        // 并发下载文件
        List<CompletableFuture<Void>> futures = allFiles.stream()
                .map(file -> CompletableFuture.runAsync(() -> {
                    try {
                        concurrencyLimiter.acquire();
                        activeRequests.incrementAndGet();
                        try {
                            S3File downloadedFile = downloadFile(bucketName, file.getKey());
                            synchronized (downloadedFiles) {
                                downloadedFiles.add(downloadedFile);
                            }
                            successCount.incrementAndGet();
                        } finally {
                            activeRequests.decrementAndGet();
                            concurrencyLimiter.release();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        String error = "下载被中断: " + file.getKey();
                        synchronized (errors) {
                            errors.add(error);
                        }
                        failureCount.incrementAndGet();
                    } catch (Exception e) {
                        String error = "下载失败: " + file.getKey() + " - " + e.getMessage();
                        log.error(error, e);
                        synchronized (errors) {
                            errors.add(error);
                        }
                        failureCount.incrementAndGet();
                    }
                }, executorService))
                .toList();

        // 等待所有下载完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 计算总大小
        for (S3File file : downloadedFiles) {
            totalSize += file.getSizeInBytes();
        }

        Instant endTime = Instant.now();

        S3DownloadResult result = S3DownloadResult.builder()
                .bucketName(bucketName)
                .folderPrefix(prefix)
                .files(downloadedFiles)
                .startTime(startTime)
                .endTime(endTime)
                .totalFileCount(allFiles.size())
                .successCount(successCount.get())
                .failureCount(failureCount.get())
                .totalSizeInBytes(totalSize)
                .errors(errors)
                .build();

        log.info("文件夹下载完成 - {}", result);
        return result;
    }

    @Override
    public S3DownloadResult downloadFolderToDirectory(String bucketName, String prefix, Path localDirectory) {
        log.info("开始下载文件夹到本地 - Bucket: {}, Prefix: {}, LocalDir: {}", bucketName, prefix, localDirectory);

        Instant startTime = Instant.now();
        List<S3File> allFiles = listFiles(bucketName, prefix);
        List<S3File> downloadedFiles = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        long totalSize = 0;

        // 确保本地目录存在
        try {
            if (!Files.exists(localDirectory)) {
                Files.createDirectories(localDirectory);
            }
        } catch (IOException e) {
            throw new RuntimeException("创建本地目录失败: " + localDirectory, e);
        }

        // 并发下载文件到本地
        List<CompletableFuture<Void>> futures = allFiles.stream()
                .map(file -> CompletableFuture.runAsync(() -> {
                    try {
                        concurrencyLimiter.acquire();
                        activeRequests.incrementAndGet();
                        try {
                            // 计算本地文件路径
                            String relativePath = file.getRelativePath(prefix);
                            Path localPath = localDirectory.resolve(relativePath);

                            downloadFileToPath(bucketName, file.getKey(), localPath);

                            // 构建带元数据的 S3File（但不加载内容到内存）
                            S3File downloadedFile = S3File.builder()
                                    .key(file.getKey())
                                    .fileName(file.getFileName())
                                    .prefix(file.getPrefix())
                                    .sizeInBytes(file.getSizeInBytes())
                                    .lastModified(file.getLastModified())
                                    .etag(file.getEtag())
                                    .build();

                            synchronized (downloadedFiles) {
                                downloadedFiles.add(downloadedFile);
                            }
                            successCount.incrementAndGet();
                        } finally {
                            activeRequests.decrementAndGet();
                            concurrencyLimiter.release();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        String error = "下载被中断: " + file.getKey();
                        synchronized (errors) {
                            errors.add(error);
                        }
                        failureCount.incrementAndGet();
                    } catch (Exception e) {
                        String error = "下载失败: " + file.getKey() + " - " + e.getMessage();
                        log.error(error, e);
                        synchronized (errors) {
                            errors.add(error);
                        }
                        failureCount.incrementAndGet();
                    }
                }, executorService))
                .toList();

        // 等待所有下载完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 计算总大小
        for (S3File file : allFiles) {
            totalSize += file.getSizeInBytes();
        }

        Instant endTime = Instant.now();

        S3DownloadResult result = S3DownloadResult.builder()
                .bucketName(bucketName)
                .folderPrefix(prefix)
                .files(downloadedFiles)
                .startTime(startTime)
                .endTime(endTime)
                .totalFileCount(allFiles.size())
                .successCount(successCount.get())
                .failureCount(failureCount.get())
                .totalSizeInBytes(totalSize)
                .errors(errors)
                .build();

        log.info("文件夹下载到本地完成 - {}", result);
        return result;
    }

    @Override
    public CompletableFuture<S3DownloadResult> downloadFolderAsync(String bucketName, String prefix) {
        return CompletableFuture.supplyAsync(() -> downloadFolder(bucketName, prefix), executorService);
    }

    @Override
    public CompletableFuture<S3DownloadResult> downloadFolderToDirectoryAsync(
            String bucketName, String prefix, Path localDirectory) {
        return CompletableFuture.supplyAsync(
                () -> downloadFolderToDirectory(bucketName, prefix, localDirectory),
                executorService
        );
    }

    @Override
    public List<S3File> downloadFiles(String bucketName, List<String> keys) {
        log.info("批量下载文件 - Bucket: {}, Count: {}", bucketName, keys.size());

        List<S3File> files = new ArrayList<>();

        for (String key : keys) {
            try {
                S3File file = downloadFile(bucketName, key);
                files.add(file);
            } catch (Exception e) {
                log.error("下载文件失败: {}", key, e);
            }
        }

        return files;
    }

    @Override
    public CompletableFuture<List<S3File>> downloadFilesAsync(String bucketName, List<String> keys) {
        return CompletableFuture.supplyAsync(() -> {
            List<CompletableFuture<S3File>> futures = keys.stream()
                    .map(key -> CompletableFuture.supplyAsync(
                            () -> downloadFile(bucketName, key),
                            executorService
                    ))
                    .toList();

            return futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
        }, executorService);
    }

    @Override
    public boolean fileExists(String bucketName, String key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.headObject(headObjectRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            log.error("检查文件是否存在失败: {}", key, e);
            throw new RuntimeException("检查文件是否存在失败: " + key, e);
        }
    }

    @Override
    public S3File getFileMetadata(String bucketName, String key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            HeadObjectResponse response = s3Client.headObject(headObjectRequest);

            return S3File.builder()
                    .key(key)
                    .fileName(extractFileName(key))
                    .prefix(extractPrefix(key))
                    .sizeInBytes(response.contentLength())
                    .lastModified(response.lastModified())
                    .etag(response.eTag())
                    .contentType(response.contentType())
                    .storageClass(response.storageClassAsString())
                    .build();

        } catch (S3Exception e) {
            log.error("获取文件元数据失败: {}", key, e);
            throw new RuntimeException("获取文件元数据失败: " + key, e);
        }
    }

    @Override
    public void uploadFile(String bucketName, String key, byte[] content) {
        log.debug("上传文件 - Bucket: {}, Key: {}, Size: {} bytes", bucketName, key, content.length);

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(content));
            log.debug("文件上传成功 - Key: {}", key);

        } catch (S3Exception e) {
            log.error("上传文件失败: {}", key, e);
            throw new RuntimeException("上传文件失败: " + key, e);
        }
    }

    @Override
    public void uploadFileFromPath(String bucketName, String key, Path localPath) {
        log.debug("从本地上传文件 - Bucket: {}, Key: {}, Path: {}", bucketName, key, localPath);

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.putObject(putObjectRequest, localPath);
            log.debug("文件从本地上传成功 - Key: {}, Path: {}", key, localPath);

        } catch (S3Exception e) {
            log.error("从本地上传文件失败: {}", key, e);
            throw new RuntimeException("从本地上传文件失败: " + key, e);
        }
    }

    @Override
    public void deleteFile(String bucketName, String key) {
        log.debug("删除文件 - Bucket: {}, Key: {}", bucketName, key);

        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.debug("文件删除成功 - Key: {}", key);

        } catch (S3Exception e) {
            log.error("删除文件失败: {}", key, e);
            throw new RuntimeException("删除文件失败: " + key, e);
        }
    }

    @Override
    public String getProviderName() {
        return "AWS S3 (" + config.region() + ")";
    }

    @Override
    public boolean isAvailable() {
        try {
            // 简单的可用性检查 - 尝试列出存储桶
            if (config.bucketName() != null && !config.bucketName().isEmpty()) {
                s3Client.headBucket(HeadBucketRequest.builder()
                        .bucket(config.bucketName())
                        .build());
            }
            return true;
        } catch (Exception e) {
            log.error("S3 服务不可用: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public int getMaxConcurrency() {
        return concurrencyLimiter.availablePermits();
    }

    @Override
    public void shutdown() {
        log.info("关闭 AWS S3 适配器");

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.error("线程池未能正常关闭");
                }
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        if (s3Client != null) {
            s3Client.close();
        }

        log.info("AWS S3 适配器已关闭");
    }

    /**
     * 获取活跃请求数
     */
    public int getActiveRequests() {
        return activeRequests.get();
    }

    /**
     * 从 S3Object 构建 S3File（不含内容）
     */
    private S3File buildS3File(String bucketName, S3Object s3Object) {
        return S3File.builder()
                .key(s3Object.key())
                .fileName(extractFileName(s3Object.key()))
                .prefix(extractPrefix(s3Object.key()))
                .sizeInBytes(s3Object.size())
                .lastModified(s3Object.lastModified())
                .etag(s3Object.eTag())
                .storageClass(s3Object.storageClassAsString())
                .build();
    }

    /**
     * 从键提取文件名
     */
    private String extractFileName(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        int lastSlash = key.lastIndexOf('/');
        return lastSlash >= 0 ? key.substring(lastSlash + 1) : key;
    }

    /**
     * 从键提取前缀
     */
    private String extractPrefix(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        int lastSlash = key.lastIndexOf('/');
        return lastSlash >= 0 ? key.substring(0, lastSlash + 1) : "";
    }
}
