package top.yumbo.ai.reviewer.domain.hackathon.model;

import lombok.Data;
import lombok.Builder;

import java.util.HashMap;
import java.util.Map;

/**
 * 黑客松评分配置
 *
 * 允许通过配置文件动态调整评分维度和权重
 *
 * @author AI-Reviewer Team
 * @version 2.0
 * @since 2025-11-13
 */
@Data
@Builder
public class HackathonScoringConfig {

    // ==================== 评分维度权重 ====================

    /**
     * 代码质量权重 (默认40%)
     */
    @Builder.Default
    private double codeQualityWeight = 0.40;

    /**
     * 创新性权重 (默认30%)
     */
    @Builder.Default
    private double innovationWeight = 0.30;

    /**
     * 完成度权重 (默认20%)
     */
    @Builder.Default
    private double completenessWeight = 0.20;

    /**
     * 文档质量权重 (默认10%)
     */
    @Builder.Default
    private double documentationWeight = 0.10;

    // ==================== 代码质量子维度权重 ====================

    /**
     * 基础质量权重 (默认40%)
     */
    @Builder.Default
    private double baseQualityWeight = 0.40;

    /**
     * 复杂度控制权重 (默认30%)
     */
    @Builder.Default
    private double complexityWeight = 0.30;

    /**
     * 代码坏味道权重 (默认20%)
     */
    @Builder.Default
    private double codeSmellWeight = 0.20;

    /**
     * 架构设计权重 (默认10%)
     */
    @Builder.Default
    private double architectureWeight = 0.10;

    // ==================== 创新性子维度权重 ====================

    /**
     * 技术栈创新权重 (默认30%)
     */
    @Builder.Default
    private double techStackWeight = 0.30;

    /**
     * 设计模式权重 (默认30%)
     */
    @Builder.Default
    private double designPatternWeight = 0.30;

    /**
     * AI评价权重 (默认25%)
     */
    @Builder.Default
    private double aiEvaluationWeight = 0.25;

    /**
     * 独特性权重 (默认15%)
     */
    @Builder.Default
    private double uniquenessWeight = 0.15;

    // ==================== 完成度子维度权重 ====================

    /**
     * 代码结构权重 (默认40%)
     */
    @Builder.Default
    private double structureWeight = 0.40;

    /**
     * 功能实现权重 (默认30%)
     */
    @Builder.Default
    private double functionalityWeight = 0.30;

    /**
     * 测试覆盖权重 (默认20%)
     */
    @Builder.Default
    private double testCoverageWeight = 0.20;

    /**
     * 代码规范权重 (默认10%)
     */
    @Builder.Default
    private double codeStandardWeight = 0.10;

    // ==================== 评分阈值配置 ====================

    /**
     * 复杂度阈值
     */
    @Builder.Default
    private Map<String, Double> complexityThresholds = createDefaultComplexityThresholds();

    /**
     * 代码坏味道扣分
     */
    @Builder.Default
    private Map<String, Integer> codeSmellPenalties = createDefaultCodeSmellPenalties();

    /**
     * 设计模式加分
     */
    @Builder.Default
    private Map<String, Integer> designPatternBonus = createDefaultDesignPatternBonus();

    // ==================== AST分析配置 ====================

    /**
     * 是否启用AST深度分析 (默认true)
     */
    @Builder.Default
    private boolean enableASTAnalysis = true;

    /**
     * 方法长度阈值 (默认50行)
     */
    @Builder.Default
    private int longMethodThreshold = 50;

    /**
     * 高复杂度阈值 (默认10)
     */
    @Builder.Default
    private int highComplexityThreshold = 10;

    /**
     * 大类阈值-方法数 (默认20)
     */
    @Builder.Default
    private int godClassMethodThreshold = 20;

    /**
     * 大类阈值-字段数 (默认15)
     */
    @Builder.Default
    private int godClassFieldThreshold = 15;

    // ==================== 默认配置创建方法 ====================

    private static Map<String, Double> createDefaultComplexityThresholds() {
        Map<String, Double> thresholds = new HashMap<>();
        thresholds.put("excellent", 5.0);    // < 5 优秀
        thresholds.put("good", 7.0);         // 5-7 良好
        thresholds.put("medium", 10.0);      // 7-10 中等
        thresholds.put("poor", 15.0);        // 10-15 较差
        // > 15 很差
        return thresholds;
    }

    private static Map<String, Integer> createDefaultCodeSmellPenalties() {
        Map<String, Integer> penalties = new HashMap<>();
        penalties.put("CRITICAL", 3);
        penalties.put("HIGH", 2);
        penalties.put("MEDIUM", 1);
        penalties.put("LOW", 0); // 改为0，避免小数
        return penalties;
    }

    private static Map<String, Integer> createDefaultDesignPatternBonus() {
        Map<String, Integer> bonus = new HashMap<>();
        bonus.put("CREATIONAL", 2);   // 创建型模式
        bonus.put("STRUCTURAL", 3);    // 结构型模式
        bonus.put("BEHAVIORAL", 3);    // 行为型模式
        bonus.put("ARCHITECTURAL", 4); // 架构模式
        bonus.put("COMBINATION", 5);   // 组合使用 (3+种)
        return bonus;
    }

    /**
     * 验证权重总和是否为1.0
     */
    public boolean validateWeights() {
        double total = codeQualityWeight + innovationWeight + completenessWeight + documentationWeight;
        return Math.abs(total - 1.0) < 0.001;
    }

    /**
     * 创建默认配置
     */
    public static HackathonScoringConfig createDefault() {
        return HackathonScoringConfig.builder().build();
    }

    /**
     * 从配置文件加载（预留接口）
     */
    public static HackathonScoringConfig loadFromFile(String configPath) {
        // TODO: 实现从YAML/JSON配置文件加载
        log.warn("从配置文件加载尚未实现，使用默认配置");
        return createDefault();
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HackathonScoringConfig.class);
}

