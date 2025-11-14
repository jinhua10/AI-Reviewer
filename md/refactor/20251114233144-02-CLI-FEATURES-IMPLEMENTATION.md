# AI-Reviewer 项目交互式 CLI 功能实现详细设计（第2部分）

**生成时间**: 2025-11-14 23:31:44  
**分析人员**: 世界顶级架构师  
**文档类型**: 功能设计文档

---

## 📋 概述

本报告详细设计 HackathonInteractiveApp 中待实现的 4 个核心功能：
1. 批量评审项目
2. 团队管理
3. 排行榜显示
4. 结果导出

这些功能是构建完整黑客松评审系统的关键，同时为未来扩展多文件类型处理奠定基础。

---

## 🎯 功能 1: 批量评审项目

### 当前状态
```java
private void reviewBatchProjects() {
    System.out.println("\n📦 === 批量评审项目 ===\n");
    System.out.println("请输入包含多个项目URL的文件路径（每行一个URL，格式：团队名,URL）");
    System.out.print("文件路径: ");
    
    String filePath = scanner.nextLine().trim();
    // TODO: 实现批量评审逻辑
    System.out.println("💡 批量评审功能正在开发中...");
}
```

### 设计方案

#### 输入文件格式

**CSV 格式** (`teams.csv`):
```csv
team_name,repo_url,contact_email,submission_time
春队,https://github.com/team-spring/hackathon-project,team@spring.io,2025-11-14T10:00:00
云队,https://gitee.com/team-cloud/ai-platform,cloud@team.com,2025-11-14T11:30:00
创新小组,https://github.com/innovators/smart-system,info@innovators.com,2025-11-14T12:00:00
```

**JSON 格式** (`teams.json`):
```json
{
  "hackathon": {
    "name": "2025春季AI黑客松",
    "date": "2025-11-14",
    "teams": [
      {
        "teamName": "春队",
        "repoUrl": "https://github.com/team-spring/hackathon-project",
        "contactEmail": "team@spring.io",
        "submissionTime": "2025-11-14T10:00:00",
        "tags": ["AI", "云原生"]
      },
      {
        "teamName": "云队",
        "repoUrl": "https://gitee.com/team-cloud/ai-platform",
        "contactEmail": "cloud@team.com",
        "submissionTime": "2025-11-14T11:30:00",
        "tags": ["大数据", "AI"]
      }
    ]
  }
}
```

**YAML 格式** (`teams.yaml`):
```yaml
hackathon:
  name: "2025春季AI黑客松"
  date: "2025-11-14"
  
  teams:
    - team_name: "春队"
      repo_url: "https://github.com/team-spring/hackathon-project"
      contact_email: "team@spring.io"
      submission_time: "2025-11-14T10:00:00"
      tags: ["AI", "云原生"]
      
    - team_name: "云队"
      repo_url: "https://gitee.com/team-cloud/ai-platform"
      contact_email: "cloud@team.com"
      submission_time: "2025-11-14T11:30:00"
      tags: ["大数据", "AI"]
```

#### 完整实现代码

