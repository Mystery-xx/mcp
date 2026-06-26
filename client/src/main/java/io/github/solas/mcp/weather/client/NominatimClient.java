package io.github.solas.mcp.weather.client;

import io.github.solas.mcp.weather.api.Location;
import io.github.solas.mcp.weather.api.WeatherApiException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import io.github.solas.mcp.weather.client.RateLimiter;

/**
 * Nominatim geocoding client with in-memory caching.
 * Implements 24-hour TTL cache as required by Nominatim usage policy.
 */
public class NominatimClient {

    private static final String NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org/search";
    private static final String USER_AGENT = "WeatherMcpServer/1.0";
    private static final long CACHE_TTL_MILLIS = 24 * 60 * 60 * 1000L; // 24 hours

    private final RestTemplate restTemplate;
    private final ConcurrentHashMap<String, CachedLocation> cache;
    private final RateLimiter rateLimiter;

    public NominatimClient() {
        this.restTemplate = new RestTemplate();
        this.cache = new ConcurrentHashMap<>();
        this.rateLimiter = new RateLimiter();
    }

    public NominatimClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.cache = new ConcurrentHashMap<>();
        this.rateLimiter = new RateLimiter();
    }

    public NominatimClient(RestTemplate restTemplate, RateLimiter rateLimiter) {
        this.restTemplate = restTemplate;
        this.cache = new ConcurrentHashMap<>();
        this.rateLimiter = rateLimiter;
    }

    /**
     * Geocode a single location query.
     * Returns the top result or null if not found.
     *
     * @param query search query (city name, address, etc.)
     * @return Location object or null if not found
     * @throws WeatherApiException on API error
     */
    public Location geocode(String query) throws WeatherApiException {
        List<Location> results = geocodeMany(query, 1);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Geocode a query with multiple results.
     * Uses in-memory cache with 24-hour TTL.
     *
     * @param query search query (city name, address, etc.)
     * @param limit maximum number of results (1-40)
     * @return list of Location objects, may be empty
     * @throws WeatherApiException on API error
     */
    public List<Location> geocodeMany(String query, int limit) throws WeatherApiException {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        // Normalize query for cache key
        String cacheKey = buildCacheKey(query, limit);

        // Check cache first
        CachedLocation cached = cache.get(cacheKey);
        if (cached != null && !isExpired(cached)) {
            return cached.locations();
        }

        // Fetch from API
        try {
            rateLimiter.acquire(); // Rate limit before API call
            List<Location> locations = fetchFromApi(query, limit);

            // Cache the result (even if empty to avoid repeated failed lookups)
            cache.put(cacheKey, new CachedLocation(locations, Instant.now()));

            return locations;
        } catch (WeatherApiException e) {
            throw e;
        } catch (Exception e) {
            throw WeatherApiException.apiError("Failed to geocode: " + e.getMessage());
        }
    }

    /**
     * Clear all cached entries.
     */
    public void clearCache() {
        cache.clear();
    }

    /**
     * Get current cache size.
     */
    public int getCacheSize() {
        return cache.size();
    }

    private String buildCacheKey(String query, int limit) {
        return query.toLowerCase().trim() + ":" + limit;
    }

    private boolean isExpired(CachedLocation cached) {
        long age = Instant.now().toEpochMilli() - cached.timestamp().toEpochMilli();
        return age > CACHE_TTL_MILLIS;
    }

    private List<Location> fetchFromApi(String query, int limit) throws WeatherApiException {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        int safeLimit = Math.max(1, Math.min(40, limit)); // Nominatim max is 40

        String url = String.format(
            "%s?q=%s&format=json&limit=%d&addressdetails=1",
            NOMINATIM_BASE_URL,
            encodedQuery,
            safeLimit
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", USER_AGENT);
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<NominatimResponse[]> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, requestEntity, NominatimResponse[].class);

        if (response.getStatusCode() == HttpStatus.NOT_FOUND || response.getBody() == null) {
            return Collections.emptyList();
        }

        List<Location> locations = new ArrayList<>();
        for (NominatimResponse result : response.getBody()) {
            locations.add(mapToLocation(result));
        }

        return locations;
    }

    private Location mapToLocation(NominatimResponse result) {
        String name = extractName(result);
        String country = extractCountry(result);
        double latitude = result.lat != null ? result.lat : 0.0;
        double longitude = result.lon != null ? result.lon : 0.0;
        String timezone = result.address != null ? result.address.state : null;
        double elevation = 0.0; // Nominatim doesn't provide elevation

        return new Location(name, country, latitude, longitude, timezone, elevation);
    }

    private String extractName(NominatimResponse result) {
        if (result.address != null) {
            // Prefer specific address fields
            if (result.address.houseNumber != null && result.address.road != null) {
                return result.address.houseNumber + " " + result.address.road;
            }
            if (result.address.name != null) {
                return result.address.name;
            }
            if (result.address.city != null) {
                return result.address.city;
            }
            if (result.address.town != null) {
                return result.address.town;
            }
            if (result.address.village != null) {
                return result.address.village;
            }
            if (result.address.county != null) {
                return result.address.county;
            }
            if (result.address.state != null) {
                return result.address.state;
            }
        }
        // Fallback to display_name
        return result.displayName != null ? result.displayName : "Unknown";
    }

    private String extractCountry(NominatimResponse result) {
        if (result.address != null && result.address.countryCode != null) {
            return result.address.countryCode.toUpperCase();
        }
        if (result.address != null && result.address.country != null) {
            return result.address.country;
        }
        return "Unknown";
    }

    /**
     * Internal response DTO for Nominatim API.
     */
    private static class NominatimResponse {
        @com.fasterxml.jackson.annotation.JsonProperty("place_id")
        private String placeId;

        @com.fasterxml.jackson.annotation.JsonProperty("osm_type")
        private String osmType;

        @com.fasterxml.jackson.annotation.JsonProperty("osm_id")
        private String osmId;

        @com.fasterxml.jackson.annotation.JsonProperty("lat")
        private Double lat;

        @com.fasterxml.jackson.annotation.JsonProperty("lon")
        private Double lon;

        @com.fasterxml.jackson.annotation.JsonProperty("display_name")
        private String displayName;

        @com.fasterxml.jackson.annotation.JsonProperty("address")
        private Address address;

        // Getters
        public String getPlaceId() { return placeId; }
        public String getOsmType() { return osmType; }
        public String getOsmId() { return osmId; }
        public Double getLat() { return lat; }
        public Double getLon() { return lon; }
        public String getDisplayName() { return displayName; }
        public Address getAddress() { return address; }
    }

    /**
     * Address details from Nominatim response.
     */
    private static class Address {
        @com.fasterxml.jackson.annotation.JsonProperty("house_number")
        private String houseNumber;

        @com.fasterxml.jackson.annotation.JsonProperty("road")
        private String road;

        @com.fasterxml.jackson.annotation.JsonProperty("neighbourhood")
        private String neighbourhood;

        @com.fasterxml.jackson.annotation.JsonProperty("suburb")
        private String suburb;

        @com.fasterxml.jackson.annotation.JsonProperty("city")
        private String city;

        @com.fasterxml.jackson.annotation.JsonProperty("town")
        private String town;

        @com.fasterxml.jackson.annotation.JsonProperty("village")
        private String village;

        @com.fasterxml.jackson.annotation.JsonProperty("county")
        private String county;

        @com.fasterxml.jackson.annotation.JsonProperty("state")
        private String state;

        @com.fasterxml.jackson.annotation.JsonProperty("postcode")
        private String postcode;

        @com.fasterxml.jackson.annotation.JsonProperty("country")
        private String country;

        @com.fasterxml.jackson.annotation.JsonProperty("country_code")
        private String countryCode;

        @com.fasterxml.jackson.annotation.JsonProperty("name")
        private String name;

        // Getters
        public String getHouseNumber() { return houseNumber; }
        public String getRoad() { return road; }
        public String getNeighbourhood() { return neighbourhood; }
        public String getSuburb() { return suburb; }
        public String getCity() { return city; }
        public String getTown() { return town; }
        public String getVillage() { return village; }
        public String getCounty() { return county; }
        public String getState() { return state; }
        public String getPostcode() { return postcode; }
        public String getCountry() { return country; }
        public String getCountryCode() { return countryCode; }
        public String getName() { return name; }
    }

    /**
     * Cache entry with timestamp for TTL checking.
     */
    private record CachedLocation(List<Location> locations, Instant timestamp) {
    }
}
