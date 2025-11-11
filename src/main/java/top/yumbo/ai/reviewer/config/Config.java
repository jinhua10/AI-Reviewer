package top.yumbo.ai.reviewer.config;

import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * 配置管理类
 * 
 * 负责管理分析过程中的所有配置参数
 */
@Data
@Builder
public class Config {

    // 项目路径
    @Builder.Default
    private String projectPath = ".";

    // 输出目录
    @Builder.Default
    private String outputDir = "ai_reviewer_output";

    // AI 平台
    @Builder.Default
    private String aiPlatform = "deepseek";

    // API 密钥
    private String apiKey;

    // 模型名称
    @Builder.Default
    private String model = "deepseek-chat";

    // 最大 token 数
    @Builder.Default
    private int maxTokens = 4096;

    // 并发数
    @Builder.Default
    private int concurrency = 3;

    // 重试次数
    @Builder.Default
    private int retryCount = 3;

    // 分块大小
    @Builder.Default
    private int chunkSize = 8000;

    // 包含模式
    @Builder.Default
    private List<String> includePatterns = Arrays.asList("*.java", "*.py", "*.js", "*.ts", "*.go", "*.cpp", "*.c", "*.h");

    // 排除模式
    @Builder.Default
    private List<String> excludePatterns = Arrays.asList("test", "build", "target", "node_modules", ".git", ".idea", ".vscode");

    // 是否启用缓存
    @Builder.Default
    private boolean enableCache = true;

    // 报告格式
    @Builder.Default
    private List<String> reportFormats = Arrays.asList("markdown", "json");

    // 项目结构深度
    @Builder.Default
    private int treeDepth = 4;

    // 是否包含测试文件
    @Builder.Default
    private boolean includeTests = false;

    // Top K 选择
    @Builder.Default
    private int topK = -1;  // -1 表示不限制

    // 代码片段最大行数
    @Builder.Default
    private int snippetMaxLines = 200;
    
    // 每批最大字符数
    @Builder.Default
    private int maxCharsPerBatch = 100000;

    /**
     * 获取输出目录的 Path 对象
     * 
     * @return 输出目录的 Path 对象
     */
    public Path getOutputPath() {
        return Paths.get(outputDir);
    }

    /**
     * 获取项目路径的 Path 对象
     * 
     * @return 项目路径的 Path 对象
     */
    public Path getProjectPath() {
        return Paths.get(projectPath);
    }

    /**
     * 获取 API 密钥
     * 
     * @return API 密钥
     */
    public String getApiKey() {
        if (apiKey == null || apiKey.isEmpty()) {
            // 尝试从环境变量获取
            String envKey = System.getenv("DEEPSEEK_API_KEY");
            if (envKey != null && !envKey.isEmpty()) {
                return envKey;
            }
            throw new IllegalArgumentException("API 密钥未设置，请通过配置或环境变量 DEEPSEEK_API_KEY 设置");
        }
        return apiKey;
    }
}
