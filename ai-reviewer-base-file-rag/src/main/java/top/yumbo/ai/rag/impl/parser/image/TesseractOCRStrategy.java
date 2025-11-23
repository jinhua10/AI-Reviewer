package top.yumbo.ai.rag.impl.parser.image;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;

/**
 * Tesseract OCR 策略
 *
 * 使用 Tesseract OCR 提取图片中的文字
 *
 * 依赖: net.sourceforge.tess4j:tess4j
 * 需要安装 Tesseract OCR 或配置 tessdata 路径
 *
 * 使用方法:
 * 1. 添加 Maven 依赖:
 *    <dependency>
 *        <groupId>net.sourceforge.tess4j</groupId>
 *        <artifactId>tess4j</artifactId>
 *        <version>5.9.0</version>
 *    </dependency>
 *
 * 2. 下载语言包:
 *    中文: https://github.com/tesseract-ocr/tessdata/raw/main/chi_sim.traineddata
 *    英文: https://github.com/tesseract-ocr/tessdata/raw/main/eng.traineddata
 *
 * 3. 配置 tessdata 路径:
 *    System.setProperty("TESSDATA_PREFIX", "/path/to/tessdata");
 *
 * @author AI Reviewer Team
 * @since 2025-11-23
 */
@Slf4j
public class TesseractOCRStrategy implements ImageContentExtractorStrategy {

    private final String tessdataPath;
    private final String language;
    private boolean available = false;

    /**
     * 默认构造函数（中文+英文）
     */
    public TesseractOCRStrategy() {
        this(null, "chi_sim+eng");
    }

    /**
     * 自定义构造函数
     *
     * @param tessdataPath tessdata 路径（null则使用系统默认）
     * @param language 语言（chi_sim=简体中文，eng=英文）
     */
    public TesseractOCRStrategy(String tessdataPath, String language) {
        this.tessdataPath = tessdataPath;
        this.language = language;
        checkAvailability();
    }

    private void checkAvailability() {
        try {
            // 检查 Tesseract 类是否存在
            Class.forName("net.sourceforge.tess4j.Tesseract");
            available = true;
            log.info("✅ Tesseract OCR 可用 (语言: {})", language);
        } catch (ClassNotFoundException e) {
            available = false;
            log.warn("⚠️  Tesseract OCR 不可用: 缺少 tess4j 依赖");
            log.warn("💡 提示: 添加 Maven 依赖: net.sourceforge.tess4j:tess4j:5.9.0");
        }
    }

    @Override
    public String extractContent(InputStream imageStream, String imageName) {
        if (!available) {
            return String.format("[图片: %s - OCR不可用]", imageName);
        }

        try {
            // TODO: 使用 Tesseract 进行 OCR
            // Tesseract tesseract = new Tesseract();
            // if (tessdataPath != null) {
            //     tesseract.setDatapath(tessdataPath);
            // }
            // tesseract.setLanguage(language);
            //
            // BufferedImage image = ImageIO.read(imageStream);
            // String text = tesseract.doOCR(image);
            //
            // log.debug("OCR提取文字 [{}]: {} 字符", imageName, text.length());
            // return text;

            // 临时实现：返回提示信息
            return String.format("[图片: %s - OCR功能待完整实现]", imageName);

        } catch (Exception e) {
            log.error("OCR处理失败: {}", imageName, e);
            return String.format("[图片: %s - OCR处理失败]", imageName);
        }
    }

    @Override
    public String extractContent(File imageFile) {
        if (!available) {
            return String.format("[图片: %s - OCR不可用]", imageFile.getName());
        }

        try {
            // TODO: 使用 Tesseract 进行 OCR
            // Tesseract tesseract = new Tesseract();
            // if (tessdataPath != null) {
            //     tesseract.setDatapath(tessdataPath);
            // }
            // tesseract.setLanguage(language);
            //
            // String text = tesseract.doOCR(imageFile);
            //
            // log.info("OCR提取文字 [{}]: {} 字符", imageFile.getName(), text.length());
            // return text;

            // 临时实现：返回提示信息
            return String.format("[图片: %s - OCR功能待完整实现]", imageFile.getName());

        } catch (Exception e) {
            log.error("OCR处理失败: {}", imageFile.getName(), e);
            return String.format("[图片: %s - OCR处理失败]", imageFile.getName());
        }
    }

    @Override
    public String getStrategyName() {
        return "Tesseract OCR";
    }

    @Override
    public boolean isAvailable() {
        return available;
    }
}

