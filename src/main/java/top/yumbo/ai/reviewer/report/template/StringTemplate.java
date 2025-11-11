package top.yumbo.ai.reviewer.report.template;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.reviewer.entity.AnalysisResult;
import top.yumbo.ai.reviewer.exception.AnalysisException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于字符串模板的报告模板实现
 */
@Slf4j
public class StringTemplate implements ReportTemplate {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    private final String name;
    private final String description;
    private final TemplateType type;
    private final String templateContent;
    private final String[] variables;

    public StringTemplate(String name, String description, TemplateType type, String templateContent) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.templateContent = templateContent;
        this.variables = extractVariables(templateContent);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public TemplateType getType() {
        return type;
    }

    @Override
    public String render(AnalysisResult result, Map<String, Object> variables) throws AnalysisException {
        try {
            Map<String, Object> context = buildContext(result, variables);
            return replaceVariables(templateContent, context);
        } catch (Exception e) {
            log.error("模板渲染失败: {}", e.getMessage(), e);
            throw new AnalysisException("模板渲染失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean validate() {
        return name != null && !name.trim().isEmpty() &&
               templateContent != null && !templateContent.trim().isEmpty() &&
               type != null;
    }

    @Override
    public String[] getVariables() {
        return variables.clone();
    }

    /**
     * 构建渲染上下文
     */
    private Map<String, Object> buildContext(AnalysisResult result, Map<String, Object> extraVariables) {
        Map<String, Object> context = new HashMap<>();

        // 基本项目信息
        context.put("projectName", result.getProjectName());
        context.put("projectPath", result.getProjectPath());
        context.put("projectType", result.getProjectType());
        context.put("analysisTimestamp", formatTimestamp(result.getAnalysisTimestamp()));

        // 评分信息
        context.put("overallScore", result.getOverallScore());
        context.put("architectureScore", result.getArchitectureScore());
        context.put("codeQualityScore", result.getCodeQualityScore());
        context.put("technicalDebtScore", result.getTechnicalDebtScore());
        context.put("functionalityScore", result.getFunctionalityScore());
        context.put("businessValueScore", result.getBusinessValueScore());
        context.put("testCoverageScore", result.getTestCoverageScore());

        // 分析维度
        context.put("analyzedDimensions", result.getAnalyzedDimensions());

        // 摘要报告
        if (result.getSummaryReport() != null) {
            context.put("summaryTitle", result.getSummaryReport().getTitle());
            context.put("summaryContent", result.getSummaryReport().getContent());
            context.put("keyFindings", result.getSummaryReport().getKeyFindings());
            context.put("recommendations", result.getSummaryReport().getRecommendations());
            context.put("analysisTime", result.getSummaryReport().getAnalysisTime());
        }

        // 详细报告
        if (result.getDetailReport() != null) {
            context.put("detailTitle", result.getDetailReport().getTitle());
            context.put("detailContent", result.getDetailReport().getContent());

            // 各维度详细分析
            if (result.getDetailReport().getArchitectureAnalysis() != null) {
                context.put("architectureAnalysis", result.getDetailReport().getArchitectureAnalysis());
            }
            if (result.getDetailReport().getCodeQualityAnalysis() != null) {
                context.put("codeQualityAnalysis", result.getDetailReport().getCodeQualityAnalysis());
            }
            if (result.getDetailReport().getTechnicalDebtAnalysis() != null) {
                context.put("technicalDebtAnalysis", result.getDetailReport().getTechnicalDebtAnalysis());
            }
            if (result.getDetailReport().getFunctionalityAnalysis() != null) {
                context.put("functionalityAnalysis", result.getDetailReport().getFunctionalityAnalysis());
            }
            if (result.getDetailReport().getBusinessValueAnalysis() != null) {
                context.put("businessValueAnalysis", result.getDetailReport().getBusinessValueAnalysis());
            }
            if (result.getDetailReport().getTestCoverageAnalysis() != null) {
                context.put("testCoverageAnalysis", result.getDetailReport().getTestCoverageAnalysis());
            }
        }

        // 问题列表
        context.put("issues", result.getIssues());

        // 添加额外变量
        if (extraVariables != null) {
            context.putAll(extraVariables);
        }

        return context;
    }

    /**
     * 替换模���变量
     */
    private String replaceVariables(String template, Map<String, Object> context) {
        String result = template;
        Matcher matcher = VARIABLE_PATTERN.matcher(template);

        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object value = context.get(variableName);

            if (value != null) {
                String replacement = formatValue(value);
                result = result.replace("${" + variableName + "}", replacement);
            } else {
                log.warn("模板变量未找到值: {}", variableName);
                result = result.replace("${" + variableName + "}", "");
            }
        }

        return result;
    }

    /**
     * 格式化变量值
     */
    private String formatValue(Object value) {
        if (value == null) {
            return "";
        }

        if (value instanceof String) {
            return (String) value;
        }

        if (value instanceof Number) {
            return value.toString();
        }

        if (value instanceof Boolean) {
            return value.toString();
        }

        if (value instanceof Iterable) {
            StringBuilder sb = new StringBuilder();
            for (Object item : (Iterable<?>) value) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(formatValue(item));
            }
            return sb.toString();
        }

        // 对于复杂对象，返回其字符串表示
        return value.toString();
    }

    /**
     * 提取模板中的变量
     */
    private String[] extractVariables(String template) {
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        Map<String, Boolean> variables = new HashMap<>();

        while (matcher.find()) {
            variables.put(matcher.group(1), true);
        }

        return variables.keySet().toArray(new String[0]);
    }

    /**
     * 格式化时间戳
     */
    private String formatTimestamp(long timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timestamp));
    }
}
