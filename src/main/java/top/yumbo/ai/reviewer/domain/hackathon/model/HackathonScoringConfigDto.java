package top.yumbo.ai.reviewer.domain.hackathon.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 黑客松评分配置数据传输对象
 * 用于从YAML/JSON文件加载配置
 * 使用JsonAlias支持下划线和驼峰两种命名风格
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-14
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HackathonScoringConfigDto {

    private ScoringDto scoring;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoringDto {
        private Map<String, DimensionDto> dimensions;
        private List<RuleDto> rules;

        @JsonAlias("ast_analysis")
        private AstAnalysisDto astAnalysis;

        @JsonAlias("file_type_configs")
        private Map<String, FileTypeConfigDto> fileTypeConfigs;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DimensionDto {
        private Double weight;

        @JsonAlias("display_name")
        private String displayName;

        private String description;
        private Boolean enabled;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleDto {
        private String name;
        private String type;
        private Double weight;
        private String strategy;
        private String description;

        @JsonAlias("positive_keywords")
        private Map<String, Integer> positiveKeywords;

        @JsonAlias("negative_keywords")
        private Map<String, Integer> negativeKeywords;

        private Boolean enabled;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AstAnalysisDto {
        private Boolean enabled;
        private Map<String, Object> thresholds;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileTypeConfigDto {
        private Boolean enabled;

        @JsonAlias("supported_formats")
        private List<String> supportedFormats;

        @JsonAlias("max_size_mb")
        private Integer maxSizeMb;

        @JsonAlias("quality_check")
        private QualityCheckDto qualityCheck;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QualityCheckDto {
        @JsonAlias("min_resolution")
        private List<Integer> minResolution;

        @JsonAlias("max_resolution")
        private List<Integer> maxResolution;

        @JsonAlias("min_duration_seconds")
        private Integer minDurationSeconds;

        @JsonAlias("max_duration_seconds")
        private Integer maxDurationSeconds;

        @JsonAlias("min_pages")
        private Integer minPages;

        @JsonAlias("max_pages")
        private Integer maxPages;
    }
}

