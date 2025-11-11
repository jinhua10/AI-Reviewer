package top.yumbo.ai.reviewer.analyzer;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.reviewer.config.Config;
import top.yumbo.ai.reviewer.entity.AnalysisResult;
import top.yumbo.ai.reviewer.entity.DetailReport;
import top.yumbo.ai.reviewer.entity.FileChunk;
import top.yumbo.ai.reviewer.entity.SummaryReport;
import top.yumbo.ai.reviewer.exception.AnalysisException;
import top.yumbo.ai.reviewer.service.AIService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * AI 分析器
 * 
 * 负责调用AI服务分析代码块，并汇总分析结果
 */
@Slf4j
public class AIAnalyzer {

    private final Config config;
    private final AIService aiService;
    private final ExecutorService executorService;

    public AIAnalyzer(Config config, AIService aiService, ExecutorService executorService) {
        this.config = config;
        this.aiService = aiService;
        this.executorService = executorService;
    }

    /**
     * 分析代码块
     * 
     * @param chunks 代码块列表
     * @return 分析结果
     * @throws AnalysisException 如果分析过程中发生错误
     */
    public AnalysisResult analyze(List<FileChunk> chunks) throws AnalysisException {
        log.info("开始分析代码块，共 {} 个", chunks.size());

        try {
            // 第一批次：项目骨架分析
            SummaryReport summaryReport = analyzeProjectSkeleton(chunks);

            // 第二批次：核心模块分析
            List<DetailReport> detailReports = analyzeCoreModules(chunks);

            // 第三批次：跨模块逻辑分析
            analyzeCrossModuleLogic(chunks, detailReports);

            // 汇总分析结果
            AnalysisResult result = AnalysisResult.builder()
                    .summaryReport(summaryReport)
                    .detailReports(detailReports)
                    .build();

            log.info("代码分析完成");
            return result;

        } catch (Exception e) {
            throw new AnalysisException("代码分析过程中发生错误", e);
        }
    }

    /**
     * 分析项目骨架
     * 
     * @param chunks 代码块列表
     * @return 项目概要报告
     */
    private SummaryReport analyzeProjectSkeleton(List<FileChunk> chunks) {
        log.info("分析项目骨架...");

        // 筛选入口文件、配置文件等核心文件
        List<FileChunk> skeletonChunks = chunks.stream()
                .filter(chunk -> isSkeletonFile(chunk.getSourceFile().getPath()))
                .collect(Collectors.toList());

        // 构建分析提示
        String prompt = buildSkeletonPrompt(skeletonChunks);

        // 调用AI服务分析
        String response = callAIServiceWithRetry(prompt);

        // 解析响应
        return parseSummaryResponse(response);
    }

    /**
     * 分析核心模块
     * 
     * @param chunks 代码块列表
     * @return 详细报告列表
     */
    private List<DetailReport> analyzeCoreModules(List<FileChunk> chunks) {
        log.info("分析核心模块...");

        // 按模块分组
        var moduleGroups = chunks.stream()
                .collect(Collectors.groupingBy(chunk -> getModuleName(chunk.getSourceFile().getPath())));

        List<CompletableFuture<DetailReport>> futures = new ArrayList<>();

        // 并发分析各模块
        for (var entry : moduleGroups.entrySet()) {
            String moduleName = entry.getKey();
            List<FileChunk> moduleChunks = entry.getValue();

            CompletableFuture<DetailReport> future = CompletableFuture.supplyAsync(() -> {
                // 构建分析提示
                String prompt = buildModulePrompt(moduleName, moduleChunks);

                // 调用AI服务分析
                String response = callAIServiceWithRetry(prompt);

                // 解析响应
                return parseDetailResponse(moduleName, response);
            }, executorService);

            futures.add(future);
        }

        // 等待所有分析完成
        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    /**
     * 分析跨模块逻辑
     * 
     * @param chunks 代码块列表
     * @param detailReports 详细报告列表
     */
    private void analyzeCrossModuleLogic(List<FileChunk> chunks, List<DetailReport> detailReports) {
        log.info("分析跨模块逻辑...");

        // 筛选接口定义、流程代码等
        List<FileChunk> crossModuleChunks = chunks.stream()
                .filter(chunk -> isCrossModuleFile(chunk.getSourceFile().getPath()))
                .collect(Collectors.toList());

        // 构建分析提示
        String prompt = buildCrossModulePrompt(crossModuleChunks, detailReports);

        // 调用AI服务分析
        String response = callAIServiceWithRetry(prompt);

        // 更新详细报告
        updateReportsWithCrossModuleAnalysis(response, detailReports);
    }

    /**
     * 判断是否为骨架文件
     * 
     * @param filePath 文件路径
     * @return 如果是骨架文件返回 true，否则返回 false
     */
    private boolean isSkeletonFile(String filePath) {
        String path = filePath.toLowerCase();

        return path.contains("main.") || path.contains("app.") || path.contains("index.") ||
               path.endsWith("pom.xml") || path.endsWith("package.json") ||
               path.endsWith("config.yml") || path.endsWith("config.yaml") ||
               path.endsWith("application.properties") || path.endsWith("application.yml") ||
               path.contains("readme");
    }

    /**
     * 获取模块名称
     * 
     * @param filePath 文件路径
     * @return 模块名称
     */
    private String getModuleName(String filePath) {
        String path = filePath.toLowerCase();

        if (path.contains("controller") || path.contains("api")) {
            return "API层";
        } else if (path.contains("service")) {
            return "业务逻辑层";
        } else if (path.contains("model") || path.contains("entity") || path.contains("dto")) {
            return "数据模型层";
        } else if (path.contains("util") || path.contains("helper")) {
            return "工具类";
        } else if (path.contains("config")) {
            return "配置模块";
        } else {
            return "其他模块";
        }
    }

    /**
     * 判断是否为跨模块文件
     * 
     * @param filePath 文件路径
     * @return 如果是跨模块文件返回 true，否则返回 false
     */
    private boolean isCrossModuleFile(String filePath) {
        String path = filePath.toLowerCase();

        return path.contains("controller") || path.contains("api") ||
               path.contains("service") && (path.contains("controller") || path.contains("api"));
    }

    /**
     * 构建项目骨架分析提示
     * 
     * @param skeletonChunks 骨架文件块列表
     * @return 分析提示
     */
    private String buildSkeletonPrompt(List<FileChunk> skeletonChunks) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请基于以下信息理解项目的整体定位和技术栈：\n\n");

        // 添加文件内容
        for (FileChunk chunk : skeletonChunks) {
            prompt.append("文件: ").append(chunk.getSourceFile().getPath()).append("\n");
            prompt.append("```").append("\n");
            prompt.append(chunk.getContent()).append("\n");
            prompt.append("```").append("\n\n");
        }

        prompt.append("请输出：\n");
        prompt.append("- 项目的核心功能（用1-2句话概括）\n");
        prompt.append("- 使用的主技术栈（语言、框架、数据库等）\n");
        prompt.append("- 从入口文件看，项目的启动流程是怎样的？\n");

        return prompt.toString();
    }

