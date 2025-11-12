# AI-Reviewer 架构改进行动清单

> **生成时间**: 2025-11-12 16:35:00  
> **执行周期**: 4周  
> **团队规模**: 2-3人  
> **风险等级**: 中

---

## 📋 执行概览

本清单将 **7个关键问题** 拆解为 **35个可执行任务**，每个任务都有明确的：
- ✅ 验收标准
- ⏱️ 预计工时
- 👤 建议负责人
- 🔗 依赖关系

---

## 🎯 第一周: P0 紧急修复

### 任务1: 统一 RepositoryPort 接口

#### 1.1 增强 RepositoryPort 定义 (1h)

**文件**: `application/port/output/RepositoryPort.java`

**任务**:
- [ ] 添加 `cloneRepositoryAtCommit()` 方法
- [ ] 添加 `getCommitHistory()` 方法
- [ ] 添加 `getBranches()` 方法
- [ ] 添加 `getRepositorySize()` 方法
- [ ] 统一异常定义 `RepositoryException`

**验收标准**:
```java
// 编译通过
mvn compile

// 接口方法签名正确
interface RepositoryPort {
    Path cloneRepository(CloneRequest request);
    Path cloneRepositoryAtCommit(String url, String commit);
    List<CommitInfo> getCommitHistory(String url, int max);
    List<String> getBranches(String url);
    // ...
}
```

**负责人**: 后端开发 A

---

#### 1.2 修改 GitHubAdapter 实现 RepositoryPort (1.5h)

**文件**: `adapter/output/repository/GitHubAdapter.java`

**任务**:
- [ ] 移除对 GitHubPort 的依赖
- [ ] 实现 RepositoryPort 所有方法
- [ ] 适配 CloneRequest 参数
- [ ] 更新单元测试

**代码示例**:
```java
public class GitHubAdapter implements RepositoryPort {
    @Override
    public Path cloneRepository(CloneRequest request) throws RepositoryException {
        // 使用 JGit 实现克隆
        return Git.cloneRepository()
            .setURI(request.url())
            .setBranch(request.branch())
            .setDirectory(localPath.toFile())
            .call()
            .getRepository()
            .getDirectory()
            .toPath();
    }
    
    // 实现其他方法...
}
```

**验收标准**:
- [ ] 编译通过
- [ ] 单元测试通过
- [ ] 可以克隆 GitHub 仓库

**依赖**: 任务1.1 完成

---

#### 1.3 修改 GiteeAdapter 实现 RepositoryPort (1.5h)

**文件**: `adapter/output/repository/GiteeAdapter.java`

**任务**: 同 1.2，针对 Gitee

**验收标准**:
- [ ] 编译通过
- [ ] 单元测试通过
- [ ] 可以克隆 Gitee 仓库

**依赖**: 任务1.1 完成

---

#### 1.4 删除 GitHubPort 接口 (0.5h)

**任务**:
- [ ] 删除 `adapter/input/hackathon/domain/port/GitHubPort.java`
- [ ] 删除相关测试文件
- [ ] 更新所有 import 语句

**命令**:
```bash
# 1. 删除文件
rm -rf src/main/java/top/yumbo/ai/reviewer/adapter/input/hackathon/domain/port/

# 2. 查找所有引用
grep -r "GitHubPort" src/

# 3. 替换为 RepositoryPort
find src/ -name "*.java" -exec sed -i 's/GitHubPort/RepositoryPort/g' {} \;
```

**验收标准**:
- [ ] GitHubPort 相关文件已删除
- [ ] 编译通过
- [ ] 所有测试通过

**依赖**: 任务1.2, 1.3 完成

---

#### 1.5 移动适配器到正确位置 (0.5h)

**任务**:
```bash
# 移动 GitHubAdapter
mv src/main/java/top/yumbo/ai/reviewer/adapter/input/hackathon/adapter/output/github/GitHubAdapter.java \
   src/main/java/top/yumbo/ai/reviewer/adapter/output/repository/GitHubAdapter.java

# 移动 GiteeAdapter
mv src/main/java/top/yumbo/ai/reviewer/adapter/input/hackathon/adapter/output/gitee/GiteeAdapter.java \
   src/main/java/top/yumbo/ai/reviewer/adapter/output/repository/GiteeAdapter.java

# 删除空目录
rm -rf src/main/java/top/yumbo/ai/reviewer/adapter/input/hackathon/adapter/
```

