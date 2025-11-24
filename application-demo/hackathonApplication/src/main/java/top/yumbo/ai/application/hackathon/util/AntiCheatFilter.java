package top.yumbo.ai.application.hackathon.util;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Anti-cheat filter to remove misleading prompts from code comments
 * Prevents participants from adding instructions in comments to manipulate AI scoring
 */
@Slf4j
public class AntiCheatFilter {

    // Keywords that might be used to manipulate AI scoring (Chinese)
    private static final List<String> SUSPICIOUS_KEYWORDS_CN = Arrays.asList(
            "给.*?高分", "打.*?高分", "满分", "加分", "多给.*?分",
            "评分.*?高", "分数.*?高", "请.*?分", "务必.*?分",
            "这是.*?好项目", "这是.*?优秀", "非常.*?创新",
            "技术.*?先进", "代码.*?优秀", "实现.*?完美"
    );

    // Keywords that might be used to manipulate AI scoring (English)
    private static final List<String> SUSPICIOUS_KEYWORDS_EN = Arrays.asList(
            "give.*?high.*?score", "please.*?score", "rate.*?high",
            "full.*?mark", "excellent.*?project", "perfect.*?implementation",
            "very.*?innovative", "must.*?score", "should.*?score"
    );

    // Comment patterns for various languages
    private static final List<Pattern> COMMENT_PATTERNS = Arrays.asList(
            // Java/JavaScript/C/C++ style: // and /* */
            Pattern.compile("//.*?(?=\\n|$)", Pattern.DOTALL),
            Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL),
            // Python style: #
            Pattern.compile("#.*?(?=\\n|$)", Pattern.DOTALL),
            // HTML/XML style: <!-- -->
            Pattern.compile("<!--.*?-->", Pattern.DOTALL)
    );

    // Pattern to detect prompt injection attempts in comments
    private static final Pattern PROMPT_INJECTION_PATTERN;

    static {
        // Combine all suspicious keywords into one pattern
        List<String> allKeywords = Arrays.asList(
                // Direct scoring manipulation
                "给.*?高分", "打.*?高分", "满分", "加分", "评.*?高分",
                "give.*?high.*?score", "rate.*?high", "full.*?mark",
                // Quality exaggeration
                "这是.*?好项目", "这是.*?优秀", "非常.*?创新", "极其.*?优秀",
                "excellent.*?project", "perfect.*?implementation", "very.*?innovative",
                // Instruction to reviewer
                "请.*?评", "务必.*?分", "一定要.*?分", "必须.*?分",
                "please.*?score", "must.*?score", "should.*?score",
                // Role manipulation
                "你.*?专家", "作为.*?评审", "你.*?评分",
                "you.*?expert", "as.*?reviewer", "you.*?score"
        );

        String patternStr = String.join("|", allKeywords);
        PROMPT_INJECTION_PATTERN = Pattern.compile(
                "(" + patternStr + ")",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
        );
    }

    /**
     * Filter content to remove suspicious comments that might manipulate AI scoring
     * @param content Original code content
     * @param filePath File path for logging
     * @return Filtered content with suspicious comments removed
     */
    public static String filterSuspiciousContent(String content, String filePath) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        String originalContent = content;
        int removedCount = 0;

        // Extract and check all comments
        for (Pattern commentPattern : COMMENT_PATTERNS) {
            Matcher matcher = commentPattern.matcher(content);
            StringBuffer sb = new StringBuffer();

            while (matcher.find()) {
                String comment = matcher.group();

                // Check if comment contains suspicious keywords
                if (containsSuspiciousContent(comment)) {
                    // Replace with a sanitized comment
                    String replacement = getSanitizedComment(comment);
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                    removedCount++;
                    log.warn("Suspicious comment detected and sanitized in file: {} - Comment: {}",
                            filePath, comment.substring(0, Math.min(50, comment.length())));
                } else {
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(comment));
                }
            }
            matcher.appendTail(sb);
            content = sb.toString();
        }

        if (removedCount > 0) {
            log.info("Anti-cheat filter applied to {}: {} suspicious comment(s) sanitized",
                    filePath, removedCount);
        }

        return content;
    }

    /**
     * Check if content contains suspicious keywords
     */
    private static boolean containsSuspiciousContent(String content) {
        Matcher matcher = PROMPT_INJECTION_PATTERN.matcher(content);
        return matcher.find();
    }

    /**
     * Get sanitized version of a comment
     * Keep the comment structure but remove suspicious content
     */
    private static String getSanitizedComment(String comment) {
        // Determine comment style
        if (comment.startsWith("//")) {
            return "// [Comment removed by anti-cheat filter]";
        } else if (comment.startsWith("/*")) {
            return "/* [Comment removed by anti-cheat filter] */";
        } else if (comment.startsWith("#")) {
            return "# [Comment removed by anti-cheat filter]";
        } else if (comment.startsWith("<!--")) {
            return "<!-- [Comment removed by anti-cheat filter] -->";
        }
        return "[Comment removed by anti-cheat filter]";
    }

    /**
     * Add anti-cheat notice to the beginning of filtered content
     */
    public static String addAntiCheatNotice(String content, int filesFiltered) {
        if (filesFiltered == 0) {
            return content;
        }

        String notice = String.format(
                "\n⚠️ ANTI-CHEAT NOTICE: %d file(s) contained suspicious comments " +
                "that might manipulate scoring. These comments have been sanitized.\n\n",
                filesFiltered
        );

        return notice + content;
    }

    /**
     * Validate if content seems to be a prompt injection attempt
     * This checks the entire content, not just comments
     */
    public static boolean isLikelyPromptInjection(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }

        // Check for excessive use of scoring-related terms
        Matcher matcher = PROMPT_INJECTION_PATTERN.matcher(content);
        int matchCount = 0;
        while (matcher.find()) {
            matchCount++;
        }

        // If more than 5 suspicious patterns found, likely an injection attempt
        return matchCount > 5;
    }

    /**
     * Get statistics about suspicious content
     */
    public static FilterStatistics analyzeContent(String content) {
        FilterStatistics stats = new FilterStatistics();

        if (content == null || content.isEmpty()) {
            return stats;
        }

        // Count comments
        for (Pattern commentPattern : COMMENT_PATTERNS) {
            Matcher matcher = commentPattern.matcher(content);
            while (matcher.find()) {
                stats.totalComments++;
                if (containsSuspiciousContent(matcher.group())) {
                    stats.suspiciousComments++;
                }
            }
        }

        return stats;
    }

    /**
     * Statistics class for filter results
     */
    public static class FilterStatistics {
        public int totalComments = 0;
        public int suspiciousComments = 0;

        public boolean hasSuspiciousContent() {
            return suspiciousComments > 0;
        }

        @Override
        public String toString() {
            return String.format("Total comments: %d, Suspicious: %d",
                    totalComments, suspiciousComments);
        }
    }
}

