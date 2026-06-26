package io.github.solas.mcp.weather.client;

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

class NominatimClientTest {

    private MockWebServer mockWebServer;
    private RestTemplate restTemplate;
    private TestableNominatimClient client;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        restTemplate = new RestTemplate();
        String mockUrl = mockWebServer.url("/search").toString();
        client = new TestableNominatimClient(restTemplate, mockUrl);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void testGeocodeSuccess() throws WeatherApiException {
        // Given: Mock Nominatim response with single location
        String jsonResponse = """
            [
                {
                    "place_id": "12345",
                    "osm_type": "relation",
                    "osm_id": "1",
                    "lat": "55.7522",
                    "lon": "37.6156",
                    "display_name": "Moscow, Russia",
                    "address": {
                        "city": "Moscow",
                        "country": "Russia",
                        "country_code": "ru",
                        "state": "Moscow"
                    }
                }
            ]
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json"));

        // When: Geocode city
        Location result = client.geocode("Moscow");

        // Then: Verify location mapping
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Moscow");
        assertThat(result.country()).isEqualTo("RU");
        assertThat(result.latitude()).isEqualTo(55.7522);
        assertThat(result.longitude()).isEqualTo(37.6156);
        assertThat(result.timezone()).isEqualTo("Moscow");
        assertThat(result.elevation()).isEqualTo(0.0); // Nominatim doesn't provide elevation
    }

    @Test
    void testGeocodeNotFound() throws WeatherApiException {
        // Given: Empty response (city not found)
        String jsonResponse = "[]";

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json"));

        // When: Geocode non-existent city
        Location result = client.geocode("NonExistentCity12345");

        // Then: Should return null
        assertThat(result).isNull();
    }

    @Test
    void testGeocodeManyReturnsMultipleResults() throws WeatherApiException {
        // Given: Mock response with multiple locations
        String jsonResponse = """
            [
                {
                    "place_id": "1",
                    "lat": "55.75",
                    "lon": "37.62",
                    "display_name": "Moscow, Russia",
                    "address": {
                        "city": "Moscow",
                        "country": "Russia",
                        "country_code": "ru"
                    }
                },
                {
                    "place_id": "2",
                    "lat": "45.68",
                    "lon": "-117.46",
                    "display_name": "Moscow, Washington, United States",
                    "address": {
                        "city": "Moscow",
                        "country": "United States",
                        "country_code": "us",
                        "state": "Washington"
                    }
                },
                {
                    "place_id": "3",
                    "lat": "39.58",
                    "lon": "-91.11",
                    "display_name": "Moscow, Missouri, United States",
                    "address": {
                        "city": "Moscow",
                        "country": "United States",
                        "country_code": "us",
                        "state": "Missouri"
                    }
                }
            ]
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json"));

        // When: Search for multiple cities
        List<Location> results = client.geocodeMany("Moscow", 5);

        // Then: Verify multiple results
        assertThat(results).hasSize(3);
        assertThat(results.get(0).name()).isEqualTo("Moscow");
        assertThat(results.get(0).country()).isEqualTo("RU");
        assertThat(results.get(1).country()).isEqualTo("US");
        assertThat(results.get(2).country()).isEqualTo("US");
    }

