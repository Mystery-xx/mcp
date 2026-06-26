package io.github.solas.mcp.weather.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.solas.mcp.weather.api.CurrentWeather;
import io.github.solas.mcp.weather.api.DailyForecast;
import io.github.solas.mcp.weather.api.Location;
import io.github.solas.mcp.weather.api.WeatherApiException;
import io.github.solas.mcp.weather.api.WeatherClient;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MCP tool implementations for weather queries.
 * Registers three tools: get_current_weather, get_forecast, search_city.
 */
@Component
public class WeatherMcpTools {

    private final WeatherClient weatherClient;
    private final ObjectMapper objectMapper;

    public WeatherMcpTools(WeatherClient weatherClient, ObjectMapper objectMapper) {
        this.weatherClient = weatherClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Register MCP tools with the server.
     */
    public void registerTools(McpSyncServer server) {
        if (server == null) {
            return;
        }

        try {
            // Register get_current_weather tool
            Map<String, Object> currentWeatherSchema = Map.of(
                "type", "object",
                "properties", Map.of("city", Map.of("type", "string", "description", "City name (e.g., \"Moscow\", \"Санкт-Петербург\")")),
                "required", List.of("city")
            );
            McpServerFeatures.SyncToolSpecification currentWeatherTool = McpServerFeatures.SyncToolSpecification.builder()
                .tool(McpSchema.Tool.builder("get_current_weather", currentWeatherSchema)
                    .description("Get current weather conditions for a specified city")
                    .build())
                .callHandler((exchange, request) -> handleGetCurrentWeather(request))
                .build();
            server.addTool(currentWeatherTool);

            // Register get_forecast tool
            Map<String, Object> forecastSchema = Map.of(
                "type", "object",
                "properties", Map.of(
                    "city", Map.of("type", "string", "description", "City name"),
                    "days", Map.of("type", "integer", "description", "Number of days (1-16, default: 7)")
                ),
                "required", List.of("city")
            );
            McpServerFeatures.SyncToolSpecification forecastTool = McpServerFeatures.SyncToolSpecification.builder()
                .tool(McpSchema.Tool.builder("get_forecast", forecastSchema)
                    .description("Get weather forecast for a specified city")
                    .build())
                .callHandler((exchange, request) -> handleGetForecast(request))
                .build();
            server.addTool(forecastTool);

            // Register search_city tool
            Map<String, Object> searchSchema = Map.of(
                "type", "object",
                "properties", Map.of("query", Map.of("type", "string", "description", "Search term (city name or partial match)")),
                "required", List.of("query")
            );
            McpServerFeatures.SyncToolSpecification searchTool = McpServerFeatures.SyncToolSpecification.builder()
                .tool(McpSchema.Tool.builder("search_city", searchSchema)
                    .description("Search for cities matching a query string")
                    .build())
                .callHandler((exchange, request) -> handleSearchCity(request))
                .build();
            server.addTool(searchTool);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register MCP tools", e);
        }
    }

    /**
     * Handle get_current_weather tool call.
     * Returns dual TextContent: human-readable summary + JSON data.
     */
    McpSchema.CallToolResult handleGetCurrentWeather(McpSchema.CallToolRequest request) {
        try {
            String city = (String) request.arguments().get("city");
            if (city == null || city.trim().isEmpty()) {
                return createErrorResult("City parameter is required");
            }

            CurrentWeather weather = weatherClient.getCurrentWeather(city);
            boolean isRussian = LanguageUtils.isRussian(city);

            // Create human-readable summary
            String summary = isRussian
                ? String.format("В %s: %.1f°C, %s. Ощущается как %.1f°C, влажность %d%%, давление %d гПа, ветер %s %.1f м/с",
                    city, weather.temperature(), weather.description(), weather.feelsLike(),
                    weather.humidity(), weather.pressure(), weather.windDirection(), weather.windSpeed())
                : String.format("In %s: %.1f°C, %s. Feels like %.1f°C, humidity %d%%, pressure %d hPa, wind %s %.1f m/s",
                    city, weather.temperature(), weather.description(), weather.feelsLike(),
                    weather.humidity(), weather.pressure(), weather.windDirection(), weather.windSpeed());

            // Create JSON data
            String jsonData = objectMapper.writeValueAsString(Map.of(
                "city", city,
                "temperature", weather.temperature(),
                "feelsLike", weather.feelsLike(),
                "humidity", weather.humidity(),
                "pressure", weather.pressure(),
                "windSpeed", weather.windSpeed(),
                "windDirection", weather.windDirection(),
                "weatherCode", weather.weatherCode(),
                "description", weather.description()
            ));

            List<McpSchema.Content> content = new ArrayList<>();
            content.add(new McpSchema.TextContent(summary));
            content.add(new McpSchema.TextContent(jsonData));

            return new McpSchema.CallToolResult(content, false, null, null);

        } catch (WeatherApiException e) {
            return createErrorResult("Error: " + e.getMessage());
        } catch (JsonProcessingException e) {
            return createErrorResult("Error formatting response: " + e.getMessage());
        } catch (Exception e) {
            return createErrorResult("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Handle get_forecast tool call.
     * Returns dual TextContent: human-readable summary + JSON array.
     */
    McpSchema.CallToolResult handleGetForecast(McpSchema.CallToolRequest request) {
        try {
            String city = (String) request.arguments().get("city");
            Object daysObj = request.arguments().get("days");
            int days = (daysObj instanceof Number) ? ((Number) daysObj).intValue() : 7;

            // Validate days parameter
            if (days < 1 || days > 16) {
                return createErrorResult(String.format("Days parameter must be between 1 and 16, got: %d", days));
            }

            if (city == null || city.trim().isEmpty()) {
                return createErrorResult("City parameter is required");
            }

            List<DailyForecast> forecast = weatherClient.getForecast(city, days);
            boolean isRussian = LanguageUtils.isRussian(city);

            // Create human-readable summary
            String summary = isRussian
                ? String.format("%d-дневный прогноз для %s:", days, city)
                : String.format("%d-day forecast for %s:", days, city);

            StringBuilder details = new StringBuilder(summary);
            for (DailyForecast day : forecast) {
                details.append(String.format("\n  %s: %.1f°C / %.1f°C, осадки %.1f мм, код погоды %d",
                    day.date(), day.temperatureMax(), day.temperatureMin(),
                    day.precipitationSum(), day.weatherCode()));
            }

            // Create JSON array
            String jsonData = objectMapper.writeValueAsString(forecast.stream()
                .map(d -> Map.of(
                    "date", d.date(),
                    "temperatureMax", d.temperatureMax(),
                    "temperatureMin", d.temperatureMin(),
                    "precipitationSum", d.precipitationSum(),
                    "weatherCode", d.weatherCode()
                ))
                .toList());

            List<McpSchema.Content> content = new ArrayList<>();
            content.add(new McpSchema.TextContent(details.toString()));
            content.add(new McpSchema.TextContent(jsonData));

            return new McpSchema.CallToolResult(content, false, null, null);

        } catch (IllegalArgumentException e) {
            return createErrorResult("Error: " + e.getMessage());
        } catch (WeatherApiException e) {
            return createErrorResult("Error: " + e.getMessage());
        } catch (JsonProcessingException e) {
            return createErrorResult("Error formatting response: " + e.getMessage());
        } catch (Exception e) {
            return createErrorResult("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Handle search_city tool call.
     * Returns dual TextContent: human-readable summary + JSON array of locations.
     */
    McpSchema.CallToolResult handleSearchCity(McpSchema.CallToolRequest request) {
        try {
            String query = (String) request.arguments().get("query");
            if (query == null || query.trim().isEmpty()) {
                return createErrorResult("Query parameter is required");
            }

            List<Location> locations = weatherClient.searchCities(query);

            if (locations.isEmpty()) {
                return createErrorResult("No locations found for query: " + query);
            }

            // Create human-readable summary
            String summary = String.format("Found %d location(s) matching '%s':", locations.size(), query);
            StringBuilder details = new StringBuilder(summary);
            for (Location loc : locations) {
                details.append(String.format("\n  %s, %s (%.4f, %.4f), %s, %.0fм",
                    loc.name(), loc.country(), loc.latitude(), loc.longitude(),
                    loc.timezone(), loc.elevation()));
            }

            // Create JSON array
            String jsonData = objectMapper.writeValueAsString(locations.stream()
                .map(l -> Map.of(
                    "name", l.name(),
                    "country", l.country(),
                    "latitude", l.latitude(),
                    "longitude", l.longitude(),
                    "timezone", l.timezone(),
                    "elevation", l.elevation()
                ))
                .toList());

            List<McpSchema.Content> content = new ArrayList<>();
            content.add(new McpSchema.TextContent(details.toString()));
            content.add(new McpSchema.TextContent(jsonData));

            return new McpSchema.CallToolResult(content, false, null, null);

        } catch (WeatherApiException e) {
            return createErrorResult("Error: " + e.getMessage());
        } catch (JsonProcessingException e) {
            return createErrorResult("Error formatting response: " + e.getMessage());
        } catch (Exception e) {
            return createErrorResult("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Create an error result.
     */
    private McpSchema.CallToolResult createErrorResult(String message) {
        List<McpSchema.Content> content = List.of(new McpSchema.TextContent(message));
        return new McpSchema.CallToolResult(content, true, null, null);
    }
}
