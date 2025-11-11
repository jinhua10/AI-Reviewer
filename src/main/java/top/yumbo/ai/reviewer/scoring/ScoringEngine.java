package top.yumbo.ai.reviewer.scoring;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.reviewer.exception.AnalysisException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 评分引擎 - 管理所有评分规则并执行评分计算
 */
@Slf4j
public class ScoringEngine {

    private final Map<String, ScoringRule> rules = new ConcurrentHashMap<>();

    /**
     * 注册评分规则
     */
    public void registerRule(ScoringRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("评分规则不能为空");
        }

        if (!rule.validate()) {
            throw new IllegalArgumentException("评分规则验证失败: " + rule.getName());
        }

        rules.put(rule.getName(), rule);
        log.info("注册评分规则: {} ({})", rule.getName(), rule.getType());
    }

    /**
     * 注销评分规则
     */
    public void unregisterRule(String ruleName) {
        ScoringRule removed = rules.remove(ruleName);
        if (removed != null) {
            log.info("注销评分规则: {}", ruleName);
        }
    }

    /**
     * 获取评分规则
     */
    public ScoringRule getRule(String ruleName) {
        return rules.get(ruleName);
    }

    /**
     * 获取所有评分规则
     */
    public Map<String, ScoringRule> getAllRules() {
        return new HashMap<>(rules);
    }

    /**
     * 根据类型获取评分规则
     */
    public Map<String, ScoringRule> getRulesByType(ScoringRule.RuleType type) {
        Map<String, ScoringRule> result = new HashMap<>();
        for (Map.Entry<String, ScoringRule> entry : rules.entrySet()) {
            if (entry.getValue().getType() == type) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * 计算综合评分
     */
    public int calculateOverallScore(Map<String, String> analysisResults,
                                   Map<String, Double> weights,
                                   ScoringRule.ScoringContext context) throws AnalysisException {

        if (analysisResults == null || analysisResults.isEmpty()) {
            return 0;
        }

        double totalWeightedScore = 0.0;
        double totalWeight = 0.0;

        for (Map.Entry<String, String> entry : analysisResults.entrySet()) {
            String dimension = entry.getKey();
            String analysisResult = entry.getValue();

            // 查找对应的评分规则
            ScoringRule rule = findRuleForDimension(dimension);
            if (rule == null) {
                log.warn("未找到维度 {} 的评分规则，使用默认评分", dimension);
                continue;
            }

            // 计算维度评分
            int dimensionScore = rule.calculateScore(analysisResult, context);
            double weight = weights.getOrDefault(dimension, rule.getWeight());

            totalWeightedScore += dimensionScore * weight;
            totalWeight += weight;

            log.debug("维度评分: {} = {} (权重: {})", dimension, dimensionScore, weight);
        }

        if (totalWeight == 0.0) {
            return 0;
        }

        int overallScore = (int) Math.round(totalWeightedScore / totalWeight);
        return Math.max(0, Math.min(100, overallScore));
    }

    /**
     * 计算单个维度评分
     */
    public int calculateDimensionScore(String dimension, String analysisResult,
                                     ScoringRule.ScoringContext context) throws AnalysisException {

        ScoringRule rule = findRuleForDimension(dimension);
        if (rule == null) {
            log.warn("未找到维度 {} 的评分规则，返回默认分数", dimension);
            return 50; // 默认中等分数
        }

        return rule.calculateScore(analysisResult, context);
    }

    /**
     * 查找维度对应的评分规则
     */
    private ScoringRule findRuleForDimension(String dimension) {
        // 首先尝试精确匹配
        ScoringRule rule = rules.get(dimension);
        if (rule != null) {
            return rule;
        }

        // 然后尝试类型匹配
        ScoringRule.RuleType ruleType = mapDimensionToRuleType(dimension);
        if (ruleType != null) {
            Map<String, ScoringRule> typeRules = getRulesByType(ruleType);
            if (!typeRules.isEmpty()) {
                // 返回第一个匹配的规则
                return typeRules.values().iterator().next();
            }
        }

        return null;
    }

    /**
     * 将维度名称映射到规则类型
     */
    private ScoringRule.RuleType mapDimensionToRuleType(String dimension) {
        switch (dimension.toLowerCase()) {
            case "architecture":
                return ScoringRule.RuleType.ARCHITECTURE;
            case "code_quality":
                return ScoringRule.RuleType.CODE_QUALITY;
            case "technical_debt":
                return ScoringRule.RuleType.TECHNICAL_DEBT;
            case "functionality":
                return ScoringRule.RuleType.FUNCTIONALITY;
            case "business_value":
                return ScoringRule.RuleType.BUSINESS_VALUE;
            case "test_coverage":
                return ScoringRule.RuleType.TEST_COVERAGE;
            default:
                return null;
        }
    }

    /**
     * 清空所有规则
     */
    public void clearRules() {
        rules.clear();
        log.info("清空所有评分规则");
    }

    /**
     * 获取规则统计信息
     */
    public ScoringStats getStats() {
        Map<ScoringRule.RuleType, Integer> typeCount = new HashMap<>();
        for (ScoringRule rule : rules.values()) {
            typeCount.merge(rule.getType(), 1, Integer::sum);
        }

        return new ScoringStats(rules.size(), typeCount);
    }

    /**
     * 评分统计信息
     */
    public static class ScoringStats {
        private final int totalRules;
        private final Map<ScoringRule.RuleType, Integer> rulesByType;

        public ScoringStats(int totalRules, Map<ScoringRule.RuleType, Integer> rulesByType) {
            this.totalRules = totalRules;
            this.rulesByType = rulesByType;
        }

        public int getTotalRules() { return totalRules; }
        public Map<ScoringRule.RuleType, Integer> getRulesByType() { return rulesByType; }
    }
}
