package top.yumbo.ai.reviewer.report;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.reviewer.entity.AnalysisResult;
import top.yumbo.ai.reviewer.entity.DetailReport;
import top.yumbo.ai.reviewer.entity.SummaryReport;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 报告构建器 - 生成各种格式的分析报告
 */
@Slf4j
public class ReportBuilder {

    /**
     * 生成Markdown格式的报告
     */
    public String generateMarkdownReport(AnalysisResult result) {
        StringBuilder md = new StringBuilder();

        // 标题
        md.append("# 项目分析报告\n\n");
        md.append("**项目名称:** ").append(result.getProjectName()).append("\n");
        md.append("**分析时间:** ").append(formatTimestamp(result.getAnalysisTimestamp())).append("\n\n");

        // 总体评分
        md.append("## 总体评分\n\n");
        md.append("**综合评分: ").append(result.getOverallScore()).append("/100**\n\n");

        // 各维度评分
        md.append("### 评分详情\n\n");
        md.append("| 维度 | 评分 | 权重 |\n");
        md.append("|------|------|------|\n");
        md.append("| 架构设计 | ").append(result.getArchitectureScore()).append("/100 | 20% |\n");
        md.append("| 代码质量 | ").append(result.getCodeQualityScore()).append("/100 | 20% |\n");
        md.append("| 技术债务 | ").append(result.getTechnicalDebtScore()).append("/100 | 15% |\n");
        md.append("| 功能完整性 | ").append(result.getFunctionalityScore()).append("/100 | 20% |\n");
        md.append("| 商业价值 | ").append(result.getBusinessValueScore()).append("/100 | 15% |\n");
        md.append("| 测试覆盖率 | ").append(result.getTestCoverageScore()).append("/100 | 10% |\n");
        md.append("\n");

        // 摘要报告
        if (result.getSummaryReport() != null) {
            md.append("## 执行摘要\n\n");
            md.append(result.getSummaryReport().getContent()).append("\n\n");

            if (result.getSummaryReport().getKeyFindings() != null) {
                md.append("### 关键发现\n\n");
                for (String finding : result.getSummaryReport().getKeyFindings()) {
                    md.append("- ").append(finding).append("\n");
                }
                md.append("\n");
            }

            if (result.getSummaryReport().getRecommendations() != null) {
                md.append("### 建议改进\n\n");
                for (String recommendation : result.getSummaryReport().getRecommendations()) {
                    md.append("- ").append(recommendation).append("\n");
                }
                md.append("\n");
            }
        }

        // 详细报告
        if (result.getDetailReport() != null) {
            appendDetailReport(md, result.getDetailReport());
        }

        return md.toString();
    }

