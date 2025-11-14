# AWS S3 文件下载服务 - 六边形架构实现

## 概述

本文档介绍如何在 AI-Reviewer 项目中使用符合六边形架构的 AWS S3 文件下载服务。该服务使用 IAM 角色默认凭证，无需配置 Access Key。

## 架构设计

### 六边形架构层次

```
┌─────────────────────────────────────────────────────────┐
│                    Application Core                      │
│  ┌─────────────────────────────────────────────────┐   │
│  │           Domain Models (领域模型)               │   │
│  │  - S3File: S3 文件领域模型                       │   │
│  │  - S3DownloadResult: 下载结果领域模型            │   │
│  └─────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────┐   │
│  │        Application Service (应用服务)            │   │
│  │  - S3StorageService: S3 存储服务                 │   │
│  └─────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────┐   │
│  │          Output Ports (输出端口)                 │   │
│  │  - S3StoragePort: S3 存储接口定义                │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│              Adapters (适配器层)                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │        Output Adapters (输出适配器)              │   │
│  │  - S3StorageAdapter: AWS S3 实现                 │   │
│  │  - S3StorageConfig: S3 配置                      │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│            Infrastructure (基础设施)                      │
│  - AWS S3 SDK                                           │
│  - IAM Role Credentials                                 │
└─────────────────────────────────────────────────────────┘
```

## 核心组件

### 1. Domain Models (领域模型)

#### S3File
```java
@Data
@Builder
public class S3File {
    private final String key;              // S3 键
    private final String fileName;          // 文件名
    private final String prefix;            // 前缀
    private final long sizeInBytes;         // 大小
    private final Instant lastModified;     // 修改时间
    private final String etag;              // ETag
    private String contentType;             // 内容类型
    private byte[] content;                 // 文件内容（按需加载）
    private String storageClass;            // 存储类别
}
```

#### S3DownloadResult
```java
@Data
@Builder
public class S3DownloadResult {
    private final String bucketName;
    private final String folderPrefix;
    private final List<S3File> files;
    private final Instant startTime;
    private final Instant endTime;
    private final int totalFileCount;
    private final int successCount;
    private final int failureCount;
    private final long totalSizeInBytes;
    private final List<String> errors;
}
```

### 2. Output Port (输出端口)

#### S3StoragePort
定义与 S3 交互的接口，核心方法：

```java
public interface S3StoragePort {
    // 列出文件
    List<S3File> listFiles(String bucketName, String prefix);
    
    // 下载单个文件
    S3File downloadFile(String bucketName, String key);
    
    // 下载文件夹
    S3DownloadResult downloadFolder(String bucketName, String prefix);
    
    // 下载文件夹到本地目录
    S3DownloadResult downloadFolderToDirectory(
        String bucketName, String prefix, Path localDirectory);
    
    // 异步方法
    CompletableFuture<S3DownloadResult> downloadFolderAsync(...);
    
    // 上传、删除等其他方法...
}
```

### 3. Adapter (适配器)

#### S3StorageAdapter
实现 `S3StoragePort` 接口，连接到 AWS S3：

**特性：**
- ✅ 支持 IAM 角色默认凭证
- ✅ 支持显式凭证（开发环境）
- ✅ 并发下载（可配置）
- ✅ 自动重试机制
- ✅ 异步操作支持
- ✅ 进度跟踪
- ✅ 错误处理

### 4. Application Service (应用服务)

#### S3StorageService
编排业务逻辑：

```java
public class S3StorageService {
    // 下载项目进行审查
    S3DownloadResult downloadProjectForReview(...);
    
    // 列出源代码文件
    List<S3File> listSourceCodeFiles(...);
    
    // 获取项目统计
    String getProjectStatistics(...);
    
    // 上传审查报告
    void uploadReviewReport(...);
}
```

## 使用指南

### 配置方式

#### 方式 1: 使用 IAM 角色（推荐 - AWS 环境）

**config.yaml:**
```yaml
s3Storage:
  region: "us-east-1"
  bucketName: "my-project-bucket"
  maxConcurrency: 10
  connectTimeout: 30000
  readTimeout: 60000
  maxRetries: 3
  retryDelay: 1000
```

**不需要设置 `accessKeyId` 和 `secretAccessKey`！**

#### 方式 2: 使用显式凭证（开发环境）

**config.yaml:**
```yaml
s3Storage:
  region: "us-east-1"
  bucketName: "my-project-bucket"
  accessKeyId: "YOUR_ACCESS_KEY_ID"
  secretAccessKey: "YOUR_SECRET_ACCESS_KEY"
  maxConcurrency: 10
```

### 代码示例

#### 示例 1: 基本使用（IAM 角色）

