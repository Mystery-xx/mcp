package io.github.solas.mcp.weather.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Location information for weather data.
 */
public record Location(
    @JsonProperty("name") String name,
    @JsonProperty("country") String country,
    @JsonProperty("latitude") double latitude,
    @JsonProperty("longitude") double longitude,
    @JsonProperty("timezone") String timezone,
    @JsonProperty("elevation") double elevation
) {
}
