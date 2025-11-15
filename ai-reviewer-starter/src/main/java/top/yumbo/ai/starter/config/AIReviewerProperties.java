package top.yumbo.ai.starter.config;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
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
    private AI ai = new AI();
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
    public static class AI {
        private String provider = "bedrock";
        private String model = "arn:aws:bedrock:us-east-1:590184013141:inference-profile/us.anthropic.claude-sonnet-4-5-20250929-v1:0";
        private String region="us-west-1";
        private String apiKey;
        private String endpoint;
        private String sysPrompt;
        private String userPrompt;
        private Double temperature = 0.7;
        private Integer maxTokens = 2000;
        private Integer timeoutSeconds = 30;
        private Integer maxRetries = 3;
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
