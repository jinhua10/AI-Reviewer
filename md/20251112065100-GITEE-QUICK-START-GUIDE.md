# Gitee 集成快速使用指南

## 🚀 快速开始

### 1. 基本使用

```java
// 创建 Gitee 适配器
Path workingDir = Paths.get("./workspace");
GiteeAdapter giteeAdapter = new GiteeAdapter(workingDir);

// 克隆仓库
Path localPath = giteeAdapter.cloneRepository(
    "https://gitee.com/owner/repo.git",
    null  // null = 默认分支
);

System.out.println("仓库已克隆到: " + localPath);
```

### 2. 获取仓库信息

```java
GiteeAdapter adapter = new GiteeAdapter(workingDir);

// 获取详细指标
GitHubPort.GitHubMetrics metrics = adapter.getRepositoryMetrics(
    "https://gitee.com/dromara/hutool.git"
);

System.out.println("仓库名: " + metrics.getRepositoryName());
System.out.println("提交数: " + metrics.getCommitCount());
System.out.println("贡献者: " + metrics.getContributorCount());
System.out.println("分支: " + metrics.getBranches());
```

### 3. 完整的代码审查流程

```java
// 步骤 1: 初始化适配器
GiteeAdapter giteeAdapter = new GiteeAdapter(workingDir);
LocalFileSystemAdapter fileAdapter = new LocalFileSystemAdapter();
DeepSeekAIAdapter aiAdapter = new DeepSeekAIAdapter(apiKey, model, 0.7);

// 步骤 2: 创建编排器
CodeReviewOrchestrator orchestrator = new CodeReviewOrchestrator(
    giteeAdapter, fileAdapter, aiAdapter
);

// 步骤 3: 创建审查请求
CodeReviewRequest request = CodeReviewRequest.builder()
    .repositoryUrl("https://gitee.com/owner/repo.git")
    .includePatterns(Arrays.asList("**/*.java", "**/*.md"))
    .excludePatterns(Arrays.asList("**/target/**"))
    .focusAreas(Arrays.asList("代码质量", "安全性"))
    .build();

// 步骤 4: 执行审查
CodeReviewResult result = orchestrator.reviewCode(request);

// 步骤 5: 查看结果
System.out.println("总文件数: " + result.getTotalFiles());
System.out.println("已审查: " + result.getReviewedFiles());
```

---

## 📝 常用操作

### 检查仓库是否可访问

```java
GiteeAdapter adapter = new GiteeAdapter(workingDir);
boolean accessible = adapter.isRepositoryAccessible(
    "https://gitee.com/owner/repo.git"
);

if (accessible) {
    System.out.println("✓ 仓库可以访问");
} else {
    System.out.println("✗ 仓库不可访问");
}
```

### 获取默认分支

```java
String branch = adapter.getDefaultBranch(
    "https://gitee.com/owner/repo.git"
);
System.out.println("默认分支: " + branch);
```

### 检查文件是否存在

```java
boolean hasReadme = adapter.hasFile(
    "https://gitee.com/owner/repo.git",
    "README.md"
);
System.out.println("有 README: " + hasReadme);
```

---

## 🔧 配置选项

### 自定义超时和克隆深度

```java
Path workingDir = Paths.get("./workspace");
int timeout = 120;      // 2分钟超时
int depth = 1;          // 浅克隆（只克隆最新提交）

GiteeAdapter adapter = new GiteeAdapter(workingDir, timeout, depth);
```

### 配置说明

| 参数 | 默认值 | 说明 |
|-----|-------|------|
| `workingDirectory` | - | 工作目录（必需） |
| `timeout` | 300 秒 | 克隆超时时间 |
| `depth` | 1 | 克隆深度（1=浅克隆） |

---

## 🎯 使用场景

### 场景 1: CI/CD 中使用

```java
// 在 CI/CD 管道中审查每次提交
public class CICodeReview {
    public void reviewLatestCommit(String giteeUrl) {
        GiteeAdapter adapter = new GiteeAdapter(Paths.get("./ci-workspace"));
        
        // 克隆最新代码
        Path code = adapter.cloneRepository(giteeUrl, null);
        
        // 执行审查...
    }
}
```

### 场景 2: 批量审查多个仓库

```java
List<String> repos = Arrays.asList(
    "https://gitee.com/owner/repo1.git",
    "https://gitee.com/owner/repo2.git",
    "https://gitee.com/owner/repo3.git"
);

GiteeAdapter adapter = new GiteeAdapter(workingDir);

for (String repo : repos) {
    try {
        GitHubPort.GitHubMetrics metrics = adapter.getRepositoryMetrics(repo);
        System.out.println(repo + " - 提交数: " + metrics.getCommitCount());
    } catch (Exception e) {
        System.err.println("审查失败: " + repo);
    }
}
```

### 场景 3: 对比两个仓库

