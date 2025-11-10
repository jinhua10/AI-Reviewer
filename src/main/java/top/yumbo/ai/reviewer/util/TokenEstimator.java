package top.yumbo.ai.reviewer.util;

import java.util.regex.Pattern;

/**
 * Token 估算器（简化版）
 * 基于启发式规则估算文本的 Token 数量
 */
public class TokenEstimator {

    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4E00-\\u9FFF]");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    /**
     * 估算文本的 Token 数量
     * 规则：
     * - 中文字符：每个字符约 1.5 tokens
     * - 英文单词：每个单词约 1.3 tokens
     * - 代码符号：计入字符总数
     */
    public static int estimate(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        // 统计中文字符数
        int chineseCount = 0;
        for (char c : text.toCharArray()) {
            if (CHINESE_PATTERN.matcher(String.valueOf(c)).matches()) {
                chineseCount++;
            }
        }

        // 统计英文单词数
        String withoutChinese = CHINESE_PATTERN.matcher(text).replaceAll("");
        String[] words = WHITESPACE_PATTERN.split(withoutChinese.trim());
        int englishWordCount = words.length;

        // 估算总 Token 数
        double chineseTokens = chineseCount * 1.5;
        double englishTokens = englishWordCount * 1.3;

        return (int) Math.ceil(chineseTokens + englishTokens);
    }

    /**
     * 估算文件的 Token 数量（考虑文件类型）
     */
    public static int estimateForFile(String content, String fileExtension) {
        int baseTokens = estimate(content);

        // 代码文件通常有更多符号和结构，增加 20% 的估算
        if (isCodeFile(fileExtension)) {
            return (int) (baseTokens * 1.2);
        }

        return baseTokens;
    }

    private static boolean isCodeFile(String extension) {
        return extension != null && (
            extension.endsWith(".java") ||
            extension.endsWith(".py") ||
            extension.endsWith(".js") ||
            extension.endsWith(".ts") ||
            extension.endsWith(".go") ||
            extension.endsWith(".rs")
        );
    }
}

