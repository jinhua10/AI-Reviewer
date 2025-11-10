package top.yumbo.ai.reviewer.service;

import top.yumbo.ai.reviewer.exception.AnalysisException;

/**
 * AI 服务接口（简化版）
 * 定义统一的 AI 调用规范
 */
public interface AIService {

    /**
     * 调用 AI 进行分析
     *
     * @param prompt 提示词（包含待分析的代码）
     * @param maxTokens 最大响应 Token 数
     * @return AI 返回的分析结果
     * @throws AnalysisException 调用失败时抛出
     */
    String analyze(String prompt, int maxTokens) throws AnalysisException;

    /**
     * 获取模型支持的最大 Token 数
     */
    int getMaxTokens();

    /**
     * 获取模型名称
     */
    String getModelName();
}

