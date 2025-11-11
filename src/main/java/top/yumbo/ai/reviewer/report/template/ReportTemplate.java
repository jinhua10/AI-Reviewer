package top.yumbo.ai.reviewer.report.template;

import top.yumbo.ai.reviewer.entity.AnalysisResult;
import top.yumbo.ai.reviewer.exception.AnalysisException;

import java.util.Map;

/**
 * 报告模板接口
 */
public interface ReportTemplate {

    /**
     * 获取模板名称
     */
    String getName();

    /**
     * 获取模板描述
     */
    String getDescription();

    /**
     * 获取模板类型
     */
    TemplateType getType();

    /**
     * 渲染报告
     * @param result 分析结果
     * @param variables 额外变量
     * @return 渲染后的报告内容
     */
    String render(AnalysisResult result, Map<String, Object> variables) throws AnalysisException;

    /**
     * 验证模板
     */
    boolean validate();

    /**
     * 获取模板变量列表
     */
    String[] getVariables();

    /**
     * 模板类型枚举
     */
    enum TemplateType {
        MARKDOWN,
        HTML,
        JSON,
        XML,
        PLAIN_TEXT
    }
}