```java
// 1. 创建配置
S3StorageConfig config = S3StorageConfig.builder()
    .region("us-east-1")
    .bucketName("my-project-bucket")
    .maxConcurrency(10)
    .build();

// 2. 创建适配器
S3StorageAdapter adapter = new S3StorageAdapter(config);

// 3. 创建服务
S3StorageService service = new S3StorageService(adapter);

// 4. 使用服务
try {
    // 下载项目到本地
    S3DownloadResult result = service.downloadProjectForReview(
        "my-project-bucket",
        "projects/my-app/",
        Paths.get("./temp-projects/my-app")
    );
    
    System.out.println("下载完成: " + result);
    System.out.println("成功率: " + result.getSuccessRate() + "%");
    System.out.println("速度: " + result.getDownloadSpeedMBps() + " MB/s");
    
} finally {
    service.shutdown();
}
```

#### 示例 2: 列出和过滤文件

```java
S3StorageService service = new S3StorageService(adapter);

// 列出所有源代码文件
List<S3File> sourceFiles = service.listSourceCodeFiles(
    "my-bucket", 
    "projects/my-app/"
);

sourceFiles.forEach(file -> {
    System.out.println(file.getFileName() + " - " + 
                      file.getSizeInKB() + " KB");
});

// 列出所有配置文件
List<S3File> configFiles = service.listConfigFiles(
    "my-bucket", 
    "projects/my-app/"
);
```

#### 示例 3: 异步下载

```java
// 异步下载多个项目
CompletableFuture<S3DownloadResult> future1 = 
    service.downloadProjectForReviewAsync(
        "my-bucket",
        "projects/project-1/",
        Paths.get("./temp/project-1")
    );

CompletableFuture<S3DownloadResult> future2 = 
    service.downloadProjectForReviewAsync(
        "my-bucket",
        "projects/project-2/",
        Paths.get("./temp/project-2")
    );

// 等待所有完成
CompletableFuture.allOf(future1, future2).thenRun(() -> {
    System.out.println("所有项目下载完成");
});
```

#### 示例 4: 直接使用适配器

```java
S3StorageAdapter adapter = new S3StorageAdapter(config);

try {
    // 检查文件是否存在
    boolean exists = adapter.fileExists("my-bucket", "path/to/file.txt");
    
    if (exists) {
        // 获取元数据
        S3File metadata = adapter.getFileMetadata("my-bucket", "path/to/file.txt");
        System.out.println("文件大小: " + metadata.getSizeInKB() + " KB");
        
        // 下载文件
        S3File file = adapter.downloadFile("my-bucket", "path/to/file.txt");
        String content = new String(file.getContent());
        System.out.println(content);
    }
    
} finally {
    adapter.shutdown();
}
```

#### 示例 5: 上传审查报告

```java
S3StorageService service = new S3StorageService(adapter);

// 生成报告
String reportJson = generateReviewReport();

// 上传到 S3
service.uploadReviewReport(
    "my-bucket",
    "reports/my-app-review-" + System.currentTimeMillis() + ".json",
    reportJson.getBytes()
);
```

## IAM 角色配置

### EC2/ECS 实例 IAM 策略

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject",
        "s3:ListBucket",
        "s3:HeadBucket"
      ],
      "Resource": [
        "arn:aws:s3:::my-project-bucket",
        "arn:aws:s3:::my-project-bucket/*"
      ]
    }
  ]
}
```

### 最小权限策略（只读）

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::my-project-bucket/projects/*"
      ]
    }
  ]
}
```

## 性能优化

### 1. 并发配置

```yaml
s3Storage:
  maxConcurrency: 20  # 增加并发数（默认 10）
```

**建议：**
- 小文件（< 1MB）：20-50
- 大文件（> 10MB）：5-10
- 混合：10-20

### 2. 超时配置

```yaml
s3Storage:
  connectTimeout: 30000  # 连接超时（毫秒）
  readTimeout: 60000     # 读取超时（毫秒）
```

### 3. 重试配置

```yaml
s3Storage:
  maxRetries: 3      # 最大重试次数
  retryDelay: 1000   # 重试延迟（毫秒）
```

### 4. 使用传输加速

```yaml
s3Storage:
  useAccelerateEndpoint: true  # 启用 S3 传输加速
```

**注意：** 需要在 S3 存储桶上启用传输加速功能。

## 监控和日志

### 日志级别

```yaml
logging:
  level: "INFO"  # DEBUG, INFO, WARN, ERROR
```

### 关键日志

```
[INFO] 使用默认 AWS 凭证链（IAM 角色）
[INFO] AWS S3 适配器初始化完成 - 区域: us-east-1, 最大并发: 10
[INFO] 列出 S3 文件 - Bucket: my-bucket, Prefix: projects/
[INFO] 找到 150 个文件
[INFO] 开始下载文件夹 - Bucket: my-bucket, Prefix: projects/my-app/
[INFO] 文件夹下载完成 - S3DownloadResult{total=150, success=150, failed=0, ...}
```

### 性能指标

