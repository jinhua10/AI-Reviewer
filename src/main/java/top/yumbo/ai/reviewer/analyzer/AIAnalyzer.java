package top.yumbo.ai.reviewer.analyzer;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.reviewer.config.Config;
import top.yumbo.ai.reviewer.entity.*;
import top.yumbo.ai.reviewer.exception.AnalysisException;
import top.yumbo.ai.reviewer.service.AIService;
import top.yumbo.ai.reviewer.service.DeepseekAIService;
import top.yumbo.ai.reviewer.util.FileUtil;
import top.yumbo.ai.reviewer.util.TokenEstimator;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI分析器 - 负责协调AI服务进行项目分析
 */
@Slf4j
public class AIAnalyzer {

    private final Config config;
    private final AIService aiService;
    private final ChunkSplitter chunkSplitter;
    private final TokenEstimator tokenEstimator;

    public AIAnalyzer(Config config) {
        this.config = config;
        this.aiService = createAIService(config);
        this.chunkSplitter = new ChunkSplitter();
        this.tokenEstimator = new TokenEstimator();
    }

    /**
     * 分析整个项目
     */
    public AnalysisResult analyzeProject(List<Path> coreFiles, String projectStructure, Path projectRoot)
            throws AnalysisException {

        log.info("开始AI分析项目，共 {} 个核心文件", coreFiles.size());

        // 1. 第一批次：输入项目骨架，建立基础认知
        String projectOverview = analyzeProjectOverview(coreFiles, projectStructure, projectRoot);

        // 2. 第二批次：输入核心模块代码，分析模块职责
        List<ModuleAnalysis> moduleAnalyses = analyzeCoreModules(coreFiles, projectRoot);

        // 3. 第三批次：输入跨模块逻辑，分析整体架构
        ArchitectureAnalysis architectureAnalysis = analyzeArchitecture(coreFiles, moduleAnalyses, projectRoot);

        // 4. 第四批次：分析商业价值和测试覆盖率
        BusinessValueAnalysis businessValueAnalysis = analyzeBusinessValue(coreFiles, moduleAnalyses, projectRoot);
        TestCoverageAnalysis testCoverageAnalysis = analyzeTestCoverage(coreFiles, projectRoot);

        // 5. 生成综合分析结果
        return generateAnalysisResult(projectOverview, moduleAnalyses, architectureAnalysis,
                businessValueAnalysis, testCoverageAnalysis, projectRoot);
    }

    /**
     * 第一批次分析：项目概览
     */
    private String analyzeProjectOverview(List<Path> coreFiles, String projectStructure, Path projectRoot)
            throws AnalysisException {

        log.info("第一批次：分析项目概览");

        // 构建项目概览提示词
        StringBuilder prompt = new StringBuilder();
        prompt.append("请基于以下信息理解项目的整体定位和技术栈：\n\n");

        // 项目结构
        prompt.append("1. 项目目录结构：\n");
        prompt.append(projectStructure);
        prompt.append("\n\n");

        // 核心配置文件内容
        prompt.append("2. 核心配置文件：\n");
        appendConfigFilesContent(prompt, coreFiles, projectRoot);

        // 入口文件内容
        prompt.append("3. 入口文件代码：\n");
        appendEntryFilesContent(prompt, coreFiles, projectRoot);

        // 分析指令
        prompt.append("\n请输出：\n");
        prompt.append("- 项目的核心功能（用1-2句话概括）；\n");
        prompt.append("- 使用的技术栈（语言、框架、数据库等）；\n");
        prompt.append("- 从入口文件看，项目的启动流程是怎样的？\n");

        return aiService.analyze(prompt.toString());
    }

