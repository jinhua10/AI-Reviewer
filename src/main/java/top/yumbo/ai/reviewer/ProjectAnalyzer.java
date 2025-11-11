package top.yumbo.ai.reviewer;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.reviewer.config.Config;
import top.yumbo.ai.reviewer.entity.AnalysisResult;
import top.yumbo.ai.reviewer.exception.AnalysisException;

import java.util.Arrays;

/**
 * 项目分析器 - 命令行工具入口
 */
@Slf4j
public class ProjectAnalyzer {

    public static void main(String[] args) {
        try {
            Arguments arguments = parseArguments(args);

            // 加载配置
            Config config = Config.loadFromFile(arguments.configPath);

            // 创建AI评审器
            AIReviewer reviewer = AIReviewer.builder()
                    .withConfig(config)
                    .build();

            // 执行分析
            AnalysisResult result = reviewer.analyzeProject(arguments.projectPath);

            // 输出结果
            System.out.println("=== 项目分析结果 ===");
            System.out.println("项目路径: " + arguments.projectPath);
            System.out.println("总体评分: " + result.getOverallScore() + "/100");
            System.out.println("架构评分: " + result.getArchitectureScore() + "/100");
            System.out.println("代码质量评分: " + result.getCodeQualityScore() + "/100");
            System.out.println("技术债务评分: " + result.getTechnicalDebtScore() + "/100");
            System.out.println("功能评分: " + result.getFunctionalityScore() + "/100");
            System.out.println("商业价值评分: " + result.getBusinessValueScore() + "/100");
            System.out.println("测试覆盖率评分: " + result.getTestCoverageScore() + "/100");

            System.out.println("\n=== 详细报告 ===");
            System.out.println(result.getSummaryReport().getContent());

            if (arguments.outputPath != null) {
                // 保存详细报告到文件
                saveReportToFile(result, arguments.outputPath);
                System.out.println("详细报告已保存到: " + arguments.outputPath);
            }

        } catch (Exception e) {
            log.error("分析失败", e);
            System.err.println("错误: " + e.getMessage());
            printUsage();
            System.exit(1);
        }
    }

    private static Arguments parseArguments(String[] args) throws AnalysisException {
        Arguments arguments = new Arguments();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--project":
                case "-p":
                    if (i + 1 < args.length) {
                        arguments.projectPath = args[++i];
                    } else {
                        throw new AnalysisException("缺少项目路径参数");
                    }
                    break;
                case "--config":
                case "-c":
                    if (i + 1 < args.length) {
                        arguments.configPath = args[++i];
                    } else {
                        throw new AnalysisException("缺少配置文件路径参数");
                    }
                    break;
                case "--output":
                case "-o":
                    if (i + 1 < args.length) {
                        arguments.outputPath = args[++i];
                    } else {
                        throw new AnalysisException("缺少输出文件路径参数");
                    }
                    break;
                case "--help":
                case "-h":
                    printUsage();
                    System.exit(0);
                    break;
                default:
                    throw new AnalysisException("未知参数: " + args[i]);
            }
        }

        if (arguments.projectPath == null) {
            throw new AnalysisException("必须指定项目路径 (--project)");
        }

        // 设��默认配置路径
        if (arguments.configPath == null) {
            arguments.configPath = "config.yaml";
        }

        return arguments;
    }

    private static void printUsage() {
        System.out.println("用法: java -jar ai-reviewer.jar [选项]");
        System.out.println("选项:");
        System.out.println("  -p, --project <路径>    要分析的项目根目录路径 (必需)");
        System.out.println("  -c, --config <文件>     配置文件路径 (默认: config.yaml)");
        System.out.println("  -o, --output <文件>     输出详细报告的文件路径 (可选)");
        System.out.println("  -h, --help              显示此帮助信息");
        System.out.println();
        System.out.println("示例:");
        System.out.println("  java -jar ai-reviewer.jar --project /path/to/project");
        System.out.println("  java -jar ai-reviewer.jar -p /path/to/project -c my-config.yaml -o report.md");
    }

    private static void saveReportToFile(AnalysisResult result, String outputPath) {
        // TODO: 实现保存报告到文件的功能
        log.info("保存报告到文件: {}", outputPath);
    }

    /**
     * 命令行参数类
     */
    static class Arguments {
        String projectPath;
        String configPath;
        String outputPath;
    }
}
