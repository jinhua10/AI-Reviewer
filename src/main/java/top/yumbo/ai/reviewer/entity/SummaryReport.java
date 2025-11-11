package top.yumbo.ai.reviewer.entity;

import lombok.Builder;
import lombok.Data;

/**
 * 概要报告实体
 * 
 * 包含项目的整体概要信息
 */
@Data
@Builder
public class SummaryReport {

    /**
     * 核心功能
     */
    private String coreFunction;

    /**
     * 技术栈
     */
    private String techStack;

    /**
     * 启动流程
     */
    private String startupFlow;

    /**
     * 原始响应
     */
    private String rawResponse;
}
