package top.yumbo.ai.adaptor.source;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import top.yumbo.ai.api.source.FileSourceConfig;
import top.yumbo.ai.api.source.IFileSource;
import top.yumbo.ai.api.source.SourceFile;
import top.yumbo.ai.common.exception.FileSourceException;

import java.io.InputStream;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AWS S3 file source implementation
 * <p>
 * Provides access to files stored in AWS S3 buckets.
 * Also compatible with S3-compatible services (MinIO, Ceph, etc.)
 *
 * @author AI-Reviewer Team
 * @since 1.1.0
 */
@Slf4j
public class S3FileSource implements IFileSource {

    private S3Client s3Client;
    private String bucket;
    private String prefix;
    private boolean initialized = false;

    public S3FileSource() {
        // Default constructor
    }

    @Override
    public String getSourceName() {
        return "s3";
    }

    @Override
    public boolean support(FileSourceConfig config) {
        return "s3".equalsIgnoreCase(config.getSourceType());
    }

    @Override
    public void initialize(FileSourceConfig config) throws Exception {
        validateConfig(config);

        // Configure AWS credentials
        AwsCredentials credentials = AwsBasicCredentials.create(
                config.getAccessKey(),
                config.getSecretKey()
        );

        // Build S3 client
        var clientBuilder = S3Client.builder()
                .region(Region.of(config.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials));

        // Configure custom endpoint if provided (for S3-compatible services)
        if (config.getEndpoint() != null && !config.getEndpoint().trim().isEmpty()) {
            clientBuilder.endpointOverride(java.net.URI.create(config.getEndpoint()));
            log.debug("Using custom S3 endpoint: {}", config.getEndpoint());
        }

        this.s3Client = clientBuilder.build();
        this.bucket = config.getBucket();
        this.prefix = config.getBasePath() != null ? config.getBasePath() : "";

        // Remove trailing slash from prefix
        if (this.prefix.endsWith("/")) {
            this.prefix = this.prefix.substring(0, this.prefix.length() - 1);
        }

        // Verify bucket access
        verifyBucketAccess();

        this.initialized = true;
        log.info("S3 client initialized: bucket={}, region={}, prefix={}",
                bucket, config.getRegion(), prefix);
    }

    private void validateConfig(FileSourceConfig config) throws FileSourceException {
        if (config.getAccessKey() == null || config.getAccessKey().trim().isEmpty()) {
            throw new FileSourceException("Access key is required for S3 file source");
        }
        if (config.getSecretKey() == null || config.getSecretKey().trim().isEmpty()) {
            throw new FileSourceException("Secret key is required for S3 file source");
        }
        if (config.getRegion() == null || config.getRegion().trim().isEmpty()) {
            throw new FileSourceException("Region is required for S3 file source");
        }
        if (config.getBucket() == null || config.getBucket().trim().isEmpty()) {
            throw new FileSourceException("Bucket name is required for S3 file source");
        }
    }

    private void verifyBucketAccess() throws FileSourceException {
        try {
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(bucket)
                    .build();
            s3Client.headBucket(headBucketRequest);
            log.debug("Bucket access verified: {}", bucket);
        } catch (Exception e) {
            throw new FileSourceException("Failed to access S3 bucket: " + bucket, e);
        }
    }

    @Override
    public List<SourceFile> listFiles(String path) throws Exception {
        if (!initialized) {
            throw new FileSourceException("File source not initialized");
        }

        // Construct full prefix
        String fullPrefix = path == null || path.trim().isEmpty()
                ? prefix
                : (prefix + "/" + path).replace("//", "/");

        // Remove leading slash
        if (fullPrefix.startsWith("/")) {
            fullPrefix = fullPrefix.substring(1);
        }

        List<SourceFile> result = new ArrayList<>();

        ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                .bucket(bucket);

        // Only set prefix if not empty
        if (!fullPrefix.isEmpty()) {
            requestBuilder.prefix(fullPrefix);
        }

        ListObjectsV2Request request = requestBuilder.build();
        ListObjectsV2Response response;

        do {
            response = s3Client.listObjectsV2(request);

            for (S3Object s3Object : response.contents()) {
                String key = s3Object.key();

                // Skip directory markers
                if (key.endsWith("/")) {
                    continue;
                }

                // Calculate relative path
                String relativePath = prefix.isEmpty()
                        ? key
                        : key.substring(prefix.length() + 1);

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("etag", s3Object.eTag());
                metadata.put("storageClass", s3Object.storageClassAsString());

                SourceFile sourceFile = SourceFile.builder()
                        .fileId(key)
                        .relativePath(relativePath)
                        .fileName(Paths.get(key).getFileName().toString())
                        .fileSize(s3Object.size())
                        .lastModified(LocalDateTime.ofInstant(
                                s3Object.lastModified(), ZoneId.systemDefault()))
                        .metadata(metadata)
                        .source(this)
                        .build();

                result.add(sourceFile);
            }

            // Handle pagination
            if (response.isTruncated()) {
                request = request.toBuilder()
                        .continuationToken(response.nextContinuationToken())
                        .build();
            }

        } while (response.isTruncated());

        log.info("Listed {} objects from S3 bucket: {}/{}", result.size(), bucket, fullPrefix);
        return result;
    }

    @Override
    public InputStream readFile(SourceFile file) throws Exception {
        if (!initialized) {
            throw new FileSourceException("File source not initialized");
        }

        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(file.getFileId())
                    .build();

            return s3Client.getObject(request);
        } catch (Exception e) {
            throw new FileSourceException("Failed to read S3 object: " + file.getFileId(), e);
        }
    }

    @Override
    public void close() throws Exception {
        if (s3Client != null) {
            s3Client.close();
            log.info("S3 client closed");
        }
        this.initialized = false;
    }

    @Override
    public int getPriority() {
        return 40; // Lower priority than Git and SFTP
    }

    @Override
    public boolean isInitialized() {
        return initialized && s3Client != null;
    }
}

