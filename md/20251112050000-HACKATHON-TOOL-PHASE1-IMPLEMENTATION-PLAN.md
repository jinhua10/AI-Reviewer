# 🚀 黑客松工具 Phase 1 实施计划

> **创建时间**: 2025-11-12 05:00:00  
> **方案**: 方案D - 渐进式演进  
> **阶段**: Phase 1 - 快速 MVP (1-2周)  
> **状态**: 🟢 开始实施  

---

## 📋 Phase 1 目标

### 核心目标

打造一个**隔离设计**的黑客松评审工具 MVP，为后续框架重组做好准备。

**关键原则** ⚠️:
1. ✅ **自包含**: 所有代码在 `hackathon/` 包下
2. ✅ **最小依赖**: 只依赖核心稳定接口
3. ✅ **独立配置**: 独立的配置文件
4. ✅ **清晰边界**: 不修改现有核心代码
5. ✅ **易迁移**: 未来迁移成本 < 2天

---

## 🏗️ 架构设计

### 1. 包结构设计

```
src/main/java/top/yumbo/ai/reviewer/
├── domain/                          # 现有核心 (不修改)
├── application/                     # 现有服务 (不修改)
├── adapter/
│   ├── output/                      # 现有适配器 (不修改)
│   └── input/
│       ├── cli/                     # 现有CLI (不修改)
│       └── hackathon/              # 🎯 黑客松模块 (新增)
│           ├── domain/             # 黑客松领域模型
│           │   ├── model/
│           │   │   ├── HackathonProject.java
│           │   │   ├── Team.java
│           │   │   ├── Submission.java
│           │   │   ├── Participant.java
│           │   │   └── HackathonScore.java
│           │   └── port/
│           │       ├── GitHubPort.java
│           │       └── RankingPort.java
│           ├── application/        # 黑客松应用服务
│           │   ├── HackathonAnalysisService.java
│           │   ├── HackathonScoringService.java
│           │   ├── TeamManagementService.java
│           │   └── LeaderboardService.java
│           ├── adapter/            # 黑客松适配器
│           │   ├── input/
│           │   │   ├── web/
│           │   │   │   ├── HackathonWebController.java
│           │   │   │   ├── HackathonRestAPI.java
│           │   │   │   └── dto/
│           │   │   └── cli/
│           │   │       └── HackathonCLI.java
│           │   └── output/
│           │       ├── github/
│           │       │   ├── GitHubAdapter.java
│           │       │   └── GitHubClient.java
│           │       └── ranking/
│           │           └── RedisRankingAdapter.java
│           ├── config/             # 黑客松配置
│           │   ├── HackathonConfig.java
│           │   └── HackathonProperties.java
│           └── util/               # 黑客松工具类
│               └── HackathonScoreCalculator.java
```

### 2. 依赖关系

