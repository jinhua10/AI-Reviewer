package top.yumbo.ai.reviewer.application.hackathon.service;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.reviewer.adapter.output.ast.parser.ASTParserFactory;
import top.yumbo.ai.reviewer.application.port.output.ASTParserPort;
import top.yumbo.ai.reviewer.domain.hackathon.model.HackathonScore;
import top.yumbo.ai.reviewer.domain.hackathon.model.HackathonScoringConfig;
import top.yumbo.ai.reviewer.domain.hackathon.model.DimensionScoringRegistry;
import top.yumbo.ai.reviewer.domain.hackathon.model.ScoringRule;
import top.yumbo.ai.reviewer.domain.model.Project;
import top.yumbo.ai.reviewer.domain.model.ReviewReport;
import top.yumbo.ai.reviewer.domain.model.SourceFile;
import top.yumbo.ai.reviewer.domain.model.ast.CodeInsight;
import top.yumbo.ai.reviewer.domain.model.ast.CodeSmell;
import top.yumbo.ai.reviewer.domain.model.ast.ComplexityMetrics;
import top.yumbo.ai.reviewer.domain.model.ast.DesignPattern;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 黑客松评分服务（动态配置版）
 *
 * 核心特性：
 * - 支持动态扩展评分维度
 * - 支持动态添加评分规则
 * - 完全基于配置文件，零代码修改
 * - 最大化利用AST信息
 * - 策略模式消除硬编码
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-13
 */
@Slf4j
public class HackathonScoringService {

    // AST解析器
    private final ASTParserPort astParser;

    // 动态配置
    private final HackathonScoringConfig config;

    // 策略注册表（消除硬编码）
    private final DimensionScoringRegistry scoringRegistry;

    /**
     * 构造函数（使用默认配置）
     */
    public HackathonScoringService() {
        this.astParser = new ASTParserFactory();
        this.config = HackathonScoringConfig.createDefault();
        this.scoringRegistry = initializeScoringStrategies();
        log.info("🚀 黑客松评分服务初始化完成（策略模式 - 零硬编码）");
        logConfiguration();
    }

    /**
     * 构造函数（自定义AST解析器）
     */
    public HackathonScoringService(ASTParserPort astParser) {
        this.astParser = astParser;
        this.config = HackathonScoringConfig.createDefault();
        this.scoringRegistry = initializeScoringStrategies();
        log.info("🚀 黑客松评分服务初始化完成（自定义AST + 策略模式）");
        logConfiguration();
    }

    /**
     * 输出配置信息
     */
    private void logConfiguration() {
        log.info("📊 评分维度数量: {}", config.getAllDimensions().size());
        config.getAllDimensions().forEach(dim -> {
            double weight = config.getDimensionWeight(dim);
            String displayName = config.getDimensionDisplayName(dim);
            log.info("  - {} ({}): {}", displayName, dim, String.format("%.1f%%", weight * 100));
        });
        log.info("📋 评分规则数量: {} (启用: {})",
            config.getScoringRules().size(),
            config.getEnabledRules().size());
        log.info("🔬 AST深度分析: {}", config.isEnableASTAnalysis() ? "✅ 启用" : "❌ 禁用");

        // 验证配置
        if (!config.validateConfig()) {
            log.warn("⚠️ 配置验证失败，请检查配置文件");
        }
    }

    /**
     * 初始化评分策略（消除硬编码）
     */
    private DimensionScoringRegistry initializeScoringStrategies() {
        DimensionScoringRegistry registry = DimensionScoringRegistry.createDefault();

        // 注册评分策略
        registry.registerScoringStrategy("code_quality",
            (report, project, codeInsight) -> calculateCodeQualityWithAST(report, codeInsight));
        registry.registerScoringStrategy("innovation",
            (report, project, codeInsight) -> calculateInnovationWithAST(report, project, codeInsight));
        registry.registerScoringStrategy("completeness",
            (report, project, codeInsight) -> calculateCompletenessWithAST(report, project, codeInsight));
        registry.registerScoringStrategy("documentation",
            (report, project, codeInsight) -> calculateDocumentation(project));
        registry.registerScoringStrategy("user_experience",
            (report, project, codeInsight) -> calculateUserExperienceScore(project, codeInsight));
        registry.registerScoringStrategy("performance",
            (report, project, codeInsight) -> calculatePerformanceScore(project, codeInsight));
        registry.registerScoringStrategy("security",
            (report, project, codeInsight) -> calculateSecurityScore(project, codeInsight));

        // 注册AST加分策略
        registry.registerASTBonusStrategy("code_quality", codeInsight -> {
            int bonus = 0;
            if (codeInsight.getStructure() != null &&
                codeInsight.getStructure().getArchitectureStyle() != null) {
                bonus += 5;
            }
            if (codeInsight.getComplexityMetrics() != null &&
                codeInsight.getComplexityMetrics().getHighComplexityMethodCount() == 0) {
                bonus += 5;
            }
            return bonus;
        });

        registry.registerASTBonusStrategy("innovation", codeInsight -> {
            int bonus = 0;
            if (codeInsight.getDesignPatterns() != null) {
                int patternCount = codeInsight.getDesignPatterns().getPatterns().size();
                bonus += Math.min(10, patternCount * 2);
            }
            return bonus;
        });

        registry.registerASTBonusStrategy("completeness", codeInsight -> {
            int bonus = 0;
            if (codeInsight.getClasses().size() >= 10) {
                bonus += 5;
            }
            if (codeInsight.getStatistics() != null &&
                codeInsight.getStatistics().getTotalMethods() >= 30) {
                bonus += 5;
            }
            return bonus;
        });

        log.info("✅ 评分策略注册完成: {} 个评分策略, {} 个AST加分策略",
            registry.getScoringStrategies().size(),
            registry.getAstBonusStrategies().size());

        return registry;
    }

