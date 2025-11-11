package top.yumbo.ai.reviewer;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.reviewer.config.Config;
import top.yumbo.ai.reviewer.entity.AnalysisResult;
import top.yumbo.ai.reviewer.exception.AnalysisException;
import top.yumbo.ai.reviewer.report.ReportBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Hackathon 源码评分工具
 * 专为黑客松比赛设计的快速评分工具
 */
@Slf4j
public class HackathonReviewer {

    private final AIReviewer reviewer;
    private final ReportBuilder reportBuilder;

    public HackathonReviewer() throws IOException {
        // 加载Hackathon专用配置
        Config config = Config.loadFromFile("hackathon-config.yaml");
        this.reviewer = new AIReviewer(config);
        this.reportBuilder = new ReportBuilder();
    }

    /**
     * 快速评分单个项目
     */
    public HackathonScore quickScore(String projectPath) throws AnalysisException {
        log.info("开始快速评分项目: {}", projectPath);

        Path projectRoot = Paths.get(projectPath);
        if (!Files.exists(projectRoot)) {
            throw new AnalysisException("项目路径不存在: " + projectPath);
        }

        // 执行快速分析
        AnalysisResult result = reviewer.analyzeProject(projectPath);

        // 转换为Hackathon评分
        return convertToHackathonScore(result);
    }

    /**
     * 批量评分多个项目
     */
    public List<HackathonScore> batchScore(List<String> projectPaths) throws AnalysisException {
        log.info("开始批量评分 {} 个项目", projectPaths.size());

        return projectPaths.parallelStream()
                .map(path -> {
                    try {
                        return quickScore(path);
                    } catch (Exception e) {
                        log.error("评分项目失败: {}", path, e);
                        return createErrorScore(path, e.getMessage());
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * 生成Hackathon评审报告
     */
    public void generateReport(HackathonScore score, String outputPath) throws AnalysisException {
        try {
            // 使用Hackathon专用模板生成报告
            reportBuilder.saveReport(score.getOriginalResult(), outputPath, "hackathon");
            log.info("Hackathon评审报告已生成: {}", outputPath);
        } catch (Exception e) {
            throw new AnalysisException("生成报告失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成排行榜
     */
    public String generateLeaderboard(List<HackathonScore> scores) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 🏆 Hackathon 排行榜\n\n");

        final int[] rank = {1};
        scores.stream()
                .sorted((a, b) -> Double.compare(b.getTotalScore(), a.getTotalScore()))
                .limit(10)
                .forEach(score -> {
                    sb.append(String.format("%d. **%s** - %.1f分 (%s)\n",
                            rank[0]++,
                            score.getProjectName(),
                            score.getTotalScore(),
                            score.getJudgeStatus()));
                });

        return sb.toString();
    }

    private HackathonScore convertToHackathonScore(AnalysisResult result) {
        HackathonScore score = new HackathonScore();
        score.setProjectName(result.getProjectName());
        score.setOriginalResult(result);

        // 计算Hackathon专用评分
        double architecture = result.getArchitectureScore() * 0.15;
        double codeQuality = result.getCodeQualityScore() * 0.20;
        double technicalDebt = Math.max(0, 100 - result.getTechnicalDebtScore()) * 0.10;
        double functionality = result.getFunctionalityScore() * 0.25;
        double businessValue = result.getBusinessValueScore() * 0.20;
        double testCoverage = result.getTestCoverageScore() * 0.10;

        double totalScore = architecture + codeQuality + technicalDebt +
                           functionality + businessValue + testCoverage;

        score.setTotalScore(totalScore);
        score.setArchitectureScore(result.getArchitectureScore());
        score.setCodeQualityScore(result.getCodeQualityScore());
        score.setFunctionalityScore(result.getFunctionalityScore());
        score.setBusinessValueScore(result.getBusinessValueScore());
        score.setTestCoverageScore(result.getTestCoverageScore());

        // 确定评审状态
        if (totalScore >= 85) {
            score.setJudgeStatus("🏆 优秀 - 进入决赛");
        } else if (totalScore >= 70) {
            score.setJudgeStatus("🥈 良好 - 晋级复赛");
        } else if (totalScore >= 50) {
            score.setJudgeStatus("🥉 及格 - 基础奖项");
        } else {
            score.setJudgeStatus("📜 参与奖");
        }

        return score;
    }

    private HackathonScore createErrorScore(String projectPath, String errorMessage) {
        HackathonScore score = new HackathonScore();
        score.setProjectName(Paths.get(projectPath).getFileName().toString());
        score.setTotalScore(0.0);
        score.setJudgeStatus("❌ 评分失败: " + errorMessage);
        return score;
    }

    /**
     * 获取项目的核心文件列表
     */
    private List<Path> getCoreFiles(Path projectRoot) throws AnalysisException {
        try {
            return Files.walk(projectRoot)
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String fileName = path.getFileName().toString().toLowerCase();
                        return fileName.endsWith(".java") ||
                               fileName.endsWith(".py") ||
                               fileName.endsWith(".js") ||
                               fileName.endsWith(".ts") ||
                               fileName.endsWith(".html") ||
                               fileName.endsWith(".css") ||
                               fileName.contains("readme") ||
                               fileName.contains("main") ||
                               fileName.contains("app");
                    })
                    .limit(50) // 限制文件数量
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new AnalysisException("获取项目文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * Hackathon评分结果类
     */
    public static class HackathonScore {
        private String projectName;
        private double totalScore;
        private double architectureScore;
        private double codeQualityScore;
        private double functionalityScore;
        private double businessValueScore;
        private double testCoverageScore;
        private String judgeStatus;
        private AnalysisResult originalResult;

        // Getters and setters
        public String getProjectName() { return projectName; }
        public void setProjectName(String projectName) { this.projectName = projectName; }

        public double getTotalScore() { return totalScore; }
        public void setTotalScore(double totalScore) { this.totalScore = totalScore; }

        public double getArchitectureScore() { return architectureScore; }
        public void setArchitectureScore(double architectureScore) { this.architectureScore = architectureScore; }

        public double getCodeQualityScore() { return codeQualityScore; }
        public void setCodeQualityScore(double codeQualityScore) { this.codeQualityScore = codeQualityScore; }

        public double getFunctionalityScore() { return functionalityScore; }
        public void setFunctionalityScore(double functionalityScore) { this.functionalityScore = functionalityScore; }

        public double getBusinessValueScore() { return businessValueScore; }
        public void setBusinessValueScore(double businessValueScore) { this.businessValueScore = businessValueScore; }

        public double getTestCoverageScore() { return testCoverageScore; }
        public void setTestCoverageScore(double testCoverageScore) { this.testCoverageScore = testCoverageScore; }

        public String getJudgeStatus() { return judgeStatus; }
        public void setJudgeStatus(String judgeStatus) { this.judgeStatus = judgeStatus; }

        public AnalysisResult getOriginalResult() { return originalResult; }
        public void setOriginalResult(AnalysisResult originalResult) { this.originalResult = originalResult; }

        @Override
        public String toString() {
            return String.format("%s: %.1f分 (%s)",
                    projectName, totalScore, judgeStatus);
        }
    }
}
