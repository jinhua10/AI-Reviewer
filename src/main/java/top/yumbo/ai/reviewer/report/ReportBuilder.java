package top.yumbo.ai.reviewer.report;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.reviewer.config.Config;
import top.yumbo.ai.reviewer.entity.AnalysisResult;
import top.yumbo.ai.reviewer.exception.AnalysisException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 报告生成器
 * 
 * 负责根据分析结果生成各种格式的报告
 */
@Slf4j
public class ReportBuilder {

    private final Config config;

    public ReportBuilder(Config config) {
        this.config = config;
    }

    /**
     * 生成报告
     * 
     * @param result 分析结果
     * @throws AnalysisException 如果生成报告时发生错误
     */
    public void build(AnalysisResult result) throws AnalysisException {
        try {
            // 确保输出目录存在
            Path outputDir = config.getOutputPath();
            Files.createDirectories(outputDir);

            // 生成各种格式的报告
            for (String format : config.getReportFormats()) {
                switch (format.toLowerCase()) {
                    case "markdown":
                        buildMarkdownReport(result, outputDir);
                        break;
                    case "json":
                        buildJsonReport(result, outputDir);
                        break;
                    case "html":
                        buildHtmlReport(result, outputDir);
                        break;
                    default:
                        log.warn("不支持的报告格式: {}", format);
                }
            }

            log.info("报告生成完成，输出目录: {}", outputDir);

        } catch (IOException e) {
            throw new AnalysisException("生成报告时发生错误", e);
        }
    }

    /**
     * 生成Markdown报告
     * 
     * @param result 分析结果
     * @param outputDir 输出目录
     * @throws IOException 如果生成报告时发生错误
     */
    private void buildMarkdownReport(AnalysisResult result, Path outputDir) throws IOException {
        StringBuilder report = new StringBuilder();

        // 报告标题
        report.append("# 代码分析报告\n\n");
        report.append("生成时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");

        // 项目概览
        report.append("## 项目概览\n\n");
        if (result.getSummaryReport() != null) {
            report.append("### 核心功能\n\n");
            report.append(result.getSummaryReport().getCoreFunction()).append("\n\n");

            report.append("### 技术栈\n\n");
            report.append(result.getSummaryReport().getTechStack()).append("\n\n");

            report.append("### 启动流程\n\n");
            report.append(result.getSummaryReport().getStartupFlow()).append("\n\n");
        }

        // 模块分析
        report.append("## 模块分析\n\n");
        if (result.getDetailReports() != null) {
            for (var detailReport : result.getDetailReports()) {
                report.append("### ").append(detailReport.getModuleName()).append("\n\n");

                report.append("#### 职责\n\n");
                report.append(detailReport.getResponsibilities()).append("\n\n");

                report.append("#### 设计模式\n\n");
                report.append(detailReport.getDesignPatterns()).append("\n\n");

                if (detailReport.getCrossModuleAnalysis() != null) {
                    report.append("#### 跨模块分析\n\n");
                    report.append(detailReport.getCrossModuleAnalysis()).append("\n\n");
                }
            }
        }

        // 保存报告
        Path reportFile = outputDir.resolve("analysis_report.md");
        Files.writeString(reportFile, report.toString());

        log.info("Markdown报告已生成: {}", reportFile);
    }

    /**
     * 生成JSON报告
     * 
     * @param result 分析结果
     * @param outputDir 输出目录
     * @throws IOException 如果生成报告时发生错误
     */
    private void buildJsonReport(AnalysisResult result, Path outputDir) throws IOException {
        // 添加元数据
        result.setGeneratedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        result.setProjectPath(config.getProjectPath().toString());

        // 序列化为JSON
        String json = JSON.toJSONString(result, JSONWriter.Feature.PrettyFormat);

        // 保存报告
        Path reportFile = outputDir.resolve("analysis_report.json");
        Files.writeString(reportFile, json);

        log.info("JSON报告已生成: {}", reportFile);
    }

