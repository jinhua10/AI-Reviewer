package top.yumbo.ai.reviewer.domain.hackathon.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 黑客松评分配置文件加载测试
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-14
 */
@DisplayName("黑客松评分配置文件加载测试")
class HackathonScoringConfigLoaderTest {

    @Test
    @DisplayName("测试从YAML文件加载配置")
    void testLoadFromYamlFile() {
        // 使用示例配置文件
        String configPath = "src/main/resources/hackathon-scoring-config-example.yaml";

        HackathonScoringConfig config = HackathonScoringConfig.loadFromFile(configPath);

        // 验证基本信息
        assertNotNull(config);
        assertNotNull(config.getDimensionWeights());
        assertNotNull(config.getScoringRules());

        // 验证维度配置
        assertEquals(0.40, config.getDimensionWeight("code_quality"), 0.001);
        assertEquals(0.30, config.getDimensionWeight("innovation"), 0.001);
        assertEquals(0.20, config.getDimensionWeight("completeness"), 0.001);
        assertEquals(0.10, config.getDimensionWeight("documentation"), 0.001);

        // 验证维度显示名称
        assertEquals("代码质量", config.getDimensionDisplayName("code_quality"));
        assertEquals("创新性", config.getDimensionDisplayName("innovation"));

        // 验证规则数量
        assertTrue(config.getScoringRules().size() >= 3, "应该至少有3个评分规则");

        // 验证AST分析是否启用
        assertTrue(config.isEnableASTAnalysis());

        // 验证AST阈值
        assertNotNull(config.getAstThresholds());
        assertTrue(config.getAstThresholds().containsKey("long_method"));

        // 验证配置有效性
        assertTrue(config.validateConfig(), "配置应该通过验证");

        System.out.println("✅ YAML配置文件加载成功");
        System.out.println("维度数量: " + config.getDimensionWeights().size());
        System.out.println("规则数量: " + config.getScoringRules().size());
    }

    @Test
    @DisplayName("测试从JSON文件加载配置")
    void testLoadFromJsonFile() {
        // 使用示例配置文件
        String configPath = "src/main/resources/hackathon-scoring-config-example.json";

        HackathonScoringConfig config = HackathonScoringConfig.loadFromFile(configPath);

        // 验证基本信息
        assertNotNull(config);
        assertNotNull(config.getDimensionWeights());
        assertNotNull(config.getScoringRules());

        // 验证维度配置
        assertEquals(0.40, config.getDimensionWeight("code_quality"), 0.001);
        assertEquals(0.30, config.getDimensionWeight("innovation"), 0.001);

        // 验证规则数量
        assertTrue(config.getScoringRules().size() >= 2, "应该至少有2个评分规则");

        // 验证AST分析是否启用
        assertTrue(config.isEnableASTAnalysis());

        // 验证配置有效性
        assertTrue(config.validateConfig(), "配置应该通过验证");

        System.out.println("✅ JSON配置文件加载成功");
        System.out.println("维度数量: " + config.getDimensionWeights().size());
        System.out.println("规则数量: " + config.getScoringRules().size());
    }

    @Test
    @DisplayName("测试不存在的配置文件返回默认配置")
    void testLoadFromNonExistentFile() {
        String configPath = "non-existent-config.yaml";

        HackathonScoringConfig config = HackathonScoringConfig.loadFromFile(configPath);

        // 应该返回默认配置
        assertNotNull(config);
        assertTrue(config.getDimensionWeights().size() > 0);
        assertTrue(config.getScoringRules().size() > 0);

        System.out.println("✅ 不存在的文件正确返回默认配置");
    }