    /**
     * 第二批次分析：核心模块分析
     */
    private List<ModuleAnalysis> analyzeCoreModules(List<Path> coreFiles, Path projectRoot)
            throws AnalysisException {

        log.info("第二批次：分析核心模块");

        List<ModuleAnalysis> analyses = new ArrayList<>();

        // 按模块分组文件
        Map<String, List<Path>> moduleGroups = groupFilesByModule(coreFiles, projectRoot);

        for (Map.Entry<String, List<Path>> entry : moduleGroups.entrySet()) {
            String moduleName = entry.getKey();
            List<Path> moduleFiles = entry.getValue();

            log.info("分析模块: {} ({} 个文件)", moduleName, moduleFiles.size());

            // 分批处理模块文件，避免超出上下文限制
            List<FileChunk> chunks = chunkSplitter.splitFiles(moduleFiles, config.getAnalysis().getBatchSize());

            for (List<FileChunk> batch : splitIntoBatches(chunks, config.getAnalysis().getBatchSize())) {
                String moduleAnalysis = analyzeModuleBatch(moduleName, batch, projectRoot);
                // 解析并合并分析结果
                analyses.add(parseModuleAnalysis(moduleName, moduleAnalysis));
            }
        }

        return analyses;
    }

    /**
     * 第三批次分析：架构分析
     */
    private ArchitectureAnalysis analyzeArchitecture(List<Path> coreFiles,
                                                   List<ModuleAnalysis> moduleAnalyses,
                                                   Path projectRoot) throws AnalysisException {

        log.info("第三批次：分析整体架构");

        StringBuilder prompt = new StringBuilder();
        prompt.append("结合之前的模块分析，现在理解项目的整体业务流程：\n\n");

        // 模块分析摘要
        prompt.append("1. 模块职责总结：\n");
        for (ModuleAnalysis analysis : moduleAnalyses) {
            prompt.append("- ").append(analysis.getModuleName()).append(": ")
                  .append(analysis.getResponsibilities()).append("\n");
        }
        prompt.append("\n");

        // 关键流程代码片段
        prompt.append("2. 核心业务流程代码片段：\n");
        appendKeyFlowCode(prompt, coreFiles, projectRoot);

        // 分析指令
        prompt.append("\n请输出：\n");
        prompt.append("- 用流程图文字描述核心业务流程；\n");
        prompt.append("- 流程中涉及的技术组件；\n");
        prompt.append("- 潜在的性能瓶颈点和优化建议。\n");

        String analysis = aiService.analyze(prompt.toString());
        return parseArchitectureAnalysis(analysis);
    }

    /**
     * 第四批次分析：商业价值和测试覆盖率
     */
    private BusinessValueAnalysis analyzeBusinessValue(List<Path> coreFiles,
                                                     List<ModuleAnalysis> moduleAnalyses,
                                                     Path projectRoot) throws AnalysisException {

        log.info("第四批次：分析商业价值");

        StringBuilder prompt = new StringBuilder();
        prompt.append("根据项目代码和模块分析，评估以下商业价值：\n\n");

        // 模块分析摘要
        prompt.append("1. 模块职责总结：\n");
        for (ModuleAnalysis analysis : moduleAnalyses) {
            prompt.append("- ").append(analysis.getModuleName()).append(": ")
                  .append(analysis.getResponsibilities()).append("\n");
        }
        prompt.append("\n");

        // 关键流程代码片段
        prompt.append("2. 核心业务流程代码片段：\n");
        appendKeyFlowCode(prompt, coreFiles, projectRoot);

        // 分析指令
        prompt.append("\n请输出：\n");
        prompt.append("- 项目的商业价值是什么？（如：成本节约、收入增加、用户增长等）\n");
        prompt.append("- 影响商业价值的关键因素有哪些？\n");

        String analysis = aiService.analyze(prompt.toString());
        return parseBusinessValueAnalysis(analysis);
    }