```
┌─────────────────────────────────────────────────────┐
│         Hackathon Module (隔离模块)                  │
│  ┌───────────────────────────────────────────────┐  │
│  │          Hackathon Domain                     │  │
│  │    - HackathonProject, Team, Submission      │  │
│  └────────────────┬──────────────────────────────┘  │
│                   │                                  │
│  ┌────────────────▼──────────────────────────────┐  │
│  │      Hackathon Application Services          │  │
│  │  - HackathonScoringService                   │  │
│  │  - LeaderboardService                        │  │
│  └────────────────┬──────────────────────────────┘  │
│                   │                                  │
│  ┌────────────────▼──────────────────────────────┐  │
│  │        Hackathon Adapters                     │  │
│  │  - Web API, GitHub Adapter                   │  │
│  └───────────────────────────────────────────────┘  │
└─────────────────────┬───────────────────────────────┘
                      │
                      │ 依赖 (只读，不修改)
                      ▼
┌─────────────────────────────────────────────────────┐
│         Core Framework (现有代码，不修改)            │
│  ┌───────────────────────────────────────────────┐  │
│  │    Domain (Project, SourceFile, etc.)        │  │
│  └────────────────┬──────────────────────────────┘  │
│  ┌────────────────▼──────────────────────────────┐  │
│  │    Application (ProjectAnalysisService)      │  │
│  └────────────────┬──────────────────────────────┘  │
│  ┌────────────────▼──────────────────────────────┐  │
│  │    Adapters (AI, Cache, FileSystem)          │  │
│  └───────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

---

## 📝 Phase 1 功能清单

### MVP 核心功能

#### 1. 黑客松项目管理 ✨
- [ ] 创建黑客松项目
- [ ] 团队注册
- [ ] 提交代码（GitHub URL）
- [ ] 查看项目状态

#### 2. 自动化代码评审 ✨
- [ ] 自动拉取 GitHub 代码
- [ ] 调用核心框架进行分析
- [ ] 生成黑客松专属评分
- [ ] 保存评审历史

#### 3. 评分系统 ✨
- [ ] 代码质量评分 (40%)
- [ ] 创新性评分 (30%)
- [ ] 完成度评分 (20%)
- [ ] 文档质量评分 (10%)
- [ ] 综合排名计算

#### 4. 排行榜 ✨
- [ ] 实时排行榜
- [ ] 分类排行（语言、赛道）
- [ ] 历史记录查询

#### 5. Web API ✨
- [ ] RESTful API 接口
- [ ] 认证与授权
- [ ] API 文档（Swagger）

#### 6. CLI 工具 ✨
- [ ] 黑客松项目创建
- [ ] 代码提交命令
- [ ] 排行榜查看

---

## 🎯 Week 1 任务分解

### Day 1-2: 领域模型与核心服务

**任务**:
1. ✅ 创建黑客松领域模型
   - HackathonProject
   - Team
   - Submission
   - Participant
   - HackathonScore

2. ✅ 创建核心应用服务
   - HackathonAnalysisService (封装核心分析)
   - HackathonScoringService (黑客松评分)
   - TeamManagementService (团队管理)

3. ✅ 编写单元测试
   - 领域模型测试
   - 服务层测试

**预期产出**:
- 完整的领域模型
- 核心服务实现
- 50+ 单元测试

---

### Day 3-4: GitHub 集成

**任务**:
1. ✅ 实现 GitHubAdapter
   - 代码克隆
   - 仓库信息获取
   - Commit 历史分析

2. ✅ 集成代码分析流程
   - GitHub → 本地 → 分析 → 评分

3. ✅ 编写集成测试
   - GitHub API 测试
   - 端到端分析测试

**预期产出**:
- GitHub 集成完成
- 自动化分析流程
- 集成测试覆盖

---

### Day 5-6: Web API 与排行榜

**任务**:
1. ✅ 实现 RESTful API
   - 项目创建 API
   - 代码提交 API
   - 排行榜查询 API

2. ✅ 实现排行榜服务
   - 实时排名计算
   - Redis 缓存集成
   - 排行榜更新机制

3. ✅ API 文档生成
   - Swagger/OpenAPI 配置
   - API 使用示例

**预期产出**:
- 完整的 Web API
- 实时排行榜
- API 文档

---

### Day 7: CLI 工具与测试

**任务**:
1. ✅ 实现 CLI 工具
   - hackathon create
   - hackathon submit
   - hackathon leaderboard

2. ✅ 端到端测试
   - 完整流程测试
   - 性能测试
   - 边界测试

3. ✅ 文档编写
   - 用户手册
   - 开发文档

**预期产出**:
- CLI 工具完成
- 完整测试覆盖
- 用户文档

---

## 🔧 技术栈

### 新增依赖

```xml
<!-- GitHub API -->
<dependency>
    <groupId>org.kohsuke</groupId>
    <artifactId>github-api</artifactId>
    <version>1.318</version>
</dependency>

<!-- Spring Boot Web (RESTful API) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>3.2.0</version>
</dependency>

<!-- Redis (排行榜) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
    <version>3.2.0</version>
</dependency>

<!-- Swagger (API 文档) -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>

<!-- JGit (Git 操作) -->
<dependency>
    <groupId>org.eclipse.jgit</groupId>
    <artifactId>org.eclipse.jgit</artifactId>
    <version>6.8.0.202311291450-r</version>
</dependency>
```

---

## 📊 评分系统设计

### 黑客松评分模型

```java
public class HackathonScore {
    // 代码质量 (40%)
    private int codeQuality;        // 0-100
    
    // 创新性 (30%)
    private int innovation;         // 0-100
    
    // 完成度 (20%)
    private int completeness;       // 0-100
    
    // 文档质量 (10%)
    private int documentation;      // 0-100
    
    // 综合得分
    public int calculateTotalScore() {
        return (int) (
            codeQuality * 0.4 +
            innovation * 0.3 +
            completeness * 0.2 +
            documentation * 0.1
        );
    }
}
```

### 评分维度详解

#### 1. 代码质量 (40%)
- **来源**: 核心框架的 `ReviewReport.overallScore`
- **权重**: 40%
- **评估项**:
  - 代码规范性
  - 架构合理性
  - 测试覆盖率
  - 代码复杂度

#### 2. 创新性 (30%)
- **来源**: AI 分析 + 关键词识别
- **权重**: 30%
- **评估项**:
  - 新技术使用
  - 解决方案独特性
  - 功能创新点
  - 技术难度

#### 3. 完成度 (20%)
- **来源**: 项目结构分析
- **权重**: 20%
- **评估项**:
  - 核心功能完整性
  - 代码提交频率
  - README 完善度
  - 测试用例数量

#### 4. 文档质量 (10%)
- **来源**: Markdown 文件分析
- **权重**: 10%
- **评估项**:
  - README 质量
  - 代码注释
  - API 文档
  - 使用说明

---

## 🔒 安全性考虑

### 1. GitHub 访问控制
```yaml
hackathon:
  github:
    access-token: ${GITHUB_TOKEN}  # 环境变量
    rate-limit:
      max-requests: 100
      time-window: 3600  # 1小时
