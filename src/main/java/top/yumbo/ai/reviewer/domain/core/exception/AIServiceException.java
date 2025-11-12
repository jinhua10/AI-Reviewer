package top.yumbo.ai.reviewer.domain.core.exception;

/**
 * AI服务异常
 *
 * 当AI服务调用失败时抛出此异常
 *
 * @author AI-Reviewer Team
 * @version 1.0
 * @since 2025-11-12
 */
public class AIServiceException extends TechnicalException {

    private final String provider;

    /**
     * 构造函数
     *
     * @param provider AI提供商名称
     * @param message 错误消息
     */
    public AIServiceException(String provider, String message) {
        super(String.format("AI服务调用失败 [%s]: %s", provider, message));
        this.provider = provider;
    }

    /**
     * 构造函数（带原因）
     *
     * @param provider AI提供商名称
     * @param message 错误消息
     * @param cause 原始异常
     */
    public AIServiceException(String provider, String message, Throwable cause) {
        super(String.format("AI服务调用失败 [%s]: %s", provider, message), cause);
        this.provider = provider;
    }

    /**
     * 获取AI提供商名称
     *
     * @return 提供商名称
     */
    public String getProvider() {
        return provider;
    }

    /**
     * API Key无效异常
     *
     * @param provider 提供商名称
     * @return AI服务异常
     */
    public static AIServiceException invalidApiKey(String provider) {
        return new AIServiceException(provider, "API Key 无效或已过期");
    }

    /**
     * API调用超时异常
     *
     * @param provider 提供商名称
     * @return AI服务异常
     */
    public static AIServiceException timeout(String provider) {
        return new AIServiceException(provider, "API 调用超时");
    }

    /**
     * 配额超限异常
     *
     * @param provider 提供商名称
     * @return AI服务异常
     */
    public static AIServiceException quotaExceeded(String provider) {
        return new AIServiceException(provider, "API 配额已用尽");
    }
}

