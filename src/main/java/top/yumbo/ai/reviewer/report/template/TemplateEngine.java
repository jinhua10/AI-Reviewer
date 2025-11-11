package top.yumbo.ai.reviewer.report.template;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.reviewer.entity.AnalysisResult;
import top.yumbo.ai.reviewer.exception.AnalysisException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模板引擎 - 管理报告模板并执行渲染
 */
@Slf4j
public class TemplateEngine {

    private final Map<String, ReportTemplate> templates = new ConcurrentHashMap<>();

    /**
     * 注册模板
     */
    public void registerTemplate(ReportTemplate template) {
        if (template == null) {
            throw new IllegalArgumentException("模板不能为空");
        }

        if (!template.validate()) {
            throw new IllegalArgumentException("模板验证失败: " + template.getName());
        }

        templates.put(template.getName(), template);
        log.info("注册报告模板: {} ({})", template.getName(), template.getType());
    }

    /**
     * 注销模板
     */
    public void unregisterTemplate(String templateName) {
        ReportTemplate removed = templates.remove(templateName);
        if (removed != null) {
            log.info("注销报告模板: {}", templateName);
        }
    }

    /**
     * 获取模板
     */
    public ReportTemplate getTemplate(String templateName) {
        return templates.get(templateName);
    }

    /**
     * 获取所有模板
     */
    public Map<String, ReportTemplate> getAllTemplates() {
        return new HashMap<>(templates);
    }

