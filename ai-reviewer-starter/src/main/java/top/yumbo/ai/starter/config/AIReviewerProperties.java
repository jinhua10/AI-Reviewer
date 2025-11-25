package top.yumbo.ai.starter.config;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import top.yumbo.ai.api.model.AIConfig;

import java.util.ArrayList;
import java.util.List;
/**
 * Configuration properties for AI Reviewer
 */
@Data
@ConfigurationProperties(prefix = "ai-reviewer")
public class AIReviewerProperties {
    private Scanner scanner = new Scanner();
    private Parser parser = new Parser();
    private AIConfig ai = new AIConfig();
    private Processor processor = new Processor();
    private Executor executor = new Executor();
    @Data
    public static class Scanner {
        private List<String> includePatterns;
        private List<String> excludePatterns;
        private String maxFileSize;
    }

    @Data
    public static class Parser {
        private List<String> enabledParsers;
    }

    @Data
    public static class Processor {
        private String type;
        private String outputFormat;
        private String outputPath;
    }

    @Data
    public static class Executor {
        private Integer threadPoolSize;
        private Integer maxQueueSize;
    }

    @Data
    public static class Batch {
        private Integer threadPoolSize = 4;
        private String tempExtractDir = "./temp/extracted-projects";
        private String downloadScriptPath = "/home/jinhua/AI-Reviewer/download";
        private Integer scanIntervalMinutes = 2;
    }

    private Batch batch = new Batch();
}