    /**
     * 生成HTML报告
     * 
     * @param result 分析结果
     * @param outputDir 输出目录
     * @throws IOException 如果生成报告时发生错误
     */
    private void buildHtmlReport(AnalysisResult result, Path outputDir) throws IOException {
        StringBuilder report = new StringBuilder();

        // HTML头部
        report.append("<!DOCTYPE html>\n");
        report.append("<html lang=\"zh-CN\">\n");
        report.append("<head>\n");
        report.append("    <meta charset=\"UTF-8\">\n");
        report.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        report.append("    <title>代码分析报告</title>\n");
        report.append("    <style>\n");
        report.append("        body { font-family: Arial, sans-serif; line-height: 1.6; max-width: 1200px; margin: 0 auto; padding: 20px; }\n");
        report.append("        h1, h2, h3 { color: #333; }\n");
        report.append("        h1 { border-bottom: 2px solid #eee; padding-bottom: 10px; }\n");
        report.append("        h2 { border-bottom: 1px solid #eee; padding-bottom: 8px; }\n");
        report.append("        .metadata { background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin-bottom: 20px; }\n");
        report.append("        .module { margin-bottom: 30px; border: 1px solid #ddd; border-radius: 5px; overflow: hidden; }\n");
        report.append("        .module-header { background-color: #f1f1f1; padding: 10px 15px; font-weight: bold; }\n");
        report.append("        .module-content { padding: 15px; }\n");
        report.append("        pre { background-color: #f8f9fa; padding: 10px; border-radius: 5px; overflow-x: auto; }\n");
        report.append("        code { background-color: #f8f9fa; padding: 2px 4px; border-radius: 3px; }\n");
        report.append("    </style>\n");
        report.append("</head>\n");
        report.append("<body>\n");

        // 报告标题和元信息
        report.append("    <h1>代码分析报告</h1>\n");
        report.append("    <div class=\"metadata\">\n");
        report.append("        <p><strong>生成时间:</strong> ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("</p>\n");
        report.append("        <p><strong>项目路径:</strong> ").append(escapeHtml(config.getProjectPath().toString())).append("</p>\n");
        report.append("    </div>\n");

        // 项目概览
        report.append("    <h2>项目概览</h2>\n");
        if (result.getSummaryReport() != null) {
            report.append("    <h3>核心功能</h3>\n");
            report.append("    <p>").append(escapeHtml(result.getSummaryReport().getCoreFunction())).append("</p>\n");

            report.append("    <h3>技术栈</h3>\n");
            report.append("    <p>").append(escapeHtml(result.getSummaryReport().getTechStack())).append("</p>\n");

            report.append("    <h3>启动流程</h3>\n");
            report.append("    <p>").append(escapeHtml(result.getSummaryReport().getStartupFlow())).append("</p>\n");
        }

        // 模块分析
        report.append("    <h2>模块分析</h2>\n");
        if (result.getDetailReports() != null) {
            for (var detailReport : result.getDetailReports()) {
                report.append("    <div class=\"module\">\n");
                report.append("        <div class=\"module-header\">").append(escapeHtml(detailReport.getModuleName())).append("</div>\n");
                report.append("        <div class=\"module-content\">\n");

                report.append("            <h3>职责</h3>\n");
                report.append("            <p>").append(escapeHtml(detailReport.getResponsibilities())).append("</p>\n");

                report.append("            <h3>设计模式</h3>\n");
                report.append("            <p>").append(escapeHtml(detailReport.getDesignPatterns())).append("</p>\n");

                if (detailReport.getCrossModuleAnalysis() != null) {
                    report.append("            <h3>跨模块分析</h3>\n");
                    report.append("            <p>").append(escapeHtml(detailReport.getCrossModuleAnalysis())).append("</p>\n");
                }

                report.append("        </div>\n");
                report.append("    </div>\n");
            }
        }

        // HTML尾部
        report.append("</body>\n");
        report.append("</html>\n");

        // 保存报告
        Path reportFile = outputDir.resolve("analysis_report.html");
        Files.writeString(reportFile, report.toString());

        log.info("HTML报告已生成: {}", reportFile);
    }

    /**
     * 转义HTML特殊字符
     * 
     * @param text 原始文本
     * @return 转义后的文本
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }

        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
}
