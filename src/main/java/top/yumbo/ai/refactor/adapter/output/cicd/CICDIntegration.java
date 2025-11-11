package top.yumbo.ai.refactor.adapter.output.cicd;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.refactor.application.service.QualityGateEngine;
import top.yumbo.ai.refactor.domain.model.ReviewReport;

import java.util.HashMap;
import java.util.Map;

/**
 * CI/CD集成支持
 * 提供CI/CD友好的输出和控制
 */
@Slf4j
public class CICDIntegration {

    private final QualityGateEngine gateEngine;
    private boolean failOnQualityGate = true;
    private String outputFormat = "text";

    public CICDIntegration(QualityGateEngine gateEngine) {
        this.gateEngine = gateEngine;
    }

    /**
     * 运行CI/CD检查
     * @return 退出码 (0=成功, 1=失败)
     */
    public int runCICheck(ReviewReport report) {
        log.info("开始CI/CD质量检查: project={}", report.getProjectName());

        // 执行质量门禁检查
        QualityGateEngine.GateResult result = gateEngine.checkGates(report);

        // 输出结果
        outputResult(result);

        // 返回退出码
        if (result.isPassed()) {
            log.info("✅ CI检查通过");
            return 0;
        } else {
            log.warn("❌ CI检查失败");
            return failOnQualityGate ? 1 : 0;
        }
    }

    /**
     * 输出结果
     */
    private void outputResult(QualityGateEngine.GateResult result) {
        switch (outputFormat.toLowerCase()) {
            case "json":
                System.out.println(generateJsonOutput(result));
                break;
            case "github":
                System.out.println(generateGitHubOutput(result));
                break;
            case "gitlab":
                System.out.println(generateGitLabOutput(result));
                break;
            default:
                System.out.println(gateEngine.generateGateReport(result));
        }
    }

    /**
     * 生成JSON输出
     */
    private String generateJsonOutput(QualityGateEngine.GateResult result) {
        Map<String, Object> output = new HashMap<>();
        output.put("project", result.getProjectName());
        output.put("score", result.getOverallScore());
        output.put("passed", result.isPassed());
        output.put("failed_gates", result.getFailedGates().size());
        output.put("warnings", result.getWarnings().size());

        return com.alibaba.fastjson2.JSON.toJSONString(output,
                com.alibaba.fastjson2.JSONWriter.Feature.PrettyFormat);
    }

    /**
     * 生成GitHub Actions输出
     */
    private String generateGitHubOutput(QualityGateEngine.GateResult result) {
        StringBuilder sb = new StringBuilder();

        // GitHub Actions注解格式
        if (!result.isPassed()) {
            for (QualityGateEngine.FailedGate gate : result.getFailedGates()) {
                sb.append(String.format("::error title=%s::%s\n",
                        gate.getName(), gate.getReason()));
            }
        }

        for (QualityGateEngine.FailedGate warning : result.getWarnings()) {
            sb.append(String.format("::warning title=%s::%s\n",
                    warning.getName(), warning.getReason()));
        }

        // 摘要
        sb.append("\n## 质量门禁检查结果\n\n");
        sb.append(String.format("- 项目: %s\n", result.getProjectName()));
        sb.append(String.format("- 评分: %d/100\n", result.getOverallScore()));
        sb.append(String.format("- 结果: %s\n", result.isPassed() ? "✅ 通过" : "❌ 失败"));

        return sb.toString();
    }

    /**
     * 生成GitLab CI输出
     */
    private String generateGitLabOutput(QualityGateEngine.GateResult result) {
        StringBuilder sb = new StringBuilder();

        sb.append("质量门禁检查结果\n");
        sb.append("==================\n\n");
        sb.append(String.format("项目: %s\n", result.getProjectName()));
        sb.append(String.format("评分: %d/100\n", result.getOverallScore()));
        sb.append(String.format("结果: %s\n\n", result.isPassed() ? "✅ 通过" : "❌ 失败"));

        if (!result.getFailedGates().isEmpty()) {
            sb.append("失败项:\n");
            for (QualityGateEngine.FailedGate gate : result.getFailedGates()) {
                sb.append(String.format("  - %s: %s\n", gate.getName(), gate.getReason()));
            }
        }

        return sb.toString();
    }

    /**
     * 生成状态徽章URL
     */
    public String generateBadgeUrl(QualityGateEngine.GateResult result) {
        String status = result.isPassed() ? "passing" : "failing";
        String color = result.isPassed() ? "success" : "critical";
        int score = result.getOverallScore();

        return String.format(
                "https://img.shields.io/badge/Quality%%20Score-%d%%2F100-%s?style=flat-square",
                score, getScoreColor(score)
        );
    }

    /**
     * 获取评分颜色
     */
    private String getScoreColor(int score) {
        if (score >= 90) return "brightgreen";
        if (score >= 80) return "green";
        if (score >= 70) return "yellow";
        if (score >= 60) return "orange";
        return "red";
    }

    /**
     * 设置失败时退出码
     */
    public void setFailOnQualityGate(boolean failOnQualityGate) {
        this.failOnQualityGate = failOnQualityGate;
    }

    /**
     * 设置输出格式
     */
    public void setOutputFormat(String format) {
        this.outputFormat = format;
    }

    /**
     * 从环境变量检测CI平台
     */
    public static String detectCIPlatform() {
        if (System.getenv("GITHUB_ACTIONS") != null) {
            return "github";
        } else if (System.getenv("GITLAB_CI") != null) {
            return "gitlab";
        } else if (System.getenv("JENKINS_URL") != null) {
            return "jenkins";
        } else if (System.getenv("CIRCLECI") != null) {
            return "circleci";
        }
        return "unknown";
    }

    /**
     * 创建默认CI集成实例
     */
    public static CICDIntegration createDefault() {
        QualityGateEngine engine = new QualityGateEngine();
        CICDIntegration integration = new CICDIntegration(engine);

        // 自动检测CI平台并设置输出格式
        String platform = detectCIPlatform();
        if ("github".equals(platform) || "gitlab".equals(platform)) {
            integration.setOutputFormat(platform);
        }

        return integration;
    }
}

