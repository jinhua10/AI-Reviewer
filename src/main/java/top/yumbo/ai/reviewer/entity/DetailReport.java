package top.yumbo.ai.reviewer.entity;

import lombok.Builder;
import lombok.Data;

/**
 * 详细报告实体
 * 
 * 包含模块的详细信息
 */
@Data
@Builder
public class DetailReport {

    /**
     * 模块名称
     */
    private String moduleName;

    /**
     * 职责
     */
    private String responsibilities;

    /**
     * 设计模式
     */
    private String designPatterns;

    /**
     * 跨模块分析
     */
    private String crossModuleAnalysis;

    /**
     * 原始响应
     */
    private String rawResponse;
}
