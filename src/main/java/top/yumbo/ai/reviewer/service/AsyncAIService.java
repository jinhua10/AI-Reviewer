package top.yumbo.ai.reviewer.service;

import top.yumbo.ai.reviewer.exception.AnalysisException;

import java.util.concurrent.CompletableFuture;

/**
 * 异步AI服务接口
 */
public interface AsyncAIService extends AIService {

    /**
     * 异步分析单个提示词
     * @param prompt 分析提示词
     * @return 异步结果
     */
    CompletableFuture<String> analyzeAsync(String prompt);

    /**
     * 异步批量分析
     * @param prompts 提示词数组
     * @return 异步结果数组
     */
    CompletableFuture<String[]> analyzeBatchAsync(String[] prompts);

    /**
     * 获取并发限制
     */
    int getMaxConcurrency();

    /**
     * 设置并发限制
     */
    void setMaxConcurrency(int maxConcurrency);
}
