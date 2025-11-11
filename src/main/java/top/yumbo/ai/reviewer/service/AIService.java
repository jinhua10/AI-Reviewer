package top.yumbo.ai.reviewer.service;

/**
 * AI服务接口
 * 
 * 定义了与AI服务交互的基本方法
 */
public interface AIService extends AutoCloseable {

    /**
     * 分析内容
     * 
     * @param prompt 提示内容
     * @param maxTokens 最大token数
     * @return AI响应
     */
    String analyze(String prompt, int maxTokens);

    /**
     * 获取最大token数
     * 
     * @return 最大token数
     */
    int getMaxTokens();

    /**
     * 获取模型名称
     * 
     * @return 模型名称
     */
    String getModelName();
}
