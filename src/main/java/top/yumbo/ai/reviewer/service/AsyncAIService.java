package top.yumbo.ai.reviewer.service;

import top.yumbo.ai.reviewer.exception.AnalysisException;

import java.util.concurrent.CompletableFuture;

/**
 * 异步AI服务接口
 */
public interface AsyncAIService {

    /**
     * 同步分析单个提示词（为了向后兼容）
     * @param prompt 分析提示词
     * @return 分析结果
     */
    String analyze(String prompt) throws AnalysisException;

    /**
     * 同步批量分析（为了向后兼容）
     * @param prompts 提示词数组
     * @return 分析结果数组
     */
    String[] analyzeBatch(String[] prompts) throws AnalysisException;

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

    /**
     * 获取服务提供商名称
     */
    String getProviderName();

    /**
     * 检查服务是否可用
     */
    boolean isAvailable();
}
