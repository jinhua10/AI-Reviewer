package top.yumbo.ai.reviewer.report;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.yumbo.ai.reviewer.config.Config;
import top.yumbo.ai.reviewer.entity.AnalysisResult;
import top.yumbo.ai.reviewer.entity.DetailReport;
import top.yumbo.ai.reviewer.entity.SummaryReport;
import top.yumbo.ai.reviewer.exception.AnalysisException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 报告生成器 v2.1 - 支持配置文件定义的报告格式和路径
 *
 * 存储结构：
 * - {projectPath}-AI/cache/         # 缓存文件
 * - {projectPath}-AI/details/       # 单个文件的详细报告
 *   ├── json/                        # JSON 格式详细报告
 *   ├── markdown/                    # Markdown 格式详细报告
 *   └── pdf/                         # PDF 格式详细报告
 * - {projectPath}/                  # 项目根目录
 *   ├── review-report.json          # 总报告（根据配置生成）
 *   ├── review-report.md            # 总报告（根据配置生成）
 *   └── review-report.pdf           # 总报告（根据配置生成）
 */
public class ReportBuilder {

    private static final Logger log = LoggerFactory.getLogger(ReportBuilder.class);

    private final Config config;
    private final Path projectAIDir;
    private final Path detailsDir;

    public ReportBuilder(Config config) {
        this.config = config;
        // {projectPath}-AI 目录
        this.projectAIDir = config.getProjectPath().getParent()
                .resolve(config.getProjectPath().getFileName() + "-AI");
        // details 子目录
        this.detailsDir = projectAIDir.resolve("details");
    }

    /**
     * 生成并保存所有格式的报告
     */
    public void buildReports(AnalysisResult result) throws AnalysisException {
        log.info("开始生成报告，格式: {}", config.getReportFormats());

        try {
            // 1. 创建必要的目录结构
            createDirectories();

            // 2. 生成文件级详细报告（如果启用）
            if (config.isGenerateFileDetails()) {
                generateDetailReports(result.getDetailReports());
            }

            // 3. 生成总报告（存储在项目根目录）
            generateSummaryReports(result);

            log.info("报告生成完成");
            log.info("  - 详细报告: {}", detailsDir);
            log.info("  - 总报告: {}", config.getProjectPath());

        } catch (IOException e) {
            throw AnalysisException.fileError("Failed to generate reports", e);
        }
    }

    /**
     * 创建目录结构
     */
    private void createDirectories() throws IOException {
        // 创建 {projectPath}-AI/details/{format} 目录
        for (String format : config.getReportFormats()) {
            Path formatDir = detailsDir.resolve(format);
            Files.createDirectories(formatDir);
            log.debug("创建目录: {}", formatDir);
        }
    }

    /**
     * 生成文件级详细报告
     * 存储路径: {projectPath}-AI/details/{format}/
     */
    private void generateDetailReports(List<DetailReport> detailReports) throws IOException {
        log.info("生成 {} 个文件的详细报告", detailReports.size());

        for (DetailReport detail : detailReports) {
            String safeFileName = sanitizeFileName(detail.getFileName());

            for (String format : config.getReportFormats()) {
                Path formatDir = detailsDir.resolve(format);

                switch (format.toLowerCase()) {
                    case "json" -> generateDetailJson(detail, formatDir, safeFileName);
                    case "markdown", "md" -> generateDetailMarkdown(detail, formatDir, safeFileName);
                    case "pdf" -> generateDetailPdf(detail, formatDir, safeFileName);
                    default -> log.warn("不支持的详细报告格式: {}", format);
                }
            }
        }
    }

    /**
     * 生成总报告
     * 存储路径: {projectPath}/review-report.{format}
     */
    private void generateSummaryReports(AnalysisResult result) throws IOException {
        log.info("生成总报告");

        Path projectRoot = config.getProjectPath();

        for (String format : config.getReportFormats()) {
            switch (format.toLowerCase()) {
                case "json" -> generateSummaryJson(result, projectRoot);
                case "markdown", "md" -> generateSummaryMarkdown(result, projectRoot);
                case "pdf" -> generateSummaryPdf(result, projectRoot);
                default -> log.warn("不支持的总报告格式: {}", format);
            }
        }
    }

