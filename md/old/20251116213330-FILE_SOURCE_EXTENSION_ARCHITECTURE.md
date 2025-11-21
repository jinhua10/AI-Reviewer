# AI-Reviewer 项目架构分析与文件来源扩展方案
**文档创建时间**: 2025-11-16 21:33:30  
**架构师**: AI Architecture Analysis  
**版本**: v1.0
---
## 📋 目录
1. [当前架构分析](#1-当前架构分析)
2. [核心架构模式](#2-核心架构模式)
3. [文件来源扩展需求](#3-文件来源扩展需求)
4. [扩展架构设计](#4-扩展架构设计)
5. [实施方案](#5-实施方案)
6. [代码示例](#6-代码示例)
7. [最佳实践建议](#7-最佳实践建议)
---
## 1. 当前架构分析
### 1.1 整体架构层次
当前 AI-Reviewer 采用**分层架构 + 适配器模式**，结构清晰，职责分离：
\\\
应用层 (Application Layer)
    ↓
核心引擎层 (AI Engine - Core Layer)
    ↓
适配器注册中心 (Adapter Registry - SPI)
    ↓
核心接口层 (API Layer)
\\\
### 1.2 当前模块结构
| 模块 | 职责 | 关键类 |
|------|------|--------|
| **ai-reviewer-api** | 核心接口定义 | IFileParser, IAIService, IResultProcessor |
| **ai-reviewer-common** | 通用工具类 | FileUtil, StringUtil, 异常类 |
| **ai-reviewer-core** | 核心编排引擎 | AIEngine, AdapterRegistry, FileScanner |
| **ai-reviewer-adaptor-parser** | 文件解析适配器 | JavaFileParser, PythonFileParser |
| **ai-reviewer-adaptor-ai** | AI服务适配器 | HttpBasedAIAdapter |
| **ai-reviewer-adaptor-processor** | 结果处理适配器 | CodeReviewProcessor |
| **ai-reviewer-starter** | Spring Boot自动配置 | AutoConfiguration, Properties |
### 1.3 当前执行流程
\\\
ExecutionContext 创建
    ↓
FileScanner.scan(Path directory) ← 【当前局限：仅支持本地目录】
    ↓
FileFilter.filter(patterns)
    ↓
IFileParser.parse(File)
    ↓
IAIService.invoke(PreProcessedData)
    ↓
IResultProcessor.process(AIResponse)
    ↓
ProcessResult 返回
\\\
### 1.4 当前架构的优势
✅ **高度模块化**: 每个模块职责单一，边界清晰  
✅ **适配器模式**: 通过 SPI 支持插件化扩展  
✅ **解耦合设计**: 接口与实现分离  
✅ **Spring Boot 集成**: 开箱即用的自动配置  
✅ **多线程支持**: ExecutorService 实现并发处理
### 1.5 当前架构的局限性
❌ **文件来源单一**: 仅支持本地文件系统（Path directory）  
❌ **缺少来源抽象**: FileScanner 直接依赖 java.nio.file.Path  
❌ **无法支持远程源**: 无法从 SFTP、Git、S3 等获取文件  
❌ **扩展性受限**: 增加新来源需要修改核心代码
---
## 2. 核心架构模式
### 2.1 适配器模式 (Adapter Pattern)
当前项目已经在解析器、AI服务、处理器中使用了适配器模式：
\\\java
// 接口定义
public interface IFileParser {
    boolean support(File file);
    PreProcessedData parse(File file) throws Exception;
    int getPriority();
}
// 适配器注册
AdapterRegistry registry = new AdapterRegistry();
registry.registerParser(new JavaFileParser());
registry.registerParser(new PythonFileParser());
\\\
### 2.2 策略模式 (Strategy Pattern)
AdapterRegistry 使用策略模式选择合适的适配器：
\\\java
public Optional<IFileParser> getParser(File file) {
    return parsers.values().stream()
        .filter(parser -> parser.support(file))
        .max(Comparator.comparingInt(IFileParser::getPriority));
}
\\\
### 2.3 责任链模式 (Chain of Responsibility)
执行流程形成处理链：
- 文件扫描 → 文件过滤 → 文件解析 → AI处理 → 结果处理
---
## 3. 文件来源扩展需求
### 3.1 业务场景
| 场景 | 文件来源 | 使用案例 |
|------|----------|----------|
| **本地开发** | 本地文件系统 | 开发者本地代码审查 |
| **远程服务器** | SFTP/FTP | 审查服务器上的代码 |
| **版本控制** | Git (GitHub/GitLab/Gitee) | CI/CD 集成，PR 审查 |
| **云存储** | AWS S3, Azure Blob, OSS | 云端代码存储审查 |
| **容器环境** | Docker Volume, K8s PVC | 容器化应用代码审查 |
| **归档文件** | ZIP, TAR, JAR | 压缩包内代码审查 |
### 3.2 扩展目标
🎯 **支持多种文件来源**，而不修改核心引擎代码  
🎯 **统一抽象层**，隐藏不同来源的实现细节  
🎯 **插件化架构**，通过 SPI 动态加载文件源适配器  
🎯 **保持兼容性**，现有代码无需修改  
🎯 **易于扩展**，添加新来源只需实现接口
---
## 4. 扩展架构设计
### 4.1 设计原则
1. **开放封闭原则 (OCP)**: 对扩展开放，对修改封闭
2. **依赖倒置原则 (DIP)**: 依赖抽象而非具体实现
3. **单一职责原则 (SRP)**: 每个适配器只负责一种文件源
4. **接口隔离原则 (ISP)**: 接口设计精简，只包含必要方法
### 4.2 新增抽象层：IFileSource
引入 **文件源接口**，抽象文件获取逻辑：
\\\java
/**
 * 文件源接口 - 统一抽象不同来源的文件获取
 */
public interface IFileSource {
    /**
     * 获取文件源名称
     */
    String getSourceName();
    /**
     * 检查是否支持该类型的文件源
     */
    boolean support(FileSourceConfig config);
    /**
     * 初始化连接/会话
     */
    void initialize(FileSourceConfig config) throws Exception;
    /**
     * 获取文件列表
     */
    List<SourceFile> listFiles(String basePath) throws Exception;
    /**
     * 读取文件内容
     */
    InputStream readFile(SourceFile file) throws Exception;
    /**
     * 关闭连接/清理资源
     */
    void close() throws Exception;
    /**
     * 优先级（数字越大优先级越高）
     */
    default int getPriority() {
        return 0;
    }
}
\\\
### 4.3 数据模型设计
#### FileSourceConfig - 文件源配置
\\\java
@Data
@Builder
public class FileSourceConfig {
    // 文件源类型
    private String sourceType; // "local", "sftp", "git", "s3", etc.
    // 基础路径
    private String basePath;
    // 连接配置
    private String host;
    private Integer port;
    private String username;
    private String password;
    private String privateKeyPath;
    // Git 特定配置
    private String repositoryUrl;
    private String branch;
    private String commitId;
    private String accessToken;
    // S3 特定配置
    private String accessKey;
    private String secretKey;
    private String region;
    private String bucket;
    // 通用配置
    private Map<String, Object> customParams;
    private int connectionTimeout;
    private int readTimeout;
}
\\\
#### SourceFile - 文件源文件抽象
\\\java
@Data
@Builder
public class SourceFile {
    // 文件标识
    private String fileId;
    // 文件路径（相对于源）
    private String relativePath;
    // 文件名
    private String fileName;
    // 文件大小
    private long fileSize;
    // 最后修改时间
    private LocalDateTime lastModified;
    // 文件元数据
    private Map<String, Object> metadata;
    // 文件源引用（用于读取内容）
    private IFileSource source;
    /**
     * 读取文件内容
     */
    public InputStream getInputStream() throws Exception {
        return source.readFile(this);
    }
    /**
     * 转换为临时本地文件
     */
    public File toTempFile() throws Exception {
        File tempFile = File.createTempFile("ai-reviewer-", "-" + fileName);
        try (InputStream in = getInputStream();
             FileOutputStream out = new FileOutputStream(tempFile)) {
            in.transferTo(out);
        }
        return tempFile;
    }
}
\\\
### 4.4 修改后的架构层次
\\\
应用层 (Application Layer)
    ↓
核心引擎层 (AI Engine - Core Layer)
    ├─ FileSourceScanner (新增) ← 使用 IFileSource
    ├─ FileFilter
    └─ AIEngine
    ↓
适配器注册中心 (Adapter Registry - SPI)
    ├─ FileSourceRegistry (新增)
    ├─ ParserRegistry
    ├─ AIServiceRegistry
    └─ ProcessorRegistry
    ↓
核心接口层 (API Layer)
    ├─ IFileSource (新增) ← 文件源抽象
    ├─ IFileParser
    ├─ IAIService
    └─ IResultProcessor
    ↓
文件源适配器层 (FileSource Adapters - 新增模块)
    ├─ LocalFileSource
    ├─ SftpFileSource
    ├─ GitFileSource
    └─ S3FileSource
\\\
### 4.5 ExecutionContext 扩展
\\\java
@Data
@Builder
public class ExecutionContext {
    // 原有字段...
    // 新增：文件源配置（替代 targetDirectory）
    private FileSourceConfig fileSourceConfig;
    // 向后兼容：保留 targetDirectory
    @Deprecated
    private Path targetDirectory;
    // ...其他字段
}
\\\
---
## 5. 实施方案
### 5.1 新增模块：ai-reviewer-adaptor-source
创建新的Maven模块，包含文件源适配器实现。
\\\xml
<!-- pom.xml -->
<project>
    <groupId>top.yumbo.ai</groupId>
    <artifactId>ai-reviewer-adaptor-source</artifactId>
    <version>1.0</version>
    <dependencies>
        <dependency>
            <groupId>top.yumbo.ai</groupId>
            <artifactId>ai-reviewer-api</artifactId>
        </dependency>
        <!-- SFTP 支持 -->
        <dependency>
            <groupId>com.jcraft</groupId>
            <artifactId>jsch</artifactId>
            <version>0.1.55</version>
        </dependency>
        <!-- Git 支持 -->
        <dependency>
            <groupId>org.eclipse.jgit</groupId>
            <artifactId>org.eclipse.jgit</artifactId>
            <version>6.7.0.202309050840-r</version>
        </dependency>
        <!-- AWS S3 支持 -->
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>s3</artifactId>
            <version>2.21.0</version>
        </dependency>
    </dependencies>
</project>
\\\
### 5.2 目录结构
\\\
ai-reviewer-adaptor-source/
├── pom.xml
└── src/
    └── main/
        ├── java/
        │   └── top/yumbo/ai/adaptor/source/
        │       ├── LocalFileSource.java
        │       ├── SftpFileSource.java
        │       ├── GitFileSource.java
        │       ├── S3FileSource.java
        │       └── AbstractFileSource.java
        └── resources/
            └── META-INF/
                └── services/
                    └── top.yumbo.ai.api.source.IFileSource
\\\
### 5.3 核心接口修改
在 **ai-reviewer-api** 模块中新增接口：
\\\java
// IFileSource.java
package top.yumbo.ai.api.source;
public interface IFileSource extends AutoCloseable {
    String getSourceName();
    boolean support(FileSourceConfig config);
    void initialize(FileSourceConfig config) throws Exception;
    List<SourceFile> listFiles(String basePath) throws Exception;
    InputStream readFile(SourceFile file) throws Exception;
    default int getPriority() { return 0; }
}
\\\
### 5.4 核心引擎修改
修改 **AIEngine** 以支持 IFileSource：
\\\java
public class AIEngine {
    protected final AdapterRegistry registry;
    protected final FileSourceScanner fileSourceScanner; // 新增
    protected final FileFilter fileFilter;
    public AIEngine(AdapterRegistry registry) {
        this.registry = registry;
        this.fileSourceScanner = new FileSourceScanner(registry); // 新增
        this.fileFilter = new FileFilter();
    }
    public ProcessResult execute(ExecutionContext context) {
        // ...
        // Step 1: 获取文件源
        IFileSource fileSource = registry.getFileSource(context.getFileSourceConfig())
            .orElseThrow(() -> new AIReviewerException("No file source found"));
        try {
            fileSource.initialize(context.getFileSourceConfig());
            // Step 2: 扫描文件
            List<SourceFile> sourceFiles = fileSource.listFiles(
                context.getFileSourceConfig().getBasePath()
            );
            // Step 3: 过滤文件
            List<SourceFile> filteredFiles = fileFilter.filter(
                sourceFiles, 
                context.getIncludePatterns(),
                context.getExcludePatterns()
            );
            // Step 4: 解析文件（需要转换为临时File）
            List<PreProcessedData> preprocessedDataList = parseSourceFiles(filteredFiles);
            // ...后续流程不变
        } finally {
            fileSource.close();
        }
    }
    private List<PreProcessedData> parseSourceFiles(List<SourceFile> files) {
        return files.stream()
            .map(sf -> {
                try {
                    File tempFile = sf.toTempFile();
                    IFileParser parser = registry.getParser(tempFile)
                        .orElseThrow(() -> new AIReviewerException("No parser found"));
                    return parser.parse(tempFile);
                } catch (Exception e) {
                    throw new AIReviewerException("Parse failed", e);
                }
            })
            .collect(Collectors.toList());
    }
}
\\\
### 5.5 AdapterRegistry 扩展
\\\java
public class AdapterRegistry {
    private final Map<String, IFileSource> fileSources = new ConcurrentHashMap<>();
    public void registerFileSource(IFileSource fileSource) {
        fileSources.put(fileSource.getSourceName(), fileSource);
        log.info("Registered file source: {}", fileSource.getSourceName());
    }
    public Optional<IFileSource> getFileSource(FileSourceConfig config) {
        return fileSources.values().stream()
            .filter(fs -> fs.support(config))
            .max(Comparator.comparingInt(IFileSource::getPriority));
    }
    public Collection<IFileSource> getAllFileSources() {
        return Collections.unmodifiableCollection(fileSources.values());
    }
}
\\\
---
## 6. 代码示例
### 6.1 本地文件源实现
\\\java
package top.yumbo.ai.adaptor.source;
@Slf4j
public class LocalFileSource implements IFileSource {
    private Path basePath;
    @Override
    public String getSourceName() {
        return "local";
    }
    @Override
    public boolean support(FileSourceConfig config) {
        return "local".equalsIgnoreCase(config.getSourceType());
    }
    @Override
    public void initialize(FileSourceConfig config) throws Exception {
        this.basePath = Paths.get(config.getBasePath());
        if (!Files.exists(basePath)) {
            throw new FileSourceException("Path does not exist: " + basePath);
        }
    }
    @Override
    public List<SourceFile> listFiles(String path) throws Exception {
        Path targetPath = basePath.resolve(path);
        List<SourceFile> result = new ArrayList<>();
        Files.walk(targetPath)
            .filter(Files::isRegularFile)
            .forEach(p -> {
                try {
                    result.add(SourceFile.builder()
                        .fileId(p.toString())
                        .relativePath(basePath.relativize(p).toString())
                        .fileName(p.getFileName().toString())
                        .fileSize(Files.size(p))
                        .lastModified(LocalDateTime.ofInstant(
                            Files.getLastModifiedTime(p).toInstant(),
                            ZoneId.systemDefault()))
                        .source(this)
                        .build());
                } catch (IOException e) {
                    log.warn("Failed to process file: {}", p, e);
                }
            });
        return result;
    }
    @Override
    public InputStream readFile(SourceFile file) throws Exception {
        return Files.newInputStream(Paths.get(file.getFileId()));
    }
    @Override
    public void close() {
        // 本地文件源无需关闭操作
    }
    @Override
    public int getPriority() {
        return 100; // 最高优先级，作为默认实现
    }
}
\\\
### 6.2 SFTP 文件源实现
\\\java
package top.yumbo.ai.adaptor.source;
import com.jcraft.jsch.*;
@Slf4j
public class SftpFileSource implements IFileSource {
    private Session session;
    private ChannelSftp sftpChannel;
    private String basePath;
    @Override
    public String getSourceName() {
        return "sftp";
    }
    @Override
    public boolean support(FileSourceConfig config) {
        return "sftp".equalsIgnoreCase(config.getSourceType());
    }
    @Override
    public void initialize(FileSourceConfig config) throws Exception {
        JSch jsch = new JSch();
        // 配置私钥（如果有）
        if (config.getPrivateKeyPath() != null) {
            jsch.addIdentity(config.getPrivateKeyPath());
        }
        // 创建会话
        session = jsch.getSession(
            config.getUsername(),
            config.getHost(),
            config.getPort() != null ? config.getPort() : 22
        );
        if (config.getPassword() != null) {
            session.setPassword(config.getPassword());
        }
        // 跳过主机密钥检查（生产环境应严格验证）
        session.setConfig("StrictHostKeyChecking", "no");
        session.setTimeout(config.getConnectionTimeout());
        // 连接
        session.connect();
        // 打开SFTP通道
        Channel channel = session.openChannel("sftp");
        channel.connect();
        sftpChannel = (ChannelSftp) channel;
        this.basePath = config.getBasePath();
        log.info("SFTP connection established: {}@{}", 
            config.getUsername(), config.getHost());
    }
    @Override
    public List<SourceFile> listFiles(String path) throws Exception {
        List<SourceFile> result = new ArrayList<>();
        String fullPath = basePath + "/" + path;
        listFilesRecursive(fullPath, "", result);
        return result;
    }
    private void listFilesRecursive(String currentPath, String relativePath, 
                                    List<SourceFile> result) throws SftpException {
        @SuppressWarnings("unchecked")
        Vector<ChannelSftp.LsEntry> entries = sftpChannel.ls(currentPath);
        for (ChannelSftp.LsEntry entry : entries) {
            String filename = entry.getFilename();
            if (".".equals(filename) || "..".equals(filename)) {
                continue;
            }
            SftpATTRS attrs = entry.getAttrs();
            String entryPath = currentPath + "/" + filename;
            String entryRelative = relativePath.isEmpty() ? filename : relativePath + "/" + filename;
            if (attrs.isDir()) {
                // 递归处理目录
                listFilesRecursive(entryPath, entryRelative, result);
            } else {
                result.add(SourceFile.builder()
                    .fileId(entryPath)
                    .relativePath(entryRelative)
                    .fileName(filename)
                    .fileSize(attrs.getSize())
                    .lastModified(LocalDateTime.ofEpochSecond(
                        attrs.getMTime(), 0, ZoneOffset.UTC))
                    .source(this)
                    .build());
            }
        }
    }
    @Override
    public InputStream readFile(SourceFile file) throws Exception {
        return sftpChannel.get(file.getFileId());
    }
    @Override
    public void close() throws Exception {
        if (sftpChannel != null && sftpChannel.isConnected()) {
            sftpChannel.disconnect();
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
        log.info("SFTP connection closed");
    }
    @Override
    public int getPriority() {
        return 50;
    }
}
\\\
### 6.3 Git 文件源实现
\\\java
package top.yumbo.ai.adaptor.source;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.transport.*;
@Slf4j
public class GitFileSource implements IFileSource {
    private Repository repository;
    private Path localClonePath;
    @Override
    public String getSourceName() {
        return "git";
    }
    @Override
    public boolean support(FileSourceConfig config) {
        return "git".equalsIgnoreCase(config.getSourceType());
    }
    @Override
    public void initialize(FileSourceConfig config) throws Exception {
        // 创建临时目录
        localClonePath = Files.createTempDirectory("ai-reviewer-git-");
        log.info("Cloning repository: {} to {}", 
            config.getRepositoryUrl(), localClonePath);
        // 配置克隆命令
        CloneCommand cloneCommand = Git.cloneRepository()
            .setURI(config.getRepositoryUrl())
            .setDirectory(localClonePath.toFile())
            .setBranch(config.getBranch() != null ? config.getBranch() : "main");
        // 配置认证
        if (config.getAccessToken() != null) {
            cloneCommand.setCredentialsProvider(
                new UsernamePasswordCredentialsProvider(
                    config.getAccessToken(), ""));
        } else if (config.getUsername() != null && config.getPassword() != null) {
            cloneCommand.setCredentialsProvider(
                new UsernamePasswordCredentialsProvider(
                    config.getUsername(), config.getPassword()));
        }
        // 执行克隆
        Git git = cloneCommand.call();
        repository = git.getRepository();
        // 如果指定了 commitId，checkout 到该提交
        if (config.getCommitId() != null) {
            git.checkout().setName(config.getCommitId()).call();
        }
        git.close();
        log.info("Repository cloned successfully");
    }
    @Override
    public List<SourceFile> listFiles(String path) throws Exception {
        Path basePath = localClonePath.resolve(path);
        List<SourceFile> result = new ArrayList<>();
        Files.walk(basePath)
            .filter(Files::isRegularFile)
            .filter(p -> !p.toString().contains(".git")) // 排除 .git 目录
            .forEach(p -> {
                try {
                    result.add(SourceFile.builder()
                        .fileId(p.toString())
                        .relativePath(localClonePath.relativize(p).toString())
                        .fileName(p.getFileName().toString())
                        .fileSize(Files.size(p))
                        .lastModified(LocalDateTime.ofInstant(
                            Files.getLastModifiedTime(p).toInstant(),
                            ZoneId.systemDefault()))
                        .source(this)
                        .build());
                } catch (IOException e) {
                    log.warn("Failed to process file: {}", p, e);
                }
            });
        return result;
    }
    @Override
    public InputStream readFile(SourceFile file) throws Exception {
        return Files.newInputStream(Paths.get(file.getFileId()));
    }
    @Override
    public void close() throws Exception {
        if (repository != null) {
            repository.close();
        }
        // 清理临时目录
        if (localClonePath != null && Files.exists(localClonePath)) {
            FileUtils.deleteDirectory(localClonePath.toFile());
            log.info("Cleaned up temporary git clone: {}", localClonePath);
        }
    }
    @Override
    public int getPriority() {
        return 60;
    }
}
\\\
### 6.4 AWS S3 文件源实现
\\\java
package top.yumbo.ai.adaptor.source;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.*;
import software.amazon.awssdk.services.s3.model.*;
@Slf4j
public class S3FileSource implements IFileSource {
    private S3Client s3Client;
    private String bucket;
    private String prefix;
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
        // 配置认证
        AwsCredentials credentials = AwsBasicCredentials.create(
            config.getAccessKey(),
            config.getSecretKey()
        );
        // 创建S3客户端
        this.s3Client = S3Client.builder()
            .region(Region.of(config.getRegion()))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .build();
        this.bucket = config.getBucket();
        this.prefix = config.getBasePath();
        log.info("S3 client initialized: bucket={}, region={}", 
            bucket, config.getRegion());
    }
    @Override
    public List<SourceFile> listFiles(String path) throws Exception {
        List<SourceFile> result = new ArrayList<>();
        String fullPrefix = prefix + "/" + path;
        ListObjectsV2Request request = ListObjectsV2Request.builder()
            .bucket(bucket)
            .prefix(fullPrefix)
            .build();
        ListObjectsV2Response response;
        do {
            response = s3Client.listObjectsV2(request);
            for (S3Object s3Object : response.contents()) {
                String key = s3Object.key();
                // 跳过"目录"对象
                if (key.endsWith("/")) {
                    continue;
                }
                result.add(SourceFile.builder()
                    .fileId(key)
                    .relativePath(key.substring(prefix.length() + 1))
                    .fileName(Paths.get(key).getFileName().toString())
                    .fileSize(s3Object.size())
                    .lastModified(LocalDateTime.ofInstant(
                        s3Object.lastModified(), ZoneId.systemDefault()))
                    .source(this)
                    .metadata(Map.of("etag", s3Object.eTag()))
                    .build());
            }
            // 处理分页
            request = request.toBuilder()
                .continuationToken(response.nextContinuationToken())
                .build();
        } while (response.isTruncated());
        return result;
    }
    @Override
    public InputStream readFile(SourceFile file) throws Exception {
        GetObjectRequest request = GetObjectRequest.builder()
            .bucket(bucket)
            .key(file.getFileId())
            .build();
        return s3Client.getObject(request);
    }
    @Override
    public void close() {
        if (s3Client != null) {
            s3Client.close();
            log.info("S3 client closed");
        }
    }
    @Override
    public int getPriority() {
        return 40;
    }
}
\\\
---
## 7. 最佳实践建议
### 7.1 配置管理
在 application.yml 中配置不同的文件源：
\\\yaml
ai-reviewer:
  file-source:
    # 本地文件源
    local:
      type: local
      base-path: /path/to/project
    # SFTP 文件源
    sftp:
      type: sftp
      host: sftp.example.com
      port: 22
      username: user
      password: pass # 建议使用环境变量
      base-path: /remote/project
      connection-timeout: 30000
    # Git 文件源
    git:
      type: git
      repository-url: https://github.com/user/repo.git
      branch: main
      access-token: ''''
      base-path: src
    # S3 文件源
    s3:
      type: s3
      bucket: my-code-bucket
      region: us-east-1
      access-key: ''''
      secret-key: ''''
      base-path: projects/myproject
\\\
### 7.2 使用示例
#### 示例 1: 审查本地项目
\\\java
FileSourceConfig config = FileSourceConfig.builder()
    .sourceType("local")
    .basePath("/path/to/project")
    .build();
ExecutionContext context = ExecutionContext.builder()
    .fileSourceConfig(config)
    .includePatterns(List.of("**/*.java"))
    .excludePatterns(List.of("**/target/**"))
    .aiConfig(aiConfig)
    .processorConfig(processorConfig)
    .build();
ProcessResult result = aiEngine.execute(context);
\\\
#### 示例 2: 审查 SFTP 服务器上的代码
\\\java
FileSourceConfig config = FileSourceConfig.builder()
    .sourceType("sftp")
    .host("sftp.example.com")
    .port(22)
    .username("developer")
    .password(System.getenv("SFTP_PASSWORD"))
    .basePath("/app/src")
    .connectionTimeout(30000)
    .build();
ExecutionContext context = ExecutionContext.builder()
    .fileSourceConfig(config)
    .includePatterns(List.of("**/*.java", "**/*.py"))
    .excludePatterns(List.of("**/test/**"))
    .aiConfig(aiConfig)
    .processorConfig(processorConfig)
    .build();
ProcessResult result = aiEngine.execute(context);
\\\
#### 示例 3: 审查 GitHub 仓库
\\\java
FileSourceConfig config = FileSourceConfig.builder()
    .sourceType("git")
    .repositoryUrl("https://github.com/user/repo.git")
    .branch("develop")
    .accessToken(System.getenv("GITHUB_TOKEN"))
    .basePath("src/main/java")
    .build();
ExecutionContext context = ExecutionContext.builder()
    .fileSourceConfig(config)
    .includePatterns(List.of("**/*.java"))
    .aiConfig(aiConfig)
    .processorConfig(processorConfig)
    .build();
ProcessResult result = aiEngine.execute(context);
\\\
#### 示例 4: 审查 AWS S3 中的代码
\\\java
FileSourceConfig config = FileSourceConfig.builder()
    .sourceType("s3")
    .bucket("my-code-archive")
    .region("us-west-2")
    .accessKey(System.getenv("AWS_ACCESS_KEY"))
    .secretKey(System.getenv("AWS_SECRET_KEY"))
    .basePath("projects/myapp/src")
    .build();
ExecutionContext context = ExecutionContext.builder()
    .fileSourceConfig(config)
    .includePatterns(List.of("**/*.java"))
    .aiConfig(aiConfig)
    .processorConfig(processorConfig)
    .build();
ProcessResult result = aiEngine.execute(context);
\\\
### 7.3 Application Demo 扩展示例
在 application-demo 下创建新的应用示例：
\\\
application-demo/
├── hackathonApplication/          # 现有示例
├── sftpReviewApplication/         # 新增：SFTP 审查示例
├── gitReviewApplication/          # 新增：Git 审查示例
└── s3ReviewApplication/           # 新增：S3 审查示例
\\\
#### sftpReviewApplication 示例
\\\java
@SpringBootApplication
public class SftpReviewApplication {
    @Autowired
    private AIEngine aiEngine;
    @Autowired
    private AIReviewerProperties properties;
    public static void main(String[] args) {
        SpringApplication.run(SftpReviewApplication.class, args);
    }
    @Bean
    CommandLineRunner runSftpReview() {
        return args -> {
            FileSourceConfig sftpConfig = FileSourceConfig.builder()
                .sourceType("sftp")
                .host(properties.getFileSource().getSftp().getHost())
                .port(properties.getFileSource().getSftp().getPort())
                .username(properties.getFileSource().getSftp().getUsername())
                .password(properties.getFileSource().getSftp().getPassword())
                .basePath(properties.getFileSource().getSftp().getBasePath())
                .build();
            ExecutionContext context = ExecutionContext.builder()
                .fileSourceConfig(sftpConfig)
                .includePatterns(properties.getScanner().getIncludePatterns())
                .excludePatterns(properties.getScanner().getExcludePatterns())
                .aiConfig(buildAIConfig(properties))
                .processorConfig(buildProcessorConfig(properties))
                .threadPoolSize(properties.getExecutor().getThreadPoolSize())
                .build();
            ProcessResult result = aiEngine.execute(context);
            if (result.isSuccess()) {
                log.info("SFTP Review completed successfully!");
                log.info("Report: {}", result.getReportPath());
            } else {
                log.error("Review failed: {}", result.getErrorMessage());
            }
        };
    }
}
\\\
### 7.4 性能优化建议
#### 7.4.1 连接池管理
对于频繁访问的远程源，建议使用连接池：
\\\java
public class PooledSftpFileSource implements IFileSource {
    private static final ObjectPool<ChannelSftp> sftpPool = 
        new GenericObjectPool<>(new SftpChannelFactory());
    @Override
    public InputStream readFile(SourceFile file) throws Exception {
        ChannelSftp channel = sftpPool.borrowObject();
        try {
            return channel.get(file.getFileId());
        } finally {
            sftpPool.returnObject(channel);
        }
    }
}
\\\
#### 7.4.2 缓存机制
对于 Git 仓库，可以缓存已克隆的本地副本：
\\\java
public class CachedGitFileSource extends GitFileSource {
    private static final Map<String, Path> repoCache = new ConcurrentHashMap<>();
    @Override
    public void initialize(FileSourceConfig config) throws Exception {
        String cacheKey = config.getRepositoryUrl() + "#" + config.getBranch();
        if (repoCache.containsKey(cacheKey)) {
            localClonePath = repoCache.get(cacheKey);
            // 执行 git pull 更新
            Git.open(localClonePath.toFile()).pull().call();
        } else {
            super.initialize(config);
            repoCache.put(cacheKey, localClonePath);
        }
    }
}
\\\
#### 7.4.3 并行下载
对于大量小文件，使用并行下载：
\\\java
private List<PreProcessedData> parseSourceFiles(List<SourceFile> files) {
    return files.parallelStream()
        .map(sf -> {
            try {
                File tempFile = sf.toTempFile();
                IFileParser parser = registry.getParser(tempFile)
                    .orElseThrow(() -> new AIReviewerException("No parser found"));
                return parser.parse(tempFile);
            } catch (Exception e) {
                log.error("Failed to parse: {}", sf.getFileName(), e);
                return null;
            }
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
}
\\\
### 7.5 安全最佳实践
#### 7.5.1 凭证管理
永远不要在代码中硬编码凭证：
\\\java
// ❌ 错误示例
FileSourceConfig config = FileSourceConfig.builder()
    .username("admin")
    .password("password123") // 危险！
    .build();
// ✅ 正确示例
FileSourceConfig config = FileSourceConfig.builder()
    .username(System.getenv("SFTP_USERNAME"))
    .password(System.getenv("SFTP_PASSWORD"))
    .build();
\\\
#### 7.5.2 SSH 密钥认证
优先使用 SSH 密钥而非密码：
\\\java
FileSourceConfig config = FileSourceConfig.builder()
    .sourceType("sftp")
    .host("sftp.example.com")
    .username("developer")
    .privateKeyPath("/home/user/.ssh/id_rsa")
    .build();
\\\
#### 7.5.3 临时文件清理
确保临时文件被正确清理：
\\\java
public File toTempFile() throws Exception {
    File tempFile = File.createTempFile("ai-reviewer-", "-" + fileName);
    tempFile.deleteOnExit(); // JVM 退出时删除
    try (InputStream in = getInputStream();
         FileOutputStream out = new FileOutputStream(tempFile)) {
        in.transferTo(out);
    }
    return tempFile;
}
\\\
### 7.6 错误处理
#### 7.6.1 重试机制
\\\java
public class ResilientFileSource implements IFileSource {
    private final IFileSource delegate;
    private final int maxRetries = 3;
    @Override
    public InputStream readFile(SourceFile file) throws Exception {
        int attempt = 0;
        Exception lastException = null;
        while (attempt < maxRetries) {
            try {
                return delegate.readFile(file);
            } catch (Exception e) {
                lastException = e;
                attempt++;
                if (attempt < maxRetries) {
                    Thread.sleep(1000 * attempt); // 指数退避
                }
            }
        }
        throw new FileSourceException("Failed after " + maxRetries + " attempts", lastException);
    }
}
\\\
#### 7.6.2 超时控制
\\\java
public InputStream readFile(SourceFile file) throws Exception {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Future<InputStream> future = executor.submit(() -> doReadFile(file));
    try {
        return future.get(30, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
        future.cancel(true);
        throw new FileSourceException("Read timeout: " + file.getFileName());
    } finally {
        executor.shutdown();
    }
}
\\\
---
## 8. 实施路线图
### 阶段 1：基础架构 (1-2周)
- [ ] 在 ai-reviewer-api 中定义 IFileSource 接口
- [ ] 添加 FileSourceConfig 和 SourceFile 数据模型
- [ ] 扩展 AdapterRegistry 支持文件源注册
- [ ] 修改 ExecutionContext 添加文件源配置
- [ ] 更新 AIEngine 支持 IFileSource
### 阶段 2：本地实现 (1周)
- [ ] 创建 ai-reviewer-adaptor-source 模块
- [ ] 实现 LocalFileSource（向后兼容）
- [ ] 编写单元测试
- [ ] 集成测试验证
### 阶段 3：远程源实现 (2-3周)
- [ ] 实现 SftpFileSource
- [ ] 实现 GitFileSource (支持 GitHub/GitLab/Gitee)
- [ ] 实现 S3FileSource
- [ ] 为每个实现编写测试
### 阶段 4：应用示例 (1周)
- [ ] 创建 sftpReviewApplication 示例
- [ ] 创建 gitReviewApplication 示例
- [ ] 创建 s3ReviewApplication 示例
- [ ] 编写使用文档
### 阶段 5：优化与发布 (1-2周)
- [ ] 性能优化（连接池、缓存）
- [ ] 安全审计
- [ ] 文档完善
- [ ] 发布新版本
---
## 9. 总结
### 9.1 架构优势
✅ **高度可扩展**: 通过 IFileSource 接口轻松添加新的文件源  
✅ **向后兼容**: 现有代码无需修改，通过 LocalFileSource 保持兼容  
✅ **职责清晰**: 文件获取与文件处理解耦  
✅ **插件化**: 通过 SPI 动态加载适配器  
✅ **统一抽象**: 不同来源的文件使用统一的 SourceFile 表示
### 9.2 技术栈
| 组件 | 技术选型 | 版本 |
|------|----------|------|
| SFTP | JSch | 0.1.55 |
| Git | JGit | 6.7.0+ |
| AWS S3 | AWS SDK for Java 2.x | 2.21.0+ |
| 连接池 | Apache Commons Pool 2 | 2.11.1+ |
| 缓存 | Caffeine | 3.1.8+ |
### 9.3 下一步行动
1. **Review 本架构文档** - 与团队讨论设计方案
2. **创建 POC** - 实现 LocalFileSource 和 SftpFileSource 原型
3. **性能测试** - 验证大规模文件处理能力
4. **安全评审** - 确保凭证管理和数据传输安全
5. **开始实施** - 按照路线图逐步推进
---
## 10. 附录
### 10.1 相关设计模式
- **适配器模式**: IFileSource 适配不同的文件来源
- **工厂模式**: AdapterRegistry 作为工厂创建适配器
- **策略模式**: 根据配置选择不同的文件源策略
- **模板方法模式**: AbstractFileSource 提供通用实现模板
- **代理模式**: ResilientFileSource 为原始文件源添加重试逻辑
### 10.2 参考资料
- [JGit Documentation](https://www.eclipse.org/jgit/documentation/)
- [AWS SDK for Java 2.x](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/)
- [JSch - Java Secure Channel](http://www.jcraft.com/jsch/)
- [Design Patterns in Java](https://refactoring.guru/design-patterns/java)
### 10.3 联系方式
如有任何问题或建议，请联系架构团队：
- 📧 Email: architecture@example.com
- 💬 Slack: #ai-reviewer-dev
- 📝 Wiki: https://wiki.example.com/ai-reviewer
---
**文档结束**
© 2025 AI-Reviewer Architecture Team. All Rights Reserved.
