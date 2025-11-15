package top.yumbo.ai.reviewer.adapter.processor;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.reviewer.application.service.FileProcessingStrategy;
import top.yumbo.ai.reviewer.domain.model.SourceFile;

/**
 * 数据文件处理策略
 * 支持 JSON, XML, CSV, YAML 等格式
 *
 * @author AI-Reviewer Team
 * @version 2.0
 */
@Slf4j
public class DataProcessingStrategy implements FileProcessingStrategy {

    @Override
    public boolean supports(SourceFile file) {
        return file.getMainCategory() == SourceFile.FileMainCategory.DATA;
    }

    @Override
    public ProcessingResult process(SourceFile file) {
        log.info("处理数据文件: {} ({})", file.getFileName(), file.getFileType());

        try {
            // 基本信息提取
            DataAnalysis analysis = DataAnalysis.builder()
                    .fileName(file.getFileName())
                    .fileType(file.getFileType().getName())
                    .fileSize(file.getSizeInBytes())
                    .lineCount(file.getLineCount())
                    .build();

            // 将分析结果存储到文件对象
            file.setAnalysisResult(analysis);

            log.debug("数据文件分析完成: {} - 行数: {}, 大小: {} KB",
                    file.getFileName(),
                    file.getLineCount(),
                    file.getSizeInBytes() / 1024);

            return ProcessingResult.success(file, analysis);

        } catch (Exception e) {
            log.error("数据文件处理失败: {}", file.getFileName(), e);
            return ProcessingResult.failure(file, e);
        }
    }

    @Data
    @Builder
    public static class DataAnalysis {
        private String fileName;
        private String fileType;
        private long fileSize;
        private int lineCount;
    }
}

