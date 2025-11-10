package top.yumbo.ai.reviewer.entity;

import java.util.List;

/**
 * 分析结果数据模型
 * 包含详细报告和摘要报告
 */
public class AnalysisResult {
    private final String projectPath;
    private final int totalFiles;
    private final int analyzedFiles;
    private final int skippedFiles;
    private final int totalChunks;
    private final int successfulChunks;
    private final int failedChunks;
    private final long duration;
    private final List<DetailReport> detailReports;
    private final SummaryReport summaryReport;

    private AnalysisResult(Builder builder) {
        this.projectPath = builder.projectPath;
        this.totalFiles = builder.totalFiles;
        this.analyzedFiles = builder.analyzedFiles;
        this.skippedFiles = builder.skippedFiles;
        this.totalChunks = builder.totalChunks;
        this.successfulChunks = builder.successfulChunks;
        this.failedChunks = builder.failedChunks;
        this.duration = builder.duration;
        this.detailReports = builder.detailReports;
        this.summaryReport = builder.summaryReport;
    }

    // Getters
    public String getProjectPath() { return projectPath; }
    public int getTotalFiles() { return totalFiles; }
    public int getAnalyzedFiles() { return analyzedFiles; }
    public int getSkippedFiles() { return skippedFiles; }
    public int getTotalChunks() { return totalChunks; }
    public int getSuccessfulChunks() { return successfulChunks; }
    public int getFailedChunks() { return failedChunks; }
    public long getDuration() { return duration; }
    public List<DetailReport> getDetailReports() { return detailReports; }
    public SummaryReport getSummaryReport() { return summaryReport; }

    public String getSummary() {
        return String.format(
            "分析完成: 项目=%s, 总文件=%d, 分析=%d, 跳过=%d, 块=%d, 成功=%d, 失败=%d, 耗时=%dms",
            projectPath, totalFiles, analyzedFiles, skippedFiles,
            totalChunks, successfulChunks, failedChunks, duration
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String projectPath;
        private int totalFiles;
        private int analyzedFiles;
        private int skippedFiles;
        private int totalChunks;
        private int successfulChunks;
        private int failedChunks;
        private long duration;
        private List<DetailReport> detailReports;
        private SummaryReport summaryReport;

        public Builder projectPath(String projectPath) { this.projectPath = projectPath; return this; }
        public Builder totalFiles(int totalFiles) { this.totalFiles = totalFiles; return this; }
        public Builder analyzedFiles(int analyzedFiles) { this.analyzedFiles = analyzedFiles; return this; }
        public Builder skippedFiles(int skippedFiles) { this.skippedFiles = skippedFiles; return this; }
        public Builder totalChunks(int totalChunks) { this.totalChunks = totalChunks; return this; }
        public Builder successfulChunks(int successfulChunks) { this.successfulChunks = successfulChunks; return this; }
        public Builder failedChunks(int failedChunks) { this.failedChunks = failedChunks; return this; }
        public Builder duration(long duration) { this.duration = duration; return this; }
        public Builder detailReports(List<DetailReport> detailReports) { this.detailReports = detailReports; return this; }
        public Builder summaryReport(SummaryReport summaryReport) { this.summaryReport = summaryReport; return this; }

        public AnalysisResult build() {
            return new AnalysisResult(this);
        }
    }

    public static AnalysisResult empty(String projectPath) {
        return builder()
            .projectPath(projectPath)
            .totalFiles(0)
            .analyzedFiles(0)
            .skippedFiles(0)
            .totalChunks(0)
            .successfulChunks(0)
            .failedChunks(0)
            .duration(0)
            .detailReports(List.of())
            .summaryReport(SummaryReport.empty())
            .build();
    }
}

