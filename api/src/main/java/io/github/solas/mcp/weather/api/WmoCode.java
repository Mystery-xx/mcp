package io.github.solas.mcp.weather.api;

import java.util.Map;

/**
 * Weather code mapping for wttr.in service (113-395) to human-readable descriptions.
 * Based on wttr.in weather code interpretation.
 */
public final class WmoCode {

    private static final Map<Integer, String> CODE_MAP = Map.ofEntries(
        // Clear / Sunny
        Map.entry(113, "Sunny/Clear"),
        Map.entry(116, "Partly Cloudy"),
        Map.entry(119, "Cloudy"),
        Map.entry(122, "Overcast"),

        // Mist / Fog
        Map.entry(143, "Mist"),
        Map.entry(248, "Fog"),
        Map.entry(260, "Freezing fog"),

        // Drizzle
        Map.entry(263, "Light drizzle"),
        Map.entry(266, "Light drizzle"),
        Map.entry(281, "Freezing drizzle"),
        Map.entry(284, "Heavy freezing drizzle"),

        // Rain
        Map.entry(176, "Patchy rain nearby"),
        Map.entry(293, "Patchy light rain"),
        Map.entry(296, "Light rain"),
        Map.entry(299, "Moderate rain at times"),
        Map.entry(302, "Moderate rain"),
        Map.entry(305, "Heavy rain at times"),
        Map.entry(308, "Heavy rain"),
        Map.entry(353, "Light rain shower"),
        Map.entry(356, "Moderate or heavy rain shower"),
        Map.entry(359, "Torrential rain shower"),

        // Freezing Rain
        Map.entry(311, "Light freezing rain"),
        Map.entry(314, "Moderate or heavy freezing rain"),

        // Snow
        Map.entry(179, "Patchy snow nearby"),
        Map.entry(227, "Blowing snow"),
        Map.entry(230, "Blizzard"),
        Map.entry(320, "Patchy light snow"),
        Map.entry(323, "Light snow showers"),
        Map.entry(326, "Light snow"),
        Map.entry(329, "Patchy moderate snow"),
        Map.entry(332, "Moderate snow"),
        Map.entry(335, "Patchy heavy snow"),
        Map.entry(338, "Heavy snow"),
        Map.entry(350, "Ice pellets"),
        Map.entry(368, "Light snow showers"),
        Map.entry(371, "Moderate or heavy snow showers"),

        // Sleet
        Map.entry(182, "Patchy sleet nearby"),
        Map.entry(185, "Patchy freezing drizzle nearby"),
        Map.entry(317, "Light sleet"),
        Map.entry(362, "Light sleet showers"),
        Map.entry(365, "Moderate or heavy sleet showers"),

        // Thunder
        Map.entry(200, "Thundery outbreaks in nearby"),
        Map.entry(386, "Patchy light rain in area with thunder"),
        Map.entry(389, "Moderate or heavy rain in area with thunder"),
        Map.entry(392, "Patchy light snow in area with thunder"),
        Map.entry(395, "Moderate or heavy snow in area with thunder")
    );

    private WmoCode() {
        // Utility class
    }

    /**
     * Get human-readable description for WMO weather code.
     *
     * @param code WMO code (0-99)
     * @return Description for known codes, or "WMO code: XX" for unknown codes
     */
    public static String getDescription(int code) {
        return CODE_MAP.getOrDefault(code, "WMO code: " + code);
    }
}