```java
/**
 * 批量评审项目
 * 支持 CSV, JSON, YAML 格式的团队列表文件
 */
private void reviewBatchProjects() {
    System.out.println("\n📦 === 批量评审项目 ===\n");
    System.out.println("支持的文件格式:");
    System.out.println("  • CSV:  team_name,repo_url,contact_email,submission_time");
    System.out.println("  • JSON: 结构化JSON格式（参见文档）");
    System.out.println("  • YAML: 结构化YAML格式（参见文档）\n");
    
    System.out.print("📁 输入文件路径: ");
    String filePath = scanner.nextLine().trim();
    
    Path inputFile = Paths.get(filePath);
    if (!Files.exists(inputFile)) {
        System.out.println("❌ 文件不存在: " + filePath);
        return;
    }
    
    try {
        // 解析团队列表
        List<TeamSubmission> submissions = parseTeamSubmissions(inputFile);
        
        if (submissions.isEmpty()) {
            System.out.println("❌ 未找到有效的团队提交信息");
            return;
        }
        
        System.out.println("\n✅ 解析完成，共找到 " + submissions.size() + " 个团队\n");
        
        // 显示团队列表
        displayTeamList(submissions);
        
        // 确认执行
        System.out.print("\n是否开始批量评审？[Y/n]: ");
        String confirm = scanner.nextLine().trim();
        if (!confirm.isEmpty() && !confirm.equalsIgnoreCase("Y")) {
            System.out.println("❌ 已取消批量评审");
            return;
        }
        
        // 配置并发参数
        System.out.print("并发评审数（建议1-5）[默认3]: ");
        String concurrencyInput = scanner.nextLine().trim();
        int concurrency = concurrencyInput.isEmpty() ? 3 : Integer.parseInt(concurrencyInput);
        
        // 执行批量评审
        BatchReviewResult result = executeBatchReview(submissions, concurrency);
        
        // 显示结果
        displayBatchResult(result);
        
        // 保存结果
        saveBatchResult(result);
        
    } catch (Exception e) {
        System.out.println("❌ 批量评审失败: " + e.getMessage());
        log.error("Batch review failed", e);
    }
}

/**
 * 解析团队提交文件
 */
private List<TeamSubmission> parseTeamSubmissions(Path filePath) throws IOException {
    String fileName = filePath.getFileName().toString().toLowerCase();
    String content = Files.readString(filePath);
    
    if (fileName.endsWith(".csv")) {
        return parseCSV(content);
    } else if (fileName.endsWith(".json")) {
        return parseJSON(content);
    } else if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
        return parseYAML(content);
    } else {
        throw new IllegalArgumentException("不支持的文件格式: " + fileName);
    }
}

/**
 * 解析 CSV 格式
 */
private List<TeamSubmission> parseCSV(String content) {
    List<TeamSubmission> submissions = new ArrayList<>();
    String[] lines = content.split("\n");
    
    // 跳过标题行
    for (int i = 1; i < lines.length; i++) {
        String line = lines[i].trim();
        if (line.isEmpty()) continue;
        
        String[] parts = line.split(",");
        if (parts.length >= 2) {
            TeamSubmission submission = TeamSubmission.builder()
                .teamName(parts[0].trim())
                .repoUrl(parts[1].trim())
                .contactEmail(parts.length > 2 ? parts[2].trim() : "")
                .submissionTime(parts.length > 3 ? parseDateTime(parts[3].trim()) : LocalDateTime.now())
                .build();
            submissions.add(submission);
        }
    }
    
    return submissions;
}

/**
 * 解析 JSON 格式
 */
private List<TeamSubmission> parseJSON(String content) {
    ObjectMapper mapper = new ObjectMapper();
    try {
        BatchSubmissionDto dto = mapper.readValue(content, BatchSubmissionDto.class);
        return dto.getHackathon().getTeams().stream()
            .map(this::convertToTeamSubmission)
            .toList();
    } catch (JsonProcessingException e) {
        throw new RuntimeException("JSON 解析失败", e);
    }
}

/**
 * 解析 YAML 格式
 */
private List<TeamSubmission> parseYAML(String content) {
    Yaml yaml = new Yaml(new Constructor(BatchSubmissionDto.class));
    BatchSubmissionDto dto = yaml.load(content);
    return dto.getHackathon().getTeams().stream()
        .map(this::convertToTeamSubmission)
        .toList();
}

/**
 * 执行批量评审
 */
private BatchReviewResult executeBatchReview(List<TeamSubmission> submissions, int concurrency) {
    System.out.println("\n⏳ 开始批量评审...");
    System.out.println("并发数: " + concurrency);
    System.out.println("总任务数: " + submissions.size());
    System.out.println();
    
    ExecutorService executor = Executors.newFixedThreadPool(concurrency);
    List<CompletableFuture<ReviewResult>> futures = new ArrayList<>();
    
    AtomicInteger completed = new AtomicInteger(0);
    AtomicInteger failed = new AtomicInteger(0);
    
    long startTime = System.currentTimeMillis();
    
    // 提交所有评审任务
    for (TeamSubmission submission : submissions) {
        CompletableFuture<ReviewResult> future = CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("📝 [" + (completed.get() + 1) + "/" + submissions.size() + "] 评审: " + submission.getTeamName());
                
                // 克隆项目
                Path projectPath = cloneProject(submission.getRepoUrl());
                
                // 扫描文件
                List<SourceFile> files = fileSystemAdapter.scanProjectFiles(projectPath);
                
                // 创建项目对象
                Project project = Project.builder()
                    .name(submission.getTeamName())
                    .rootPath(projectPath)
                    .type(detectProjectType(files))
                    .sourceFiles(files)
                    .build();
                
                // 执行分析
                AnalysisTask task = analysisService.analyzeProject(project);
                
                // 等待完成
                while (!task.isCompleted() && !task.isFailed()) {
                    Thread.sleep(1000);
                }
                
                if (task.isCompleted()) {
                    ReviewReport report = analysisService.getAnalysisResult(task.getTaskId());
                    completed.incrementAndGet();
                    
                    System.out.println("✅ [" + completed.get() + "/" + submissions.size() + "] 完成: " + 
                        submission.getTeamName() + " (得分: " + report.getTotalScore() + ")");
                    
                    return ReviewResult.success(submission, report);
                } else {
                    failed.incrementAndGet();
                    System.out.println("❌ [" + (completed.get() + failed.get()) + "/" + submissions.size() + "] 失败: " + 
                        submission.getTeamName());
                    return ReviewResult.failure(submission, task.getErrorMessage());
                }
                
            } catch (Exception e) {
                failed.incrementAndGet();
                System.out.println("❌ [" + (completed.get() + failed.get()) + "/" + submissions.size() + "] 异常: " + 
                    submission.getTeamName() + " - " + e.getMessage());
                return ReviewResult.failure(submission, e.getMessage());
            }
        }, executor);
        
        futures.add(future);
    }
    
    // 等待所有任务完成
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    
    long endTime = System.currentTimeMillis();
    long duration = endTime - startTime;
    
    executor.shutdown();
    
    // 收集结果
    List<ReviewResult> results = futures.stream()
        .map(CompletableFuture::join)
        .toList();
    
    return BatchReviewResult.builder()
        .totalCount(submissions.size())
        .successCount(completed.get())
        .failureCount(failed.get())
        .duration(duration)
        .results(results)
        .build();
}

/**
 * 显示团队列表
 */
private void displayTeamList(List<TeamSubmission> submissions) {
    System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    System.out.printf("%-5s %-20s %-50s%n", "序号", "团队名称", "仓库URL");
    System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    
    for (int i = 0; i < submissions.size(); i++) {
        TeamSubmission submission = submissions.get(i);
        System.out.printf("%-5d %-20s %-50s%n", 
            i + 1, 
            truncate(submission.getTeamName(), 20),
            truncate(submission.getRepoUrl(), 50));
    }
    
    System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
}

/**
 * 显示批量评审结果
 */
private void displayBatchResult(BatchReviewResult result) {
    System.out.println("\n" + "═".repeat(80));
    System.out.println("📊 批量评审完成");
    System.out.println("═".repeat(80));
    System.out.println("总数: " + result.getTotalCount());
    System.out.println("成功: " + result.getSuccessCount() + " ✅");
    System.out.println("失败: " + result.getFailureCount() + " ❌");
    System.out.println("耗时: " + formatDuration(result.getDuration()));
    System.out.println("平均: " + (result.getDuration() / result.getTotalCount() / 1000) + " 秒/项目");
    System.out.println("═".repeat(80));
    
    // 显示成功的评审结果（按分数排序）
    List<ReviewResult> successResults = result.getResults().stream()
        .filter(ReviewResult::isSuccess)
        .sorted(Comparator.comparing(r -> r.getReport().getTotalScore(), Comparator.reverseOrder()))
        .toList();
    
    if (!successResults.isEmpty()) {
        System.out.println("\n🏆 评审结果（按分数排序）:");
        System.out.println("━".repeat(80));
        System.out.printf("%-5s %-20s %-10s %-10s %-10s %-10s %-10s%n", 
            "排名", "团队", "总分", "代码质量", "创新性", "完成度", "文档");
        System.out.println("━".repeat(80));
        
        for (int i = 0; i < successResults.size(); i++) {
            ReviewResult r = successResults.get(i);
            ReviewReport report = r.getReport();
            System.out.printf("%-5d %-20s %-10d %-10d %-10d %-10d %-10d%n",
                i + 1,
                truncate(r.getSubmission().getTeamName(), 20),
                report.getTotalScore(),
                report.getDimensions().get("codeQuality"),
                report.getDimensions().get("innovation"),
                report.getDimensions().get("completeness"),
                report.getDimensions().get("documentation"));
        }
        System.out.println("━".repeat(80));
    }
    
    // 显示失败的评审
    List<ReviewResult> failedResults = result.getResults().stream()
        .filter(r -> !r.isSuccess())
        .toList();
    
    if (!failedResults.isEmpty()) {
        System.out.println("\n❌ 失败的评审:");
        for (ReviewResult r : failedResults) {
            System.out.println("  • " + r.getSubmission().getTeamName() + ": " + r.getErrorMessage());
        }
    }
}

/**
 * 保存批量评审结果
 */
private void saveBatchResult(BatchReviewResult result) {
    try {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        
        // 保存 JSON 格式
        Path jsonPath = Paths.get("batch-review-" + timestamp + ".json");
        String jsonContent = new ObjectMapper()
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(result);
        Files.writeString(jsonPath, jsonContent);
        System.out.println("\n✅ 结果已保存: " + jsonPath);
        
        // 保存 CSV 格式（简化版）
        Path csvPath = Paths.get("batch-review-" + timestamp + ".csv");
        StringBuilder csv = new StringBuilder();
        csv.append("排名,团队名称,总分,代码质量,创新性,完成度,文档,状态\n");
        
        List<ReviewResult> sorted = result.getResults().stream()
            .filter(ReviewResult::isSuccess)
            .sorted(Comparator.comparing(r -> r.getReport().getTotalScore(), Comparator.reverseOrder()))
            .toList();
        
        for (int i = 0; i < sorted.size(); i++) {
            ReviewResult r = sorted.get(i);
            ReviewReport report = r.getReport();
            csv.append(String.format("%d,%s,%d,%d,%d,%d,%d,成功\n",
                i + 1,
                r.getSubmission().getTeamName(),
                report.getTotalScore(),
                report.getDimensions().get("codeQuality"),
                report.getDimensions().get("innovation"),
                report.getDimensions().get("completeness"),
                report.getDimensions().get("documentation")));
        }
        
        Files.writeString(csvPath, csv.toString());
        System.out.println("✅ CSV 已保存: " + csvPath);
        
    } catch (Exception e) {
        System.out.println("❌ 保存结果失败: " + e.getMessage());
        log.error("Save batch result failed", e);
    }
}

// 辅助类

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
class TeamSubmission {
    private String teamName;
    private String repoUrl;
    private String contactEmail;
    private LocalDateTime submissionTime;
    private List<String> tags;
}

@Data
@Builder
class ReviewResult {
    private TeamSubmission submission;
    private ReviewReport report;
    private boolean success;
    private String errorMessage;
    
    public static ReviewResult success(TeamSubmission submission, ReviewReport report) {
        return ReviewResult.builder()
            .submission(submission)
            .report(report)
            .success(true)
            .build();
    }
    
    public static ReviewResult failure(TeamSubmission submission, String errorMessage) {
        return ReviewResult.builder()
            .submission(submission)
            .success(false)
            .errorMessage(errorMessage)
            .build();
    }
}

@Data
@Builder
class BatchReviewResult {
    private int totalCount;
    private int successCount;
    private int failureCount;
    private long duration;
    private List<ReviewResult> results;
}
```

