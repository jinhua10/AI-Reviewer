
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class FixStringEscapes {
    public static void main(String[] args) throws IOException {
        // 修复ProjectAnalyzer.java
        Path projectAnalyzerPath = Paths.get("D:/Jetbrains/hackathon/AI-Reviewer/src/main/java/top/yumbo/ai/reviewer/ProjectAnalyzer.java");
        List<String> projectAnalyzerLines = Files.readAllLines(projectAnalyzerPath);

        for (int i = 0; i < projectAnalyzerLines.size(); i++) {
            String line = projectAnalyzerLines.get(i);
            // 修复未转义的换行符
            if (line.contains("append("") && line.contains(" ") && !line.contains("\\n")) {
                line = line.replace(" ", "\\n");
                projectAnalyzerLines.set(i, line);
            }
        }

        Files.write(projectAnalyzerPath, projectAnalyzerLines);

        // 修复ReportBuilder.java
        Path reportBuilderPath = Paths.get("D:/Jetbrains/hackathon/AI-Reviewer/src/main/java/top/yumbo/ai/reviewer/report/ReportBuilder.java");
        List<String> reportBuilderLines = Files.readAllLines(reportBuilderPath);

        for (int i = 0; i < reportBuilderLines.size(); i++) {
            String line = reportBuilderLines.get(i);
            // 修复未转义的换行符
            if (line.contains("append("") && line.contains(" ") && !line.contains("\\n")) {
                line = line.replace(" ", "\\n");
                reportBuilderLines.set(i, line);
            }
        }

        Files.write(reportBuilderPath, reportBuilderLines);

        System.out.println("修复完成!");
    }
}