    @Test
    @DisplayName("测试不支持的文件格式抛出异常")
    void testLoadFromUnsupportedFormat(@TempDir Path tempDir) throws IOException {
        // 创建一个.txt文件
        Path txtFile = tempDir.resolve("config.txt");
        Files.writeString(txtFile, "invalid content");

        // 应该抛出IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            HackathonScoringConfig.loadFromFile(txtFile.toString());
        });

        System.out.println("✅ 不支持的格式正确抛出异常");
    }

    @Test
    @DisplayName("测试从YAML字符串加载完整配置")
    void testLoadCompleteYamlConfig(@TempDir Path tempDir) throws IOException {
        String yamlContent = """
            scoring:
              dimensions:
                code_quality:
                  weight: 0.40
                  display_name: "代码质量"
                  description: "评估代码质量"
                  enabled: true
                innovation:
                  weight: 0.30
                  display_name: "创新性"
                  enabled: true
                completeness:
                  weight: 0.20
                  display_name: "完成度"
                  enabled: true
                documentation:
                  weight: 0.10
                  display_name: "文档"
                  enabled: true
              rules:
                - name: "test-rule"
                  type: "code_quality"
                  weight: 1.0
                  strategy: "keyword_matching"
                  enabled: true
                  positive_keywords:
                    "测试": 10
              ast_analysis:
                enabled: true
                thresholds:
                  long_method: 50
            """;

        // 写入临时文件
        Path configFile = tempDir.resolve("test-config.yaml");
        Files.writeString(configFile, yamlContent);

        // 加载配置
        HackathonScoringConfig config = HackathonScoringConfig.loadFromFile(configFile.toString());

        // 验证
        assertNotNull(config);
        assertEquals(4, config.getDimensionWeights().size());
        assertEquals(1, config.getScoringRules().size());
        assertTrue(config.isEnableASTAnalysis());

        // 验证权重总和为1.0
        assertTrue(config.validateWeights());

        System.out.println("✅ 完整YAML配置加载并验证成功");
    }

    @Test
    @DisplayName("测试禁用的维度不被加载")
    void testDisabledDimensionsNotLoaded(@TempDir Path tempDir) throws IOException {
        String yamlContent = """
            scoring:
              dimensions:
                code_quality:
                  weight: 0.50
                  display_name: "代码质量"
                  enabled: true
                innovation:
                  weight: 0.30
                  display_name: "创新性"
                  enabled: false
                completeness:
                  weight: 0.20
                  display_name: "完成度"
                  enabled: true
              rules: []
              ast_analysis:
                enabled: true
                thresholds: {}
            """;

        Path configFile = tempDir.resolve("disabled-test.yaml");
        Files.writeString(configFile, yamlContent);

        HackathonScoringConfig config = HackathonScoringConfig.loadFromFile(configFile.toString());

        // innovation维度应该被跳过
        assertEquals(2, config.getDimensionWeights().size());
        assertFalse(config.getDimensionWeights().containsKey("innovation"));
        assertTrue(config.getDimensionWeights().containsKey("code_quality"));
        assertTrue(config.getDimensionWeights().containsKey("completeness"));

        // 注意：由于禁用了维度，权重不为1.0是正常的，这里不验证权重总和

        System.out.println("✅ 禁用的维度正确被跳过");
    }

    @Test
    @DisplayName("测试禁用的规则不被加载")
    void testDisabledRulesNotLoaded(@TempDir Path tempDir) throws IOException {
        String yamlContent = """
            scoring:
              dimensions:
                code_quality:
                  weight: 1.0
                  enabled: true
              rules:
                - name: "enabled-rule"
                  type: "code_quality"
                  weight: 1.0
                  strategy: "keyword_matching"
                  enabled: true
                - name: "disabled-rule"
                  type: "code_quality"
                  weight: 1.0
                  strategy: "keyword_matching"
                  enabled: false
              ast_analysis:
                enabled: true
                thresholds: {}
            """;

        Path configFile = tempDir.resolve("disabled-rules-test.yaml");
        Files.writeString(configFile, yamlContent);

        HackathonScoringConfig config = HackathonScoringConfig.loadFromFile(configFile.toString());

        // 只有启用的规则被加载
        assertEquals(1, config.getScoringRules().size());
        assertEquals("enabled-rule", config.getScoringRules().get(0).getName());
        assertTrue(config.getScoringRules().get(0).isEnabled());

        System.out.println("✅ 禁用的规则正确被跳过");
    }
}