### 使用示例

```bash
# 1. 准备团队列表文件
$ cat teams.csv
team_name,repo_url,contact_email,submission_time
春队,https://github.com/spring-team/project,spring@team.com,2025-11-14T10:00:00
云队,https://github.com/cloud-team/project,cloud@team.com,2025-11-14T11:00:00

# 2. 运行批量评审
$ java -jar ai-reviewer.jar
选择: 2. 📦 批量评审项目

📁 输入文件路径: teams.csv

✅ 解析完成，共找到 2 个团队

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
序号  团队名称              仓库URL
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1     春队                  https://github.com/spring-team/project
2     云队                  https://github.com/cloud-team/project
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

是否开始批量评审？[Y/n]: Y
并发评审数（建议1-5）[默认3]: 3

⏳ 开始批量评审...
并发数: 3
总任务数: 2

📝 [1/2] 评审: 春队
📝 [2/2] 评审: 云队
✅ [1/2] 完成: 春队 (得分: 85)
✅ [2/2] 完成: 云队 (得分: 92)

═══════════════════════════════════════════════════════════════════════════════
📊 批量评审完成
═══════════════════════════════════════════════════════════════════════════════
总数: 2
成功: 2 ✅
失败: 0 ❌
耗时: 2分35秒
平均: 77 秒/项目
═══════════════════════════════════════════════════════════════════════════════

🏆 评审结果（按分数排序）:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
排名  团队              总分      代码质量    创新性      完成度      文档
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1     云队              92        88          95          90          85
2     春队              85        82          85          88          80
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

✅ 结果已保存: batch-review-20251114_153000.json
✅ CSV 已保存: batch-review-20251114_153000.csv
```