```

### 2. API 认证
```java
@Configuration
public class SecurityConfig {
    // JWT Token 认证
    // API Key 认证
    // 防止恶意提交
}
```

### 3. 代码隔离
```java
// 每个项目在独立目录分析
// 分析后自动清理
// 防止代码泄露
```

---

## 📈 性能目标

### MVP 性能指标

| 指标 | 目标 | 备注 |
|------|------|------|
| GitHub 克隆时间 | < 30秒 | 中等项目 |
| 代码分析时间 | < 2分钟 | 依赖核心框架 |
| 评分计算时间 | < 5秒 | 纯计算 |
| 排行榜更新 | 实时 | Redis 缓存 |
| API 响应时间 | < 200ms | 查询类 API |
| 并发支持 | 10+ | MVP 阶段 |

---

## 🧪 测试策略

### 测试覆盖目标

```
单元测试:     80%+ 覆盖率
集成测试:     核心流程 100% 覆盖
端到端测试:   主要场景覆盖
性能测试:     压力测试
```

### 测试场景

#### 1. 正常流程
- ✅ 创建黑客松项目
- ✅ 提交 GitHub URL
- ✅ 自动分析评分
- ✅ 查看排行榜

#### 2. 异常场景
- ⚠️ GitHub 仓库不存在
- ⚠️ 网络超时
- ⚠️ 代码格式错误
- ⚠️ 分析失败

#### 3. 边界场景
- 🔍 空项目
- 🔍 超大项目
- 🔍 多语言混合
- 🔍 私有仓库

---

## 📦 交付物清单

### Week 1 交付物

- [ ] **代码**
  - ✅ 领域模型 (5 个类)
  - ✅ 应用服务 (4 个服务)
  - ✅ GitHub 适配器
  - ✅ Web API 接口
  - ✅ CLI 工具

- [ ] **测试**
  - ✅ 单元测试 (100+ 用例)
  - ✅ 集成测试 (20+ 用例)
  - ✅ 端到端测试 (10+ 用例)

- [ ] **文档**
  - ✅ API 文档 (Swagger)
  - ✅ 用户手册
  - ✅ 开发文档
  - ✅ 部署指南

- [ ] **配置**
  - ✅ application-hackathon.yml
  - ✅ docker-compose.yml (Redis)
  - ✅ 环境变量配置

---

## 🚀 部署计划

### 开发环境部署

```bash
# 1. 启动 Redis
docker-compose up -d redis

# 2. 配置环境变量
export GITHUB_TOKEN=your_github_token
export DEEPSEEK_API_KEY=your_deepseek_key

# 3. 启动应用
mvn spring-boot:run -Dspring.profiles.active=hackathon

# 4. 访问 API 文档
open http://localhost:8080/swagger-ui.html
```

### 测试环境部署

```bash
# 使用 Docker 部署
docker build -t ai-reviewer-hackathon:latest .
docker run -p 8080:8080 \
  -e GITHUB_TOKEN=$GITHUB_TOKEN \
  -e DEEPSEEK_API_KEY=$DEEPSEEK_API_KEY \
  ai-reviewer-hackathon:latest
```

---

## 🎯 Week 2 规划

### 功能完善
- [ ] 团队协作功能
- [ ] 评审历史查询
- [ ] 评分详情展示
- [ ] 排行榜筛选

### 性能优化
- [ ] 并发分析优化
- [ ] 缓存策略优化
- [ ] 数据库性能调优

### 用户体验
- [ ] Web UI 界面
- [ ] 邮件通知
- [ ] Webhook 集成

---

## ✅ 成功标准

### MVP 验收标准

1. **功能完整性** ✅
   - 核心功能全部实现
   - API 接口可用
   - CLI 工具可用

2. **质量标准** ✅
   - 测试覆盖率 > 80%
   - 所有测试通过
   - 无严重 Bug

3. **性能标准** ✅
   - 满足性能目标
   - 并发支持达标
   - 响应时间符合预期

4. **文档完善** ✅
   - API 文档完整
   - 用户手册清晰
   - 部署文档可用

5. **可迁移性** ⚠️ **关键**
   - 代码隔离清晰
   - 依赖最小化
   - 迁移成本 < 2天

---

## 🎊 开始实施

接下来我将开始创建黑客松模块的代码框架。首先创建领域模型！

**准备好了吗？让我们开始创作吧！** 🚀

---

**文档创建时间**: 2025-11-12 05:00:00  
**预计完成时间**: 2025-11-26 (2周后)  
**当前状态**: 🟢 Phase 1 启动  
**下一步**: 创建领域模型代码


