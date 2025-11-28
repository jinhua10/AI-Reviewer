package top.yumbo.ai.application.hackathon.util;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for extracting scores from AI review responses
 */
@Slf4j
public class ScoreExtractor {

    // Pattern to match total score like "【Total Score】: 85/100 points" or "Total Score: 85/100"
    private static final Pattern TOTAL_SCORE_PATTERN = Pattern.compile(
        "(?:【)?Total Score(?:】)?\\s*[:\\：]\\s*(\\d+(?:\\.\\d+)?)/100",
        Pattern.CASE_INSENSITIVE
    );

    // Pattern for Chinese format "【总分】: 85/100 分"
    private static final Pattern CHINESE_SCORE_PATTERN = Pattern.compile(
        "【?总分】?\\s*[:\\：]\\s*(\\d+(?:\\.\\d+)?)/100",
        Pattern.CASE_INSENSITIVE
    );

    // Alternative pattern for score at the beginning
    private static final Pattern SCORE_ALT_PATTERN = Pattern.compile(
        "Score\\s*[:\\：]\\s*(\\d+(?:\\.\\d+)?)",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Extract score from AI response content
     * @param content The AI response content
     * @return The extracted score, or null if not found
     */
    public static Double extractScore(String content) {
        if (content == null || content.isEmpty()) {
            log.warn("Empty content provided for score extraction");
            return null;
        }

        // Try Chinese pattern first
        Matcher matcher = CHINESE_SCORE_PATTERN.matcher(content);
        if (matcher.find()) {
            try {
                double score = Double.parseDouble(matcher.group(1));
                log.debug("Extracted score (Chinese): {}", score);
                return score;
            } catch (NumberFormatException e) {
                log.warn("Failed to parse score: {}", matcher.group(1), e);
            }
        }

        // Try English pattern
        matcher = TOTAL_SCORE_PATTERN.matcher(content);
        if (matcher.find()) {
            try {
                double score = Double.parseDouble(matcher.group(1));
                log.debug("Extracted score (English): {}", score);
                return score;
            } catch (NumberFormatException e) {
                log.warn("Failed to parse score: {}", matcher.group(1), e);
            }
        }

        // Try alternative pattern
        matcher = SCORE_ALT_PATTERN.matcher(content);
        if (matcher.find()) {
            try {
                double score = Double.parseDouble(matcher.group(1));
                log.debug("Extracted score (alt pattern): {}", score);
                return score;
            } catch (NumberFormatException e) {
                log.warn("Failed to parse score: {}", matcher.group(1), e);
            }
        }

        log.warn("No score found in content");
        return null;
    }

    /**
     * Format score for use in filename (replace decimal point with underscore)
     * @param score The score to format
     * @return Formatted string like "85_5" for 85.5 or "85_0" for 85.0
     */
    public static String formatScoreForFilename(Double score) {
        if (score == null) {
            return "unknown";
        }
        // Format to one decimal place and replace dot with underscore
        String formatted = String.format("%.1f", score);
        return formatted.replace(".", "_");
    }

    /**
     * Extract Overall Comment from AI response content
     * @param content The AI response content
     * @return The extracted overall comment, or empty string if not found
     */
    public static String extractOverallComment(String content) {
        if (content == null || content.isEmpty()) {
            log.warn("Empty content provided for overall comment extraction");
            return "";
        }

        // Pattern to match Chinese overall comment section "【总体评语】"
        Pattern chinesePattern = Pattern.compile(
            "【?总体评语】?\\s*[:\\：]?\\s*\\n?(.+?)(?=\\n\\n|\\n【|$)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );

        Matcher matcher = chinesePattern.matcher(content);
        if (matcher.find()) {
            String comment = matcher.group(1).trim();
            comment = cleanComment(comment);
            log.debug("Extracted overall comment (Chinese): {} chars", comment.length());
            return comment;
        }

        // Pattern to match English overall comment section
        // Support variations: "【Overall Comments】", "Overall Comments:", "【Overall Comment】", "Overall Comment:"
        Pattern englishPattern = Pattern.compile(
            "(?:【)?Overall Comments?(?:】)?\\s*[:\\：]?\\s*\\n?(.+?)(?=\\n\\n|\\n【|$)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );

        matcher = englishPattern.matcher(content);
        if (matcher.find()) {
            String comment = matcher.group(1).trim();
            comment = cleanComment(comment);
            log.debug("Extracted overall comment (English): {} chars", comment.length());
            return comment;
        }

        log.warn("No overall comment found in content");
        return "";
    }

    /**
     * Clean comment text for CSV storage
     */
    private static String cleanComment(String comment) {
        // Remove any leading/trailing whitespace and newlines
        comment = comment.replaceAll("^\\s+|\\s+$", "");
        // Replace internal newlines with spaces for CSV compatibility
        comment = comment.replaceAll("\\n+", " ");
        // Remove parenthetical notes like (200字内简明总结...)
        comment = comment.replaceAll("\\([^)]*字[^)]*\\)", "");
        // Remove any remaining closing brackets: "s】", "s]", "】", "]" at the end
        comment = comment.replaceAll("s[\\]\\u3011]+|[\\]\\u3011]+$", "");
        // Compress multiple spaces to single space
        comment = comment.replaceAll("\\s+", " ");
        // Escape quotes for CSV (double them)
        comment = comment.replace("\"", "\"\"");
        return comment.trim();
    }
}