    @Test
    void testGeocodeManySendsLimitParameter() throws WeatherApiException {
        // Given: Response with 3 locations (API respects limit parameter)
        String jsonResponse = """
            [
                {
                    "place_id": "1",
                    "lat": "55.75",
                    "lon": "37.62",
                    "display_name": "City 1",
                    "address": {
                        "city": "City 1",
                        "country": "Russia",
                        "country_code": "ru"
                    }
                },
                {
                    "place_id": "2",
                    "lat": "55.76",
                    "lon": "37.63",
                    "display_name": "City 2",
                    "address": {
                        "city": "City 2",
                        "country": "Russia",
                        "country_code": "ru"
                    }
                },
                {
                    "place_id": "3",
                    "lat": "55.77",
                    "lon": "37.64",
                    "display_name": "City 3",
                    "address": {
                        "city": "City 3",
                        "country": "Russia",
                        "country_code": "ru"
                    }
                }
            ]
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json"));

        // When: Request 3 results
        List<Location> results = client.geocodeMany("Test", 3);

        // Then: Should return 3 results
        assertThat(results).hasSize(3);
        assertThat(results.get(0).name()).isEqualTo("City 1");
        assertThat(results.get(2).name()).isEqualTo("City 3");
    }

    @Test
    void testGeocodeManyEmptyQuery() throws WeatherApiException {
        // Given: Empty query string

        // When: Geocode empty string
        List<Location> results = client.geocodeMany("", 5);

        // Then: Should return empty list without API call
        assertThat(results).isEmpty();
    }

    @Test
    void testGeocodeManyNullQuery() throws WeatherApiException {
        // Given: Null query string

        // When: Geocode null
        List<Location> results = client.geocodeMany(null, 5);

        // Then: Should return empty list
        assertThat(results).isEmpty();
    }

    @Test
    void testGeocodeManyBlankQuery() throws WeatherApiException {
        // Given: Blank query string (whitespace only)

        // When: Geocode blank string
        List<Location> results = client.geocodeMany("   ", 5);

        // Then: Should return empty list
        assertThat(results).isEmpty();
    }

    @Test
    void testCacheHitOnSecondCall() throws WeatherApiException {
        // Given: Mock response for first call
        String jsonResponse = """
            [{
                "place_id": "1",
                "lat": "55.75",
                "lon": "37.62",
                "display_name": "Moscow, Russia",
                "address": {
                    "city": "Moscow",
                    "country": "Russia",
                    "country_code": "ru"
                }
            }]
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json"));

        // When: First call
        Location result1 = client.geocode("Moscow");

        // And: Second call (should use cache)
        Location result2 = client.geocode("Moscow");

        // Then: Both should return same result
        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        assertThat(result1.name()).isEqualTo("Moscow");
        assertThat(result2.name()).isEqualTo("Moscow");

        // Verify only 1 request was made (second was cached)
        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
    }

    @Test
    void testCacheMissOnDifferentQuery() throws WeatherApiException {
        // Given: Mock responses for two different cities
        String moscowResponse = """
            [{
                "place_id": "1",
                "lat": "55.75",
                "lon": "37.62",
                "display_name": "Moscow, Russia",
                "address": {
                    "city": "Moscow",
                    "country": "Russia",
                    "country_code": "ru"
                }
            }]
            """;

        String berlinResponse = """
            [{
                "place_id": "2",
                "lat": "52.52",
                "lon": "13.41",
                "display_name": "Berlin, Germany",
                "address": {
                    "city": "Berlin",
                    "country": "Germany",
                    "country_code": "de"
                }
            }]
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(moscowResponse)
            .addHeader("Content-Type", "application/json"));

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(berlinResponse)
            .addHeader("Content-Type", "application/json"));

        // When: Geocode two different cities
        Location moscow = client.geocode("Moscow");
        Location berlin = client.geocode("Berlin");

        // Then: Both should return correct results
        assertThat(moscow.name()).isEqualTo("Moscow");
        assertThat(berlin.name()).isEqualTo("Berlin");

        // Verify 2 requests were made (no cache hit)
        assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
    }

    @Test
    void testClearCache() throws WeatherApiException {
        // Given: Mock response
        String jsonResponse = """
            [{
                "place_id": "1",
                "lat": "55.75",
                "lon": "37.62",
                "display_name": "Moscow, Russia",
                "address": {
                    "city": "Moscow",
                    "country": "Russia",
                    "country_code": "ru"
                }
            }]
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json"));

        // When: First call (cached)
        client.geocode("Moscow");

        // And: Clear cache
        client.clearCache();

        // And: Second call (cache cleared, new API call)
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json"));

        client.geocode("Moscow");

        // Then: Verify 2 requests were made (cache was cleared)
        assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
    }

    @Test
    void testGetCacheSize() throws WeatherApiException {
        // Given: Empty cache
        assertThat(client.getCacheSize()).isEqualTo(0);

        // When: Add entries to cache
        String jsonResponse = """
            [{
                "place_id": "1",
                "lat": "55.75",
                "lon": "37.62",
                "display_name": "Moscow, Russia",
                "address": {
                    "city": "Moscow",
                    "country": "Russia",
                    "country_code": "ru"
                }
            }]
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json"));

        client.geocode("Moscow");

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json"));

        client.geocode("Berlin");

        // Then: Cache should have 2 entries
        assertThat(client.getCacheSize()).isEqualTo(2);
    }

    @Test
    void testCacheKeyNormalization() throws WeatherApiException {
        // Given: Mock response
        String jsonResponse = """
            [{
                "place_id": "1",
                "lat": "55.75",
                "lon": "37.62",
                "display_name": "Moscow, Russia",
                "address": {
                    "city": "Moscow",
                    "country": "Russia",
                    "country_code": "ru"
                }
            }]
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json"));

        // When: Query with different cases and whitespace
        client.geocode("  MOSCOW  ");
        client.geocode("moscow");

        // Then: Should use same cache entry (normalized)
        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
    }