    // ==================== 详细报告生成 ====================

    /**
     * 生成 JSON 格式的详细报告
     */
    private void generateDetailJson(DetailReport detail, Path outputDir, String fileName) throws IOException {
        Path reportFile = outputDir.resolve(fileName + ".json");

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"fileId\": \"").append(escapeJson(detail.getFileId())).append("\",\n");
        json.append("  \"fileName\": \"").append(escapeJson(detail.getFileName())).append("\",\n");
        json.append("  \"category\": \"").append(escapeJson(detail.getCategory())).append("\",\n");
        json.append("  \"fileSize\": ").append(detail.getFileSize()).append(",\n");
        json.append("  \"analysis\": \"").append(escapeJson(detail.getAnalysis())).append("\",\n");
        json.append("  \"issueCount\": ").append(detail.getIssues().size()).append(",\n");
        json.append("  \"issues\": [\n");

        for (int i = 0; i < detail.getIssues().size(); i++) {
            DetailReport.Issue issue = detail.getIssues().get(i);
            json.append("    {\n");
            json.append("      \"severity\": \"").append(issue.getSeverity()).append("\",\n");
            json.append("      \"type\": \"").append(escapeJson(issue.getType())).append("\",\n");
            json.append("      \"description\": \"").append(escapeJson(issue.getDescription())).append("\",\n");
            json.append("      \"line\": ").append(issue.getLine()).append("\n");
            json.append("    }").append(i < detail.getIssues().size() - 1 ? "," : "").append("\n");
        }

        json.append("  ]\n");
        json.append("}\n");