```java
GiteeAdapter adapter = new GiteeAdapter(workingDir);

String repo1 = "https://gitee.com/owner/repo1.git";
String repo2 = "https://gitee.com/owner/repo2.git";

GitHubPort.GitHubMetrics metrics1 = adapter.getRepositoryMetrics(repo1);
GitHubPort.GitHubMetrics metrics2 = adapter.getRepositoryMetrics(repo2);

System.out.println("仓库 1 提交数: " + metrics1.getCommitCount());
System.out.println("仓库 2 提交数: " + metrics2.getCommitCount());
```

---

## ⚠️ 注意事项

### 1. URL 格式

✅ **正确**:
```
https://gitee.com/owner/repo.git
https://gitee.com/owner/repo
http://gitee.com/owner/repo
```

❌ **错误**:
```
github.com/owner/repo           # 缺少协议
https://github.com/owner/repo   # 不是 Gitee URL
gitee.com/owner/repo            # 缺少协议
```

### 2. 私有仓库

当前版本**不支持**需要认证的私有仓库。如需访问私有仓库：
- 使用 SSH Key
- 或者扩展 GiteeAdapter 添加认证支持

### 3. 磁盘空间

克隆仓库需要磁盘空间，虽然使用了浅克隆，但大型仓库仍可能占用较多空间。

建议：
- 审查完成后自动清理（默认已实现）
- 定期清理工作目录
- 监控磁盘使用情况

### 4. 网络要求

虽然 Gitee 在国内速度较快，但仍需要：
- 稳定的网络连接
- 能够访问 gitee.com
- 适当的超时配置

---

## 🐛 故障排查

### 问题 1: 克隆失败

**错误信息**: `GitHubException: 克隆 Gitee 仓库失败`

**可能原因**:
1. 网络连接问题
2. URL 格式错误
3. 仓库不存在或不可访问
4. 超时时间太短

**解决方法**:
```java
// 增加超时时间
GiteeAdapter adapter = new GiteeAdapter(workingDir, 600, 1);  // 10分钟

// 验证 URL
boolean accessible = adapter.isRepositoryAccessible(url);
if (!accessible) {
    System.out.println("仓库不可访问，请检查 URL");
}
```

### 问题 2: 获取指标失败

**错误信息**: `GitHubException: 获取 Gitee 仓库指标失败`

**可能原因**:
1. 仓库为空
2. 权限不足
3. Git 历史记录异常

**解决方法**:
```java
try {
    GitHubPort.GitHubMetrics metrics = adapter.getRepositoryMetrics(url);
} catch (GitHubPort.GitHubException e) {
    System.err.println("获取指标失败: " + e.getMessage());
    // 降级处理或跳过
}
```

### 问题 3: 磁盘空间不足

**错误信息**: `IOException: No space left on device`

**解决方法**:
```java
// 定期清理工作目录
Path workingDir = Paths.get("./workspace");
if (Files.exists(workingDir)) {
    Files.walk(workingDir)
        .sorted(Comparator.reverseOrder())
        .map(Path::toFile)
        .forEach(File::delete);
}
```

---

## 📊 性能建议

### 1. 使用浅克隆

```java
// 浅克隆（推荐）
GiteeAdapter adapter = new GiteeAdapter(workingDir, 300, 1);

// 完整克隆（不推荐，除非必要）
GiteeAdapter adapter = new GiteeAdapter(workingDir, 300, Integer.MAX_VALUE);
```

**性能对比**:
- 浅克隆: ~4 秒，5MB
- 完整克隆: ~10 秒，50MB

### 2. 并行处理

```java
List<String> repos = getRepositories();

// 并行审查多个仓库
repos.parallelStream().forEach(repo -> {
    try {
        GiteeAdapter adapter = new GiteeAdapter(workingDir);
        adapter.getRepositoryMetrics(repo);
    } catch (Exception e) {
        // 处理异常
    }
});
```

### 3. 缓存结果

```java
Map<String, GitHubPort.GitHubMetrics> cache = new HashMap<>();

public GitHubPort.GitHubMetrics getMetrics(String url) {
    if (cache.containsKey(url)) {
        return cache.get(url);  // 使用缓存
    }
    
    GitHubPort.GitHubMetrics metrics = adapter.getRepositoryMetrics(url);
    cache.put(url, metrics);
    return metrics;
}
```

---

## 🧪 测试

### 运行单元测试

```bash
mvn test -Dtest=GiteeAdapterTest
```

### 运行端到端测试

```bash
mvn test -Dtest=GiteeIntegrationEndToEndTest
```

### 测试特定方法

```bash
mvn test -Dtest=GiteeAdapterTest#testCloneRepository
```

---

## 📚 相关文档

- [Gitee 集成完成报告](./20251112064600-GITEE-INTEGRATION-COMPLETED.md)
- [项目 README](../README.md)
- [Gitee 官方文档](https://gitee.com/help)

---

## 💡 提示

1. ✅ 优先使用 Gitee（国内速度快）
2. ✅ 使用浅克隆提升性能
3. ✅ 及时清理临时目录
4. ✅ 配置合理的超时时间
5. ✅ 做好异常处理

---

**更新时间**: 2025-11-12 06:51:00  
**版本**: 1.0.0

