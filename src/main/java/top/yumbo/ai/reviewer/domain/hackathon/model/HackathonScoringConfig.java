package top.yumbo.ai.reviewer.domain.hackathon.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Data;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 黑客松评分配置（支持动态扩展）
 * <p>
 * 特性：
 * 1. 支持任意数量的评分维度
 * 2. 支持动态添加自定义评分规则
 * 3. 完全配置化，无硬编码
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-13
 */
@Data
@Builder
@Slf4j
public class HackathonScoringConfig {

    // ==================== 动态评分维度 ====================

    /**
     * 动态评分维度权重映射
     * <p>
     * 示例：
     * {
     *   "code_quality": 0.40,
     *   "innovation": 0.30,
     *   "completeness": 0.20,
     *   "documentation": 0.10,
     *   "custom_dimension": 0.15  // 自定义维度
     * }
     */
    @Builder.Default
    private Map<String, Double> dimensionWeights = createDefaultDimensionWeights();

    /**
     * 维度显示名称映射（用于报告）
     */
    @Builder.Default
    private Map<String, String> dimensionDisplayNames = createDefaultDisplayNames();

    /**
     * 维度描述
     */
    @Builder.Default
    private Map<String, String> dimensionDescriptions = new HashMap<>();

    // ==================== 动态评分规则 ====================

    /**
     * 评分规则列表
     * 支持动态添加任意数量的规则
     */
    @Builder.Default
    private List<ScoringRule> scoringRules = new ArrayList<>();

    /**
     * 规则组（按维度分组）
     */
    @Builder.Default
    private Map<String, List<ScoringRule>> rulesByDimension = new HashMap<>();

    // ==================== AST分析配置 ====================

    /**
     * 是否启用AST深度分析
     */
    @Builder.Default
    private boolean enableASTAnalysis = true;

    /**
     * AST分析阈值配置
     */
    @Builder.Default
    private Map<String, Object> astThresholds = createDefaultASTThresholds();

    // ==================== 代码度量阈值 ====================

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

    // ==================== 方法：维度管理 ====================

    /**
     * 添加或更新维度
     */
    public void addDimension(String name, double weight, String displayName, String description) {
        if (weight < 0 || weight > 1) {
            throw new IllegalArgumentException("权重必须在0-1之间: " + weight);
        }

        dimensionWeights.put(name, weight);

        if (displayName != null) {
            dimensionDisplayNames.put(name, displayName);
        }

        if (description != null) {
            dimensionDescriptions.put(name, description);
        }

        log.info("添加维度: {} (权重={})", name, weight);
    }

    /**
     * 移除维度
     */
    public void removeDimension(String name) {
        dimensionWeights.remove(name);
        dimensionDisplayNames.remove(name);
        dimensionDescriptions.remove(name);
        rulesByDimension.remove(name);
        log.info("移除维度: {}", name);
    }

    /**
     * 获取维度权重
     */
    public double getDimensionWeight(String name) {
        return dimensionWeights.getOrDefault(name, 0.0);
    }

    /**
     * 获取所有维度
     */
    public Set<String> getAllDimensions() {
        return dimensionWeights.keySet();
    }

    /**
     * 获取维度显示名称
     */
    public String getDimensionDisplayName(String name) {
        return dimensionDisplayNames.getOrDefault(name, name);
    }

    // ==================== 方法：规则管理 ====================

    /**
     * 添加评分规则
     */
    public void addScoringRule(ScoringRule rule) {
        if (!rule.isValid()) {
            throw new IllegalArgumentException("无效的评分规则: " + rule.getName());
        }

        scoringRules.add(rule);

        // 添加到维度分组
        String dimension = rule.getType();
        rulesByDimension.computeIfAbsent(dimension, k -> new ArrayList<>()).add(rule);

        log.info("添加评分规则: {} (类型={}, 权重={})",
            rule.getName(), rule.getType(), rule.getWeight());
    }

    /**
     * 移除评分规则
     */
    public void removeScoringRule(String ruleName) {
        scoringRules.removeIf(rule -> rule.getName().equals(ruleName));

        // 从分组中移除
        rulesByDimension.values().forEach(rules ->
            rules.removeIf(rule -> rule.getName().equals(ruleName)));

        log.info("移除评分规则: {}", ruleName);
    }

    /**
     * 获取指定维度的规则
     */
    public List<ScoringRule> getRulesByDimension(String dimension) {
        return rulesByDimension.getOrDefault(dimension, Collections.emptyList());
    }

    /**
     * 获取所有启用的规则
     */
    public List<ScoringRule> getEnabledRules() {
        return scoringRules.stream()
            .filter(ScoringRule::isEnabled)
            .toList();
    }

    // ==================== 验证方法 ====================

    /**
     * 验证权重总和
     */
    public boolean validateWeights() {
        double total = dimensionWeights.values().stream()
            .mapToDouble(Double::doubleValue)
            .sum();

        boolean valid = Math.abs(total - 1.0) < 0.001;

        if (!valid) {
            log.warn("维度权重总和不为1.0: {}", total);
        }

        return valid;
    }