    /**
     * 生成HTML格式的报告
     */
    public String generateHtmlReport(AnalysisResult result) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"zh-CN\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>项目分析报告</title>\n");
        html.append("    <style>\n");
        html.append(getCssStyles());
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        // 标题
        html.append("    <div class=\"header\">\n");
        html.append("        <h1>项目分析报告</h1>\n");
        html.append("        <div class=\"project-info\">\n");
        html.append("            <p><strong>项目名称:</strong> ").append(result.getProjectName()).append("</p>\n");
        html.append("            <p><strong>分析时间:</strong> ").append(formatTimestamp(result.getAnalysisTimestamp())).append("</p>\n");
        html.append("        </div>\n");
        html.append("    </div>\n");

        // 总体评分
        html.append("    <div class=\"score-section\">\n");
        html.append("        <h2>总体评分</h2>\n");
        html.append("        <div class=\"overall-score\">").append(result.getOverallScore()).append("<span>/100</span></div>\n");

        html.append("        <div class=\"score-grid\">\n");
        html.append("            <div class=\"score-item\">\n");
        html.append("                <div class=\"score-label\">架构设计</div>\n");
        html.append("                <div class=\"score-value\">").append(result.getArchitectureScore()).append("</div>\n");
        html.append("            </div>\n");
        html.append("            <div class=\"score-item\">\n");
        html.append("                <div class=\"score-label\">代码质量</div>\n");
        html.append("                <div class=\"score-value\">").append(result.getCodeQualityScore()).append("</div>\n");
        html.append("            </div>\n");
        html.append("            <div class=\"score-item\">\n");
        html.append("                <div class=\"score-label\">技术债务</div>\n");
        html.append("                <div class=\"score-value\">").append(result.getTechnicalDebtScore()).append("</div>\n");
        html.append("            </div>\n");
        html.append("            <div class=\"score-item\">\n");
        html.append("                <div class=\"score-label\">功能完整性</div>\n");
        html.append("                <div class=\"score-value\">").append(result.getFunctionalityScore()).append("</div>\n");
        html.append("            </div>\n");
        html.append("            <div class=\"score-item\">\n");
        html.append("                <div class=\"score-label\">商业价值</div>\n");
        html.append("                <div class=\"score-value\">").append(result.getBusinessValueScore()).append("</div>\n");
        html.append("            </div>\n");
        html.append("            <div class=\"score-item\">\n");
        html.append("                <div class=\"score-label\">测试覆盖率</div>\n");
        html.append("                <div class=\"score-value\">").append(result.getTestCoverageScore()).append("</div>\n");
        html.append("            </div>\n");
        html.append("        </div>\n");
        html.append("    </div>\n");

        // 摘要报告
        if (result.getSummaryReport() != null) {
            html.append("    <div class=\"section\">\n");
            html.append("        <h2>执行摘要</h2>\n");
            html.append("        <div class=\"content\">").append(formatToHtml(result.getSummaryReport().getContent())).append("</div>\n");

            if (result.getSummaryReport().getKeyFindings() != null && !result.getSummaryReport().getKeyFindings().isEmpty()) {
                html.append("        <h3>关键发现</h3>\n");
                html.append("        <ul>\n");
                for (String finding : result.getSummaryReport().getKeyFindings()) {
                    html.append("            <li>").append(finding).append("</li>\n");
                }
                html.append("        </ul>\n");
            }

            if (result.getSummaryReport().getRecommendations() != null && !result.getSummaryReport().getRecommendations().isEmpty()) {
                html.append("        <h3>建议改进</h3>\n");
                html.append("        <ul>\n");
                for (String recommendation : result.getSummaryReport().getRecommendations()) {
                    html.append("            <li>").append(recommendation).append("</li>\n");
                }
                html.append("        </ul>\n");
            }
            html.append("    </div>\n");
        }

        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }

    /**
     * 保存报告到文件
     */
    public void saveReport(AnalysisResult result, String filePath, String format) throws IOException {
        String content;
        String extension;

        switch (format.toLowerCase()) {
            case "markdown":
            case "md":
                content = generateMarkdownReport(result);
                extension = ".md";
                break;
            case "html":
                content = generateHtmlReport(result);
                extension = ".html";
                break;
            default:
                content = generateMarkdownReport(result);
                extension = ".md";
        }

        Path path = Paths.get(filePath);
        if (!filePath.endsWith(extension)) {
            path = Paths.get(filePath + extension);
        }

        java.nio.file.Files.writeString(path, content);
        log.info("报告已保存到: {}", path);
    }

    /**
     * 追加详细报告内容
     */
    private void appendDetailReport(StringBuilder md, DetailReport detailReport) {
        if (detailReport.getArchitectureAnalysis() != null) {
            md.append("## 架构分析\n\n");
            appendArchitectureAnalysis(md, detailReport.getArchitectureAnalysis());
        }

        if (detailReport.getCodeQualityAnalysis() != null) {
            md.append("## 代码质量分析\n\n");
            appendCodeQualityAnalysis(md, detailReport.getCodeQualityAnalysis());
        }

        if (detailReport.getTechnicalDebtAnalysis() != null) {
            md.append("## 技术债务分析\n\n");
            appendTechnicalDebtAnalysis(md, detailReport.getTechnicalDebtAnalysis());
        }

        if (detailReport.getFunctionalityAnalysis() != null) {
            md.append("## 功能分析\n\n");
            appendFunctionalityAnalysis(md, detailReport.getFunctionalityAnalysis());
        }

        if (detailReport.getBusinessValueAnalysis() != null) {
            md.append("## 商业价值分析\n\n");
            appendBusinessValueAnalysis(md, detailReport.getBusinessValueAnalysis());
        }

        if (detailReport.getTestCoverageAnalysis() != null) {
            md.append("## 测试覆盖率分析\n\n");
            appendTestCoverageAnalysis(md, detailReport.getTestCoverageAnalysis());
        }
    }

    private void appendArchitectureAnalysis(StringBuilder md, DetailReport.ArchitectureAnalysis analysis) {
        md.append("### 概述\n\n");
        md.append(analysis.getOverview()).append("\n\n");

        if (analysis.getStrengths() != null && !analysis.getStrengths().isEmpty()) {
            md.append("### 优势\n\n");
            for (String strength : analysis.getStrengths()) {
                md.append("- ").append(strength).append("\n");
            }
            md.append("\n");
        }

        if (analysis.getWeaknesses() != null && !analysis.getWeaknesses().isEmpty()) {
            md.append("### 不足\n\n");
            for (String weakness : analysis.getWeaknesses()) {
                md.append("- ").append(weakness).append("\n");
            }
            md.append("\n");
        }

        if (analysis.getRecommendations() != null && !analysis.getRecommendations().isEmpty()) {
            md.append("### 改进建议\n\n");
            for (String recommendation : analysis.getRecommendations()) {
                md.append("- ").append(recommendation).append("\n");
            }
            md.append("\n");
        }
    }

    private void appendCodeQualityAnalysis(StringBuilder md, DetailReport.CodeQualityAnalysis analysis) {
        md.append("### 概述\n\n");
        md.append(analysis.getOverview()).append("\n\n");

        if (analysis.getIssues() != null && !analysis.getIssues().isEmpty()) {
            md.append("### 发现的问题\n\n");
            for (String issue : analysis.getIssues()) {
                md.append("- ").append(issue).append("\n");
            }
            md.append("\n");
        }

        if (analysis.getBestPractices() != null && !analysis.getBestPractices().isEmpty()) {
            md.append("### 最佳实践\n\n");
            for (String practice : analysis.getBestPractices()) {
                md.append("- ").append(practice).append("\n");
            }
            md.append("\n");
        }
    }

    private void appendTechnicalDebtAnalysis(StringBuilder md, DetailReport.TechnicalDebtAnalysis analysis) {
        md.append("### 概述\n\n");
        md.append(analysis.getOverview()).append("\n\n");

        if (analysis.getDebts() != null && !analysis.getDebts().isEmpty()) {
            md.append("### 技术债务\n\n");
            for (String debt : analysis.getDebts()) {
                md.append("- ").append(debt).append("\n");
            }
            md.append("\n");
        }

        if (analysis.getRefactoringSuggestions() != null && !analysis.getRefactoringSuggestions().isEmpty()) {
            md.append("### 重构建议\n\n");
            for (String suggestion : analysis.getRefactoringSuggestions()) {
                md.append("- ").append(suggestion).append("\n");
            }
            md.append("\n");
        }
    }

    private void appendFunctionalityAnalysis(StringBuilder md, DetailReport.FunctionalityAnalysis analysis) {
        md.append("### 概述\n\n");
        md.append(analysis.getOverview()).append("\n\n");

        if (analysis.getMissingFeatures() != null && !analysis.getMissingFeatures().isEmpty()) {
            md.append("### 缺失功能\n\n");
            for (String feature : analysis.getMissingFeatures()) {
                md.append("- ").append(feature).append("\n");
            }
            md.append("\n");
        }

        if (analysis.getImprovementSuggestions() != null && !analysis.getImprovementSuggestions().isEmpty()) {
            md.append("### 改进建议\n\n");
            for (String suggestion : analysis.getImprovementSuggestions()) {
                md.append("- ").append(suggestion).append("\n");
            }
            md.append("\n");
        }
    }

    private void appendBusinessValueAnalysis(StringBuilder md, DetailReport.BusinessValueAnalysis analysis) {
        md.append("### 概述\n\n");
        md.append(analysis.getOverview()).append("\n\n");

        if (analysis.getValueDrivers() != null && !analysis.getValueDrivers().isEmpty()) {
            md.append("### 价值驱动因素\n\n");
            for (String driver : analysis.getValueDrivers()) {
                md.append("- ").append(driver).append("\n");
            }
            md.append("\n");
        }

        if (analysis.getRisks() != null && !analysis.getRisks().isEmpty()) {
            md.append("### 风险\n\n");
            for (String risk : analysis.getRisks()) {
                md.append("- ").append(risk).append("\n");
            }
            md.append("\n");
        }

        if (analysis.getRecommendations() != null && !analysis.getRecommendations().isEmpty()) {
            md.append("### 改进建议\n\n");
            for (String recommendation : analysis.getRecommendations()) {
                md.append("- ").append(recommendation).append("\n");
            }
            md.append("\n");
        }
    }

    private void appendTestCoverageAnalysis(StringBuilder md, DetailReport.TestCoverageAnalysis analysis) {
        md.append("### 概述\n\n");
        md.append(analysis.getOverview()).append("\n\n");

        if (analysis.getCoveredFeatures() != null && !analysis.getCoveredFeatures().isEmpty()) {
            md.append("### 覆盖的代码元素\n\n");
            for (String element : analysis.getCoveredFeatures()) {
                md.append("- ").append(element).append("\n");
            }
            md.append("\n");
        }

        if (analysis.getCoveragePercentage() != null) {
            md.append("### 覆盖率\n\n");
            md.append(analysis.getCoveragePercentage()).append("\n\n");
        }

        if (analysis.getTestQualityIssues() != null && !analysis.getTestQualityIssues().isEmpty()) {
            md.append("### 测试质量问题\n\n");
            for (String issue : analysis.getTestQualityIssues()) {
                md.append("- ").append(issue).append("\n");
            }
            md.append("\n");
        }
    }

    /**
     * 获取CSS样式
     */
    private String getCssStyles() {
        return """
            body {
                font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                line-height: 1.6;
                color: #333;
                max-width: 1200px;
                margin: 0 auto;
                padding: 20px;
                background-color: #f5f5f5;
            }
            .header {
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                color: white;
                padding: 30px;
                border-radius: 10px;
                margin-bottom: 30px;
                text-align: center;
            }
            .header h1 {
                margin: 0 0 10px 0;
                font-size: 2.5em;
            }
            .project-info {
                font-size: 1.1em;
            }
            .score-section {
                background: white;
                padding: 30px;
                border-radius: 10px;
                margin-bottom: 30px;
                box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            }
            .overall-score {
                font-size: 4em;
                font-weight: bold;
                color: #4CAF50;
                text-align: center;
                margin: 20px 0;
            }
            .overall-score span {
                font-size: 0.5em;
                color: #666;
            }
            .score-grid {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                gap: 20px;
                margin-top: 30px;
            }
            .score-item {
                background: #f8f9fa;
                padding: 20px;
                border-radius: 8px;
                text-align: center;
            }
            .score-label {
                font-size: 1.1em;
                margin-bottom: 10px;
                color: #666;
            }
            .score-value {
                font-size: 2em;
                font-weight: bold;
                color: #2196F3;
            }
            .section {
                background: white;
                padding: 30px;
                border-radius: 10px;
                margin-bottom: 30px;
                box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            }
            .section h2 {
                color: #333;
                border-bottom: 2px solid #667eea;
                padding-bottom: 10px;
                margin-bottom: 20px;
            }
            .section h3 {
                color: #555;
                margin-top: 25px;
                margin-bottom: 15px;
            }
            .content {
                margin-bottom: 20px;
            }
            ul {
                padding-left: 20px;
            }
            li {
                margin-bottom: 8px;
            }
            """;
    }

    /**
     * 格式化时间戳
     */
    private String formatTimestamp(long timestamp) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new java.util.Date(timestamp));
    }

    /**
     * 转换为HTML格式
     */
    private String formatToHtml(String text) {
        if (text == null) return "";
        return text.replace("\n", "<br>");
    }
}