        Files.writeString(reportFile, json.toString());
        log.debug("生成详细报告 (JSON): {}", reportFile);
    }

    /**
     * 生成 Markdown 格式的详细报告
     */
    private void generateDetailMarkdown(DetailReport detail, Path outputDir, String fileName) throws IOException {
        Path reportFile = outputDir.resolve(fileName + ".md");

        StringBuilder md = new StringBuilder();
        md.append("# 文件分析报告\n\n");
        md.append("**文件**: `").append(detail.getFileName()).append("`\n\n");
        md.append("**类型**: ").append(detail.getCategory()).append("\n\n");
        md.append("**大小**: ").append(detail.getFileSize() / 1024).append(" KB\n\n");
        md.append("---\n\n");

        md.append("## 分析结果\n\n");
        md.append(detail.getAnalysis()).append("\n\n");

        if (!detail.getIssues().isEmpty()) {
            md.append("## 发现的问题\n\n");
            md.append("| 严重程度 | 类型 | 描述 | 行号 |\n");
            md.append("|---------|------|------|------|\n");

            for (DetailReport.Issue issue : detail.getIssues()) {
                md.append("| ").append(issue.getSeverity());
                md.append(" | ").append(issue.getType());
                md.append(" | ").append(issue.getDescription());
                md.append(" | ").append(issue.getLine() != null ? issue.getLine() : "-");
                md.append(" |\n");
            }
            md.append("\n");
        }

        md.append("---\n\n");
        md.append("*生成时间: ").append(getCurrentTime()).append("*\n");

        Files.writeString(reportFile, md.toString());
        log.debug("生成详细报告 (Markdown): {}", reportFile);
    }

    /**
     * 生成 PDF 格式的详细报告
     */
    private void generateDetailPdf(DetailReport detail, Path outputDir, String fileName) throws IOException {
        Path reportFile = outputDir.resolve(fileName + ".pdf");

        List<PdfGenerator.PdfSection> sections = new ArrayList<>();

        // 文件信息
        sections.add(new PdfGenerator.PdfSection("File Information",
            "File: " + detail.getFileName(),
            "Type: " + detail.getCategory(),
            "Size: " + (detail.getFileSize() / 1024) + " KB"
        ));

        // 分析结果
        List<String> analysisLines = new ArrayList<>();
        String analysis = detail.getAnalysis();
        if (analysis != null && !analysis.isEmpty()) {
            // 将分析结果按行分割
            String[] lines = analysis.split("\\n");
            for (String line : lines) {
                if (line.trim().length() > 0) {
                    analysisLines.add(line.trim());
                }
            }
        }
        sections.add(new PdfGenerator.PdfSection("Analysis Result", analysisLines));

        // 问题列表
        if (!detail.getIssues().isEmpty()) {
            List<String> issueLines = new ArrayList<>();
            issueLines.add("Total Issues: " + detail.getIssues().size());
            issueLines.add("");

            for (int i = 0; i < detail.getIssues().size(); i++) {
                DetailReport.Issue issue = detail.getIssues().get(i);
                issueLines.add(String.format("%d. [%s] %s",
                    i + 1, issue.getSeverity(), issue.getType()));
                issueLines.add("   " + issue.getDescription());
                if (issue.getLine() != null) {
                    issueLines.add("   Line: " + issue.getLine());
                }
                issueLines.add("");
            }

            sections.add(new PdfGenerator.PdfSection("Issues Found", issueLines));
        }

        // 生成 PDF
        PdfGenerator.generatePdf(reportFile, "File Analysis Report: " + detail.getFileName(), sections);
        log.debug("生成详细报告 (PDF): {}", reportFile);
    }

    // ==================== 总报告生成 ====================

    /**
     * 生成 JSON 格式的总报告
     */
    private void generateSummaryJson(AnalysisResult result, Path projectRoot) throws IOException {
        Path reportFile = projectRoot.resolve("review-report.json");

        SummaryReport summary = result.getSummaryReport();

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"projectName\": \"").append(escapeJson(summary.getProjectName())).append("\",\n");
        json.append("  \"timestamp\": \"").append(getCurrentTime()).append("\",\n");
        json.append("  \"projectPath\": \"").append(escapeJson(result.getProjectPath())).append("\",\n");
        json.append("  \"totalFiles\": ").append(result.getTotalFiles()).append(",\n");
        json.append("  \"analyzedFiles\": ").append(result.getAnalyzedFiles()).append(",\n");
        json.append("  \"skippedFiles\": ").append(result.getSkippedFiles()).append(",\n");
        json.append("  \"duration\": ").append(result.getDuration()).append(",\n");
        json.append("  \"qualityScore\": ").append(summary.getQualityScore()).append(",\n");
        json.append("  \"overallAssessment\": \"").append(escapeJson(summary.getOverallAssessment())).append("\",\n");
        json.append("  \"issueCounts\": {\n");

        var issueCounts = summary.getIssueCounts();
        int i = 0;
        for (var entry : issueCounts.entrySet()) {
            json.append("    \"").append(entry.getKey()).append("\": ").append(entry.getValue());
            json.append(++i < issueCounts.size() ? ",\n" : "\n");
        }

        json.append("  }\n");
        json.append("}\n");

        Files.writeString(reportFile, json.toString());
        log.info("生成总报告 (JSON): {}", reportFile);
    }

    /**
     * 生成 Markdown 格式的总报告
     */
    private void generateSummaryMarkdown(AnalysisResult result, Path projectRoot) throws IOException {
        Path reportFile = projectRoot.resolve("review-report.md");

        SummaryReport summary = result.getSummaryReport();

        StringBuilder md = new StringBuilder();
        md.append("# AI 代码审查报告\n\n");
        md.append("**项目**: ").append(summary.getProjectName()).append("\n\n");
        md.append("**生成时间**: ").append(getCurrentTime()).append("\n\n");
        md.append("---\n\n");

        // 概览
        md.append("## 📊 概览\n\n");
        md.append("| 指标 | 数值 |\n");
        md.append("|------|------|\n");
        md.append("| 项目路径 | `").append(result.getProjectPath()).append("` |\n");
        md.append("| 总文件数 | ").append(result.getTotalFiles()).append(" |\n");
        md.append("| 分析文件数 | ").append(result.getAnalyzedFiles()).append(" |\n");
        md.append("| 跳过文件数 | ").append(result.getSkippedFiles()).append(" |\n");
        md.append("| 分析耗时 | ").append(result.getDuration()).append(" ms |\n");
        md.append("| 质量评分 | **").append(String.format("%.1f", summary.getQualityScore())).append(" / 100** |\n\n");

        // 总体评估
        md.append("## 🎯 总体评估\n\n");
        md.append(summary.getOverallAssessment()).append("\n\n");

        // 问题统计
        if (!summary.getIssueCounts().isEmpty()) {
            md.append("## ⚠️ 问题统计\n\n");
            md.append("| 严重程度 | 数量 |\n");
            md.append("|---------|------|\n");
            summary.getIssueCounts().forEach((severity, count) -> {
                md.append("| **").append(severity).append("** | ").append(count).append(" |\n");
            });
            md.append("\n");
        }

        // 详细报告链接
        if (config.isGenerateFileDetails()) {
            md.append("## 📝 详细报告\n\n");
            md.append("单个文件的详细分析报告已生成在以下目录：\n\n");
            md.append("- JSON 格式: `").append(projectAIDir.getFileName()).append("/details/json/`\n");
            md.append("- Markdown 格式: `").append(projectAIDir.getFileName()).append("/details/markdown/`\n");
            if (config.getReportFormats().contains("pdf")) {
                md.append("- PDF 格式: `").append(projectAIDir.getFileName()).append("/details/pdf/`\n");
            }
            md.append("\n");
        }

        md.append("---\n\n");
        md.append("*由 AI Reviewer v2.1 生成*\n");

        Files.writeString(reportFile, md.toString());
        log.info("生成总报告 (Markdown): {}", reportFile);
    }

    /**
     * 生成 PDF 格式的总报告
     */
    private void generateSummaryPdf(AnalysisResult result, Path projectRoot) throws IOException {
        Path reportFile = projectRoot.resolve("review-report.pdf");

        SummaryReport summary = result.getSummaryReport();
        List<PdfGenerator.PdfSection> sections = new ArrayList<>();

        // 项目信息
        sections.add(new PdfGenerator.PdfSection("Project Information",
            "Project: " + summary.getProjectName(),
            "Generated: " + getCurrentTime(),
            "Path: " + result.getProjectPath()
        ));

        // 统计概览
        List<String> statsLines = new ArrayList<>();
        statsLines.add("Total Files: " + result.getTotalFiles());
        statsLines.add("Analyzed Files: " + result.getAnalyzedFiles());
        statsLines.add("Skipped Files: " + result.getSkippedFiles());
        statsLines.add("Duration: " + result.getDuration() + " ms");
        statsLines.add("Quality Score: " + String.format("%.1f / 100", summary.getQualityScore()));
        sections.add(new PdfGenerator.PdfSection("Statistics Overview", statsLines));

        // 总体评估
        List<String> assessmentLines = new ArrayList<>();
        String assessment = summary.getOverallAssessment();
        if (assessment != null && !assessment.isEmpty()) {
            String[] lines = assessment.split("\\n");
            for (String line : lines) {
                if (line.trim().length() > 0) {
                    assessmentLines.add(line.trim());
                }
            }
        }
        sections.add(new PdfGenerator.PdfSection("Overall Assessment", assessmentLines));

        // 问题统计
        if (!summary.getIssueCounts().isEmpty()) {
            List<String> issueLines = new ArrayList<>();
            summary.getIssueCounts().forEach((severity, count) -> {
                issueLines.add(severity + ": " + count);
            });
            sections.add(new PdfGenerator.PdfSection("Issue Statistics", issueLines));
        }

        // 详细报告位置
        if (config.isGenerateFileDetails()) {
            sections.add(new PdfGenerator.PdfSection("Detailed Reports",
                "Detailed analysis reports for individual files are available at:",
                "  - JSON format: " + projectAIDir.getFileName() + "/details/json/",
                "  - Markdown format: " + projectAIDir.getFileName() + "/details/markdown/",
                "  - PDF format: " + projectAIDir.getFileName() + "/details/pdf/"
            ));
        }

        // 生成 PDF
        PdfGenerator.generatePdf(reportFile, "AI Code Review Report", sections);
        log.info("生成总报告 (PDF): {}", reportFile);
    }

    // ==================== 工具方法 ====================

    /**
     * 安全化文件名
     */
    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * 转义 JSON 字符串
     */
    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 获取当前时间
     */
    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}

