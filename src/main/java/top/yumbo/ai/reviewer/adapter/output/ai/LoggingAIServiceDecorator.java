package top.yumbo.ai.reviewer.adapter.output.ai;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.reviewer.application.port.output.AIServicePort;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * AI服务端口日志装饰器
 * 为 AI 服务调用添加日志记录功能，记录入参和返回值
 *
 * <p>基于六边形架构和装饰器模式，提供透明的日志记录能力</p>
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-14
 */
@Slf4j
public class LoggingAIServiceDecorator implements AIServicePort {

    private final AIServicePort delegate;
    private final String providerName;

    /**
     * 构造函数
     *
     * @param delegate 被装饰的 AI 服务实例
     */
    public LoggingAIServiceDecorator(AIServicePort delegate) {
        this.delegate = delegate;
        this.providerName = delegate.getProviderName();
    }

    @Override
    public String analyze(String prompt) {
        long startTime = System.currentTimeMillis();
        log.info("[{}] 开始同步分析 - 提示词长度: {} 字符",
                providerName, prompt.length());
        log.debug("[{}] 同步分析输入参数:\n{}", providerName, truncatePrompt(prompt));

        try {
            String result = delegate.analyze(prompt);
            long duration = System.currentTimeMillis() - startTime;

            log.info("[{}] 同步分析完成 - 耗时: {}ms, 结果长度: {} 字符",
                    providerName, duration, result != null ? result.length() : 0);
            log.debug("[{}] 同步分析返回结果:\n{}", providerName, truncateResult(result));

            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[{}] 同步分析失败 - 耗时: {}ms, 错误: {}",
                    providerName, duration, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public CompletableFuture<String> analyzeAsync(String prompt) {
        long startTime = System.currentTimeMillis();
        log.info("[{}] 开始异步分析 - 提示词长度: {} 字符",
                providerName, prompt.length());
        log.debug("[{}] 异步分析输入参数:\n{}", providerName, truncatePrompt(prompt));

        return delegate.analyzeAsync(prompt)
                .whenComplete((result, throwable) -> {
                    long duration = System.currentTimeMillis() - startTime;

                    if (throwable != null) {
                        log.error("[{}] 异步分析失败 - 耗时: {}ms, 错误: {}",
                                providerName, duration, throwable.getMessage(), throwable);
                    } else {
                        log.info("[{}] 异步分析完成 - 耗时: {}ms, 结果长度: {} 字符",
                                providerName, duration, result != null ? result.length() : 0);
                        log.debug("[{}] 异步分析返回结果:\n{}", providerName, truncateResult(result));
                    }
                });
    }

    @Override
    public CompletableFuture<String[]> analyzeBatchAsync(String[] prompts) {
        long startTime = System.currentTimeMillis();
        int totalPrompts = prompts != null ? prompts.length : 0;
        int totalChars = prompts != null ?
                Arrays.stream(prompts).mapToInt(String::length).sum() : 0;

        log.info("[{}] 开始批量异步分析 - 提示词数量: {}, 总字符数: {}",
                providerName, totalPrompts, totalChars);
        log.debug("[{}] 批量异步分析输入参数 - 提示词数量: {}", providerName, totalPrompts);

        return delegate.analyzeBatchAsync(prompts)
                .whenComplete((results, throwable) -> {
                    long duration = System.currentTimeMillis() - startTime;

                    if (throwable != null) {
                        log.error("[{}] 批量异步分析失败 - 耗时: {}ms, 提示词数量: {}, 错误: {}",
                                providerName, duration, totalPrompts, throwable.getMessage(), throwable);
                    } else {
                        int resultCount = results != null ? results.length : 0;
                        int totalResultChars = results != null ?
                                Arrays.stream(results).mapToInt(String::length).sum() : 0;

                        log.info("[{}] 批量异步分析完成 - 耗时: {}ms, 结果数量: {}, 总字符数: {}",
                                providerName, duration, resultCount, totalResultChars);
                        log.debug("[{}] 批量异步分析返回结果 - 结果数量: {}", providerName, resultCount);
                    }
                });
    }

    @Override
    public String getProviderName() {
        return delegate.getProviderName();
    }

    @Override
    public boolean isAvailable() {
        boolean available = delegate.isAvailable();
        log.debug("[{}] 服务可用性检查: {}", providerName, available);
        return available;
    }

    @Override
    public int getMaxConcurrency() {
        int maxConcurrency = delegate.getMaxConcurrency();
        log.debug("[{}] 获取最大并发数: {}", providerName, maxConcurrency);
        return maxConcurrency;
    }

    @Override
    public void shutdown() {
        log.info("[{}] 开始关闭服务并释放资源", providerName);
        try {
            delegate.shutdown();
            log.info("[{}] 服务关闭完成", providerName);
        } catch (Exception e) {
            log.error("[{}] 服务关闭失败: {}", providerName, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 截断提示词用于日志输出（避免日志过长）
     *
     * @param prompt 原始提示词
     * @return 截断后的提示词
     */
    private String truncatePrompt(String prompt) {
        if (prompt == null) {
            return "null";
        }
        int maxLength = 500; // 最多显示500个字符
        if (prompt.length() <= maxLength) {
            return prompt;
        }
        return prompt.substring(0, maxLength) + "... [截断，总长度: " + prompt.length() + " 字符]";
    }

    /**
     * 截断结果用于日志输出（避免日志过长）
     *
     * @param result 原始结果
     * @return 截断后的结果
     */
    private String truncateResult(String result) {
        if (result == null) {
            return "null";
        }
        int maxLength = 1000; // 最多显示1000个字符
        if (result.length() <= maxLength) {
            return result;
        }
        return result.substring(0, maxLength) + "... [截断，总长度: " + result.length() + " 字符]";
    }
}