---

## 🎯 功能 2: 团队管理

### 完整实现代码

```java
/**
 * 管理团队
 */
private void manageTeams() {
    System.out.println("\n👥 === 管理团队 ===\n");
    System.out.println("  1. 📝 注册新团队");
    System.out.println("  2. 📋 查看团队列表");
    System.out.println("  3. 🔍 查看团队详情");
    System.out.println("  4. ✏️  编辑团队信息");
    System.out.println("  5. 🗑️  删除团队");
    System.out.println("  0. 🔙 返回主菜单");
    System.out.print("\n选择 [0-5]: ");
    
    String choice = scanner.nextLine().trim();
    
    switch (choice) {
        case "1" -> registerNewTeam();
        case "2" -> listAllTeams();
        case "3" -> viewTeamDetails();
        case "4" -> editTeamInfo();
        case "5" -> deleteTeam();
        case "0" -> {}
        default -> System.out.println("❌ 无效选项");
    }
}

/**
 * 注册新团队
 */
private void registerNewTeam() {
    System.out.println("\n📝 === 注册新团队 ===\n");
    
    // 团队名称
    System.out.print("团队名称: ");
    String teamName = scanner.nextLine().trim();
    
    if (teamName.isEmpty()) {
        System.out.println("❌ 团队名称不能为空");
        return;
    }
    
    // 检查重复
    if (teamService.teamExists(teamName)) {
        System.out.println("❌ 团队名称已存在");
        return;
    }
    
    // 团队描述
    System.out.print("团队描述: ");
    String description = scanner.nextLine().trim();
    
    // 成员数量
    System.out.print("成员数量: ");
    int memberCount = Integer.parseInt(scanner.nextLine().trim());
    
    // 联系方式
    System.out.print("联系邮箱: ");
    String email = scanner.nextLine().trim();
    
    // 创建团队
    Team team = Team.builder()
        .name(teamName)
        .description(description)
        .memberCount(memberCount)
        .contactEmail(email)
        .createdAt(LocalDateTime.now())
        .build();
    
    try {
        teamService.registerTeam(team);
        System.out.println("\n✅ 团队注册成功!");
        System.out.println("━".repeat(50));
        displayTeamInfo(team);
        System.out.println("━".repeat(50));
    } catch (Exception e) {
        System.out.println("❌ 注册失败: " + e.getMessage());
    }
}

/**
 * 查看团队列表
 */
private void listAllTeams() {
    System.out.println("\n📋 === 团队列表 ===\n");
    
    List<Team> teams = teamService.getAllTeams();
    
    if (teams.isEmpty()) {
        System.out.println("📭 暂无注册团队");
        return;
    }
    
    System.out.println("━".repeat(100));
    System.out.printf("%-5s %-20s %-10s %-30s %-20s%n", 
        "序号", "团队名称", "成员数", "联系邮箱", "注册时间");
    System.out.println("━".repeat(100));
    
    for (int i = 0; i < teams.size(); i++) {
        Team team = teams.get(i);
        System.out.printf("%-5d %-20s %-10d %-30s %-20s%n",
            i + 1,
            truncate(team.getName(), 20),
            team.getMemberCount(),
            truncate(team.getContactEmail(), 30),
            formatDateTime(team.getCreatedAt()));
    }
    
    System.out.println("━".repeat(100));
    System.out.println("总计: " + teams.size() + " 个团队");
}

/**
 * 查看团队详情
 */
private void viewTeamDetails() {
    System.out.println("\n🔍 === 查看团队详情 ===\n");
    System.out.print("请输入团队名称: ");
    String teamName = scanner.nextLine().trim();
    
    Optional<Team> teamOpt = teamService.getTeamByName(teamName);
    
    if (teamOpt.isEmpty()) {
        System.out.println("❌ 未找到团队: " + teamName);
        return;
    }
    
    Team team = teamOpt.get();
    
    System.out.println("\n" + "═".repeat(80));
    System.out.println("📊 团队详细信息");
    System.out.println("═".repeat(80));
    
    displayTeamInfo(team);
    
    // 显示提交历史
    List<Submission> submissions = teamService.getTeamSubmissions(teamName);
    if (!submissions.isEmpty()) {
        System.out.println("\n📝 提交历史:");
        System.out.println("━".repeat(80));
        System.out.printf("%-10s %-30s %-15s %-10s%n", 
            "提交时间", "项目URL", "状态", "得分");
        System.out.println("━".repeat(80));
        
        for (Submission submission : submissions) {
            System.out.printf("%-10s %-30s %-15s %-10s%n",
                formatDateTime(submission.getSubmittedAt()),
                truncate(submission.getProjectUrl(), 30),
                submission.getStatus(),
                submission.getScore() != null ? submission.getScore() : "N/A");
        }
        System.out.println("━".repeat(80));
    }
    
    System.out.println("═".repeat(80));
}

/**
 * 显示团队信息
 */
private void displayTeamInfo(Team team) {
    System.out.println("团队名称: " + team.getName());
    System.out.println("团队描述: " + team.getDescription());
    System.out.println("成员数量: " + team.getMemberCount());
    System.out.println("联系邮箱: " + team.getContactEmail());
    System.out.println("注册时间: " + formatDateTime(team.getCreatedAt()));
    
    if (team.getTags() != null && !team.getTags().isEmpty()) {
        System.out.println("标签: " + String.join(", ", team.getTags()));
    }
}
```

---

## 🎯 功能 3: 排行榜显示

详见下一部分报告...

---

**报告结束 - 第2部分**

继续阅读：
- 《第3部分：Deprecated 方法和架构改进》
- 《第4部分：多文件类型扩展架构设计》
- 《第5部分：AI 引擎未来演进路线图》

