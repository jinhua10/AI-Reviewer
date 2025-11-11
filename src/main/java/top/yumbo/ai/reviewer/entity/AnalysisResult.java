package top.yumbo.ai.reviewer.entity;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 分析结果实体
 * 
 * 包含整个项目的分析结果
 */
@Data
@Builder
public class AnalysisResult {

    /**
     * 概要报告
     */
    private SummaryReport summaryReport;

    /**
     * 详细报告列表
     */
    private List<DetailReport> detailReports;

    /**
     * 生成时间
     */
    private String generatedAt;

    /**
     * 项目路径
     */
    private String projectPath;

    /**
     * 获取概要内容
     * 
     * @return 概要内容
     */
    public String getSummary() {
        if (summaryReport == null) {
            return "无概要信息";
        }

        StringBuilder summary = new StringBuilder();
        summary.append("项目核心功能: ").append(summaryReport.getCoreFunction()).append("\n");
        summary.append("技术栈: ").append(summaryReport.getTechStack()).append("\n");
        summary.append("启动流程: ").append(summaryReport.getStartupFlow()).append("\n");

        return summary.toString();
    }
}
