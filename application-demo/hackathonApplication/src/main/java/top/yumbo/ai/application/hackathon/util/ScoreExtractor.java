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

        // Try primary pattern first
        Matcher matcher = TOTAL_SCORE_PATTERN.matcher(content);
        if (matcher.find()) {
            try {
                double score = Double.parseDouble(matcher.group(1));
                log.debug("Extracted score: {}", score);
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
}