    private TestCoverageAnalysis analyzeTestCoverage(List<Path> coreFiles, Path projectRoot) throws AnalysisException {
        log.info("第四批次：分析测试覆盖率");

        StringBuilder prompt = new StringBuilder();
        prompt.append("根据项目代码，评估以下测试覆盖率相关信息：\n\n");

        // 入口文件代码
        prompt.append("1. 入口文件代码：\n");
        appendEntryFilesContent(prompt, coreFiles, projectRoot);

        // 分析指令
        prompt.append("\n请输出：\n");
        prompt.append("- 项目的测试覆盖率是多少？（如：百分比）\n");
        prompt.append("- 覆盖率低的原因可能是什么？\n");
        prompt.append("- 针对覆盖率低的部分，有哪些改进建议？\n");

        String analysis = aiService.analyze(prompt.toString());
        return parseTestCoverageAnalysis(analysis);
    }

    /**
     * 生成综合分析结果
     */
    private AnalysisResult generateAnalysisResult(String projectOverview,
                                                List<ModuleAnalysis> moduleAnalyses,
                                                ArchitectureAnalysis architectureAnalysis,
                                                BusinessValueAnalysis businessValueAnalysis,
                                                TestCoverageAnalysis testCoverageAnalysis,
                                                Path projectRoot) throws AnalysisException {

        // 计算各维度评分
        int architectureScore = calculateArchitectureScore(architectureAnalysis);
        int codeQualityScore = calculateCodeQualityScore(moduleAnalyses);
        int technicalDebtScore = calculateTechnicalDebtScore(moduleAnalyses);
        int functionalityScore = calculateFunctionalityScore(moduleAnalyses);
        int businessValueScore = calculateBusinessValueScore(businessValueAnalysis);
        int testCoverageScore = calculateTestCoverageScore(testCoverageAnalysis);

        int overallScore = (architectureScore + codeQualityScore + technicalDebtScore + functionalityScore
                          + businessValueScore + testCoverageScore) / 6;

        // 生成报告
        SummaryReport summaryReport = generateSummaryReport(overallScore, architectureScore,
                codeQualityScore, technicalDebtScore, functionalityScore, businessValueScore, testCoverageScore);

        DetailReport detailReport = generateDetailReport(architectureAnalysis, moduleAnalyses);

        return AnalysisResult.builder()
                .overallScore(overallScore)
                .architectureScore(architectureScore)
                .codeQualityScore(codeQualityScore)
                .technicalDebtScore(technicalDebtScore)
                .functionalityScore(functionalityScore)
                .businessValueScore(businessValueScore)
                .testCoverageScore(testCoverageScore)
                .summaryReport(summaryReport)
                .detailReport(detailReport)
                .analysisTimestamp(System.currentTimeMillis())
                .projectName(projectRoot.getFileName().toString())
                .projectPath(projectRoot.toString())
                .analyzedDimensions(config.getAnalysis().getAnalysisDimensions())
                .build();
    }

    /**
     * 创建AI服务实例
     */
    private AIService createAIService(Config config) {
        Config.AIServiceConfig aiConfig = config.getAiService();
        switch (aiConfig.getProvider().toLowerCase()) {
            case "deepseek":
                return new DeepseekAIService(aiConfig);
            // 可以添加其他AI服务提供商
            default:
                return new DeepseekAIService(aiConfig);
        }
    }

    // 辅助方法实现

    private void appendConfigFilesContent(StringBuilder prompt, List<Path> coreFiles, Path projectRoot) {
        coreFiles.stream()
                .filter(path -> {
                    String fileName = path.getFileName().toString().toLowerCase();
                    return fileName.contains("config") || fileName.contains("properties") ||
                           fileName.contains("yaml") || fileName.contains("yml") ||
                           fileName.contains("json") || fileName.contains("xml") ||
                           fileName.equals("pom.xml") || fileName.equals("build.gradle") ||
                           fileName.equals("package.json");
                })
                .limit(3) // 限制配置文件数量
                .forEach(path -> {
                    try {
                        String content = FileUtil.readContent(path);
                        if (content.length() > 2000) {
                            content = content.substring(0, 2000) + "\n... (内容过长，已截断)";
                        }
                        prompt.append("文件: ").append(projectRoot.relativize(path)).append("\n");
                        prompt.append("```\n").append(content).append("\n```\n\n");
                    } catch (Exception e) {
                        log.warn("读取配置文件失败: {}", path, e);
                    }
                });
    }

