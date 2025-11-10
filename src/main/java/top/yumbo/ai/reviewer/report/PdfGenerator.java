package top.yumbo.ai.reviewer.report;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF 生成器工具类
 * 使用 Apache PDFBox 生成 PDF 报告
 * 支持中文字体
 */
public class PdfGenerator {

    private static final Logger log = LoggerFactory.getLogger(PdfGenerator.class);

    private static final float MARGIN = 50;
    private static final float FONT_SIZE = 12;
    private static final float TITLE_FONT_SIZE = 18;
    private static final float HEADING_FONT_SIZE = 14;
    private static final float LINE_HEIGHT = 15;

    // 中文字体缓存
    private static PDType0Font chineseFont = null;
    private static boolean fontLoadAttempted = false;

    /**
     * 加载中文字体
     */
    private static PDType0Font loadChineseFont(PDDocument document) throws IOException {
        if (fontLoadAttempted) {
            return chineseFont;
        }

        fontLoadAttempted = true;

        // 常见中文字体路径（按优先级排序）
        String[] fontPaths = {
            // Windows 字体
            "C:/Windows/Fonts/msyh.ttc",      // 微软雅黑
            "C:/Windows/Fonts/simhei.ttf",    // 黑体
            "C:/Windows/Fonts/simsun.ttc",    // 宋体
            "C:/Windows/Fonts/simkai.ttf",    // 楷体
            // Linux 字体
            "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc",  // 文泉驿微米黑
            "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",    // 文泉驿正黑
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc", // Noto Sans CJK
            // macOS 字体
            "/System/Library/Fonts/PingFang.ttc",              // 苹方
            "/System/Library/Fonts/STHeiti Light.ttc",         // 华文黑体
            "/Library/Fonts/Songti.ttc"                        // 宋体
        };

        for (String path : fontPaths) {
            try {
                File fontFile = new File(path);
                if (fontFile.exists()) {
                    log.info("找到中文字体: {}", path);
                    chineseFont = PDType0Font.load(document, fontFile);
                    return chineseFont;
                }
            } catch (Exception e) {
                // 继续尝试下一个字体
                log.debug("加载字体失败: {}, {}", path, e.getMessage());
            }
        }

        log.warn("未找到可用的中文字体，PDF 中文字符可能无法正确显示");
        return null;
    }

    /**
     * 检测文本是否包含中文
     */
    private static boolean containsChinese(String text) {
        if (text == null) return false;
        return text.codePoints().anyMatch(codePoint ->
            Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN);
    }

    /**
     * 获取合适的字体（根据文本内容）
     */
    private static PDFont getFont(PDDocument document, String text, boolean bold) throws IOException {
        if (containsChinese(text)) {
            PDType0Font cnFont = loadChineseFont(document);
            if (cnFont != null) {
                return cnFont;
            }
        }
        // 降级到标准字体
        return new PDType1Font(bold ? Standard14Fonts.FontName.HELVETICA_BOLD : Standard14Fonts.FontName.HELVETICA);
    }

    /**
     * 生成 PDF 文档
     */
    public static void generatePdf(Path outputFile, String title, List<PdfSection> sections) throws IOException {
        try (PDDocument document = new PDDocument()) {
            // 尝试预加载中文字体
            loadChineseFont(document);

            PDPage currentPage = new PDPage(PDRectangle.A4);
            document.addPage(currentPage);

            float yPosition = currentPage.getMediaBox().getHeight() - MARGIN;
            PDPageContentStream contentStream = new PDPageContentStream(document, currentPage);

            // 标题
            yPosition = writeTitle(contentStream, document, title, yPosition);
            yPosition -= LINE_HEIGHT * 2;

            // 内容sections
            for (PdfSection section : sections) {
                // 检查是否需要新页面
                if (yPosition < MARGIN + 100) {
                    contentStream.close();
                    currentPage = new PDPage(PDRectangle.A4);
                    document.addPage(currentPage);
                    yPosition = currentPage.getMediaBox().getHeight() - MARGIN;
                    contentStream = new PDPageContentStream(document, currentPage);
                }

                yPosition = writeSection(contentStream, document, section, yPosition, currentPage);
            }

            contentStream.close();
            document.save(outputFile.toFile());
            log.debug("PDF 生成成功: {}", outputFile);
        }
    }

    /**
     * 写入标题
     */
    private static float writeTitle(PDPageContentStream contentStream, PDDocument document,
                                    String title, float yPosition) throws IOException {
        PDFont font = getFont(document, title, true);

        contentStream.beginText();
        contentStream.setFont(font, TITLE_FONT_SIZE);
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText(title);
        contentStream.endText();
        return yPosition - TITLE_FONT_SIZE - LINE_HEIGHT;
    }

    /**
     * 写入章节
     */
    private static float writeSection(PDPageContentStream contentStream, PDDocument document,
                                     PdfSection section, float yPosition, PDPage currentPage) throws IOException {

        // 章节标题
        if (section.getHeading() != null) {
            PDFont headingFont = getFont(document, section.getHeading(), true);

            contentStream.beginText();
            contentStream.setFont(headingFont, HEADING_FONT_SIZE);
            contentStream.newLineAtOffset(MARGIN, yPosition);
            contentStream.showText(section.getHeading());
            contentStream.endText();
            yPosition -= HEADING_FONT_SIZE + LINE_HEIGHT;
        }

        // 章节内容
        if (section.getContent() != null && !section.getContent().isEmpty()) {
            for (String line : section.getContent()) {
                // 检查是否需要换页
                if (yPosition < MARGIN) {
                    break;
                }

                // 根据行内容选择字体
                PDFont contentFont = getFont(document, line, false);

                // 处理长文本换行
                List<String> wrappedLines = wrapText(line,
                    currentPage.getMediaBox().getWidth() - 2 * MARGIN, FONT_SIZE);

                for (String wrappedLine : wrappedLines) {
                    if (yPosition < MARGIN) {
                        break;
                    }

                    contentStream.beginText();
                    contentStream.setFont(contentFont, FONT_SIZE);
                    contentStream.newLineAtOffset(MARGIN + 10, yPosition);
                    contentStream.showText(wrappedLine);
                    contentStream.endText();
                    yPosition -= LINE_HEIGHT;
                }
            }
        }

        return yPosition - LINE_HEIGHT;
    }

    /**
     * 文本换行处理
     */
    private static List<String> wrapText(String text, float maxWidth, float fontSize) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return lines;
        }

        // 简单的换行逻辑：按字符数估算
        int maxCharsPerLine = (int) (maxWidth / (fontSize * 0.5));

        if (text.length() <= maxCharsPerLine) {
            lines.add(text);
            return lines;
        }

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxCharsPerLine, text.length());

            // 尝试在空格处断行
            if (end < text.length()) {
                int lastSpace = text.lastIndexOf(' ', end);
                if (lastSpace > start) {
                    end = lastSpace;
                }
            }

            lines.add(text.substring(start, end).trim());
            start = end + 1;
        }

        return lines;
    }

    /**
     * PDF 章节
     */
    public static class PdfSection {
        private String heading;
        private List<String> content;

        public PdfSection(String heading, List<String> content) {
            this.heading = heading;
            this.content = content;
        }

        public PdfSection(String heading, String... lines) {
            this.heading = heading;
            this.content = List.of(lines);
        }

        public String getHeading() { return heading; }
        public List<String> getContent() { return content; }
    }
}