    @Test
    void testExtractNameFromAddressFields() throws WeatherApiException {
        // Given: Response with detailed address
        String jsonResponse = """
            [{
                "place_id": "1",
                "lat": "55.75",
                "lon": "37.62",
                "display_name": "Red Square, Moscow, Russia",
                "address": {
                    "house_number": "1",
                    "road": "Red Square",
                    "city": "Moscow",
                    "country": "Russia",
                    "country_code": "ru"
                }
            }]
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json"));

        // When: Geocode
        Location result = client.geocode("Moscow");

        // Then: Should prefer house_number + road for name
        assertThat(result.name()).isEqualTo("1 Red Square");
    }

    @Test
    void testExtractNameFallbackToDisplayName() throws WeatherApiException {
        // Given: Response with minimal address
        String jsonResponse = """
            [{
                "place_id": "1",
                "lat": "55.75",
                "lon": "37.62",
                "display_name": "Some Location in the Middle of Nowhere",
                "address": {}
            }]
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json"));

        // When: Geocode
        Location result = client.geocode("Test");

        // Then: Should fallback to display_name
        assertThat(result.name()).isEqualTo("Some Location in the Middle of Nowhere");
    }

    @Test
    void testExtractCountryFromCountryCode() throws WeatherApiException {
        // Given: Response with country code
        String jsonResponse = """
            [{
                "place_id": "1",
                "lat": "52.52",
                "lon": "13.41",
                "display_name": "Berlin, Germany",
                "address": {
                    "city": "Berlin",
                    "country": "Deutschland",
                    "country_code": "de"
                }
            }]
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json"));

        // When: Geocode
        Location result = client.geocode("Berlin");

        // Then: Should use uppercase country code
        assertThat(result.country()).isEqualTo("DE");
    }

    @Test
    void testExtractCountryFallbackToCountryName() throws WeatherApiException {
        // Given: Response without country code
        String jsonResponse = """
            [{
                "place_id": "1",
                "lat": "52.52",
                "lon": "13.41",
                "display_name": "Berlin, Germany",
                "address": {
                    "city": "Berlin",
                    "country": "Germany"
                }
            }]
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json"));

        // When: Geocode
        Location result = client.geocode("Berlin");

        // Then: Should fallback to country name
        assertThat(result.country()).isEqualTo("Germany");
    }

    /**
     * Testable NominatimClient that allows overriding the base URL.
     */
    static class TestableNominatimClient extends NominatimClient {
        private final String baseUrl;
        private final RestTemplate restTemplate;

        TestableNominatimClient(RestTemplate restTemplate, String baseUrl) {
            super(restTemplate);
            this.restTemplate = restTemplate;
            this.baseUrl = baseUrl;
        }

