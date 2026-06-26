package io.github.solas.mcp.weather.api;

/**
 * Utility class for language detection.
 */
public class LanguageUtils {

    private LanguageUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Check if text contains Cyrillic characters.
     * Uses Unicode range \u0400-\u04FF (Cyrillic block).
     *
     * @param text the text to check
     * @return true if text contains at least one Cyrillic character, false otherwise
     */
    public static boolean isRussian(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return text.matches(".*[\\u0400-\\u04FF].*");
    }

    /**
     * Get locale code based on city name.
     * Returns "ru" if city contains Cyrillic characters, "en" otherwise.
     *
     * @param city the city name
     * @return "ru" if isRussian(city), else "en"
     */
    public static String getLocale(String city) {
        return isRussian(city) ? "ru" : "en";
    }
}