    /**
     * 根据类型获取模板
     */
    public Map<String, ReportTemplate> getTemplatesByType(ReportTemplate.TemplateType type) {
        Map<String, ReportTemplate> result = new HashMap<>();
        for (Map.Entry<String, ReportTemplate> entry : templates.entrySet()) {
            if (entry.getValue().getType() == type) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * 渲染报告
     */
    public String render(String templateName, AnalysisResult result) throws AnalysisException {
        return render(templateName, result, null);
    }

    /**
     * 渲染报告（带额外变量）
     */
    public String render(String templateName, AnalysisResult result, Map<String, Object> variables) throws AnalysisException {
        ReportTemplate template = templates.get(templateName);
        if (template == null) {
            throw new AnalysisException("模板不存在: " + templateName);
        }

        log.debug("使用模板渲染报告: {}", templateName);
        return template.render(result, variables);
    }

    /**
     * 渲染报告（使用默认模板）
     */
    public String renderDefault(AnalysisResult result, ReportTemplate.TemplateType type) throws AnalysisException {
        return renderDefault(result, type, null);
    }

    /**
     * 渲染报告（使用默认模板，带额外变量）
     */
    public String renderDefault(AnalysisResult result, ReportTemplate.TemplateType type, Map<String, Object> variables) throws AnalysisException {
        // 查找默认模板
        String defaultTemplateName = "default-" + type.name().toLowerCase();
        ReportTemplate template = templates.get(defaultTemplateName);

        if (template == null) {
            // 如果没有找到特定类型的默认模板，使用第一个匹配类型的模板
            Map<String, ReportTemplate> typeTemplates = getTemplatesByType(type);
            if (!typeTemplates.isEmpty()) {
                template = typeTemplates.values().iterator().next();
            }
        }

        if (template == null) {
            throw new AnalysisException("未找到类型 " + type + " 的可用模板");
        }

        log.debug("使用默认模板渲染报告: {} ({})", template.getName(), type);
        return template.render(result, variables);
    }

    /**
     * 创建内置模板
     */
    public void createBuiltInTemplates() {
        // Markdown默认模板
        String markdownTemplate = createMarkdownTemplate();
        registerTemplate(new StringTemplate("default-markdown", "默认Markdown报告模板",
                ReportTemplate.TemplateType.MARKDOWN, markdownTemplate));

        // HTML默认模板
        String htmlTemplate = createHtmlTemplate();
        registerTemplate(new StringTemplate("default-html", "默认HTML报告模板",
                ReportTemplate.TemplateType.HTML, htmlTemplate));

        log.info("创建内置报告模板完成");
    }

    /**
     * 创建Markdown模板
     */
    private String createMarkdownTemplate() {
        return """
            # 项目分析报告

            **项目名称:** ${projectName}
            **分析时间:** ${analysisTimestamp}

            ## 总体评分

            **综合评分: ${overallScore}/100**

            ### 评分详情

            | 维度 | 评分 | 权重 |
            |------|------|------|
            | 架构设计 | ${architectureScore}/100 | 20% |
            | 代码质量 | ${codeQualityScore}/100 | 20% |
            | 技术债务 | ${technicalDebtScore}/100 | 15% |
            | 功能完整性 | ${functionalityScore}/100 | 20% |
            | 商业价值 | ${businessValueScore}/100 | 15% |
            | 测试覆盖率 | ${testCoverageScore}/100 | 10% |

            ## 执行摘要

            ${summaryContent}

            ### 关键发现

            ${keyFindings}

            ### 建议改进

            ${recommendations}
            """;
    }

    /**
     * 创建HTML模板
     */
    private String createHtmlTemplate() {
        return """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>项目分析报告</title>
                <style>
                    body { font-family: 'Segoe UI', sans-serif; margin: 40px; background: #f5f5f5; }
                    .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; border-radius: 10px; text-align: center; }
                    .score-section { background: white; padding: 30px; border-radius: 10px; margin: 20px 0; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .overall-score { font-size: 4em; font-weight: bold; color: #4CAF50; text-align: center; margin: 20px 0; }
                    .score-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin-top: 30px; }
                    .score-item { background: #f8f9fa; padding: 20px; border-radius: 8px; text-align: center; }
                    .score-value { font-size: 2em; font-weight: bold; color: #2196F3; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>项目分析报告</h1>
                    <div class="project-info">
                        <p><strong>项目名称:</strong> ${projectName}</p>
                        <p><strong>分析时间:</strong> ${analysisTimestamp}</p>
                    </div>
                </div>

                <div class="score-section">
                    <h2>总体评分</h2>
                    <div class="overall-score">${overallScore}<span>/100</span></div>

                    <div class="score-grid">
                        <div class="score-item">
                            <div class="score-label">架构设计</div>
                            <div class="score-value">${architectureScore}</div>
                        </div>
                        <div class="score-item">
                            <div class="score-label">代码质量</div>
                            <div class="score-value">${codeQualityScore}</div>
                        </div>
                        <div class="score-item">
                            <div class="score-label">技术债务</div>
                            <div class="score-value">${technicalDebtScore}</div>
                        </div>
                        <div class="score-item">
                            <div class="score-label">功能完整性</div>
                            <div class="score-value">${functionalityScore}</div>
                        </div>
                        <div class="score-item">
                            <div class="score-label">商业价值</div>
                            <div class="score-value">${businessValueScore}</div>
                        </div>
                        <div class="score-item">
                            <div class="score-label">测试覆盖率</div>
                            <div class="score-value">${testCoverageScore}</div>
                        </div>
                    </div>
                </div>

                <div class="score-section">
                    <h2>执行摘要</h2>
                    <div class="content">${summaryContent}</div>

                    <h3>关键发现</h3>
                    <ul>
                        <li>${keyFindings}</li>
                    </ul>

                    <h3>建议改进</h3>
                    <ul>
                        <li>${recommendations}</li>
                    </ul>
                </div>
            </body>
            </html>
            """;
    }

    /**
     * 清空所有模板
     */
    public void clearTemplates() {
        templates.clear();
        log.info("清空所有报告模板");
    }

    /**
     * 获取模板统计信息
     */
    public TemplateStats getStats() {
        Map<ReportTemplate.TemplateType, Integer> typeCount = new HashMap<>();
        for (ReportTemplate template : templates.values()) {
            typeCount.merge(template.getType(), 1, Integer::sum);
        }

        return new TemplateStats(templates.size(), typeCount);
    }

    /**
     * 模板统计信息
     */
    public static class TemplateStats {
        private final int totalTemplates;
        private final Map<ReportTemplate.TemplateType, Integer> templatesByType;

        public TemplateStats(int totalTemplates, Map<ReportTemplate.TemplateType, Integer> templatesByType) {
            this.totalTemplates = totalTemplates;
            this.templatesByType = templatesByType;
        }

        public int getTotalTemplates() { return totalTemplates; }
        public Map<ReportTemplate.TemplateType, Integer> getTemplatesByType() { return templatesByType; }
    }
}