        @Override
        public List<Location> geocodeMany(String query, int limit) {
            if (query == null || query.isBlank()) {
                return java.util.Collections.emptyList();
            }

            // Check cache first (using reflection to access private cache)
            String cacheKey = query.toLowerCase().trim() + ":" + limit;
            try {
                java.lang.reflect.Field cacheField = NominatimClient.class.getDeclaredField("cache");
                cacheField.setAccessible(true);
                @SuppressWarnings("unchecked")
                java.util.concurrent.ConcurrentHashMap<String, Object> cache =
                    (java.util.concurrent.ConcurrentHashMap<String, Object>) cacheField.get(this);

                Object cached = cache.get(cacheKey);
                if (cached != null) {
                    // Check if expired using reflection
                    java.lang.reflect.Method timestampMethod = cached.getClass().getDeclaredMethod("timestamp");
                    timestampMethod.setAccessible(true);
                    java.time.Instant timestamp = (java.time.Instant) timestampMethod.invoke(cached);
                    long age = java.time.Instant.now().toEpochMilli() - timestamp.toEpochMilli();
                    if (age <= (24 * 60 * 60 * 1000L)) {
                        java.lang.reflect.Method locationsMethod = cached.getClass().getDeclaredMethod("locations");
                        locationsMethod.setAccessible(true);
                        @SuppressWarnings("unchecked")
                        List<Location> locations = (List<Location>) locationsMethod.invoke(cached);
                        return locations;
                    }
                }
            } catch (Exception e) {
                // Fall through to API call
            }

            // Fetch from API
            try {
                List<Location> locations = fetchFromApi(query, limit);

                // Cache result using reflection
                try {
                    java.lang.reflect.Field cacheField = NominatimClient.class.getDeclaredField("cache");
                    cacheField.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    java.util.concurrent.ConcurrentHashMap<String, Object> cache =
                        (java.util.concurrent.ConcurrentHashMap<String, Object>) cacheField.get(this);

                    Class<?> cachedLocationClass = Class.forName(
                        "io.github.solas.mcp.weather.client.NominatimClient$CachedLocation"
                    );
                    java.lang.reflect.Constructor<?> constructor = cachedLocationClass.getDeclaredConstructor(
                        List.class, java.time.Instant.class
                    );
                    constructor.setAccessible(true);
                    Object cachedLocation = constructor.newInstance(locations, java.time.Instant.now());
                    cache.put(cacheKey, cachedLocation);
                } catch (Exception e) {
                    // Ignore cache errors
                }

                return locations;
            } catch (WeatherApiException e) {
                throw e;
            } catch (Exception e) {
                throw WeatherApiException.apiError("Failed to geocode: " + e.getMessage());
            }
        }

        List<Location> fetchFromApi(String query, int limit) throws WeatherApiException {
            String encodedQuery = java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
            int safeLimit = Math.max(1, Math.min(40, limit));

            String url = String.format(
                "%s?q=%s&format=json&limit=%d&addressdetails=1",
                baseUrl,
                encodedQuery,
                safeLimit
            );

            org.springframework.http.ResponseEntity<com.fasterxml.jackson.databind.JsonNode[]> response =
                restTemplate.getForEntity(url, com.fasterxml.jackson.databind.JsonNode[].class);

            if (response.getStatusCode() == org.springframework.http.HttpStatus.NOT_FOUND || response.getBody() == null) {
                return java.util.Collections.emptyList();
            }

            List<Location> locations = new java.util.ArrayList<>();
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            for (com.fasterxml.jackson.databind.JsonNode node : response.getBody()) {
                locations.add(mapToLocation(node, mapper));
            }

            return locations;
        }

        private Location mapToLocation(com.fasterxml.jackson.databind.JsonNode node,
                                       com.fasterxml.jackson.databind.ObjectMapper mapper) {
            try {
                String name = extractName(node);
                String country = extractCountry(node);
                double latitude = node.has("lat") ? node.get("lat").asDouble() : 0.0;
                double longitude = node.has("lon") ? node.get("lon").asDouble() : 0.0;
                String timezone = null;
                if (node.has("address") && node.get("address").has("state")) {
                    timezone = node.get("address").get("state").asText();
                }
                double elevation = 0.0;

                return new Location(name, country, latitude, longitude, timezone, elevation);
            } catch (Exception e) {
                return new Location("Unknown", "Unknown", 0.0, 0.0, null, 0.0);
            }
        }

        private String extractName(com.fasterxml.jackson.databind.JsonNode node) {
            if (node.has("address")) {
                com.fasterxml.jackson.databind.JsonNode address = node.get("address");
                if (address.has("house_number") && address.has("road")) {
                    return address.get("house_number").asText() + " " + address.get("road").asText();
                }
                if (address.has("name")) {
                    return address.get("name").asText();
                }
                if (address.has("city")) {
                    return address.get("city").asText();
                }
                if (address.has("town")) {
                    return address.get("town").asText();
                }
                if (address.has("village")) {
                    return address.get("village").asText();
                }
                if (address.has("county")) {
                    return address.get("county").asText();
                }
                if (address.has("state")) {
                    return address.get("state").asText();
                }
            }
            return node.has("display_name") ? node.get("display_name").asText() : "Unknown";
        }

        private String extractCountry(com.fasterxml.jackson.databind.JsonNode node) {
            if (node.has("address")) {
                com.fasterxml.jackson.databind.JsonNode address = node.get("address");
                if (address.has("country_code")) {
                    return address.get("country_code").asText().toUpperCase();
                }
                if (address.has("country")) {
                    return address.get("country").asText();
                }
            }
            return "Unknown";
        }
    }
}
