package top.yumbo.ai.reviewer.entity;

import java.util.List;
import java.util.Map;

/**
 * 详细报告：针对单个文件的分析结果
 * 简化版：只保留核心字段
 */
public class DetailReport {
    private final String fileId;              // 文件相对路径
    private final String fileName;            // 文件名
    private final String category;            // 文件类别
    private final long fileSize;              // 文件大小
    private final String analysis;            // AI 分析内容
    private final List<Issue> issues;         // 发现的问题
    private final Map<String, Object> metrics; // 指标数据

    public DetailReport(String fileId, String fileName, String category, long fileSize,
                       String analysis, List<Issue> issues, Map<String, Object> metrics) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.category = category;
        this.fileSize = fileSize;
        this.analysis = analysis;
        this.issues = issues;
        this.metrics = metrics;
    }

    // Getters
    public String getFileId() { return fileId; }
    public String getFileName() { return fileName; }
    public String getCategory() { return category; }
    public long getFileSize() { return fileSize; }
    public String getAnalysis() { return analysis; }
    public List<Issue> getIssues() { return issues; }
    public Map<String, Object> getMetrics() { return metrics; }

    /**
     * 问题数据模型
     */
    public static class Issue {
        private final String severity;   // 严重程度: HIGH/MEDIUM/LOW
        private final String type;       // 问题类型
        private final String description; // 问题描述
        private final Integer line;      // 行号（可选）

        public Issue(String severity, String type, String description, Integer line) {
            this.severity = severity;
            this.type = type;
            this.description = description;
            this.line = line;
        }

        public String getSeverity() { return severity; }
        public String getType() { return type; }
        public String getDescription() { return description; }
        public Integer getLine() { return line; }
    }

    @Override
    public String toString() {
        return String.format("DetailReport{file=%s, issues=%d}", fileName, issues.size());
    }
}

