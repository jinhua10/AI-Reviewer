package top.yumbo.ai.reviewer.adapter.processor;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.reviewer.application.service.FileProcessingStrategy;
import top.yumbo.ai.reviewer.domain.model.SourceFile;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文档文件处理策略
 * 支持 PDF, Word, Markdown, Text 等格式
 *
 * @author AI-Reviewer Team
 * @version 2.0
 */
@Slf4j
public class DocumentProcessingStrategy implements FileProcessingStrategy {

    @Override
    public boolean supports(SourceFile file) {
        return file.getMainCategory() == SourceFile.FileMainCategory.DOCUMENT;
    }

    @Override
    public ProcessingResult process(SourceFile file) {
        log.info("处理文档文件: {} ({})", file.getFileName(), file.getFileType());

        try {
            // 1. 提取文本内容
            DocumentContent content = extractContent(file);

            // 2. 分析文档结构
            DocumentStructure structure = analyzeStructure(content);

            // 3. 质量评估
            DocumentQuality quality = assessQuality(content, structure);

            DocumentAnalysis analysis = DocumentAnalysis.builder()
                    .content(content)
                    .structure(structure)
                    .quality(quality)
                    .build();

            // 将分析结果存储到文件对象
            file.setAnalysisResult(analysis);

            log.debug("文档分析完成: {} - 字数: {}, 章节: {}, 质量分: {}",
                    file.getFileName(),
                    content.getWordCount(),
                    structure.getSectionCount(),
                    quality.getScore());

            return ProcessingResult.success(file, analysis);

        } catch (Exception e) {
            log.error("文档处理失败: {}", file.getFileName(), e);
            return ProcessingResult.failure(file, e);
        }
    }

    /**
     * 提取文档内容
     */
    private DocumentContent extractContent(SourceFile file) throws Exception {
        String text;

        // 根据文件类型提取内容
        if (file.getFileType() == SourceFile.FileType.PDF) {
            // TODO: 集成 Apache PDFBox
            text = file.getContent();
            log.warn("PDF 解析暂未实现，使用原始内容");
        } else if (file.getFileType() == SourceFile.FileType.WORD) {
            // TODO: 集成 Apache POI
            text = file.getContent();
            log.warn("Word 解析暂未实现，使用原始内容");
        } else {
            // Markdown, Text 等文本文件
            if (file.getContent() != null) {
                text = file.getContent();
            } else {
                text = Files.readString(file.getPath());
            }
        }

        int wordCount = countWords(text);
        int characterCount = text.length();
        int lineCount = text.split("\n").length;

        return DocumentContent.builder()
                .text(text)
                .wordCount(wordCount)
                .characterCount(characterCount)
                .lineCount(lineCount)
                .build();
    }

    /**
     * 分析文档结构
     */
    private DocumentStructure analyzeStructure(DocumentContent content) {
        String text = content.getText();

        // 检测章节标题
        List<Section> sections = detectSections(text);

        // 检测列表
        int listCount = countLists(text);

        // 检测代码块
        int codeBlockCount = countCodeBlocks(text);

        // 检测链接
        int linkCount = countLinks(text);

        return DocumentStructure.builder()
                .sections(sections)
                .sectionCount(sections.size())
                .listCount(listCount)
                .codeBlockCount(codeBlockCount)
                .linkCount(linkCount)
                .hasTableOfContents(sections.size() >= 3)
                .build();
    }

    /**
     * 检测章节
     */
    private List<Section> detectSections(String text) {
        List<Section> sections = new ArrayList<>();
        String[] lines = text.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            // Markdown 标题 (#, ##, ###, ...)
            if (line.startsWith("#")) {
                int level = 0;
                while (level < line.length() && line.charAt(level) == '#') {
                    level++;
                }
                String title = line.substring(level).trim();
                sections.add(new Section(level, title, i + 1));
            }
        }

        return sections;
    }

    /**
     * 统计列表数量
     */
    private int countLists(String text) {
        Pattern pattern = Pattern.compile("^\\s*[-*+]\\s+.+", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /**
     * 统计代码块数量
     */
    private int countCodeBlocks(String text) {
        Pattern pattern = Pattern.compile("```[\\s\\S]*?```");
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /**
     * 统计链接数量
     */
    private int countLinks(String text) {
        Pattern pattern = Pattern.compile("\\[([^\\]]+)\\]\\(([^)]+)\\)");
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /**
     * 统计单词数
     */
    private int countWords(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        String[] words = text.trim().split("\\s+");
        return words.length;
    }

    /**
     * 评估文档质量
     */
    private DocumentQuality assessQuality(DocumentContent content, DocumentStructure structure) {
        int score = 100;
        List<String> issues = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        // 1. 长度检查
        int wordCount = content.getWordCount();
        if (wordCount < 100) {
            score -= 20;
            issues.add(String.format("内容过短 (%d words)", wordCount));
            suggestions.add("建议增加更多内容以提供完整信息");
        }

        // 2. 结构检查
        if (structure.getSectionCount() == 0) {
            score -= 15;
            issues.add("缺少章节结构");
            suggestions.add("建议添加标题和章节以改善可读性");
        }

        // 3. 可读性检查
        int avgWordsPerLine = wordCount / Math.max(1, content.getLineCount());
        if (avgWordsPerLine > 30) {
            score -= 10;
            issues.add("行过长，可读性差");
            suggestions.add("建议使用更短的段落以提高可读性");
        }

        // 4. 格式检查
        if (structure.getCodeBlockCount() > 0 && structure.getListCount() == 0) {
            suggestions.add("考虑使用列表来组织要点");
        }

        String grade = calculateGrade(score);

        return DocumentQuality.builder()
                .score(score)
                .grade(grade)
                .issues(issues)
                .suggestions(suggestions)
                .isValid(score >= 60)
                .build();
    }

    private String calculateGrade(int score) {
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "F";
    }

    // ========== 数据模型 ==========

    @Data
    @Builder
    public static class DocumentContent {
        private String text;
        private int wordCount;
        private int characterCount;
        private int lineCount;
    }

    @Data
    @Builder
    public static class DocumentStructure {
        private List<Section> sections;
        private int sectionCount;
        private int listCount;
        private int codeBlockCount;
        private int linkCount;
        private boolean hasTableOfContents;
    }

    @Data
    @Builder
    public static class DocumentQuality {
        private int score;
        private String grade;
        private List<String> issues;
        private List<String> suggestions;
        private boolean isValid;
    }

    @Data
    @Builder
    public static class DocumentAnalysis {
        private DocumentContent content;
        private DocumentStructure structure;
        private DocumentQuality quality;
    }

    public record Section(int level, String title, int lineNumber) {
    }
}