**验收标准**:
- [ ] 目录结构正确
- [ ] 包名已更新
- [ ] 编译通过

**依赖**: 任务1.4 完成

---

### 任务2: 引入依赖注入框架

#### 2.1 添加 Guice 依赖 (0.5h)

**文件**: `pom.xml`, `hackathon-ai.xml`

**任务**:
```xml
<dependency>
    <groupId>com.google.inject</groupId>
    <artifactId>guice</artifactId>
    <version>7.0.0</version>
</dependency>
```

**验收标准**:
- [ ] 依赖添加成功
- [ ] Maven 构建通过
- [ ] JAR 包含 Guice

---

#### 2.2 创建配置加载器 (2h)

**文件**: `infrastructure/config/ConfigurationLoader.java`

**任务**:
- [ ] 实现 YAML 文件加载
- [ ] 实现环境变量覆盖
- [ ] 实现配置验证
- [ ] 添加单元测试

**代码框架**:
```java
public class ConfigurationLoader {
    public static Configuration load() {
        Configuration config = new Configuration();
        
        // 1. 从 YAML 加载
        loadFromYaml(config);
        
        // 2. 环境变量覆盖
        overrideFromEnv(config);
        
        // 3. 验证
        validate(config);
        
        return config;
    }
}
```

**验收标准**:
- [ ] 可以加载 config.yaml
- [ ] 环境变量覆盖生效
- [ ] 配置验证工作
- [ ] 单元测试通过

---

#### 2.3 创建 Guice 模块 (2h)

**文件**: `infrastructure/di/ApplicationModule.java`

**任务**:
- [ ] 定义所有接口绑定
- [ ] 实现 AI 服务工厂方法
- [ ] 实现仓库服务工厂方法
- [ ] 配置单例作用域

**代码框架**:
```java
public class ApplicationModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ProjectAnalysisUseCase.class)
            .to(ProjectAnalysisService.class)
            .in(Singleton.class);
        
        bind(CachePort.class)
            .to(FileCacheAdapter.class)
            .in(Singleton.class);
    }
    
    @Provides
    @Singleton
    public AIServicePort provideAIService(Configuration config) {
        return AIServiceFactory.create(config.getAIConfig());
    }
}
```

**验收标准**:
- [ ] 所有服务正确绑定
- [ ] 工厂方法工作
- [ ] 单例作用域正确

---

#### 2.4 修改 CommandLineAdapter 使用 DI (2h)

**文件**: `adapter/input/cli/CommandLineAdapter.java`

**任务**:
- [ ] 删除硬编码依赖创建
- [ ] 添加 @Inject 构造函数
- [ ] 修改 main 方法使用 Injector
- [ ] 更新测试

**代码变更**:
```java
// Before ❌
public CommandLineAdapter() {
    DeepSeekAIAdapter aiAdapter = createAIAdapter();
    this.analysisUseCase = new ProjectAnalysisService(aiAdapter, ...);
}

// After ✅
@Inject
public CommandLineAdapter(
        ProjectAnalysisUseCase analysisUseCase,
        ReportGenerationUseCase reportUseCase) {
    this.analysisUseCase = analysisUseCase;
    this.reportUseCase = reportUseCase;
}

public static void main(String[] args) {
    Configuration config = ConfigurationLoader.load();
    Injector injector = Guice.createInjector(new ApplicationModule(config));
    CommandLineAdapter cli = injector.getInstance(CommandLineAdapter.class);
    cli.run(args);
}
```

**验收标准**:
- [ ] 无硬编码依赖
- [ ] 可以切换 AI 服务
- [ ] config.yaml 配置生效
- [ ] 所有测试通过

**依赖**: 任务2.1, 2.2, 2.3 完成

---

#### 2.5 创建 AI 服务工厂 (1h)

**文件**: `adapter/output/ai/AIServiceFactory.java`

