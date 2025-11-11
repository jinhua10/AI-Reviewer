package top.yumbo.ai.reviewer.util;

/**
 * Token估算器
 * 
 * 用于估算文本的token数量
 */
public class TokenEstimator {

    /**
     * 估算文本的token数量
     * 
     * @param text 文本
     * @return 估算的token数量
     */
    public int estimate(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        // 简单实现：1个token约等于4个字符
        // 对于英文和中文混合文本，这个估算比较粗糙
        // 但对于我们的使用场景已经足够
        return (text.length() + 3) / 4;
    }

    /**
     * 估算代码的token数量
     * 
     * @param code 代码
     * @return 估算的token数量
     */
    public int estimateCode(String code) {
        if (code == null || code.isEmpty()) {
            return 0;
        }

        // 对于代码，token数量通常比纯文本少
        // 因为代码中有大量重复的符号和关键词
        // 这里使用一个稍微不同的估算方法
        int charCount = code.length();
        int lineCount = code.split("\\R").length;

        // 基础token数量
        int tokens = (charCount + 3) / 4;

        // 根据行数调整，因为每行通常会有一些结构开销
        tokens += lineCount / 2;

        return tokens;
    }
}
