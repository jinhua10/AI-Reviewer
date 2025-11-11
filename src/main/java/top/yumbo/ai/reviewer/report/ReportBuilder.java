package top.yumbo.ai.reviewer.report;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.reviewer.entity.AnalysisResult;
import top.yumbo.ai.reviewer.entity.DetailReport;
import top.yumbo.ai.reviewer.entity.SummaryReport;
import top.yumbo.ai.reviewer.report.template.ReportTemplate;
import top.yumbo.ai.reviewer.report.template.TemplateEngine;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 报告构建器 - 生成各种格式的分析报告
 */
@Slf4j
public class ReportBuilder {

    private final TemplateEngine templateEngine;

    public ReportBuilder() {
        this.templateEngine = new TemplateEngine();
        // 创建内置模板
        templateEngine.createBuiltInTemplates();
    }

    /**
     * 生成Markdown格式的报告
     */
    public String generateMarkdownReport(AnalysisResult result) {
        try {
            return templateEngine.renderDefault(result, ReportTemplate.TemplateType.MARKDOWN);
        } catch (Exception e) {
            log.error("生成Markdown报告失败", e);
            return generateFallbackMarkdownReport(result);
        }
    }

    /**
     * 生成HTML格式的报告
     */
    public String generateHtmlReport(AnalysisResult result) {
        try {
            return templateEngine.renderDefault(result, ReportTemplate.TemplateType.HTML);
        } catch (Exception e) {
            log.error("生成HTML报告失败", e);
            return generateFallbackHtmlReport(result);
        }
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
     * 获取模板引擎
     */
    public TemplateEngine getTemplateEngine() {
        return templateEngine;
    }

    /**
     * 降级Markdown报告生成（当模板引擎失败时使用）
     */
    private String generateFallbackMarkdownReport(AnalysisResult result) {
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

        return md.toString();
    }

    /**
     * 降级HTML报告生成（当模板引擎失败时使用）
     */
    private String generateFallbackHtmlReport(AnalysisResult result) {
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

        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
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
            """;
    }

    /**
     * 格式化时间戳
     */
    private String formatTimestamp(long timestamp) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new java.util.Date(timestamp));
    }
}