**任务**:
```java
public class AIServiceFactory {
    public static AIServicePort create(AIServiceConfig config) {
        return switch (config.provider()) {
            case "deepseek" -> new DeepSeekAIAdapter(config);
            case "openai" -> new OpenAIAdapter(config);
            case "claude" -> new ClaudeAdapter(config);
            case "gemini" -> new GeminiAdapter(config);
            case "bedrock" -> new BedrockAdapter(config);
            default -> throw new IllegalArgumentException(
                "未知的 AI 服务: " + config.provider()
            );
        };
    }
}
```

**验收标准**:
- [ ] 支持所有 5 种 AI 服务
- [ ] 根据配置正确创建
- [ ] 错误提示友好

---

### 任务3: 修复配置管理

#### 3.1 定义 Configuration 类 (1h)

**文件**: `infrastructure/config/Configuration.java`

**任务**:
```java
@Data
public class Configuration {
    // AI 配置
    private String aiProvider;
    private String aiApiKey;
    private String aiBaseUrl;
    private String aiModel;
    private int aiMaxTokens;
    private double aiTemperature;
    
    // 文件系统配置
    private List<String> fileSystemIncludePatterns;
    private List<String> fileSystemExcludePatterns;
    
    // 缓存配置
    private boolean cacheEnabled;
    private int cacheTtlHours;
    
    // 验证方法
    public void validate() {
        if (aiApiKey == null || aiApiKey.isBlank()) {
            throw new IllegalStateException("AI API Key 未配置");
        }
        // 其他验证...
    }
}
```

**验收标准**:
- [ ] 包含所有配置项
- [ ] 验证方法完整
- [ ] 支持构建器模式

---

#### 3.2 实现 YAML 映射 (1.5h)

**任务**:
- [ ] 使用 Jackson YAML 解析
- [ ] 处理嵌套结构
- [ ] 支持默认值
- [ ] 错误处理

**验收标准**:
- [ ] 可以解析 config.yaml
- [ ] 所有字段正确映射
- [ ] 错误提示清晰

---

#### 3.3 集成测试 (0.5h)

**任务**:
```java
@Test
public void shouldLoadConfigFromYaml() {
    Configuration config = ConfigurationLoader.load();
    
    assertThat(config.getAIProvider()).isEqualTo("deepseek");
    assertThat(config.getAIApiKey()).isNotNull();
}

@Test
public void shouldOverrideFromEnv() {
    System.setProperty("AI_PROVIDER", "openai");
    Configuration config = ConfigurationLoader.load();
    
    assertThat(config.getAIProvider()).isEqualTo("openai");
}
```

**验收标准**:
- [ ] YAML 加载测试通过
- [ ] 环境变量覆盖测试通过
- [ ] 配置验证测试通过

---

### 第一周验收

**完成标准**:
- [ ] RepositoryPort 统一，GitHubPort 已删除
- [ ] 依赖注入工作，可切换 AI 服务
- [ ] config.yaml 配置正确加载和生效
- [ ] 所有单元测试通过
- [ ] 集成测试通过
- [ ] JAR 可以正常构建和运行

**验收命令**:
```bash
# 1. 编译
mvn clean compile

# 2. 测试
mvn test

# 3. 打包
mvn clean package -DskipTests -f hackathon-ai.xml

# 4. 运行
java -jar target/hackathon-ai.jar --help

# 5. 测试 AI 服务切换
export AI_PROVIDER=openai
java -jar target/hackathon-ai.jar --project ./test-project
```

---

## 🎯 第二周: P1 架构优化

### 任务4: 重构目录结构 (2h)

#### 4.1 清理黑客松目录

**任务**:
```bash
# 删除错误的嵌套结构
rm -rf src/main/java/top/yumbo/ai/reviewer/adapter/input/hackathon/

# 所有适配器已在任务1.5中移动
```

---

#### 4.2 更新包导入

**任务**:
- [ ] 查找所有受影响的文件
- [ ] 批量更新 import 语句
- [ ] 验证编译通过

---

### 任务5: 实现任务持久化 (6h)

#### 5.1 定义 Repository 接口 (1h)

