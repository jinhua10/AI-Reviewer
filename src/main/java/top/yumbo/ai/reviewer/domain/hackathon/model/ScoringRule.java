package top.yumbo.ai.reviewer.domain.hackathon.model;

import lombok.Data;
import lombok.Builder;

import java.util.*;

/**
 * 黑客松评分规则定义
 *
 * 支持动态添加自定义评分规则
 *
 * @author AI-Reviewer Team
 * @version 2.0
 * @since 2025-11-13
 */
@Data
@Builder
public class ScoringRule {

    /**
     * 规则名称
     */
    private String name;

    /**
     * 规则描述
     */
    private String description;

    /**
     * 规则类型
     */
    private String type;

    /**
     * 规则权重
     */
    private double weight;

    /**
     * 是否启用
     */
    @Builder.Default
    private boolean enabled = true;

    /**
     * 评分策略
     */
    private String strategy;

    /**
     * 规则配置
     */
    @Builder.Default
    private Map<String, Object> config = new HashMap<>();

    /**
     * 正向关键词及其分数
     */
    @Builder.Default
    private Map<String, Integer> positiveKeywords = new HashMap<>();

    /**
     * 负向关键词及其分数
     */
    @Builder.Default
    private Map<String, Integer> negativeKeywords = new HashMap<>();

    /**
     * 自定义评分函数（预留）
     */
    private String customScoringFunction;

    /**
     * 应用规则到项目内容
     */
    public int applyRule(String projectContent) {
        if (!enabled) {
            return 0;
        }

        int score = 0;
        String lowerContent = projectContent.toLowerCase();

        // 正向关键词加分
        for (Map.Entry<String, Integer> entry : positiveKeywords.entrySet()) {
            if (lowerContent.contains(entry.getKey().toLowerCase())) {
                score += entry.getValue();
            }
        }

        // 负向关键词扣分
        for (Map.Entry<String, Integer> entry : negativeKeywords.entrySet()) {
            if (lowerContent.contains(entry.getKey().toLowerCase())) {
                score += entry.getValue(); // 注意：负值已经在配置中
            }
        }

        return (int) (score * weight);
    }

    /**
     * 验证规则配置
     */
    public boolean isValid() {
        return name != null && !name.isEmpty()
            && type != null && !type.isEmpty()
            && weight >= 0 && weight <= 1;
    }
}