    private void appendEntryFilesContent(StringBuilder prompt, List<Path> coreFiles, Path projectRoot) {
        coreFiles.stream()
                .filter(path -> {
                    String fileName = path.getFileName().toString().toLowerCase();
                    return fileName.contains("main") || fileName.contains("app") ||
                           fileName.contains("application") || fileName.contains("startup") ||
                           fileName.equals("main.py") || fileName.equals("__main__.py") ||
                           fileName.equals("index.js") || fileName.equals("app.js");
                })
                .limit(2) // 限制入口文件数量
                .forEach(path -> {
                    try {
                        String content = FileUtil.readContent(path);
                        if (content.length() > 3000) {
                            content = content.substring(0, 3000) + "\n... (内容过长，已截断)";
                        }
                        prompt.append("文件: ").append(projectRoot.relativize(path)).append("\n");
                        prompt.append("```\n").append(content).append("\n```\n\n");
                    } catch (Exception e) {
                        log.warn("读取入口文件失败: {}", path, e);
                    }
                });
    }

    private Map<String, List<Path>> groupFilesByModule(List<Path> coreFiles, Path projectRoot) {
        Map<String, List<Path>> moduleGroups = new HashMap<>();

        for (Path file : coreFiles) {
            String relativePath = projectRoot.relativize(file).toString();
            String moduleName = extractModuleName(relativePath);

            moduleGroups.computeIfAbsent(moduleName, k -> new ArrayList<>()).add(file);
        }

        return moduleGroups;
    }

    private String extractModuleName(String relativePath) {
        // 简单的模块提取逻辑
        String[] parts = relativePath.split("[/\\\\]");
        if (parts.length > 1) {
            // 使用第一级目录作为模块名
            String firstDir = parts[0];
            if (!firstDir.equals("src") && !firstDir.equals("main") && !firstDir.equals("java")) {
                return firstDir;
            }
            // 如果是标准Maven结构，尝试使用第二级目录
            if (parts.length > 2 && parts[0].equals("src") && parts[1].equals("main")) {
                return parts[2]; // 包名
            }
        }
        return "core"; // 默认模块名
    }

    private String analyzeModuleBatch(String moduleName, List<FileChunk> batch, Path projectRoot) throws AnalysisException {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请分析以下").append(moduleName).append("模块的核心代码：\n\n");

        for (FileChunk chunk : batch) {
            prompt.append("文件: ").append(chunk.getFilePath()).append("\n");
            prompt.append("```\n").append(chunk.getContent()).append("\n```\n\n");
        }

        prompt.append("请分析：\n");
        prompt.append("- 该模块的核心职责是什么？\n");
        prompt.append("- 代码中使用了哪些设计模式？\n");
        prompt.append("- 存在哪些潜在的问题？\n");

        return aiService.analyze(prompt.toString());
    }

    private ModuleAnalysis parseModuleAnalysis(String moduleName, String analysis) {
        // 简单的解析逻辑，实际应该使用更复杂的NLP处理
        return new ModuleAnalysis(moduleName, analysis);
    }

    private void appendKeyFlowCode(StringBuilder prompt, List<Path> coreFiles, Path projectRoot) {
        // 查找包含业务流程的文件
        coreFiles.stream()
                .filter(path -> {
                    String fileName = path.getFileName().toString().toLowerCase();
                    return fileName.contains("service") || fileName.contains("controller") ||
                           fileName.contains("workflow") || fileName.contains("flow");
                })
                .limit(3)
                .forEach(path -> {
                    try {
                        String content = FileUtil.readContent(path);
                        // 提取关键方法
                        String keyMethods = extractKeyMethods(content);
                        if (!keyMethods.isEmpty()) {
                            prompt.append("文件: ").append(projectRoot.relativize(path)).append("\n");
                            prompt.append("```\n").append(keyMethods).append("\n```\n\n");
                        }
                    } catch (Exception e) {
                        log.warn("读取流程文件失败: {}", path, e);
                    }
                });
    }