    // 创新技术关键词列表
    private static final List<String> INNOVATION_KEYWORDS = List.of(
        "AI", "机器学习", "深度学习", "大模型", "区块链", "云原生",
        "微服务", "serverless", "GraphQL", "WebAssembly", "Rust",
        "Kubernetes", "Docker", "React", "Vue3", "Next.js",
        "Spring Boot", "Redis", "MongoDB", "Elasticsearch"
    );

    // README质量评分正则
    private static final Pattern README_SECTIONS = Pattern.compile(
        "(简介|Introduction|功能|Features|安装|Installation|使用|Usage|API|文档|Documentation)",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * 计算黑客松综合评分（动态配置版）
     *
     * 特性：
     * - 动态维度评分（根据配置文件）
     * - 动态规则应用（支持任意数量规则）
     * - AST深度分析
     * - 完全配置化
     *
     * @param reviewReport 核心评审报告
     * @param project 项目信息
     * @return 黑客松评分
     */
    public HackathonScore calculateScore(ReviewReport reviewReport, Project project) {
        if (reviewReport == null || project == null) {
            throw new IllegalArgumentException("评审报告和项目信息不能为空");
        }

        log.info("📊 开始黑客松动态评分: {}", project.getName());

        // 1. AST解析（如果启用）
        CodeInsight codeInsight = null;
        if (config.isEnableASTAnalysis()) {
            codeInsight = parseProjectWithAST(project);
        } else {
            log.info("AST分析已禁用，使用基础评分");
        }

        // 2. 动态维度评分
        Map<String, Integer> dimensionScores = new HashMap<>();

        for (String dimensionName : config.getAllDimensions()) {
            int dimensionScore = calculateDimensionScore(
                dimensionName,
                reviewReport,
                project,
                codeInsight
            );
            dimensionScores.put(dimensionName, dimensionScore);

            log.info("  ✓ {}: {} 分",
                config.getDimensionDisplayName(dimensionName),
                dimensionScore);
        }

        // 3. 计算加权总分
        double weightedTotal = 0.0;
        for (Map.Entry<String, Integer> entry : dimensionScores.entrySet()) {
            String dimension = entry.getKey();
            int score = entry.getValue();
            double weight = config.getDimensionWeight(dimension);
            weightedTotal += score * weight;
        }

        int totalScore = (int) Math.round(weightedTotal);

        // 4. 构建HackathonScore（向后兼容）
        HackathonScore score = buildCompatibleScore(dimensionScores, totalScore);

        log.info("🎯 评分完成: 总分={}, 等级={}", totalScore, score.getGrade());

        return score;
    }

    /**
     * AST解析
     */
    private CodeInsight parseProjectWithAST(Project project) {
        try {
            if (astParser.supports(project.getType().name())) {
                log.info("🔬 使用AST解析器分析项目: {}", project.getType());
                CodeInsight codeInsight = astParser.parseProject(project);
                log.info("  ✓ AST解析完成: 类数={}, 方法数={}, 设计模式={}",
                    codeInsight.getClasses().size(),
                    codeInsight.getStatistics() != null ? codeInsight.getStatistics().getTotalMethods() : 0,
                    codeInsight.getDesignPatterns() != null ? codeInsight.getDesignPatterns().getPatterns().size() : 0);
                return codeInsight;
            } else {
                log.info("项目类型 {} 不支持AST解析", project.getType());
                return null;
            }
        } catch (Exception e) {
            log.warn("AST解析失败，降级到基础评分: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 计算单个维度得分
     */
    private int calculateDimensionScore(
            String dimensionName,
            ReviewReport reviewReport,
            Project project,
            CodeInsight codeInsight) {

        // 获取该维度的规则
        List<ScoringRule> rules = config.getRulesByDimension(dimensionName);

        if (rules.isEmpty()) {
            // 如果没有规则，使用内置评分逻辑
            return calculateDimensionScoreBuiltIn(dimensionName, reviewReport, project, codeInsight);
        }

        // 应用规则评分
        String projectContent = collectProjectContent(project, codeInsight);
        int totalScore = 0;

        for (ScoringRule rule : rules) {
            if (rule.isEnabled()) {
                int ruleScore = rule.applyRule(projectContent);
                totalScore += ruleScore;
                log.debug("    规则 {}: {} 分", rule.getName(), ruleScore);
            }
        }

        // 结合AST评分
        if (codeInsight != null) {
            totalScore += calculateASTBasedScore(dimensionName, codeInsight);
        }

        return Math.max(0, Math.min(100, totalScore));
    }

    /**
     * 构建向后兼容的HackathonScore
     */
    private HackathonScore buildCompatibleScore(Map<String, Integer> dimensionScores, int totalScore) {
        // 尝试映射到旧的固定维度
        int codeQuality = dimensionScores.getOrDefault("code_quality", totalScore);
        int innovation = dimensionScores.getOrDefault("innovation", 0);
        int completeness = dimensionScores.getOrDefault("completeness", 0);
        int documentation = dimensionScores.getOrDefault("documentation", 0);

        return HackathonScore.builder()
            .codeQuality(codeQuality)
            .innovation(innovation)
            .completeness(completeness)
            .documentation(documentation)
            .build();
    }

    /**
     * 计算代码质量分数 (0-100) - AST增强版 + 配置化
     *
     * 评分维度（可配置权重）：
     * 1. 基础质量（核心框架评分）默认40%
     * 2. 复杂度控制 默认30%
     * 3. 代码坏味道 默认20%
     * 4. 架构设计 默认10%
     */
    private int calculateCodeQualityWithAST(ReviewReport reviewReport, CodeInsight codeInsight) {
        // 基础分数（来自核心框架）
        int baseScore = reviewReport.getOverallScore();

        // 如果没有AST分析，直接返回基础分数
        if (codeInsight == null || !config.isEnableASTAnalysis()) {
            log.info("未使用AST分析，返回基础评分: {}", baseScore);
            return baseScore;
        }

        // 使用配置的权重计算各维度分数（使用默认权重）
        double baseQualityScore = baseScore * 0.40;
        double complexityScore = calculateComplexityScoreWithConfig(codeInsight) * 0.30 * 100;
        double codeSmellScore = calculateCodeSmellScoreWithConfig(codeInsight) * 0.20 * 100;
        double architectureScore = calculateArchitectureScoreWithConfig(codeInsight) * 0.10 * 100;

        int totalScore = (int) Math.round(baseQualityScore + complexityScore + codeSmellScore + architectureScore);

        log.info("代码质量评分明细: 基础={}, 复杂度={}, 坏味道={}, 架构={}, 总计={}",
            (int)baseQualityScore, (int)complexityScore, (int)codeSmellScore, (int)architectureScore, totalScore);

        return Math.min(100, totalScore);
    }

    /**
     * 计算复杂度得分 (0.0-1.0) - 使用配置阈值
     */
    private double calculateComplexityScoreWithConfig(CodeInsight codeInsight) {
        ComplexityMetrics metrics = codeInsight.getComplexityMetrics();
        if (metrics == null) {
            return 0.5; // 默认中等分数
        }

        double score = 1.0; // 满分
        double avgComplexity = metrics.getAvgCyclomaticComplexity();

        // 使用配置的复杂度阈值
        Map<String, Double> thresholds = config.getComplexityThresholds();
        double excellent = thresholds.getOrDefault("excellent", 5.0);
        double good = thresholds.getOrDefault("good", 7.0);
        double medium = thresholds.getOrDefault("medium", 10.0);
        double poor = thresholds.getOrDefault("poor", 15.0);

        // 根据平均复杂度评分
        if (avgComplexity < excellent) {
            score = 1.0; // 优秀
        } else if (avgComplexity < good) {
            score = 0.93; // 良好
        } else if (avgComplexity < medium) {
            score = 0.83; // 中等
        } else if (avgComplexity < poor) {
            score = 0.67; // 较差
        } else {
            score = 0.50; // 很差
        }

        // 高复杂度方法占比扣分
        int highComplexityCount = metrics.getHighComplexityMethodCount();
        int totalMethods = metrics.getTotalMethods();
        if (totalMethods > 0) {
            double highComplexityRatio = (double) highComplexityCount / totalMethods;
            if (highComplexityRatio > 0.30) {
                score -= 0.33; // 危险水平
            } else if (highComplexityRatio > 0.15) {
                score -= 0.17; // 警戒水平
            }
        }

        // 长方法扣分
        int longMethodCount = metrics.getLongMethodCount();
        if (longMethodCount > 0 && totalMethods > 0) {
            double longMethodRatio = (double) longMethodCount / totalMethods;
            if (longMethodRatio > 0.20) {
                score -= 0.10; // 超过20%的长方法
            }
        }

        log.debug("复杂度评分: 平均复杂度={}, 高复杂度方法占比={}%, 长方法数={}, 得分={}",
            String.format("%.2f", avgComplexity),
            String.format("%.1f", (double)highComplexityCount/totalMethods*100),
            longMethodCount,
            String.format("%.2f", score));

        return Math.max(0.0, Math.min(1.0, score));
    }

    /**
     * 计算代码坏味道得分 (0.0-1.0) - 使用配置的扣分规则
     */
    private double calculateCodeSmellScoreWithConfig(CodeInsight codeInsight) {
        List<CodeSmell> smells = codeInsight.getCodeSmells();
        if (smells == null || smells.isEmpty()) {
            log.debug("未检测到代码坏味道，满分");
            return 1.0; // 无坏味道，满分
        }

        double maxDeduction = 20.0; // 最大扣分
        double totalDeduction = 0.0;

        // 使用配置的扣分规则
        Map<String, Integer> penalties = config.getCodeSmellPenalties();

        // 统计各级别坏味道数量
        int criticalCount = 0, highCount = 0, mediumCount = 0, lowCount = 0;

        for (CodeSmell smell : smells) {
            int penalty = penalties.getOrDefault(smell.getSeverity().name(), 1);
            totalDeduction += penalty;

            // 统计数量
            switch (smell.getSeverity()) {
                case CRITICAL -> criticalCount++;
                case HIGH -> highCount++;
                case MEDIUM -> mediumCount++;
                case LOW -> lowCount++;
            }
        }

        log.debug("代码坏味道统计: CRITICAL={}, HIGH={}, MEDIUM={}, LOW={}, 总扣分={}",
            criticalCount, highCount, mediumCount, lowCount, totalDeduction);

        double score = 1.0 - (totalDeduction / maxDeduction);
        return Math.max(0.0, Math.min(1.0, score));
    }

    /**
     * 计算架构设计得分 (0.0-1.0) - 使用配置的架构评分
     */
    private double calculateArchitectureScoreWithConfig(CodeInsight codeInsight) {
        if (codeInsight.getStructure() == null) {
            log.debug("无项目结构信息，返回默认分数");
            return 0.5; // 默认中等分数
        }

        String architecture = codeInsight.getStructure().getArchitectureStyle();
        double score = 0.5; // 默认分数

        // 根据架构风格评分
        if (architecture != null) {
            if (architecture.contains("六边形") || architecture.contains("Hexagonal")) {
                score = 1.0; // 六边形架构，满分
            } else if (architecture.contains("微服务") || architecture.contains("Microservice")) {
                score = 0.9; // 微服务架构
            } else if (architecture.contains("分层") || architecture.contains("Layered")) {
                score = 0.8; // 分层架构
            } else {
                score = 0.6; // 其他架构
            }
        }

        // 检查设计模式使用（加分项）
        if (codeInsight.getDesignPatterns() != null &&
            !codeInsight.getDesignPatterns().getPatterns().isEmpty()) {
            int patternCount = codeInsight.getDesignPatterns().getPatterns().size();
            score += Math.min(0.2, patternCount * 0.05); // 每个模式+5%，最多+20%
        }

        log.debug("架构评分: 风格={}, 设计模式数={}, 得分={}",
            architecture,
            codeInsight.getDesignPatterns() != null ? codeInsight.getDesignPatterns().getPatterns().size() : 0,
            String.format("%.2f", score));

        return Math.max(0.0, Math.min(1.0, score));
    }

    /**
     * 计算创新性分数 (0-100) - AST增强版
     *
     * 评估维度：
     * 1. 使用的新技术栈 30%
     * 2. 设计模式创新性 30%
     * 3. AI 分析的创新性评价 25%
     * 4. 项目的独特性 15%
     */
    private int calculateInnovationWithAST(ReviewReport reviewReport, Project project, CodeInsight codeInsight) {
        // 1. 技术栈创新性评分 (0-30)
        int techStackScore = calculateTechStackInnovation(project);
        techStackScore = (int) (techStackScore * 0.75); // 调整为30分制

        // 2. 设计模式创新性 (0-30)
        int designPatternScore = calculateDesignPatternInnovation(codeInsight);

        // 3. AI 评价创新性 (0-25)
        int aiInnovationScore = extractInnovationFromFindings(reviewReport);
        aiInnovationScore = (int) (aiInnovationScore * 0.625); // 调整为25分制

        // 4. 项目独特性 (0-15)
        int uniquenessScore = calculateUniqueness(project);
        uniquenessScore = (int) (uniquenessScore * 0.75); // 调整为15分制

        int totalScore = techStackScore + designPatternScore + aiInnovationScore + uniquenessScore;

        log.debug("创新性评分明细: 技术栈={}, 设计模式={}, AI评价={}, 独特性={}, 总计={}",
            techStackScore, designPatternScore, aiInnovationScore, uniquenessScore, totalScore);

        return Math.min(100, totalScore);
    }

    /**
     * 计算设计模式创新性 (0-30)
     */
    private int calculateDesignPatternInnovation(CodeInsight codeInsight) {
        if (codeInsight == null || codeInsight.getDesignPatterns() == null) {
            return 10; // 默认分数
        }

        List<DesignPattern> patterns = codeInsight.getDesignPatterns().getPatterns();
        if (patterns.isEmpty()) {
            return 5; // 没有使用设计模式
        }

        int score = 10; // 基础分

        // 每种设计模式加分
        for (DesignPattern pattern : patterns) {
            switch (pattern.getType()) {
                case SINGLETON, FACTORY, BUILDER -> score += 2; // 常见模式
                case ADAPTER, DECORATOR, PROXY, FACADE -> score += 3; // 结构型模式
                case STRATEGY, OBSERVER, COMMAND, TEMPLATE_METHOD -> score += 3; // 行为型模式
                case MVC, MVVM, REPOSITORY -> score += 4; // 架构模式
                default -> score += 1;
            }
        }

        // 多种模式组合使用额外加分
        if (patterns.size() >= 3) {
            score += 5;
        }

        return Math.min(30, score);
    }

    /**
     * 计算技术栈创新性
     */
    private int calculateTechStackInnovation(Project project) {
        String projectContent = collectProjectContent(project);
        String lowerContent = projectContent.toLowerCase();

        long matchCount = INNOVATION_KEYWORDS.stream()
            .filter(keyword -> lowerContent.contains(keyword.toLowerCase()))
            .count();

        // 每个创新关键词 5 分，最高 40 分
        int score = (int) (matchCount * 5);
        log.debug("技术栈创新性: 匹配关键词数={}, 得分={}", matchCount, score);
        return Math.min(40, score);
    }

    /**
     * 从评审发现中提取创新性评分
     */
    private int extractInnovationFromFindings(ReviewReport reviewReport) {
        List<String> findings = reviewReport.getKeyFindings();
        if (findings == null || findings.isEmpty()) {
            return 20; // 默认分数
        }

        // 检查是否提到创新、新颖、独特等词汇
        String allFindings = String.join(" ", findings).toLowerCase();

        int score = 20; // 基础分

        if (allFindings.contains("创新") || allFindings.contains("innovative")) {
            score += 10;
        }
        if (allFindings.contains("新颖") || allFindings.contains("novel")) {
            score += 5;
        }
        if (allFindings.contains("独特") || allFindings.contains("unique")) {
            score += 5;
        }

        return Math.min(40, score);
    }

    /**
     * 计算项目独特性
     */
    private int calculateUniqueness(Project project) {
        // 基于项目名称和结构的独特性
        int score = 10; // 基础分

        // 如果项目有多种语言混合，加分
        if (project.getSourceFiles().stream()
            .map(SourceFile::getProjectType)
            .distinct()
            .count() > 2) {
            score += 5;
        }

        // 如果代码规模适中（500-3000行），加分
        int totalLines = project.getTotalLines();
        if (totalLines >= 500 && totalLines <= 3000) {
            score += 5;
        }

        return score;
    }

    /**
     * 计算完成度分数 (0-100) - AST增强版
     *
     * 评估维度：
     * 1. 代码结构完整性 40%
     * 2. 功能实现度 30%
     * 3. 测试覆盖率 20%
     * 4. 文档完整性 10%
     */
    private int calculateCompletenessWithAST(ReviewReport reviewReport, Project project, CodeInsight codeInsight) {
        // 1. 代码结构完整性 (0-40)
        int structureScore = calculateStructureCompleteness(codeInsight);

        // 2. 功能实现度 (0-30)
        int functionalityScore = calculateFunctionalityWithAST(project, codeInsight);

        // 3. 测试覆盖率 (0-20)
        int testScore = calculateTestCoverage(project);

        // 4. 文档完整性 (0-10)
        int docScore = (int) (calculateDocumentation(project) * 0.1);

        int totalScore = structureScore + functionalityScore + testScore + docScore;

        log.debug("完成度评分明细: 结构={}, 功能={}, 测试={}, 文档={}, 总计={}",
            structureScore, functionalityScore, testScore, docScore, totalScore);

        return Math.min(100, totalScore);
    }

    /**
     * 计算代码结构完整性 (0-40)
     */
    private int calculateStructureCompleteness(CodeInsight codeInsight) {
        if (codeInsight == null) {
            return 15; // 默认分数
        }

        int score = 0;

        // 类数量评分 (0-15)
        int classCount = codeInsight.getClasses().size();
        if (classCount >= 20) {
            score += 15;
        } else if (classCount >= 10) {
            score += 12;
        } else if (classCount >= 5) {
            score += 9;
        } else if (classCount >= 3) {
            score += 6;
        } else {
            score += 3;
        }

        // 方法数量评分 (0-10)
        if (codeInsight.getStatistics() != null) {
            int methodCount = codeInsight.getStatistics().getTotalMethods();
            if (methodCount >= 50) {
                score += 10;
            } else if (methodCount >= 30) {
                score += 8;
            } else if (methodCount >= 15) {
                score += 6;
            } else if (methodCount >= 5) {
                score += 4;
            }
        }

        // 架构清晰度 (0-10)
        if (codeInsight.getStructure() != null &&
            codeInsight.getStructure().getArchitectureStyle() != null) {
            score += 10; // 有明确的架构风格
        } else {
            score += 5;
        }

        // 接口使用 (0-5)
        if (codeInsight.getInterfaces() != null && !codeInsight.getInterfaces().isEmpty()) {
            score += 5;
        }

        return Math.min(40, score);
    }

    /**
     * 计算功能实现度 (0-30) - AST增强版
     */
    private int calculateFunctionalityWithAST(Project project, CodeInsight codeInsight) {
        int score = 0;

        // 基于文件数量评估 (0-10)
        int fileCount = project.getSourceFiles().size();
        if (fileCount >= 20) score += 10;
        else if (fileCount >= 10) score += 8;
        else if (fileCount >= 5) score += 6;
        else score += 3;

        // 基于代码行数评估 (0-10)
        int totalLines = project.getTotalLines();
        if (totalLines >= 2000) score += 10;
        else if (totalLines >= 1000) score += 8;
        else if (totalLines >= 500) score += 6;
        else if (totalLines >= 200) score += 4;
        else score += 2;

        // 基于AST分析的功能完整性 (0-10)
        if (codeInsight != null) {
            // 有多层架构
            if (codeInsight.getStructure() != null &&
                codeInsight.getStructure().getLayers().size() >= 3) {
                score += 5;
            }

            // 方法平均长度合理（不要太短也不要太长）
            if (codeInsight.getComplexityMetrics() != null) {
                double avgLength = codeInsight.getComplexityMetrics().getAvgMethodLength();
                if (avgLength >= 10 && avgLength <= 50) {
                    score += 5; // 方法长度合理
                } else {
                    score += 2;
                }
            }
        }

        return Math.min(30, score);
    }

    /**
     * 计算功能完整性（已废弃，使用calculateFunctionalityWithAST代替）
     * @deprecated 使用 {@link #calculateFunctionalityWithAST(Project, CodeInsight)} 代替
     */
    @Deprecated
    private int calculateFunctionality(Project project) {
        int score = 0;

        // 基于文件数量评估
        int fileCount = project.getSourceFiles().size();
        if (fileCount >= 5) score += 15;
        if (fileCount >= 10) score += 10;
        if (fileCount >= 20) score += 10;

        // 基于代码行数评估
        int totalLines = project.getTotalLines();
        if (totalLines >= 200) score += 5;
        if (totalLines >= 500) score += 5;
        if (totalLines >= 1000) score += 5;

        return score;
    }

    /**
     * 计算测试覆盖率分数
     */
    private int calculateTestCoverage(Project project) {
        long testFileCount = project.getSourceFiles().stream()
            .filter(file -> file.getPath().toString().toLowerCase().contains("test"))
            .count();

        long totalFiles = project.getSourceFiles().size();
        if (totalFiles == 0) return 0;

        double testRatio = (double) testFileCount / totalFiles;

        // 测试文件占比 20% 以上给满分
        return (int) Math.min(20, testRatio * 100);
    }

    /**
     * 计算文档质量分数 (0-100)
     *
     * 评估维度：
     * 1. README 完善度 (60%)
     * 2. 代码注释率 (30%)
     * 3. API 文档 (10%)
     */
    private int calculateDocumentation(Project project) {
        // 1. README 质量 (0-60)
        int readmeScore = calculateReadmeQuality(project);

        // 2. 代码注释率 (0-30)
        int commentScore = calculateCommentRatio(project);

        // 3. API 文档 (0-10)
        int apiDocScore = hasApiDoc(project) ? 10 : 0;

        return Math.min(100, readmeScore + commentScore + apiDocScore);
    }

    /**
     * 计算 README 质量
     */
    private int calculateReadmeQuality(Project project) {
        SourceFile readme = project.getSourceFiles().stream()
            .filter(file -> file.getPath().toString().toLowerCase().contains("readme"))
            .findFirst()
            .orElse(null);

        if (readme == null || readme.getContent() == null) {
            return 0; // 没有 README
        }

        String content = readme.getContent();
        int score = 20; // 有 README 基础分

        // 检查各个章节
        java.util.regex.Matcher matcher = README_SECTIONS.matcher(content);
        int sectionCount = 0;
        while (matcher.find()) {
            sectionCount++;
        }

        // 每个标准章节 8 分
        score += Math.min(40, sectionCount * 8);

        return Math.min(60, score);
    }

    /**
     * 计算代码注释率
     */
    private int calculateCommentRatio(Project project) {
        // 简化实现：假设有注释的给 20 分
        // 实际应该统计注释行数占比
        boolean hasComments = project.getSourceFiles().stream()
            .anyMatch(file -> file.getContent() != null &&
                            (file.getContent().contains("//") ||
                             file.getContent().contains("/*")));

        return hasComments ? 20 : 10;
    }

    /**
     * 检查是否有 API 文档
     */
    private boolean hasApiDoc(Project project) {
        return project.getSourceFiles().stream()
            .anyMatch(file -> file.getPath().toString().toLowerCase().contains("api") ||
                            file.getPath().toString().toLowerCase().contains("swagger") ||
                            file.getPath().toString().toLowerCase().contains("openapi"));
    }

    /**
     * 收集项目内容（用于关键词匹配）
     */
    private String collectProjectContent(Project project) {
        StringBuilder content = new StringBuilder();

        // 添加项目名称
        content.append(project.getName()).append(" ");

        // 添加所有源文件内容
        project.getSourceFiles().forEach(file -> {
            if (file.getContent() != null) {
                content.append(file.getContent()).append(" ");
            }
        });

        return content.toString();
    }

    /**
     * 收集项目内容（增强版 - 包含AST信息）
     */
    private String collectProjectContent(Project project, CodeInsight codeInsight) {
        StringBuilder content = new StringBuilder();

        // 1. 项目基本信息
        content.append("项目名称: ").append(project.getName()).append("\n");
        content.append("项目类型: ").append(project.getType().getDisplayName()).append("\n");
        content.append("文件数量: ").append(project.getSourceFiles().size()).append("\n");
        content.append("代码行数: ").append(project.getTotalLines()).append("\n\n");

        // 2. 源文件内容
        project.getSourceFiles().forEach(file -> {
            if (file.getContent() != null) {
                content.append(file.getContent()).append("\n");
            }
        });

        // 3. AST信息（如果有）
        if (codeInsight != null) {
            // 架构风格
            if (codeInsight.getStructure() != null &&
                codeInsight.getStructure().getArchitectureStyle() != null) {
                content.append("架构风格: ")
                       .append(codeInsight.getStructure().getArchitectureStyle())
                       .append("\n");
            }

            // 设计模式
            if (codeInsight.getDesignPatterns() != null) {
                codeInsight.getDesignPatterns().getPatterns().forEach(pattern -> {
                    content.append("设计模式: ").append(pattern.getName()).append("\n");
                });
            }

            // 代码质量信息
            if (codeInsight.getComplexityMetrics() != null) {
                ComplexityMetrics metrics = codeInsight.getComplexityMetrics();
                content.append("平均复杂度: ").append(metrics.getAvgCyclomaticComplexity()).append("\n");
                content.append("长方法数: ").append(metrics.getLongMethodCount()).append("\n");
            }

            // 代码坏味道
            if (!codeInsight.getCodeSmells().isEmpty()) {
                content.append("代码坏味道数量: ").append(codeInsight.getCodeSmells().size()).append("\n");
            }
        }

        return content.toString();
    }

    /**
     * 内置维度评分逻辑（策略模式 - 零硬编码）
     */
    private int calculateDimensionScoreBuiltIn(
            String dimensionName,
            ReviewReport reviewReport,
            Project project,
            CodeInsight codeInsight) {

        // 使用策略注册表（消除硬编码switch）
        DimensionScoringRegistry.ScoringStrategy strategy = scoringRegistry.getScoringStrategy(dimensionName);

        if (strategy != null) {
            return strategy.calculate(reviewReport, project, codeInsight);
        }

        // 未注册的维度返回默认分数
        log.warn("未注册的维度: {}, 返回默认分数。请在initializeScoringStrategies()中注册该维度的评分策略", dimensionName);
        return 50;
    }

    /**
     * 基于AST的额外评分（策略模式 - 零硬编码）
     */
    private int calculateASTBasedScore(String dimensionName, CodeInsight codeInsight) {
        // 使用策略注册表（消除硬编码switch）
        DimensionScoringRegistry.ASTBonusStrategy strategy = scoringRegistry.getASTBonusStrategy(dimensionName);

        if (strategy != null) {
            return strategy.calculateBonus(codeInsight);
        }

        // 未注册AST加分策略的维度返回0
        return 0;
    }

    /**
     * 用户体验评分（新维度）
     */
    private int calculateUserExperienceScore(Project project, CodeInsight codeInsight) {
        int score = 50; // 基础分

        String content = collectProjectContent(project, codeInsight).toLowerCase();

        // 检查UI相关关键词
        if (content.contains("界面") || content.contains("ui") || content.contains("前端")) {
            score += 15;
        }
        if (content.contains("响应式") || content.contains("responsive")) {
            score += 10;
        }
        if (content.contains("用户体验") || content.contains("ux")) {
            score += 10;
        }

        return Math.min(100, score);
    }

    /**
     * 性能评分（新维度）
     */
    private int calculatePerformanceScore(Project project, CodeInsight codeInsight) {
        int score = 50; // 基础分

        String content = collectProjectContent(project, codeInsight).toLowerCase();

        // 检查性能优化关键词
        if (content.contains("缓存") || content.contains("cache")) {
            score += 12;
        }
        if (content.contains("异步") || content.contains("async")) {
            score += 10;
        }
        if (content.contains("索引") || content.contains("index")) {
            score += 8;
        }
        if (content.contains("优化") || content.contains("optimization")) {
            score += 10;
        }

        return Math.min(100, score);
    }

    /**
     * 安全性评分（新维度）
     */
    private int calculateSecurityScore(Project project, CodeInsight codeInsight) {
        int score = 50; // 基础分

        String content = collectProjectContent(project, codeInsight).toLowerCase();

        // 检查安全相关关键词
        if (content.contains("验证") || content.contains("validation")) {
            score += 12;
        }
        if (content.contains("加密") || content.contains("encrypt")) {
            score += 12;
        }
        if (content.contains("授权") || content.contains("auth")) {
            score += 10;
        }
        if (content.contains("sql注入") || content.contains("xss")) {
            score += 8;
        }

        // 有安全漏洞扣分
        if (content.contains("明文密码") || content.contains("安全漏洞")) {
            score -= 20;
        }

        return Math.max(0, Math.min(100, score));
    }

    /**
     * 获取评分详细说明
     *
     * @param score 黑客松评分
     * @return 详细说明
     */
    public String getScoreExplanation(HackathonScore score) {
        return String.format(
            "【黑客松评分详情】\n\n" +
            "总分: %d / 100 (%s)\n" +
            "%s\n\n" +
            "【各维度分析】\n" +
            "1. 代码质量 (40%%): %d 分\n" +
            "   - 代码规范性、架构设计、测试覆盖率等\n\n" +
            "2. 创新性 (30%%): %d 分\n" +
            "   - 技术栈创新、解决方案独特性、功能创新点\n\n" +
            "3. 完成度 (20%%): %d 分\n" +
            "   - 核心功能完整性、代码量、测试覆盖\n\n" +
            "4. 文档质量 (10%%): %d 分\n" +
            "   - README 完善度、代码注释、API 文档\n\n" +
            "【总体评价】\n" +
            "最强项: %s\n" +
            "待提升: %s",
            score.calculateTotalScore(),
            score.getGrade(),
            score.getGradeDescription(),
            score.getCodeQuality(),
            score.getInnovation(),
            score.getCompleteness(),
            score.getDocumentation(),
            score.getStrongestDimension(),
            score.getWeakestDimension()
        );
    }
}