    /**
     * 构建模块分析提示
     * 
     * @param moduleName 模块名称
     * @param moduleChunks 模块文件块列表
     * @return 分析提示
     */
    private String buildModulePrompt(String moduleName, List<FileChunk> moduleChunks) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("基于之前对项目的理解，现在分析").append(moduleName).append("模块：\n\n");

        // 添加文件内容
        for (FileChunk chunk : moduleChunks) {
            prompt.append("文件: ").append(chunk.getSourceFile().getPath()).append("\n");
            prompt.append("```").append("\n");
            prompt.append(chunk.getContent()).append("\n");
            prompt.append("```").append("\n\n");
        }

        prompt.append("请输出：\n");
        prompt.append("- 每个类的核心职责\n");
        prompt.append("- 类之间的调用关系\n");
        prompt.append("- 代码中使用的设计模式或核心逻辑\n");

        return prompt.toString();
    }

    /**
     * 构建跨模块逻辑分析提示
     * 
     * @param crossModuleChunks 跨模块文件块列表
     * @param detailReports 详细报告列表
     * @return 分析提示
     */
    private String buildCrossModulePrompt(List<FileChunk> crossModuleChunks, List<DetailReport> detailReports) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("结合之前的模块分析，现在理解项目的整体业务流程：\n\n");

        // 添加模块分析摘要
        for (DetailReport report : detailReports) {
            prompt.append("模块: ").append(report.getModuleName()).append("\n");
            prompt.append("职责: ").append(report.getResponsibilities()).append("\n\n");
        }

        // 添加接口定义和流程代码
        for (FileChunk chunk : crossModuleChunks) {
            prompt.append("文件: ").append(chunk.getSourceFile().getPath()).append("\n");
            prompt.append("```").append("\n");
            prompt.append(chunk.getContent()).append("\n");
            prompt.append("```").append("\n\n");
        }

        prompt.append("请输出：\n");
        prompt.append("- 用流程图文字描述核心业务流程\n");
        prompt.append("- 流程中涉及的技术组件\n");
        prompt.append("- 潜在的性能瓶颈点\n");

        return prompt.toString();
    }

    /**
     * 带重试的AI服务调用
     * 
     * @param prompt 提示
     * @return AI响应
     */
    private String callAIServiceWithRetry(String prompt) {
        int retryCount = 0;
        int maxRetries = config.getRetryCount();

        while (retryCount <= maxRetries) {
            try {
                return aiService.analyze(prompt, config.getMaxTokens());
            } catch (Exception e) {
                if (retryCount == maxRetries) {
                    throw new AnalysisException("调用AI服务失败，已达到最大重试次数", e);
                }

                retryCount++;
                long delay = (long) (1000 * Math.pow(2, retryCount)); // 指数退避

                try {
                    log.warn("调用AI服务失败，{}秒后重试 ({} / {})", delay / 1000, retryCount, maxRetries);
                    TimeUnit.MILLISECONDS.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new AnalysisException("重试被中断", ie);
                }
            }
        }

        throw new AnalysisException("调用AI服务失败");
    }

    /**
     * 解析概要响应
     * 
     * @param response AI响应
     * @return 概要报告
     */
    private SummaryReport parseSummaryResponse(String response) {
        // 简单实现，实际应该使用JSON解析
        // 这里应该有更复杂的解析逻辑
        return SummaryReport.builder()
                .coreFunction("提取核心功能")
                .techStack("提取技术栈")
                .startupFlow("提取启动流程")
                .rawResponse(response)
                .build();
    }

    /**
     * 解析详细响应
     * 
     * @param moduleName 模块名称
     * @param response AI响应
     * @return 详细报告
     */
    private DetailReport parseDetailResponse(String moduleName, String response) {
        // 简单实现，实际应该使用JSON解析
        return DetailReport.builder()
                .moduleName(moduleName)
                .responsibilities("提取职责")
                .designPatterns("提取设计模式")
                .rawResponse(response)
                .build();
    }

    /**
     * 使用跨模块分析更新报告
     * 
     * @param response AI响应
     * @param detailReports 详细报告列表
     */
    private void updateReportsWithCrossModuleAnalysis(String response, List<DetailReport> detailReports) {
        // 简单实现，实际应该解析响应并更新报告
        for (DetailReport report : detailReports) {
            report.setCrossModuleAnalysis("提取跨模块分析");
        }
    }
}
