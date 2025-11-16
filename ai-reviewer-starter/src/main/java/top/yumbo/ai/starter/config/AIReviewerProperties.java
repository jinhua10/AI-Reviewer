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
        private List<String> includePatterns = new ArrayList<>();
        private List<String> excludePatterns = new ArrayList<>();
        private String maxFileSize = "10MB";
    }
    @Data
    public static class Parser {
        private List<String> enabledParsers = List.of("java", "text");
    }

    @Data
    public static class Processor {
        private String type = "code-review";
        private String outputFormat = "markdown";
        private String outputPath = "./reports";
    }
    @Data
    public static class Executor {
        private Integer threadPoolSize = 10;
        private Integer maxQueueSize = 100;
    }
}