下载结果包含详细的性能指标：

```java
S3DownloadResult result = ...;
System.out.println("总文件数: " + result.getTotalFileCount());
System.out.println("成功数: " + result.getSuccessCount());
System.out.println("失败数: " + result.getFailureCount());
System.out.println("成功率: " + result.getSuccessRate() + "%");
System.out.println("总大小: " + result.getTotalSizeInMB() + " MB");
System.out.println("耗时: " + result.getDurationSeconds() + " 秒");
System.out.println("速度: " + result.getDownloadSpeedMBps() + " MB/s");
```

## 错误处理

### 常见错误

| 错误 | 原因 | 解决方案 |
|------|------|---------|
| `Unable to load credentials` | IAM 角色未配置 | 确认 EC2 实例已附加 IAM 角色 |
| `Access Denied` | 权限不足 | 检查 IAM 策略中的 S3 权限 |
| `NoSuchBucket` | 存储桶不存在 | 检查存储桶名称和区域 |
| `NoSuchKey` | 文件不存在 | 检查文件键是否正确 |
| `SlowDown` | 请求过多 | 降低并发数或使用重试 |

### 错误处理示例

```java
try {
    S3DownloadResult result = service.downloadProjectForReview(...);
    
    if (!result.isSuccess()) {
        System.err.println("部分文件下载失败:");
        result.getErrors().forEach(System.err::println);
    }
    
} catch (RuntimeException e) {
    System.err.println("下载失败: " + e.getMessage());
    // 记录日志、发送告警等
}
```

## 测试

### 单元测试

```bash
# 运行测试（需要配置环境变量）
export TEST_S3_BUCKET=my-test-bucket
export TEST_S3_PREFIX=test-projects/sample-app/

mvn test -Dtest=S3StorageAdapterTest
```

### 集成测试

```java
@Test
void testDownloadFolder() {
    S3DownloadResult result = adapter.downloadFolder(
        "test-bucket",
        "test-prefix/"
    );
    
    assertTrue(result.isSuccess());
    assertTrue(result.getTotalFileCount() > 0);
}
```

## 最佳实践

### 1. 资源管理

始终在 `finally` 块中关闭服务：

```java
S3StorageService service = new S3StorageService(adapter);
try {
    // 使用服务
} finally {
    service.shutdown();  // 释放资源
}
```

### 2. 分批处理

对于大量文件，分批处理：

```java
List<S3File> allFiles = adapter.listFiles(bucket, prefix);
int batchSize = 100;

for (int i = 0; i < allFiles.size(); i += batchSize) {
    List<S3File> batch = allFiles.subList(
        i, 
        Math.min(i + batchSize, allFiles.size())
    );
    // 处理批次
}
```

### 3. 内存管理

下载大文件时，使用本地目录而不是内存：

```java
// ❌ 不推荐：大文件会占用大量内存
S3DownloadResult result = service.downloadProjectToMemory(bucket, prefix);

// ✅ 推荐：下载到本地磁盘
S3DownloadResult result = service.downloadProjectForReview(
    bucket, prefix, Paths.get("./temp")
);
```

### 4. 异常处理

区分可重试和不可重试的错误：

```java
try {
    S3File file = adapter.downloadFile(bucket, key);
} catch (NoSuchKeyException e) {
    // 文件不存在，不应重试
    log.error("文件不存在: {}", key);
} catch (S3Exception e) {
    // 其他 S3 错误，可能需要重试
    if (e.statusCode() == 503) {
        // 服务不可用，重试
    }
}
```

## 故障排除

### 检查清单

- [ ] EC2/ECS 实例已附加 IAM 角色
- [ ] IAM 角色有 S3 访问权限
- [ ] 存储桶名称和区域正确
- [ ] 网络连接正常
- [ ] 配置文件格式正确
- [ ] 依赖已正确安装

### 调试技巧

1. **启用 DEBUG 日志：**
   ```yaml
   logging:
     level: "DEBUG"
   ```

2. **验证凭证：**
   ```bash
   # 在 EC2 上检查
   curl http://169.254.169.254/latest/meta-data/iam/security-credentials/
   ```

3. **测试连接：**
   ```java
   boolean available = adapter.isAvailable();
   System.out.println("S3 可用: " + available);
   ```

## 总结

✅ **已实现的功能：**
- 符合六边形架构的设计
- IAM 角色默认凭证支持
- 文件夹批量下载
- 并发和异步支持
- 完善的错误处理
- 性能监控和统计

✅ **使用优势：**
- 松耦合、易测试
- 支持多种凭证方式
- 高性能并发下载
- 详细的日志和监控
- 符合企业级标准

## 相关文档

- [AWS S3 Java SDK 文档](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/home.html)
- [IAM 角色配置指南](AWS-BEDROCK-IAM-SETUP.md)
- [六边形架构说明](../md/refactor/)

