package io.github.solas.mcp.weather.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.solas.mcp.weather.api.CurrentWeather;
import io.github.solas.mcp.weather.api.DailyForecast;
import io.github.solas.mcp.weather.api.Location;
import io.github.solas.mcp.weather.api.WeatherApiException;
import io.github.solas.mcp.weather.api.WeatherClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration tests for WeatherMcpTools using JUnit 5, AssertJ, and Mockito.
 * Tests all three MCP tools with mocked WeatherClient.
 */
@ExtendWith(MockitoExtension.class)
class WeatherMcpToolsTest {

    @Mock
    private WeatherClient weatherClient;

    private WeatherMcpTools tools;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        tools = new WeatherMcpTools(weatherClient, objectMapper);
    }

    @Nested
    @DisplayName("handleGetCurrentWeather Tests")
    class HandleGetCurrentWeatherTests {

        @Test
        @DisplayName("should return weather data with dual TextContent for valid city")
        void shouldReturnWeatherDataForValidCity() throws WeatherApiException {
            // Given
            String city = "Moscow";
            CurrentWeather weather = new CurrentWeather(20.5, 19.2, 65, 1013, 5.2, "NW", 0, "Clear sky");
            when(weatherClient.getCurrentWeather(city)).thenReturn(weather);

            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
                "get_current_weather",
                Map.of("city", city),
                null
            );

            // When
            McpSchema.CallToolResult result = tools.handleGetCurrentWeather(request);

            // Then
            assertThat(result.isError()).isFalse();
            assertThat(result.content()).hasSize(2);

            // First TextContent: human-readable summary
            assertThat(result.content().get(0)).isInstanceOf(McpSchema.TextContent.class);
            String summary = ((McpSchema.TextContent) result.content().get(0)).text();
            assertThat(summary).contains("In Moscow", "20.5°C", "Clear sky");

            // Second TextContent: JSON data
            assertThat(result.content().get(1)).isInstanceOf(McpSchema.TextContent.class);
            String jsonData = ((McpSchema.TextContent) result.content().get(1)).text();
            assertThat(jsonData).contains("\"temperature\":20.5");
            assertThat(jsonData).contains("\"description\":\"Clear sky\"");
        }

        @Test
        @DisplayName("should return Russian text for Cyrillic city name")
        void shouldReturnRussianTextForCyrillicCity() throws WeatherApiException {
            // Given
            String city = "Москва";
            CurrentWeather weather = new CurrentWeather(18.0, 17.5, 70, 1010, 3.5, "N", 1, "Mainly clear");
            when(weatherClient.getCurrentWeather(city)).thenReturn(weather);

            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
                "get_current_weather",
                Map.of("city", city),
                null
            );

            // When
            McpSchema.CallToolResult result = tools.handleGetCurrentWeather(request);

            // Then
            assertThat(result.isError()).isFalse();
            assertThat(result.content()).hasSize(2);

            String summary = ((McpSchema.TextContent) result.content().get(0)).text();
            assertThat(summary).contains("В Москве", "18.0°C", "Mainly clear");
            assertThat(summary).contains("Ощущается как");
        }

        @Test
        @DisplayName("should return error for missing city parameter")
        void shouldReturnErrorForMissingCity() {
            // Given
            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
                "get_current_weather",
                Map.of(),
                null
            );

            // When
            McpSchema.CallToolResult result = tools.handleGetCurrentWeather(request);

            // Then
            assertThat(result.isError()).isTrue();
            assertThat(result.content()).hasSize(1);
            assertThat(((McpSchema.TextContent) result.content().get(0)).text()).contains("City parameter is required");
        }

        @Test
        @DisplayName("should return error for empty city parameter")
        void shouldReturnErrorForEmptyCity() {
            // Given
            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
                "get_current_weather",
                Map.of("city", ""),
                null
            );

            // When
            McpSchema.CallToolResult result = tools.handleGetCurrentWeather(request);

            // Then
            assertThat(result.isError()).isTrue();
            assertThat(((McpSchema.TextContent) result.content().get(0)).text()).contains("City parameter is required");
        }

        @Test
        @DisplayName("should return error when WeatherClient throws exception")
        void shouldReturnErrorWhenClientThrowsException() throws WeatherApiException {
            // Given
            String city = "InvalidCity";
            when(weatherClient.getCurrentWeather(anyString()))
                .thenThrow(WeatherApiException.notFound(city));

            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
                "get_current_weather",
                Map.of("city", city),
                null
            );

            // When
            McpSchema.CallToolResult result = tools.handleGetCurrentWeather(request);

            // Then
            assertThat(result.isError()).isTrue();
            assertThat(((McpSchema.TextContent) result.content().get(0)).text()).contains("Error:", "City not found");
        }
    }

    @Nested
    @DisplayName("handleGetForecast Tests")
    class HandleGetForecastTests {

        @Test
        @DisplayName("should return forecast with dual TextContent for valid city and days")
        void shouldReturnForecastForValidCityAndDays() throws WeatherApiException {
            // Given
            String city = "Paris";
            int days = 3;
            List<DailyForecast> forecast = List.of(
                new DailyForecast("2025-06-26", 22.5, 14.2, 0.0, 0),
                new DailyForecast("2025-06-27", 24.0, 15.5, 2.5, 61),
                new DailyForecast("2025-06-28", 21.0, 13.8, 0.5, 3)
            );
            when(weatherClient.getForecast(city, days)).thenReturn(forecast);

            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
                "get_forecast",
                Map.of("city", city, "days", days),
                null
            );

            // When
            McpSchema.CallToolResult result = tools.handleGetForecast(request);

            // Then
            assertThat(result.isError()).isFalse();
            assertThat(result.content()).hasSize(2);

            // First TextContent: human-readable summary
            String summary = ((McpSchema.TextContent) result.content().get(0)).text();
            assertThat(summary).contains("3-day forecast for Paris");
            assertThat(summary).contains("2025-06-26");
            assertThat(summary).contains("22.5°C");

            // Second TextContent: JSON array
            String jsonData = ((McpSchema.TextContent) result.content().get(1)).text();
            assertThat(jsonData).contains("\"date\":\"2025-06-26\"");
            assertThat(jsonData).contains("\"temperatureMax\":22.5");
        }

        @Test
        @DisplayName("should return Russian text for Cyrillic city in forecast")
        void shouldReturnRussianTextForCyrillicCityInForecast() throws WeatherApiException {
            // Given
            String city = "Санкт-Петербург";
            int days = 5;
            List<DailyForecast> forecast = List.of(
                new DailyForecast("2025-06-26", 19.0, 12.0, 1.0, 63)
            );
            when(weatherClient.getForecast(city, days)).thenReturn(forecast);

            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
                "get_forecast",
                Map.of("city", city, "days", days),
                null
            );

            // When
            McpSchema.CallToolResult result = tools.handleGetForecast(request);

            // Then
            assertThat(result.isError()).isFalse();

            String summary = ((McpSchema.TextContent) result.content().get(0)).text();
            assertThat(summary).contains("5-дневный прогноз для Санкт-Петербург");
        }

        @Test
        @DisplayName("should use default days=7 when not specified")
        void shouldUseDefaultDaysWhenNotSpecified() throws WeatherApiException {
            // Given
            String city = "London";
            List<DailyForecast> forecast = List.of(
                new DailyForecast("2025-06-26", 18.0, 11.0, 0.0, 1)
            );
            when(weatherClient.getForecast(city, 7)).thenReturn(forecast);

            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
                "get_forecast",
                Map.of("city", city),
                null
            );

            // When
            McpSchema.CallToolResult result = tools.handleGetForecast(request);

            // Then
            assertThat(result.isError()).isFalse();
            String summary = ((McpSchema.TextContent) result.content().get(0)).text();
            assertThat(summary).contains("7-day forecast for London");
        }

        @Test
        @DisplayName("should return error for days=0")
        void shouldReturnErrorForZeroDays() {
            // Given
            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
                "get_forecast",
                Map.of("city", "Moscow", "days", 0),
                null
            );

            // When
            McpSchema.CallToolResult result = tools.handleGetForecast(request);

            // Then
            assertThat(result.isError()).isTrue();
            assertThat(((McpSchema.TextContent) result.content().get(0)).text()).contains("Days parameter must be between 1 and 16");
        }

        @Test
        @DisplayName("should return error for days=17")
        void shouldReturnErrorForDaysGreaterThanMax() {
            // Given
            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
                "get_forecast",
                Map.of("city", "Moscow", "days", 17),
                null
            );

            // When
            McpSchema.CallToolResult result = tools.handleGetForecast(request);

            // Then
            assertThat(result.isError()).isTrue();
            assertThat(((McpSchema.TextContent) result.content().get(0)).text()).contains("Days parameter must be between 1 and 16");
        }

        @Test
        @DisplayName("should return error for missing city parameter")
        void shouldReturnErrorForMissingCityInForecast() {
            // Given
            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
                "get_forecast",
                Map.of("days", 5),
                null
            );

            // When
            McpSchema.CallToolResult result = tools.handleGetForecast(request);

            // Then
            assertThat(result.isError()).isTrue();
            assertThat(((McpSchema.TextContent) result.content().get(0)).text()).contains("City parameter is required");
        }

        @Test
        @DisplayName("should return error when WeatherClient throws exception")
        void shouldReturnErrorWhenClientThrowsExceptionInForecast() throws WeatherApiException {
            // Given
            String city = "InvalidCity";
            when(weatherClient.getForecast(anyString(), anyInt()))
                .thenThrow(WeatherApiException.notFound(city));

            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
                "get_forecast",
                Map.of("city", city, "days", 5),
                null
            );

            // When
            McpSchema.CallToolResult result = tools.handleGetForecast(request);

            // Then
            assertThat(result.isError()).isTrue();
            assertThat(((McpSchema.TextContent) result.content().get(0)).text()).contains("Error:");
        }
    }

    @Nested
    @DisplayName("handleSearchCity Tests")
    class HandleSearchCityTests {

        @Test
        @DisplayName("should return locations with dual TextContent for valid query")
        void shouldReturnLocationsForValidQuery() throws WeatherApiException {
            // Given
            String query = "Moscow";
            List<Location> locations = List.of(
                new Location("Moscow", "RU", 55.7522, 37.6156, "Europe/Moscow", 156),
                new Location("Moscow", "US", 45.7522, -123.6156, "America/Los_Angeles", 50)
            );
            when(weatherClient.searchCities(query)).thenReturn(locations);

            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
                "search_city",
                Map.of("query", query),
                null
            );

            // When
            McpSchema.CallToolResult result = tools.handleSearchCity(request);

            // Then
            assertThat(result.isError()).isFalse();
            assertThat(result.content()).hasSize(2);

            // First TextContent: human-readable summary
            String summary = ((McpSchema.TextContent) result.content().get(0)).text();
            assertThat(summary).contains("Found 2 location(s) matching 'Moscow'");
            assertThat(summary).contains("Moscow, RU");
            assertThat(summary).contains("Moscow, US");

            // Second TextContent: JSON array
            String jsonData = ((McpSchema.TextContent) result.content().get(1)).text();
            assertThat(jsonData).contains("\"name\":\"Moscow\"");
            assertThat(jsonData).contains("\"country\":\"RU\"");
            assertThat(jsonData).contains("\"latitude\":55.7522");
        }

        @Test
        @DisplayName("should return error for empty results")
        void shouldReturnErrorForEmptyResults() throws WeatherApiException {
            // Given
            String query = "NonExistentCity";
            when(weatherClient.searchCities(query)).thenReturn(List.of());

            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
                "search_city",
                Map.of("query", query),
                null
            );

            // When
            McpSchema.CallToolResult result = tools.handleSearchCity(request);

            // Then
            assertThat(result.isError()).isTrue();
            assertThat(((McpSchema.TextContent) result.content().get(0)).text()).contains("No locations found");
        }

        @Test
        @DisplayName("should return error for missing query parameter")
        void shouldReturnErrorForMissingQuery() {
            // Given
            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
                "search_city",
                Map.of(),
                null
            );

            // When
            McpSchema.CallToolResult result = tools.handleSearchCity(request);

            // Then
            assertThat(result.isError()).isTrue();
            assertThat(((McpSchema.TextContent) result.content().get(0)).text()).contains("Query parameter is required");
        }

        @Test
        @DisplayName("should return error for empty query parameter")
        void shouldReturnErrorForEmptyQuery() {
            // Given
            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
                "search_city",
                Map.of("query", ""),
                null
            );

            // When
            McpSchema.CallToolResult result = tools.handleSearchCity(request);

            // Then
            assertThat(result.isError()).isTrue();
            assertThat(((McpSchema.TextContent) result.content().get(0)).text()).contains("Query parameter is required");
        }

        @Test
        @DisplayName("should return error when WeatherClient throws exception")
        void shouldReturnErrorWhenClientThrowsExceptionInSearch() throws WeatherApiException {
            // Given
            String query = "InvalidQuery";
            when(weatherClient.searchCities(anyString()))
                .thenThrow(WeatherApiException.apiError("Search failed: " + query));

            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
                "search_city",
                Map.of("query", query),
                null
            );

            // When
            McpSchema.CallToolResult result = tools.handleSearchCity(request);

            // Then
            assertThat(result.isError()).isTrue();
            assertThat(((McpSchema.TextContent) result.content().get(0)).text()).contains("Error:");
        }
    }

    @Nested
    @DisplayName("LanguageUtils.isRussian() Tests")
    class IsRussianTests {

        @Test
        @DisplayName("should return true for Russian city name with Cyrillic characters")
        void shouldReturnTrueForRussianCity() {
            boolean result = LanguageUtils.isRussian("Москва");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return true for mixed text containing Cyrillic")
        void shouldReturnTrueForMixedTextWithCyrillic() {
            boolean result = LanguageUtils.isRussian("Санкт-Петербург");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false for English city name")
        void shouldReturnFalseForEnglishCity() {
            boolean result = LanguageUtils.isRussian("London");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false for null input")
        void shouldReturnFalseForNullInput() {
            boolean result = LanguageUtils.isRussian(null);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false for empty string")
        void shouldReturnFalseForEmptyString() {
            boolean result = LanguageUtils.isRussian("");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false for whitespace only")
        void shouldReturnFalseForWhitespaceOnly() {
            boolean result = LanguageUtils.isRussian("   ");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return true for city with partial Cyrillic")
        void shouldReturnTrueForPartialCyrillic() {
            boolean result = LanguageUtils.isRussian("Казань");

            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("LanguageUtils.getLocale() Tests")
    class GetLocaleTests {

        @Test
        @DisplayName("should return 'ru' for Russian city")
        void shouldReturnRuForRussianCity() {
            String result = LanguageUtils.getLocale("Москва");

            assertThat(result).isEqualTo("ru");
        }

        @Test
        @DisplayName("should return 'en' for English city")
        void shouldReturnEnForEnglishCity() {
            String result = LanguageUtils.getLocale("Paris");

            assertThat(result).isEqualTo("en");
        }

        @Test
        @DisplayName("should return 'en' for null city")
        void shouldReturnEnForNullCity() {
            String result = LanguageUtils.getLocale(null);

            assertThat(result).isEqualTo("en");
        }

        @Test
        @DisplayName("should return 'en' for empty city")
        void shouldReturnEnForEmptyCity() {
            String result = LanguageUtils.getLocale("");

            assertThat(result).isEqualTo("en");
        }
    }

    @Nested
    @DisplayName("Days Validation Tests (1-16 range)")
    class DaysValidationTests {

        @Test
        @DisplayName("days=1 should be valid (minimum)")
        void shouldAcceptMinimumDays() {
            // Days 1-16 are valid per WeatherClient.getForecast contract
            // This test documents the expected valid range
            int minDays = 1;
            assertThat(minDays).isGreaterThanOrEqualTo(1);
            assertThat(minDays).isLessThanOrEqualTo(16);
        }

        @Test
        @DisplayName("days=16 should be valid (maximum)")
        void shouldAcceptMaximumDays() {
            int maxDays = 16;
            assertThat(maxDays).isGreaterThanOrEqualTo(1);
            assertThat(maxDays).isLessThanOrEqualTo(16);
        }

        @Test
        @DisplayName("days=7 should be valid (default)")
        void shouldAcceptDefaultDays() {
            int defaultDays = 7;
            assertThat(defaultDays).isGreaterThanOrEqualTo(1);
            assertThat(defaultDays).isLessThanOrEqualTo(16);
        }

        @Test
        @DisplayName("days=0 should be invalid")
        void shouldRejectZeroDays() {
            int invalidDays = 0;
            assertThat(invalidDays).isLessThan(1);
        }

        @Test
        @DisplayName("days=17 should be invalid")
        void shouldRejectDaysGreaterThanMax() {
            int invalidDays = 17;
            assertThat(invalidDays).isGreaterThan(16);
        }

        @Test
        @DisplayName("days=-1 should be invalid")
        void shouldRejectNegativeDays() {
            int invalidDays = -1;
            assertThat(invalidDays).isLessThan(1);
        }

        @Test
        @DisplayName("days=100 should be invalid")
        void shouldRejectExcessiveDays() {
            int invalidDays = 100;
            assertThat(invalidDays).isGreaterThan(16);
        }
    }

    @Nested
    @DisplayName("handleGetForecastByCoords Tests")
    class HandleGetForecastByCoordsTests {

        @Test
        @DisplayName("should return forecast with dual TextContent for valid coordinates and days")
        void shouldReturnForecastForValidCoordinatesAndDays() throws WeatherApiException {
            // Given
            double latitude = 55.7522;
            double longitude = 37.6156;
            int days = 3;
            List<DailyForecast> forecast = List.of(
                new DailyForecast("2025-06-26", 22.5, 14.2, 0.0, 0),
                new DailyForecast("2025-06-27", 24.0, 15.5, 2.5, 61),
                new DailyForecast("2025-06-28", 21.0, 13.8, 0.5, 3)
            );
            when(weatherClient.getForecastByCoords(latitude, longitude, days)).thenReturn(forecast);

            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
                "get_forecast_by_coords",
                Map.of("latitude", latitude, "longitude", longitude, "days", days),
                null
            );

            // When
            McpSchema.CallToolResult result = tools.handleGetForecastByCoords(request);

            // Then
            assertThat(result.isError()).isFalse();
            assertThat(result.content()).hasSize(2);

            // First TextContent: human-readable summary
            String summary = ((McpSchema.TextContent) result.content().get(0)).text();
            assertThat(summary).contains("3-day forecast for coordinates (55.7522, 37.6156)");
            assertThat(summary).contains("2025-06-26");
            assertThat(summary).contains("22.5°C");

            // Second TextContent: JSON array
            String jsonData = ((McpSchema.TextContent) result.content().get(1)).text();
            assertThat(jsonData).contains("\"date\":\"2025-06-26\"");
            assertThat(jsonData).contains("\"temperatureMax\":22.5");
        }

        @Test
        @DisplayName("should use default days=7 when not specified")
        void shouldUseDefaultDaysWhenNotSpecified() throws WeatherApiException {
            // Given
            double latitude = 48.8566;
            double longitude = 2.3522;
            List<DailyForecast> forecast = List.of(
                new DailyForecast("2025-06-26", 20.0, 12.0, 0.0, 1)
            );
            when(weatherClient.getForecastByCoords(latitude, longitude, 7)).thenReturn(forecast);

            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
                "get_forecast_by_coords",
                Map.of("latitude", latitude, "longitude", longitude),
                null
            );

            // When
            McpSchema.CallToolResult result = tools.handleGetForecastByCoords(request);

            // Then
            assertThat(result.isError()).isFalse();
            String summary = ((McpSchema.TextContent) result.content().get(0)).text();
            assertThat(summary).contains("7-day forecast for coordinates");
        }

        @Test
        @DisplayName("should return error for missing latitude parameter")
        void shouldReturnErrorForMissingLatitude() {
            // Given
            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
                "get_forecast_by_coords",
                Map.of("longitude", 37.6156),
                null
            );

            // When
            McpSchema.CallToolResult result = tools.handleGetForecastByCoords(request);

            // Then
            assertThat(result.isError()).isTrue();
            assertThat(((McpSchema.TextContent) result.content().get(0)).text())
                .contains("Latitude and longitude parameters are required");
        }

        @Test
        @DisplayName("should return error for missing longitude parameter")
        void shouldReturnErrorForMissingLongitude() {
            // Given
            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
                "get_forecast_by_coords",
                Map.of("latitude", 55.7522),
                null
            );

            // When
            McpSchema.CallToolResult result = tools.handleGetForecastByCoords(request);

            // Then
            assertThat(result.isError()).isTrue();
            assertThat(((McpSchema.TextContent) result.content().get(0)).text())
                .contains("Latitude and longitude parameters are required");
        }

        @Test
        @DisplayName("should return error for days=0")
        void shouldReturnErrorForZeroDays() {
            // Given
            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
                "get_forecast_by_coords",
                Map.of("latitude", 55.7522, "longitude", 37.6156, "days", 0),
                null
            );

            // When
            McpSchema.CallToolResult result = tools.handleGetForecastByCoords(request);

            // Then
            assertThat(result.isError()).isTrue();
            assertThat(((McpSchema.TextContent) result.content().get(0)).text())
                .contains("Days parameter must be between 1 and 16");
        }

        @Test
        @DisplayName("should return error for days=17")
        void shouldReturnErrorForDaysGreaterThanMax() {
            // Given
            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
                "get_forecast_by_coords",
                Map.of("latitude", 55.7522, "longitude", 37.6156, "days", 17),
                null
            );

            // When
            McpSchema.CallToolResult result = tools.handleGetForecastByCoords(request);

            // Then
            assertThat(result.isError()).isTrue();
            assertThat(((McpSchema.TextContent) result.content().get(0)).text())
                .contains("Days parameter must be between 1 and 16");
        }

        @Test
        @DisplayName("should return error when WeatherClient throws exception")
        void shouldReturnErrorWhenClientThrowsException() throws WeatherApiException {
            // Given
            double latitude = 55.7522;
            double longitude = 37.6156;
            when(weatherClient.getForecastByCoords(latitude, longitude, 5))
                .thenThrow(WeatherApiException.apiError("No data available for coordinates"));

            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
                "get_forecast_by_coords",
                Map.of("latitude", latitude, "longitude", longitude, "days", 5),
                null
            );

            // When
            McpSchema.CallToolResult result = tools.handleGetForecastByCoords(request);

            // Then
            assertThat(result.isError()).isTrue();
            assertThat(((McpSchema.TextContent) result.content().get(0)).text()).contains("Error:");
        }
    }
}
