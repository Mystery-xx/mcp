package io.github.solas.mcp.weather.client;

import io.github.solas.mcp.weather.api.CurrentWeather;
import io.github.solas.mcp.weather.api.DailyForecast;
import io.github.solas.mcp.weather.api.Location;
import io.github.solas.mcp.weather.api.WeatherApiException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class WttrInClientTest {

    private MockWebServer mockWebServer;
    private RestTemplate restTemplate;
    private TestableWttrInClient client;
    private TestableNominatimClient nominatimClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        restTemplate = new RestTemplate();
        nominatimClient = new TestableNominatimClient(restTemplate);
        String mockUrl = mockWebServer.url("/").toString();
        client = new TestableWttrInClient(restTemplate, nominatimClient, mockUrl);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void testGetCurrentWeatherSuccess() throws WeatherApiException {
        // Given: Mock wttr.in response with current condition
        String jsonResponse = """
            {
                "current_condition": [
                    {
                        "temp_C": "20",
                        "FeelsLikeC": "19",
                        "humidity": "65",
                        "pressure": "1013",
                        "windspeedKmph": "12",
                        "winddir16Point": "NW",
                        "weatherCode": "0",
                        "weatherDesc": [{"value": "Clear sky"}]
                    }
                ],
                "weather": [],
                "request": []
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json"));

        // When: Get current weather
        CurrentWeather result = client.getCurrentWeather("Moscow");

        // Then: Verify response mapping
        assertThat(result).isNotNull();
        assertThat(result.temperature()).isEqualTo(20.0);
        assertThat(result.feelsLike()).isEqualTo(19.0);
        assertThat(result.humidity()).isEqualTo(65);
        assertThat(result.pressure()).isEqualTo(1013);
        assertThat(result.windSpeed()).isEqualTo(12.0);
        assertThat(result.windDirection()).isEqualTo("NW");
        assertThat(result.weatherCode()).isEqualTo(0);
        assertThat(result.description()).isEqualTo("Clear sky");
    }

    @Test
    void testGetCurrentWeatherWithEmptyWeatherDesc() throws WeatherApiException {
        // Given: Response with empty weather description (should fallback to WMO code)
        String jsonResponse = """
            {
                "current_condition": [
                    {
                        "temp_C": "15",
                        "FeelsLikeC": "14",
                        "humidity": "70",
                        "pressure": "1010",
                        "windspeedKmph": "8",
                        "winddir16Point": "E",
                        "weatherCode": "116",
                        "weatherDesc": []
                    }
                ],
                "weather": [],
                "request": []
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json"));

        // When: Get current weather
        CurrentWeather result = client.getCurrentWeather("Berlin");

        // Then: Should fallback to WMO code description
        assertThat(result).isNotNull();
        assertThat(result.weatherCode()).isEqualTo(116);
        assertThat(result.description()).isEqualTo("Partly Cloudy");
    }

    @Test
    void testGetCurrentWeatherWithNullWeatherDesc() throws WeatherApiException {
        // Given: Response with null weather description
        String jsonResponse = """
            {
                "current_condition": [
                    {
                        "temp_C": "18",
                        "FeelsLikeC": "17",
                        "humidity": "60",
                        "pressure": "1015",
                        "windspeedKmph": "10",
                        "winddir16Point": "S",
                        "weatherCode": "296",
                        "weatherDesc": null
                    }
                ],
                "weather": [],
                "request": []
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json"));

        // When: Get current weather
        CurrentWeather result = client.getCurrentWeather("Paris");

        // Then: Should fallback to WMO code description
        assertThat(result).isNotNull();
        assertThat(result.weatherCode()).isEqualTo(296);
        assertThat(result.description()).isEqualTo("Light rain");
    }

    @Test
    void testGetCurrentWeatherEmptyConditionList() {
        // Given: Response with empty current_condition array
        String jsonResponse = """
            {
                "current_condition": [],
                "weather": [],
                "request": []
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json"));

        // When/Then: Should throw WeatherApiException
        assertThatThrownBy(() -> client.getCurrentWeather("London"))
            .isInstanceOf(WeatherApiException.class)
            .hasMessageContaining("No current condition data available");
    }

    @Test
    void testGetCurrentWeatherServerError() {
        // Given: Server returns 500 error
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .setBody("Internal Server Error"));

        // When/Then: Should throw WeatherApiException
        assertThatThrownBy(() -> client.getCurrentWeather("Rome"))
            .isInstanceOf(WeatherApiException.class)
            .hasMessageContaining("Failed to get current weather");
    }

    @Test
    void testGetForecastSuccess() throws WeatherApiException {
        // Given: Mock forecast response with 3 days
        String jsonResponse = """
            {
                "current_condition": [],
                "weather": [
                    {
                        "date": "2024-06-25",
                        "maxtempC": "22",
                        "mintempC": "14",
                        "hourly": [
                            {"time": "0", "weatherCode": "0", "precipMM": "0.0"},
                            {"time": "1200", "weatherCode": "1", "precipMM": "0.0"},
                            {"time": "2300", "weatherCode": "0", "precipMM": "0.0"}
                        ]
                    },
                    {
                        "date": "2024-06-26",
                        "maxtempC": "20",
                        "mintempC": "13",
                        "hourly": [
                            {"time": "0", "weatherCode": "61", "precipMM": "2.5"},
                            {"time": "1200", "weatherCode": "61", "precipMM": "3.0"},
                            {"time": "2300", "weatherCode": "61", "precipMM": "1.5"}
                        ]
                    },
                    {
                        "date": "2024-06-27",
                        "maxtempC": "25",
                        "mintempC": "15",
                        "hourly": [
                            {"time": "0", "weatherCode": "0", "precipMM": "0.0"},
                            {"time": "1200", "weatherCode": "0", "precipMM": "0.0"},
                            {"time": "2300", "weatherCode": "0", "precipMM": "0.0"}
                        ]
                    }
                ],
                "request": []
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json"));

        // When: Get 3-day forecast
        List<DailyForecast> result = client.getForecast("Madrid", 3);

        // Then: Verify response mapping
        assertThat(result).hasSize(3);

        // Day 1
        assertThat(result.get(0).date()).isEqualTo("2024-06-25");
        assertThat(result.get(0).temperatureMax()).isEqualTo(22.0);
        assertThat(result.get(0).temperatureMin()).isEqualTo(14.0);
        assertThat(result.get(0).weatherCode()).isEqualTo(1); // Midday code
        assertThat(result.get(0).precipitationSum()).isEqualTo(0.0);

        // Day 2 (rainy)
        assertThat(result.get(1).date()).isEqualTo("2024-06-26");
        assertThat(result.get(1).weatherCode()).isEqualTo(61);
        assertThat(result.get(1).precipitationSum()).isEqualTo(7.0); // Sum of hourly

        // Day 3
        assertThat(result.get(2).date()).isEqualTo("2024-06-27");
        assertThat(result.get(2).temperatureMax()).isEqualTo(25.0);
    }

    @Test
    void testGetForecastUsesMiddayWeatherCode() throws WeatherApiException {
        // Given: Forecast with varied hourly codes
        String jsonResponse = """
            {
                "current_condition": [],
                "weather": [
                    {
                        "date": "2024-06-25",
                        "maxtempC": "20",
                        "mintempC": "12",
                        "hourly": [
                            {"time": "0", "weatherCode": "61", "precipMM": "0.0"},
                            {"time": "600", "weatherCode": "61", "precipMM": "0.0"},
                            {"time": "1200", "weatherCode": "0", "precipMM": "0.0"},
                            {"time": "1800", "weatherCode": "61", "precipMM": "0.0"}
                        ]
                    }
                ],
                "request": []
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json"));

        // When: Get forecast
        List<DailyForecast> result = client.getForecast("Vienna", 1);

        // Then: Should use midday (1200) weather code
        assertThat(result).hasSize(1);
        assertThat(result.get(0).weatherCode()).isEqualTo(0); // Midday code, not morning/evening
    }

    @Test
    void testGetForecastFallbackToFirstHourlyCode() throws WeatherApiException {
        // Given: Forecast without 1200 hourly entry
        String jsonResponse = """
            {
                "current_condition": [],
                "weather": [
                    {
                        "date": "2024-06-25",
                        "maxtempC": "18",
                        "mintempC": "10",
                        "hourly": [
                            {"time": "0", "weatherCode": "3", "precipMM": "0.0"},
                            {"time": "300", "weatherCode": "3", "precipMM": "0.0"}
                        ]
                    }
                ],
                "request": []
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json"));

        // When: Get forecast
        List<DailyForecast> result = client.getForecast("Prague", 1);

        // Then: Should fallback to first hourly code
        assertThat(result).hasSize(1);
        assertThat(result.get(0).weatherCode()).isEqualTo(3);
    }

    @Test
    void testGetForecastInvalidDaysParameter() {
        // Given: Invalid days parameter (out of range)

        // When/Then: Should throw IllegalArgumentException
        assertThatThrownBy(() -> client.getForecast("Warsaw", 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Days parameter must be between 1 and 16");

        assertThatThrownBy(() -> client.getForecast("Warsaw", 17))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Days parameter must be between 1 and 16");
    }

    @Test
    void testGetForecastEmptyWeatherList() {
        // Given: Response with empty weather array
        String jsonResponse = """
            {
                "current_condition": [],
                "weather": [],
                "request": []
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json"));

        // When/Then: Should throw WeatherApiException
        assertThatThrownBy(() -> client.getForecast("Athens", 3))
            .isInstanceOf(WeatherApiException.class)
            .hasMessageContaining("No weather forecast data available");
    }

    @Test
    void testGetForecastTruncatesToAvailableDays() throws WeatherApiException {
        // Given: Request 10 days but only 3 available
        String jsonResponse = """
            {
                "current_condition": [],
                "weather": [
                    {"date": "2024-06-25", "maxtempC": "20", "mintempC": "12", "hourly": [{"time": "1200", "weatherCode": "0", "precipMM": "0.0"}]},
                    {"date": "2024-06-26", "maxtempC": "21", "mintempC": "13", "hourly": [{"time": "1200", "weatherCode": "1", "precipMM": "0.0"}]},
                    {"date": "2024-06-27", "maxtempC": "22", "mintempC": "14", "hourly": [{"time": "1200", "weatherCode": "2", "precipMM": "0.0"}]}
                ],
                "request": []
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json"));

        // When: Request 10 days
        List<DailyForecast> result = client.getForecast("Budapest", 10);

        // Then: Should return only available 3 days
        assertThat(result).hasSize(3);
    }

    @Test
    void testSearchCitySuccess() throws WeatherApiException {
        // Given: Nominatim client will return a location
        Location expectedLocation = new Location("Moscow", "RU", 55.75, 37.62, "Moscow", 156.0);
        nominatimClient.setMockLocation(expectedLocation);

        // When: Search for city
        Location result = client.searchCity("Moscow");

        // Then: Should return location from Nominatim
        assertThat(result).isEqualTo(expectedLocation);
    }

    @Test
    void testSearchCityNotFound() {
        // Given: Nominatim client returns null
        nominatimClient.setMockLocation(null);

        // When/Then: Should throw WeatherApiException
        assertThatThrownBy(() -> client.searchCity("NonExistentCity"))
            .isInstanceOf(WeatherApiException.class)
            .hasMessageContaining("City not found");
    }

    @Test
    void testSearchCitiesSuccess() throws WeatherApiException {
        // Given: Nominatim client will return multiple locations
        List<Location> expectedLocations = List.of(
            new Location("Moscow", "RU", 55.75, 37.62, "Moscow", 156.0),
            new Location("Moscow", "US", 45.68, -117.46, "Washington", 240.0)
        );
        nominatimClient.setMockLocations(expectedLocations);

        // When: Search for cities
        List<Location> result = client.searchCities("Moscow");

        // Then: Should return list of locations
        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("Moscow");
        assertThat(result.get(1).country()).isEqualTo("US");
    }

    /**
     * Testable WttrInClient that allows overriding the base URL.
     */
    static class TestableWttrInClient extends WttrInClient {
        private final String baseUrl;
        private final RestTemplate restTemplate;

        TestableWttrInClient(RestTemplate restTemplate, NominatimClient nominatimClient, String baseUrl) {
            super(restTemplate, nominatimClient);
            this.restTemplate = restTemplate;
            this.baseUrl = baseUrl;
        }

        @Override
        public CurrentWeather getCurrentWeather(String city) throws WeatherApiException {
            // Override to use mock URL
            try {
                String url = String.format("%s?format=j1", baseUrl);
                org.springframework.http.ResponseEntity<com.fasterxml.jackson.databind.JsonNode> response =
                    restTemplate.getForEntity(url, com.fasterxml.jackson.databind.JsonNode.class);

                if (response.getStatusCode() != org.springframework.http.HttpStatus.OK || response.getBody() == null) {
                    throw WeatherApiException.apiError("No current weather data available for: " + city);
                }

                com.fasterxml.jackson.databind.JsonNode root = response.getBody();
                com.fasterxml.jackson.databind.JsonNode conditions = root.get("current_condition");

                if (conditions == null || conditions.isEmpty()) {
                    throw WeatherApiException.apiError("No current condition data available for: " + city);
                }

                com.fasterxml.jackson.databind.JsonNode condition = conditions.get(0);
                int weatherCode = condition.get("weatherCode").asInt();
                String description = extractDescription(condition);

                return new CurrentWeather(
                    condition.get("temp_C").asDouble(),
                    condition.get("FeelsLikeC").asDouble(),
                    condition.get("humidity").asInt(),
                    condition.get("pressure").asInt(),
                    condition.get("windspeedKmph").asInt(),
                    condition.get("winddir16Point").asText(),
                    weatherCode,
                    description
                );
            } catch (WeatherApiException e) {
                throw e;
            } catch (Exception e) {
                throw WeatherApiException.apiError("Failed to get current weather for: " + city + ": " + e.getMessage());
            }
        }

        @Override
        public List<DailyForecast> getForecast(String city, int days) throws WeatherApiException {
            if (days < 1 || days > 16) {
                throw new IllegalArgumentException("Days parameter must be between 1 and 16, got: " + days);
            }

            try {
                String url = String.format("%s?format=j1", baseUrl);
                org.springframework.http.ResponseEntity<com.fasterxml.jackson.databind.JsonNode> response =
                    restTemplate.getForEntity(url, com.fasterxml.jackson.databind.JsonNode.class);

                if (response.getStatusCode() != org.springframework.http.HttpStatus.OK || response.getBody() == null) {
                    throw WeatherApiException.apiError("No forecast data available for: " + city);
                }

                com.fasterxml.jackson.databind.JsonNode root = response.getBody();
                com.fasterxml.jackson.databind.JsonNode weatherList = root.get("weather");

                if (weatherList == null || weatherList.isEmpty()) {
                    throw WeatherApiException.apiError("No weather forecast data available for: " + city);
                }

                List<DailyForecast> forecasts = new java.util.ArrayList<>();
                int forecastCount = Math.min(days, weatherList.size());

                for (int i = 0; i < forecastCount; i++) {
                    com.fasterxml.jackson.databind.JsonNode weather = weatherList.get(i);
                    int weatherCode = getDailyWeatherCode(weather);

                    forecasts.add(new DailyForecast(
                        weather.get("date").asText(),
                        weather.get("maxtempC").asDouble(),
                        weather.get("mintempC").asDouble(),
                        getPrecipitationSum(weather),
                        weatherCode
                    ));
                }

                return forecasts;
            } catch (WeatherApiException e) {
                throw e;
            } catch (Exception e) {
                throw WeatherApiException.apiError("Failed to get forecast for: " + city + ": " + e.getMessage());
            }
        }

        // Expose private methods for testing
        private String extractDescription(com.fasterxml.jackson.databind.JsonNode condition) {
            com.fasterxml.jackson.databind.JsonNode descs = condition.get("weatherDesc");
            if (descs != null && !descs.isEmpty() && descs.get(0).has("value")) {
                return descs.get(0).get("value").asText();
            }
            return io.github.solas.mcp.weather.api.WmoCode.getDescription(condition.get("weatherCode").asInt());
        }

        private int getDailyWeatherCode(com.fasterxml.jackson.databind.JsonNode weather) {
            com.fasterxml.jackson.databind.JsonNode hourly = weather.get("hourly");
            if (hourly != null && !hourly.isEmpty()) {
                for (com.fasterxml.jackson.databind.JsonNode hour : hourly) {
                    if ("1200".equals(hour.get("time").asText())) {
                        return hour.get("weatherCode").asInt();
                    }
                }
                return hourly.get(0).get("weatherCode").asInt();
            }
            return 0;
        }

        private double getPrecipitationSum(com.fasterxml.jackson.databind.JsonNode weather) {
            com.fasterxml.jackson.databind.JsonNode hourly = weather.get("hourly");
            if (hourly != null && !hourly.isEmpty()) {
                double sum = 0.0;
                for (com.fasterxml.jackson.databind.JsonNode hour : hourly) {
                    sum += hour.get("precipMM").asDouble();
                }
                return sum;
            }
            return 0.0;
        }
    }

    /**
     * Testable NominatimClient with mock support.
     */
    static class TestableNominatimClient extends NominatimClient {
        private Location mockLocation;
        private List<Location> mockLocations;

        TestableNominatimClient(RestTemplate restTemplate) {
            super(restTemplate);
        }

        void setMockLocation(Location location) {
            this.mockLocation = location;
        }

        void setMockLocations(List<Location> locations) {
            this.mockLocations = locations;
        }

        @Override
        public Location geocode(String query) {
            return mockLocation;
        }

        @Override
        public List<Location> geocodeMany(String query, int limit) {
            return mockLocations != null ? mockLocations : java.util.Collections.emptyList();
        }
    }
}
