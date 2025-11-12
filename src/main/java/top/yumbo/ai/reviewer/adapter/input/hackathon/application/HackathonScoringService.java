package top.yumbo.ai.reviewer.adapter.input.hackathon.application;

import top.yumbo.ai.reviewer.domain.hackathon.model.HackathonScore;
import top.yumbo.ai.reviewer.domain.model.Project;
import top.yumbo.ai.reviewer.domain.model.ReviewReport;
import top.yumbo.ai.reviewer.domain.model.SourceFile;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 黑客松评分服务
 *
 * 负责将核心框架的评审报告转换为黑客松专属的四维度评分
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-12
 */
public class HackathonScoringService {

    // 创新技术关键词
    private static final List<String> INNOVATION_KEYWORDS = List.of(
        "AI", "机器学习", "深度学习", "大模型", "区块链", "云原生",
        "微服务", "serverless", "GraphQL", "WebAssembly", "Rust",
        "Kubernetes", "Docker", "React", "Vue3", "Next.js"
    );

    // README 质量评分正则
    private static final Pattern README_SECTIONS = Pattern.compile(
        "(简介|Introduction|功能|Features|安装|Installation|使用|Usage|API|文档|Documentation)",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * 计算黑客松综合评分
     *
     * @param reviewReport 核心评审报告
     * @param project 项目信息
     * @return 黑客松评分
     */
    public HackathonScore calculateScore(ReviewReport reviewReport, Project project) {
        if (reviewReport == null || project == null) {
            throw new IllegalArgumentException("评审报告和项目信息不能为空");
        }

        int codeQuality = calculateCodeQuality(reviewReport);
        int innovation = calculateInnovation(reviewReport, project);
        int completeness = calculateCompleteness(reviewReport, project);
        int documentation = calculateDocumentation(project);

        return HackathonScore.builder()
            .codeQuality(codeQuality)
            .innovation(innovation)
            .completeness(completeness)
            .documentation(documentation)
            .build();
    }

    /**
     * 计算代码质量分数 (0-100)
     *
     * 直接使用核心框架的 overallScore
     */
    private int calculateCodeQuality(ReviewReport reviewReport) {
        // 核心框架的 overallScore 已经是 0-100
        return reviewReport.getOverallScore();
    }

    /**
     * 计算创新性分数 (0-100)
     *
     * 评估维度：
     * 1. 使用的新技术栈 (40%)
     * 2. AI 分析的创新性评价 (40%)
     * 3. 项目的独特性 (20%)
     */
    private int calculateInnovation(ReviewReport reviewReport, Project project) {
        // 1. 技术栈创新性评分 (0-40)
        int techStackScore = calculateTechStackInnovation(project);

        // 2. AI 评价创新性 (0-40)
        // 从关键发现中提取创新性评价
        int aiInnovationScore = extractInnovationFromFindings(reviewReport);

        // 3. 项目独特性 (0-20)
        // 基于项目描述和功能的独特性
        int uniquenessScore = calculateUniqueness(project);

        return Math.min(100, techStackScore + aiInnovationScore + uniquenessScore);
    }

    /**
     * 计算技术栈创新性
     */
    private int calculateTechStackInnovation(Project project) {
        String projectContent = collectProjectContent(project);

        long matchCount = INNOVATION_KEYWORDS.stream()
            .filter(keyword -> projectContent.toLowerCase().contains(keyword.toLowerCase()))
            .count();

        // 每个创新关键词 5 分，最高 40 分
        return Math.min(40, (int) (matchCount * 5));
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
     * 计算完成度分数 (0-100)
     *
     * 评估维度：
     * 1. 核心功能完整性 (50%)
     * 2. 代码提交频率 (30%)
     * 3. 测试覆盖率 (20%)
     */
    private int calculateCompleteness(ReviewReport reviewReport, Project project) {
        // 1. 核心功能完整性 (0-50)
        int functionalityScore = calculateFunctionality(project);

        // 2. 代码质量作为完成度的指标 (0-30)
        int qualityScore = (int) (reviewReport.getOverallScore() * 0.3);

        // 3. 测试覆盖率 (0-20)
        int testScore = calculateTestCoverage(project);

        return Math.min(100, functionalityScore + qualityScore + testScore);
    }

    /**
     * 计算功能完整性
     */
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

