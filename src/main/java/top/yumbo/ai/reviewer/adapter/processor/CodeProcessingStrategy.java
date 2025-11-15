package top.yumbo.ai.reviewer.adapter.processor;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.reviewer.application.service.FileProcessingStrategy;
import top.yumbo.ai.reviewer.domain.model.SourceFile;
import top.yumbo.ai.reviewer.domain.model.ast.CodeInsight;

/**
 * 代码文件处理策略
 * 支持 Java, Python, JavaScript, TypeScript, Go, C++ 等代码文件
 *
 * TODO: 集成 ASTAnalysisService 进行代码分析
 *
 * @author AI-Reviewer Team
 * @version 2.0
 */
@Slf4j
public class CodeProcessingStrategy implements FileProcessingStrategy {

    @Override
    public boolean supports(SourceFile file) {
        return file.getMainCategory() == SourceFile.FileMainCategory.CODE;
    }

    @Override
    public ProcessingResult process(SourceFile file) {
        log.info("处理代码文件: {} ({})", file.getFileName(), file.getFileType());

        try {
            // TODO: 集成 ASTAnalysisService
            // CodeInsight codeInsight = astAnalysisService.analyzeFile(file);

            // 暂时返回基本信息
            CodeAnalysis analysis = CodeAnalysis.builder()
                    .fileName(file.getFileName())
                    .projectType(file.getProjectType())
                    .lineCount(file.getLineCount())
                    .build();

            file.setAnalysisResult(analysis);

            log.debug("代码分析完成: {} - 行数: {}",
                    file.getFileName(),
                    file.getLineCount());

            return ProcessingResult.success(file, analysis);

        } catch (Exception e) {
            log.error("代码文件处理失败: {}", file.getFileName(), e);
            return ProcessingResult.failure(file, e);
        }
    }

    @lombok.Builder
    @lombok.Data
    public static class CodeAnalysis {
        private String fileName;
        private top.yumbo.ai.reviewer.domain.model.ProjectType projectType;
        private int lineCount;
    }
}