    /**
     * 验证配置完整性
     */
    public boolean validateConfig() {
        if (dimensionWeights.isEmpty()) {
            log.error("至少需要一个评分维度");
            return false;
        }

        if (!validateWeights()) {
            return false;
        }

        // 验证所有规则
        for (ScoringRule rule : scoringRules) {
            if (!rule.isValid()) {
                log.error("无效的规则: {}", rule.getName());
                return false;
            }
        }

        return true;
    }

    // ==================== 默认配置创建方法 ====================

    private static Map<String, Double> createDefaultDimensionWeights() {
        Map<String, Double> weights = new LinkedHashMap<>();
        weights.put("code_quality", 0.40);
        weights.put("innovation", 0.30);
        weights.put("completeness", 0.20);
        weights.put("documentation", 0.10);
        return weights;
    }

    private static Map<String, String> createDefaultDisplayNames() {
        Map<String, String> names = new HashMap<>();
        names.put("code_quality", "代码质量");
        names.put("innovation", "创新性");
        names.put("completeness", "完成度");
        names.put("documentation", "文档质量");
        return names;
    }

    private static Map<String, Object> createDefaultASTThresholds() {
        Map<String, Object> thresholds = new HashMap<>();
        thresholds.put("long_method", 50);
        thresholds.put("high_complexity", 10);
        thresholds.put("god_class_methods", 20);
        thresholds.put("god_class_fields", 15);
        thresholds.put("too_many_parameters", 5);
        return thresholds;
    }

    private static Map<String, Double> createDefaultComplexityThresholds() {
        Map<String, Double> thresholds = new HashMap<>();
        thresholds.put("excellent", 5.0);
        thresholds.put("good", 7.0);
        thresholds.put("medium", 10.0);
        thresholds.put("poor", 15.0);
        return thresholds;
    }

    private static Map<String, Integer> createDefaultCodeSmellPenalties() {
        Map<String, Integer> penalties = new HashMap<>();
        penalties.put("CRITICAL", 3);
        penalties.put("HIGH", 2);
        penalties.put("MEDIUM", 1);
        penalties.put("LOW", 0);
        return penalties;
    }

    private static Map<String, Integer> createDefaultDesignPatternBonus() {
        Map<String, Integer> bonus = new HashMap<>();
        bonus.put("CREATIONAL", 2);
        bonus.put("STRUCTURAL", 3);
        bonus.put("BEHAVIORAL", 3);
        bonus.put("ARCHITECTURAL", 4);
        bonus.put("COMBINATION", 5);
        return bonus;
    }

    // ==================== 工厂方法 ====================

    /**
     * 创建默认配置
     */
    public static HackathonScoringConfig createDefault() {
        HackathonScoringConfig config = HackathonScoringConfig.builder().build();

        // 添加默认规则
        config.addDefaultRules();

        return config;
    }

    /**
     * 添加默认评分规则
     */
    private void addDefaultRules() {
        // 代码质量规则
        addScoringRule(ScoringRule.builder()
            .name("code-quality-basic")
            .description("基础代码质量规则")
            .type("code_quality")
            .weight(1.0)
            .strategy("keyword_matching")
            .positiveKeywords(Map.of(
                "单元测试", 20,
                "注释", 10,
                "异常处理", 15
            ))
            .negativeKeywords(Map.of(
                "代码重复", -15,
                "长方法", -10
            ))
            .build());

        // 创新性规则
        addScoringRule(ScoringRule.builder()
            .name("innovation-basic")
            .description("创新性评分规则")
            .type("innovation")
            .weight(1.0)
            .strategy("keyword_matching")
            .positiveKeywords(Map.of(
                "AI", 15,
                "机器学习", 15,
                "区块链", 10
            ))
            .build());
    }

    /**
     * 从配置文件加载
     * 支持YAML和JSON格式
     *
     * @param configPath 配置文件路径
     * @return 黑客松评分配置对象
     */
    public static HackathonScoringConfig loadFromFile(String configPath) {
        log.info("从配置文件加载评分配置: {}", configPath);

        Path path = Paths.get(configPath);
        if (!Files.exists(path)) {
            log.warn("配置文件不存在: {}, 使用默认配置", configPath);
            return createDefault();
        }

        // 根据文件扩展名选择解析器
        if (!configPath.endsWith(".yaml") && !configPath.endsWith(".yml") && !configPath.endsWith(".json")) {
            throw new IllegalArgumentException("不支持的配置文件格式: " + configPath + "。支持的格式: .yaml, .yml, .json");
        }

        try {
            String content = Files.readString(path);

            if (configPath.endsWith(".yaml") || configPath.endsWith(".yml")) {
                return loadFromYaml(content);
            } else {
                return loadFromJson(content);
            }
        } catch (IOException e) {
            log.error("加载配置文件失败: {}", configPath, e);
            throw new RuntimeException("加载配置文件失败: " + configPath, e);
        } catch (Exception e) {
            log.error("解析配置文件失败: {}", configPath, e);
            throw new RuntimeException("解析配置文件失败: " + configPath, e);
        }
    }

