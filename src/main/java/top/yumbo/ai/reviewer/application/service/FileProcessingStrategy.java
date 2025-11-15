package top.yumbo.ai.reviewer.application.service;

import lombok.Builder;
import lombok.Data;
import top.yumbo.ai.reviewer.domain.model.SourceFile;

/**
 * File processing strategy interface
 * Supports multiple file types (code, documents, media, etc.)
 *
 * @author AI-Reviewer Team
 * @version 2.0
 */
public interface FileProcessingStrategy {

    /**
     * Check if this strategy supports the given file
     */
    boolean supports(SourceFile file);

    /**
     * Process the file
     */
    ProcessingResult process(SourceFile file);

    /**
     * Get strategy name
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * Processing result
     */
    @Data
    @Builder
    class ProcessingResult {
        private SourceFile file;
        private boolean success;
        private String message;
        private Object data;
        private Exception error;

        public static ProcessingResult success(SourceFile file, Object data) {
            return ProcessingResult.builder()
                    .file(file)
                    .success(true)
                    .data(data)
                    .message("Processing completed successfully")
                    .build();
        }

        public static ProcessingResult failure(SourceFile file, String message) {
            return ProcessingResult.builder()
                    .file(file)
                    .success(false)
                    .message(message)
                    .build();
        }

        public static ProcessingResult failure(SourceFile file, Exception error) {
            return ProcessingResult.builder()
                    .file(file)
                    .success(false)
                    .message(error.getMessage())
                    .error(error)
                    .build();
        }
    }
}