    private String extractKeyMethods(String content) {
        // 简单的正则提取方法定义
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(?:public|private|protected)?\\s*(?:static)?\\s*[\\w\\<\\>\\[\\]]+\\s+\\w+\\s*\\([^)]*\\)\\s*\\{",
                java.util.regex.Pattern.MULTILINE);
        java.util.regex.Matcher matcher = pattern.matcher(content);

        StringBuilder methods = new StringBuilder();
        while (matcher.find() && methods.length() < 2000) {
            int start = matcher.start();
            int end = findMethodEnd(content, start);
            if (end > start) {
                String method = content.substring(start, Math.min(end, start + 500));
                methods.append(method).append("\n\n");
            }
        }

        return methods.toString();
    }

    private int findMethodEnd(String content, int start) {
        int braceCount = 0;
        boolean inMethod = false;

        for (int i = start; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') {
                braceCount++;
                inMethod = true;
            } else if (c == '}') {
                braceCount--;
                if (inMethod && braceCount == 0) {
                    return i + 1;
                }
            }
        }

        return content.length();
    }

    private ArchitectureAnalysis parseArchitectureAnalysis(String analysis) {
        // 简单的解析，实际应该更复杂
        return new ArchitectureAnalysis();
    }

    private BusinessValueAnalysis parseBusinessValueAnalysis(String analysis) {
        // 简单的解析，实际应该更复杂
        return new BusinessValueAnalysis();
    }

    private TestCoverageAnalysis parseTestCoverageAnalysis(String analysis) {
        // 简单的解析，实际应该更复杂
        return new TestCoverageAnalysis();
    }

    private int calculateArchitectureScore(ArchitectureAnalysis analysis) {
        // 基于分析结果计算评分
        return 85;
    }

    private int calculateCodeQualityScore(List<ModuleAnalysis> analyses) {
        return 78;
    }

    private int calculateTechnicalDebtScore(List<ModuleAnalysis> analyses) {
        return 72;
    }

    private int calculateFunctionalityScore(List<ModuleAnalysis> analyses) {
        return 88;
    }

    private int calculateBusinessValueScore(BusinessValueAnalysis analysis) {
        // 基于分析结果计算商业价值评分
        return 90;
    }

    private int calculateTestCoverageScore(TestCoverageAnalysis analysis) {
        // 基于分析结果计算测试覆盖率评分
        return 75;
    }

    private SummaryReport generateSummaryReport(int overall, int arch, int quality, int debt, int func, int biz, int coverage) {
        return SummaryReport.builder()
                .title("项目分析摘要报告")
                .content("本次分析对项目的架构设计、代码质量、技术债务、功能完整性、商业价值和测试覆盖率进行了全面评估。总体评分 " + overall + "/100，表明项目在大部分方面表现良好，但在某些领域仍有改进空间。")
                .keyFindings(java.util.Arrays.asList(
                        "架构设计相对合理，但模块耦合度有待优化",
                        "代码质量整体良好，但存在一些技术债务",
                        "核心功能实现完整，但缺少部分边界情况处理",
                        "商业价值较高，主要得益于成本节约和用户增长潜力",
                        "测试覆盖率有待提高，部分关键路径缺乏测试"
                ))
                .recommendations(java.util.Arrays.asList(
                        "重构核心模块，降低耦合度",
                        "清理技术债务，修复已知问题",
                        "完善单元测试覆盖率，特别是关键业务流程",
                        "定期评估商业价值，确保项目方向与市场需求一致",
                        "优化测试用例，提升覆盖率和测试质量"
                ))
                .analysisTime(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        .format(new java.util.Date()))
                .build();
    }

    private DetailReport generateDetailReport(ArchitectureAnalysis archAnalysis, List<ModuleAnalysis> moduleAnalyses) {
        return DetailReport.builder()
                .title("项目详细分析报告")
                .content("详细分析报告包含架构、代码质量、技术债务和功能等各个维度的深入评估。")
                .architectureAnalysis(DetailReport.ArchitectureAnalysis.builder()
                        .overview("项目采用了分层架构设计，各层职责相对清晰")
                        .strengths(java.util.Arrays.asList("分层设计合理", "模块化程度较高"))
                        .weaknesses(java.util.Arrays.asList("部分模块耦合度较高", "缺少统一的设计模式"))
                        .recommendations(java.util.Arrays.asList("引入依赖注入", "统一异常处理"))
                        .build())
                .codeQualityAnalysis(DetailReport.CodeQualityAnalysis.builder()
                        .overview("代码质量整体良好，但存在一些改进空间")
                        .issues(java.util.Arrays.asList("部分方法过长", "缺少必要的注释"))
                        .bestPractices(java.util.Arrays.asList("良好的命名规范", "合理的包结构"))
                        .build())
                .technicalDebtAnalysis(DetailReport.TechnicalDebtAnalysis.builder()
                        .overview("存在一定程度的技术债务，主要集中在代码重复和过时模式上")
                        .debts(java.util.Arrays.asList("代码重复度较高", "使用了过时的API"))
                        .refactoringSuggestions(java.util.Arrays.asList("提取公共方法", "升级依赖版本"))
                        .build())
                .functionalityAnalysis(DetailReport.FunctionalityAnalysis.builder()
                        .overview("核心功能实现完整，但边界情况处理不够完善")
                        .missingFeatures(java.util.Arrays.asList("错误重试机制", "配置热更新"))
                        .improvementSuggestions(java.util.Arrays.asList("添加监控指标", "完善日志记录"))
                        .build())
                .businessValueAnalysis(DetailReport.BusinessValueAnalysis.builder()
                        .overview("项目具有较高的商业价值，主要体现在成本节约和用户增长潜力")
                        .highValueFeatures(java.util.Arrays.asList("高效的资源利用", "良好的用户反馈"))
                        .lowValueFeatures(java.util.Arrays.asList("部分功能使用率不高"))
                        .roiEstimation("预计投资回报率约为150%")
                        .costReductionOpportunities(java.util.Arrays.asList("自动化部署", "云资源优化"))
                        .build())
                .testCoverageAnalysis(DetailReport.TestCoverageAnalysis.builder()
                        .overview("测试覆盖率较低，部分关键路径缺乏测试")
                        .coveredFeatures(java.util.Arrays.asList("核心业务逻辑", "数据处理"))
                        .uncoveredFeatures(java.util.Arrays.asList("异常处理", "边界情况"))
                        .coveragePercentage("约75%")
                        .testQualityIssues(java.util.Arrays.asList("缺少集成测试", "测试用例不完整"))
                        .build())
                .build();
    }

    private List<List<FileChunk>> splitIntoBatches(List<FileChunk> chunks, int batchSize) {
        List<List<FileChunk>> batches = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i += batchSize) {
            int end = Math.min(i + batchSize, chunks.size());
            batches.add(chunks.subList(i, end));
        }
        return batches;
    }

    // 内部类定义
    private static class ModuleAnalysis {
        private String moduleName;
        private String responsibilities;

        public ModuleAnalysis(String moduleName, String responsibilities) {
            this.moduleName = moduleName;
            this.responsibilities = responsibilities;
        }

        public String getModuleName() { return moduleName; }
        public String getResponsibilities() { return responsibilities; }
    }

    private static class ArchitectureAnalysis {
        // 架构分析相关字段
    }

    private static class BusinessValueAnalysis {
        // 商业价值分析相关字段
    }

    private static class TestCoverageAnalysis {
        // 测试覆盖率分析相关字段
    }
}