**文件**: `application/port/output/AnalysisTaskRepository.java`

```java
public interface AnalysisTaskRepository {
    void save(AnalysisTask task);
    Optional<AnalysisTask> findById(String taskId);
    void update(AnalysisTask task);
    void delete(String taskId);
    List<AnalysisTask> findAll();
    List<AnalysisTask> findExpired(Duration age);
}
```

---

#### 5.2 实现文件存储 (2h)

**文件**: `adapter/output/persistence/FileAnalysisTaskRepository.java`

**任务**:
- [ ] 使用 JSON 序列化
- [ ] 存储到 `.ai-reviewer/tasks/` 目录
- [ ] 实现 CRUD 操作
- [ ] 添加文件锁

---

#### 5.3 集成到服务 (1h)

**任务**:
- [ ] 修改 ProjectAnalysisService
- [ ] 替换 ConcurrentHashMap
- [ ] 更新测试

---

#### 5.4 添加过期清理 (1h)

**任务**:
```java
@Scheduled(cron = "0 0 * * * *")
public void cleanupExpiredTasks() {
    List<AnalysisTask> expired = taskRepository.findExpired(Duration.ofDays(7));
    expired.forEach(task -> {
        taskRepository.delete(task.getTaskId());
        reportRepository.delete(task.getTaskId());
    });
}
```

---

#### 5.5 测试 (1h)

**验收标准**:
- [ ] 任务可以持久化
- [ ] 重启后可以恢复
- [ ] 过期清理工作
- [ ] 性能可接受

---

### 任务6: 统一异常处理 (4h)

#### 6.1 使用领域异常 (2h)

**任务**:
- [ ] 替换所有 RuntimeException
- [ ] 替换所有 IllegalArgumentException
- [ ] 使用 DomainException 子类

**代码变更**:
```java
// Before ❌
if (!project.isValid()) {
    throw new IllegalArgumentException("项目信息无效");
}

// After ✅
if (!project.isValid()) {
    throw new ProjectValidationException(
        "PROJECT_INVALID",
        "项目信息验证失败",
        project.getValidationErrors()
    );
}
```

---

#### 6.2 添加全局异常处理 (1h)

**文件**: `adapter/input/cli/GlobalExceptionHandler.java`

```java
public class GlobalExceptionHandler {
    public static int handle(Exception e) {
        if (e instanceof DomainException de) {
            System.err.println("❌ " + de.getUserMessage());
            log.error("业务异常: {}", de.getErrorCode(), e);
            return 1;
        } else if (e instanceof TechnicalException te) {
            System.err.println("❌ 系统错误，请联系管理员");
            log.error("技术异常", e);
            return 2;
        } else {
            System.err.println("❌ 未知错误");
            log.error("未知异常", e);
            return 3;
        }
    }
}
```

---

#### 6.3 标准化错误消息 (1h)

**任务**:
- [ ] 定义错误码常量
- [ ] 统一错误消息格式
- [ ] 支持国际化准备

---

### 第二周验收

**完成标准**:
- [ ] 目录结构符合六边形架构
- [ ] 任务支持持久化和恢复
- [ ] 使用领域异常
- [ ] 全局异常处理工作
- [ ] 所有测试通过

---

## 🎯 第三-四周: P2 质量提升

### 任务7: 提高测试覆盖率 (12h)

#### 7.1 补充单元测试 (4h)

**任务**:
- [ ] ConfigurationLoader 测试
- [ ] AIServiceFactory 测试
- [ ] Repository 测试
- [ ] 异常处理测试

**目标覆盖率**: 80%+

---

#### 7.2 添加集成测试 (4h)

**任务**:
- [ ] 端到端黑客松流程测试
- [ ] 多 AI 服务切换测试
- [ ] 配置加载测试
- [ ] 持久化恢复测试

---

#### 7.3 性能测试 (2h)

**任务**:
```java
@Test
@Timeout(value = 30, unit = TimeUnit.SECONDS)
public void shouldAnalyzeLargeProjectWithin30Seconds() {
    Project largeProject = createProjectWithFiles(1000);
    AnalysisTask task = analysisService.analyzeProject(largeProject);
    assertThat(task.getDurationMillis()).isLessThan(30000);
}
```

