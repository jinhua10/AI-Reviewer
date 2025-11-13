package top.yumbo.ai.reviewer.domain.hackathon.model;

import lombok.Data;
import lombok.Builder;
import top.yumbo.ai.reviewer.domain.model.Project;
import top.yumbo.ai.reviewer.domain.model.ReviewReport;
import top.yumbo.ai.reviewer.domain.model.ast.CodeInsight;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 维度评分策略注册表
 *
 * 替代硬编码的switch/case，使用策略模式动态注册评分方法
 *
 * @author AI-Reviewer Team
 * @version 3.0
 * @since 2025-11-13
 */
@Data
@Builder
public class DimensionScoringRegistry {

    /**
     * 评分策略接口
     */
    @FunctionalInterface
    public interface ScoringStrategy {
        int calculate(ReviewReport report, Project project, CodeInsight codeInsight);
    }

    /**
     * AST加分策略接口
     */
    @FunctionalInterface
    public interface ASTBonusStrategy {
        int calculateBonus(CodeInsight codeInsight);
    }

    /**
     * 维度评分策略映射
     */
    @Builder.Default
    private Map<String, ScoringStrategy> scoringStrategies = new HashMap<>();

    /**
     * AST加分策略映射
     */
    @Builder.Default
    private Map<String, ASTBonusStrategy> astBonusStrategies = new HashMap<>();

    /**
     * 维度到Score字段的映射
     */
    @Builder.Default
    private Map<String, Function<HackathonScore, Integer>> scoreFieldGetters = new HashMap<>();

    /**
     * 注册维度评分策略
     */
    public void registerScoringStrategy(String dimensionName, ScoringStrategy strategy) {
        scoringStrategies.put(dimensionName, strategy);
    }

    /**
     * 注册AST加分策略
     */
    public void registerASTBonusStrategy(String dimensionName, ASTBonusStrategy strategy) {
        astBonusStrategies.put(dimensionName, strategy);
    }

    /**
     * 注册Score字段获取器
     */
    public void registerScoreFieldGetter(String dimensionName, Function<HackathonScore, Integer> getter) {
        scoreFieldGetters.put(dimensionName, getter);
    }

    /**
     * 获取评分策略
     */
    public ScoringStrategy getScoringStrategy(String dimensionName) {
        return scoringStrategies.get(dimensionName);
    }

    /**
     * 获取AST加分策略
     */
    public ASTBonusStrategy getASTBonusStrategy(String dimensionName) {
        return astBonusStrategies.get(dimensionName);
    }

    /**
     * 获取Score字段值
     */
    public Integer getScoreFieldValue(String dimensionName, HackathonScore score) {
        Function<HackathonScore, Integer> getter = scoreFieldGetters.get(dimensionName);
        return getter != null ? getter.apply(score) : null;
    }

    /**
     * 检查是否有评分策略
     */
    public boolean hasScoringStrategy(String dimensionName) {
        return scoringStrategies.containsKey(dimensionName);
    }

    /**
     * 检查是否有AST加分策略
     */
    public boolean hasASTBonusStrategy(String dimensionName) {
        return astBonusStrategies.containsKey(dimensionName);
    }

    /**
     * 创建默认注册表（向后兼容）
     */
    public static DimensionScoringRegistry createDefault() {
        DimensionScoringRegistry registry = DimensionScoringRegistry.builder().build();

        // 注册Score字段获取器
        registry.registerScoreFieldGetter("code_quality", HackathonScore::getCodeQuality);

        return registry;
    }
}

