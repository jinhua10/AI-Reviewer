package top.yumbo.ai.reviewer.application.service;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.reviewer.domain.model.SourceFile;

import java.util.List;
import java.util.Optional;

/**
 * 文件处理编排器
 * 根据文件类型自动选择合适的处理策略
 *
 * @author AI-Reviewer Team
 * @version 2.0
 */
@Slf4j
public class FileProcessingOrchestrator {

    private final List<FileProcessingStrategy> strategies;

    public FileProcessingOrchestrator(List<FileProcessingStrategy> strategies) {
        this.strategies = strategies;
        log.info("文件处理编排器初始化，共加载 {} 个策略", strategies.size());
    }

    /**
     * 处理单个文件
     */
    public FileProcessingStrategy.ProcessingResult processFile(SourceFile file) {
        log.debug("开始处理文件: {}", file.getFileName());

        // 查找支持该文件的策略
        Optional<FileProcessingStrategy> strategy = findStrategy(file);

        if (strategy.isEmpty()) {
            log.warn("未找到支持的处理策略: {} ({})", file.getFileName(), file.getMainCategory());
            return FileProcessingStrategy.ProcessingResult.failure(
                    file,
                    "不支持的文件类型: " + file.getMainCategory()
            );
        }

        try {
            FileProcessingStrategy selectedStrategy = strategy.get();
            log.debug("使用策略 {} 处理文件: {}", selectedStrategy.getName(), file.getFileName());

            return selectedStrategy.process(file);

        } catch (Exception e) {
            log.error("文件处理失败: {}", file.getFileName(), e);
            return FileProcessingStrategy.ProcessingResult.failure(file, e);
        }
    }

    /**
     * 查找支持该文件的策略
     */
    private Optional<FileProcessingStrategy> findStrategy(SourceFile file) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(file))
                .findFirst();
    }

    /**
     * 获取所有已注册的策略
     */
    public List<FileProcessingStrategy> getRegisteredStrategies() {
        return List.copyOf(strategies);
    }
}

