package top.yumbo.ai.reviewer.scoring;

import top.yumbo.ai.reviewer.exception.AnalysisException;

/**
 * 评分规则接口
 */
public interface ScoringRule {

    /**
     * 获取规则名称
     */
    String getName();

    /**
     * 获取规则描述
     */
    String getDescription();

    /**
     * 计算评分
     * @param analysisResult AI分析结果
     * @param context 评分上下文
     * @return 评分 (0-100)
     */
    int calculateScore(String analysisResult, ScoringContext context) throws AnalysisException;

    /**
     * 获取权重
     */
    double getWeight();

    /**
     * 设置权重
     */
    void setWeight(double weight);

    /**
     * 验证规则配置
     */
    boolean validate();

    /**
     * 获取规则类型
     */
    RuleType getType();

    /**
     * 规则类型枚举
     */
    enum RuleType {
        ARCHITECTURE,      // 架构评分
        CODE_QUALITY,      // 代码质量评分
        TECHNICAL_DEBT,    // 技术债务评分
        FUNCTIONALITY,     // 功能完整性评分
        BUSINESS_VALUE,    // 商业价值评分
        TEST_COVERAGE      // 测试覆盖率评分
    }

    /**
     * 评分上下文
     */
    class ScoringContext {
        private String dimension;
        private String projectType;
        private int fileCount;
        private int totalLines;
        private String language;

        public ScoringContext() {}

        public ScoringContext(String dimension, String projectType, int fileCount, int totalLines, String language) {
            this.dimension = dimension;
            this.projectType = projectType;
            this.fileCount = fileCount;
            this.totalLines = totalLines;
            this.language = language;
        }

        // Getters and setters
        public String getDimension() { return dimension; }
        public void setDimension(String dimension) { this.dimension = dimension; }

        public String getProjectType() { return projectType; }
        public void setProjectType(String projectType) { this.projectType = projectType; }

        public int getFileCount() { return fileCount; }
        public void setFileCount(int fileCount) { this.fileCount = fileCount; }

        public int getTotalLines() { return totalLines; }
        public void setTotalLines(int totalLines) { this.totalLines = totalLines; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
    }
}