    /**
     * 从YAML内容加载配置
     */
    private static HackathonScoringConfig loadFromYaml(String content) throws IOException {
        log.debug("解析YAML配置文件");
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        HackathonScoringConfigDto dto = mapper.readValue(content, HackathonScoringConfigDto.class);
        return convertFromDto(dto);
    }

    /**
     * 从JSON内容加载配置
     */
    private static HackathonScoringConfig loadFromJson(String content) throws IOException {
        log.debug("解析JSON配置文件");
        ObjectMapper mapper = new ObjectMapper();
        HackathonScoringConfigDto dto = mapper.readValue(content, HackathonScoringConfigDto.class);
        return convertFromDto(dto);
    }

    /**
     * 将DTO转换为领域模型
     */
    private static HackathonScoringConfig convertFromDto(HackathonScoringConfigDto dto) {
        if (dto == null || dto.getScoring() == null) {
            log.warn("配置文件内容为空，使用默认配置");
            return createDefault();
        }

        HackathonScoringConfigDto.ScoringDto scoring = dto.getScoring();

        // 构建配置对象
        HackathonScoringConfig config = HackathonScoringConfig.builder()
                .dimensionWeights(new LinkedHashMap<>())
                .dimensionDisplayNames(new HashMap<>())
                .dimensionDescriptions(new HashMap<>())
                .scoringRules(new ArrayList<>())
                .rulesByDimension(new HashMap<>())
                .build();

        // 转换维度配置
        if (scoring.getDimensions() != null) {
            for (Map.Entry<String, HackathonScoringConfigDto.DimensionDto> entry : scoring.getDimensions().entrySet()) {
                String dimensionKey = entry.getKey();
                HackathonScoringConfigDto.DimensionDto dimension = entry.getValue();

                // 检查是否启用（默认为true）
                boolean enabled = dimension.getEnabled() == null || dimension.getEnabled();
                if (!enabled) {
                    log.debug("维度 {} 已禁用，跳过", dimensionKey);
                    continue;
                }

                if (dimension.getWeight() != null) {
                    config.getDimensionWeights().put(dimensionKey, dimension.getWeight());
                }
                if (dimension.getDisplayName() != null) {
                    config.getDimensionDisplayNames().put(dimensionKey, dimension.getDisplayName());
                }
                if (dimension.getDescription() != null) {
                    config.getDimensionDescriptions().put(dimensionKey, dimension.getDescription());
                }
            }
        }

        // 转换规则配置
        if (scoring.getRules() != null) {
            for (HackathonScoringConfigDto.RuleDto ruleDto : scoring.getRules()) {
                // 检查是否启用（默认为true）
                boolean enabled = ruleDto.getEnabled() == null || ruleDto.getEnabled();

                // 跳过禁用的规则
                if (!enabled) {
                    log.debug("规则 {} 已禁用，跳过", ruleDto.getName());
                    continue;
                }

                ScoringRule.ScoringRuleBuilder ruleBuilder = ScoringRule.builder()
                        .name(ruleDto.getName())
                        .type(ruleDto.getType())
                        .weight(ruleDto.getWeight() != null ? ruleDto.getWeight() : 1.0)
                        .strategy(ruleDto.getStrategy())
                        .description(ruleDto.getDescription())
                        .enabled(enabled);

                if (ruleDto.getPositiveKeywords() != null) {
                    ruleBuilder.positiveKeywords(ruleDto.getPositiveKeywords());
                }
                if (ruleDto.getNegativeKeywords() != null) {
                    ruleBuilder.negativeKeywords(ruleDto.getNegativeKeywords());
                }

                config.addScoringRule(ruleBuilder.build());
            }
        }

        // 转换AST分析配置
        if (scoring.getAstAnalysis() != null) {
            HackathonScoringConfigDto.AstAnalysisDto astDto = scoring.getAstAnalysis();
            if (astDto.getEnabled() != null) {
                config.setEnableASTAnalysis(astDto.getEnabled());
            }
            if (astDto.getThresholds() != null) {
                config.setAstThresholds(new HashMap<>(astDto.getThresholds()));
            }
        }

        // 使用默认的复杂度阈值和代码坏味道扣分
        config.setComplexityThresholds(createDefaultComplexityThresholds());
        config.setCodeSmellPenalties(createDefaultCodeSmellPenalties());
        config.setDesignPatternBonus(createDefaultDesignPatternBonus());

        // 验证配置（仅警告，不强制要求权重为1.0）
        if (config.getDimensionWeights().isEmpty()) {
            log.error("配置文件验证失败：至少需要一个评分维度");
            throw new IllegalArgumentException("配置文件验证失败，至少需要一个评分维度");
        }

        // 验证权重总和（仅警告）
        if (!config.validateWeights()) {
            log.warn("注意：维度权重总和不为1.0，这可能影响评分结果");
        }

        log.info("配置文件加载成功: {} 个维度, {} 个规则",
                config.getDimensionWeights().size(),
                config.getScoringRules().size());

        return config;
    }
}

