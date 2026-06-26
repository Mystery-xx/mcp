package io.github.solas.mcp.weather.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Complete weather data including location, current conditions, and forecast.
 */
public record WeatherData(
    @JsonProperty("location") Location location,
    @JsonProperty("current") CurrentWeather current,
    @JsonProperty("forecast") List<DailyForecast> forecast
) {
}
