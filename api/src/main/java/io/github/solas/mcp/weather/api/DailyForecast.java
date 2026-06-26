package io.github.solas.mcp.weather.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Daily weather forecast.
 */
public record DailyForecast(
    @JsonProperty("date") String date,
    @JsonProperty("temperatureMax") double temperatureMax,
    @JsonProperty("temperatureMin") double temperatureMin,
    @JsonProperty("precipitationSum") double precipitationSum,
    @JsonProperty("weatherCode") int weatherCode
) {
}
