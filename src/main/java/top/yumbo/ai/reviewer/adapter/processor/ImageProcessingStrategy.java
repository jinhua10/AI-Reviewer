package top.yumbo.ai.reviewer.adapter.processor;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.reviewer.application.service.FileProcessingStrategy;
import top.yumbo.ai.reviewer.domain.model.SourceFile;

/**
 * Image file processing strategy
 * Supports JPEG, PNG, GIF, SVG, etc.
 *
 * @author AI-Reviewer Team
 * @version 2.0
 */
@Slf4j
public class ImageProcessingStrategy implements FileProcessingStrategy {

    @Override
    public boolean supports(SourceFile file) {
        return file.getMainCategory() == SourceFile.FileMainCategory.IMAGE;
    }

    @Override
    public ProcessingResult process(SourceFile file) {
        log.info("Processing image file: {} ({})", file.getFileName(), file.getFileType());

        try {
            ImageMetadata metadata = extractMetadata(file);

            ImageAnalysis analysis = ImageAnalysis.builder()
                    .metadata(metadata)
                    .build();

            file.setAnalysisResult(analysis);

            log.debug("Image analysis complete: {} - Size: {} KB, Type: {}",
                    file.getFileName(),
                    file.getSizeInBytes() / 1024,
                    file.getFileType());

            return ProcessingResult.success(file, analysis);

        } catch (Exception e) {
            log.error("Image processing failed: {}", file.getFileName(), e);
            return ProcessingResult.failure(file, e);
        }
    }

    /**
     * Extract image metadata
     * TODO: Integrate image processing library (e.g., metadata-extractor or ImageIO)
     */
    private ImageMetadata extractMetadata(SourceFile file) {
        return ImageMetadata.builder()
                .fileName(file.getFileName())
                .fileSize(file.getSizeInBytes())
                .format(file.getFileType().getName())
                .mimeType(file.getMimeType())
                .build();
    }

    @Data
    @Builder
    public static class ImageMetadata {
        private String fileName;
        private long fileSize;
        private String format;
        private String mimeType;
        private Integer width;
        private Integer height;
        private Integer colorDepth;
    }

    @Data
    @Builder
    public static class ImageAnalysis {
        private ImageMetadata metadata;
    }
}

