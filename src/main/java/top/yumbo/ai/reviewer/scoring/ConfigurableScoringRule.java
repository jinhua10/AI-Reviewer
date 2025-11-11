package top.yumbo.ai.reviewer.scoring;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.reviewer.exception.AnalysisException;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * 基于配置的评分规则实现
 */
@Slf4j
public class ConfigurableScoringRule implements ScoringRule {

    private String name;
    private String description;
    private RuleType type;
    private double weight;
    private Map<String, Object> config;

    public ConfigurableScoringRule(String name, String description, RuleType type, double weight, Map<String, Object> config) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.weight = weight;
        this.config = config;
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
    public int calculateScore(String analysisResult, ScoringContext context) throws AnalysisException {
        try {
            // 获取评分策略
            String strategy = (String) config.getOrDefault("strategy", "keyword_matching");

            switch (strategy) {
                case "keyword_matching":
                    return calculateByKeywordMatching(analysisResult, context);
                case "sentiment_analysis":
                    return calculateBySentimentAnalysis(analysisResult, context);
                case "rule_based":
                    return calculateByRuleBased(analysisResult, context);
                default:
                    log.warn("未知评分策略: {}, 使用默认策略", strategy);
                    return calculateByKeywordMatching(analysisResult, context);
            }

        } catch (Exception e) {
            log.error("评分计算失败: {}", e.getMessage(), e);
            throw new AnalysisException("评分计算失败: " + e.getMessage(), e);
        }
    }

    @Override
    public double getWeight() {
        return weight;
    }

    @Override
    public void setWeight(double weight) {
        this.weight = weight;
    }

    @Override
    public boolean validate() {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        if (type == null) {
            return false;
        }
        if (weight < 0 || weight > 1) {
            return false;
        }
        return config != null && !config.isEmpty();
    }

    @Override
    public RuleType getType() {
        return type;
    }

    /**
     * 基于关键词匹配的评分
     */
    private int calculateByKeywordMatching(String analysisResult, ScoringContext context) {
        if (analysisResult == null || analysisResult.trim().isEmpty()) {
            return 50; // 默认中等分数
        }

        // 获取关键词配置
        @SuppressWarnings("unchecked")
        Map<String, Object> keywords = (Map<String, Object>) config.get("keywords");
        if (keywords == null) {
            return 50;
        }

        int score = 50; // 基础分数
        String lowerResult = analysisResult.toLowerCase();

        // 正向关键词
        @SuppressWarnings("unchecked")
        Map<String, Integer> positiveKeywords = (Map<String, Integer>) keywords.get("positive");
        if (positiveKeywords != null) {
            for (Map.Entry<String, Integer> entry : positiveKeywords.entrySet()) {
                if (lowerResult.contains(entry.getKey().toLowerCase())) {
                    score += entry.getValue();
                }
            }
        }

        // 负向关键词
        @SuppressWarnings("unchecked")
        Map<String, Integer> negativeKeywords = (Map<String, Integer>) keywords.get("negative");
        if (negativeKeywords != null) {
            for (Map.Entry<String, Integer> entry : negativeKeywords.entrySet()) {
                if (lowerResult.contains(entry.getKey().toLowerCase())) {
                    score -= entry.getValue();
                }
            }
        }

        // 确保分数在0-100范围内
        return Math.max(0, Math.min(100, score));
    }

    /**
     * 基于情感分析的评分
     */
    private int calculateBySentimentAnalysis(String analysisResult, ScoringContext context) {
        // 简化的情感分析实现
        if (analysisResult == null || analysisResult.trim().isEmpty()) {
            return 50;
        }

        String lowerResult = analysisResult.toLowerCase();
        int score = 50;

        // 正面词汇
        String[] positiveWords = {"excellent", "good", "well", "great", "perfect", "outstanding", "excellent", "optimal"};
        for (String word : positiveWords) {
            if (lowerResult.contains(word)) {
                score += 10;
            }
        }

        // 负面词汇
        String[] negativeWords = {"poor", "bad", "terrible", "awful", "horrible", "inadequate", "insufficient"};
        for (String word : negativeWords) {
            if (lowerResult.contains(word)) {
                score -= 10;
            }
        }

        return Math.max(0, Math.min(100, score));
    }

    /**
     * 基于规则的评分
     */
    private int calculateByRuleBased(String analysisResult, ScoringContext context) {
        if (analysisResult == null || analysisResult.trim().isEmpty()) {
            return 50;
        }

        // 获取规则配置
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> rules = (java.util.List<Map<String, Object>>) config.get("rules");
        if (rules == null || rules.isEmpty()) {
            return 50;
        }

        int score = 50; // 基础分数

        for (Map<String, Object> rule : rules) {
            try {
                String condition = (String) rule.get("condition");
                String operation = (String) rule.get("operation");
                int value = ((Number) rule.get("value")).intValue();

                boolean conditionMet = evaluateCondition(analysisResult, condition, context);

                if (conditionMet) {
                    switch (operation.toLowerCase()) {
                        case "add":
                            score += value;
                            break;
                        case "subtract":
                            score -= value;
                            break;
                        case "multiply":
                            score *= (1.0 + value / 100.0);
                            break;
                        case "divide":
                            score /= (1.0 + value / 100.0);
                            break;
                        case "set":
                            score = value;
                            break;
                    }
                }

            } catch (Exception e) {
                log.warn("规则评估失败: {}", rule, e);
            }
        }

        return Math.max(0, Math.min(100, score));
    }

    /**
     * 评估条件
     */
    private boolean evaluateCondition(String analysisResult, String condition, ScoringContext context) {
        if (condition == null || condition.trim().isEmpty()) {
            return false;
        }

        String lowerResult = analysisResult.toLowerCase();
        String lowerCondition = condition.toLowerCase();

        // 简单包含检查
        if (lowerCondition.startsWith("contains:")) {
            String keyword = lowerCondition.substring(9).trim();
            return lowerResult.contains(keyword);
        }

        // 正则表达式匹配
        if (lowerCondition.startsWith("regex:")) {
            String regex = lowerCondition.substring(6).trim();
            try {
                return Pattern.matches(regex, analysisResult);
            } catch (Exception e) {
                log.warn("正则表达式无效: {}", regex, e);
                return false;
            }
        }

        // 长度检查
        if (lowerCondition.startsWith("length>")) {
            try {
                int minLength = Integer.parseInt(lowerCondition.substring(7).trim());
                return analysisResult.length() > minLength;
            } catch (NumberFormatException e) {
                log.warn("无效的长度条件: {}", condition);
                return false;
            }
        }

        // 默认包含检查
        return lowerResult.contains(lowerCondition);
    }
}
