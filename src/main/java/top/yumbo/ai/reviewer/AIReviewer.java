package top.yumbo.ai.reviewer;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.reviewer.analyzer.AIAnalyzer;
import top.yumbo.ai.reviewer.cache.AnalysisCache;
import top.yumbo.ai.reviewer.config.Config;
import top.yumbo.ai.reviewer.entity.AnalysisResult;
import top.yumbo.ai.reviewer.exception.AnalysisException;
import top.yumbo.ai.reviewer.scanner.FileScanner;
import top.yumbo.ai.reviewer.scoring.ScoringEngine;
import top.yumbo.ai.reviewer.service.AsyncAIService;
import top.yumbo.ai.reviewer.util.FileUtil;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * AI Reviewer - 企业级AI驱动的多模态项目智能评审引擎
 * 主要功能：对大型项目源码进行整体分析、打分和评价
 */
@Slf4j
public class AIReviewer {

    private final Config config;
    private final FileScanner fileScanner;
    private final AIAnalyzer aiAnalyzer;

    public AIReviewer(Config config) {
        this.config = config;
        this.fileScanner = new FileScanner(config);
        this.aiAnalyzer = new AIAnalyzer(config);
    }

    /**
     * 执行项目分析
     * @param projectPath 项目根目录路径
     * @return 分析结果
     */
    public AnalysisResult analyzeProject(String projectPath) throws AnalysisException {
        log.info("开始分析项目: {}", projectPath);

        Path rootPath = Paths.get(projectPath);
        if (!FileUtil.exists(rootPath)) {
            throw new AnalysisException("项目路径不存在: " + projectPath);
        }

        try {
            // 1. 预处理：筛选核心文件
            log.info("步骤1: 预处理项目文件");
            List<Path> coreFiles = fileScanner.scanCoreFiles(rootPath);

            // 2. 生成项目结构树
            log.info("步骤2: 生成项目结构树");
            String projectStructure = fileScanner.generateProjectStructure(rootPath);

            // 3. 分批次分析
            log.info("步骤3: 分批次AI分析");
            AnalysisResult result = aiAnalyzer.analyzeProject(coreFiles, projectStructure, rootPath);

            log.info("项目分析完成");
            return result;

        } catch (Exception e) {
            log.error("项目分析失败", e);
            throw new AnalysisException("项目分析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取AI服务实例（用于演示和测试）
     */
    public AsyncAIService getAiService() {
        return aiAnalyzer.getAiService();
    }

    /**
     * 获取缓存系统实例（用于演示和测试）
     */
    public AnalysisCache getCache() {
        return aiAnalyzer.getCache();
    }

    /**
     * 获取评分引擎实例（用于演示和测试）
     */
    public ScoringEngine getScoringEngine() {
        return aiAnalyzer.getScoringEngine();
    }

    /**
     * 配置器类，用于构建AIReviewer实例
     */
    public static class Configurer {
        private Config config;

        public Configurer() {
            this.config = new Config();
        }

        public Configurer withConfig(Config config) {
            this.config = config;
            return this;
        }

        public AIReviewer build() {
            return new AIReviewer(config);
        }
    }

    /**
     * 静态方法创建配置器
     */
    public static Configurer builder() {
        return new Configurer();
    }
}
