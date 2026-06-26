package io.github.solas.mcp.weather.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Current weather conditions.
 */
public record CurrentWeather(
    @JsonProperty("temperature") double temperature,
    @JsonProperty("feelsLike") double feelsLike,
    @JsonProperty("humidity") int humidity,
    @JsonProperty("pressure") int pressure,
    @JsonProperty("windSpeed") double windSpeed,
    @JsonProperty("windDirection") String windDirection,
    @JsonProperty("weatherCode") int weatherCode,
    @JsonProperty("description") String description
) {
}