---

#### 7.4 Mock AI 服务测试 (2h)

**任务**:
- [ ] 使用 Mockito 模拟 AI 服务
- [ ] 无需真实 API Key 运行测试
- [ ] 提高测试速度

---

### 其他优化任务 (可选)

#### 8. 实现 API 适配器 (6h)
#### 9. 添加监控指标 (4h)
#### 10. 升级日志系统 (2h)
#### 11. 添加分布式追踪 (4h)
#### 12. 实现配置热重载 (3h)

---

## 📊 进度跟踪表

| 任务 | 预计工时 | 开始日期 | 完成日期 | 状态 | 负责人 | 备注 |
|------|---------|---------|---------|------|--------|------|
| 1.1 RepositoryPort 增强 | 1h | | | ⏳ | | |
| 1.2 GitHubAdapter 修改 | 1.5h | | | ⏳ | | |
| 1.3 GiteeAdapter 修改 | 1.5h | | | ⏳ | | |
| 1.4 删除 GitHubPort | 0.5h | | | ⏳ | | |
| 1.5 移动适配器 | 0.5h | | | ⏳ | | |
| 2.1 添加 Guice | 0.5h | | | ⏳ | | |
| 2.2 配置加载器 | 2h | | | ⏳ | | |
| 2.3 Guice 模块 | 2h | | | ⏳ | | |
| 2.4 修改 CLI | 2h | | | ⏳ | | |
| 2.5 AI 工厂 | 1h | | | ⏳ | | |
| 3.1 Configuration 类 | 1h | | | ⏳ | | |
| 3.2 YAML 映射 | 1.5h | | | ⏳ | | |
| 3.3 集成测试 | 0.5h | | | ⏳ | | |
| **第一周小计** | **16h** | | | | | |
| 4.1 清理目录 | 0.5h | | | ⏳ | | |
| 4.2 更新导入 | 1.5h | | | ⏳ | | |
| 5.1 Repository 接口 | 1h | | | ⏳ | | |
| 5.2 文件存储 | 2h | | | ⏳ | | |
| 5.3 集成服务 | 1h | | | ⏳ | | |
| 5.4 过期清理 | 1h | | | ⏳ | | |
| 5.5 测试 | 1h | | | ⏳ | | |
| 6.1 领域异常 | 2h | | | ⏳ | | |
| 6.2 全局处理 | 1h | | | ⏳ | | |
| 6.3 错误消息 | 1h | | | ⏳ | | |
| **第二周小计** | **12h** | | | | | |
| 7.1 单元测试 | 4h | | | ⏳ | | |
| 7.2 集成测试 | 4h | | | ⏳ | | |
| 7.3 性能测试 | 2h | | | ⏳ | | |
| 7.4 Mock 测试 | 2h | | | ⏳ | | |
| **第三-四周小计** | **12h** | | | | | |
| **总计** | **40h** | | | | | |

---

## ✅ 每日站会检查项

### 每日同步 (15分钟)

1. **昨天完成了什么？**
   - 任务ID
   - 遇到的问题
   - 解决方案

2. **今天计划做什么？**
   - 任务ID
   - 预计完成时间

3. **有什么阻塞？**
   - 依赖未完成
   - 技术难题
   - 需要帮助

### 每周评审 (1小时)

1. **完成度检查**
   - [ ] 本周任务完成率
   - [ ] 测试覆盖率
   - [ ] 代码质量

2. **问题回顾**
   - 主要问题
   - 解决方案
   - 经验教训

3. **下周计划**
   - 调整优先级
   - 分配任务

---

## 📞 联系方式

- **技术负责人**: [待定]
- **项目经理**: [待定]
- **质量负责人**: [待定]

---

## 📝 更新日志

| 版本 | 日期 | 更新内容 | 更新人 |
|------|------|---------|--------|
| v1.0 | 2025-11-12 | 初始版本 | AI架构师 |
| | | | |

---

**下次更新**: 2025-11-19  
**状态**: ⏳ 待开始

