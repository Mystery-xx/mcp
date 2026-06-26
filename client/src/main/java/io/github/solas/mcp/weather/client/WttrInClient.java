package io.github.solas.mcp.weather.client;

import io.github.solas.mcp.weather.api.CurrentWeather;
import io.github.solas.mcp.weather.api.DailyForecast;
import io.github.solas.mcp.weather.api.LanguageUtils;
import io.github.solas.mcp.weather.api.Location;
import io.github.solas.mcp.weather.api.WeatherApiException;
import io.github.solas.mcp.weather.api.WeatherClient;
import io.github.solas.mcp.weather.api.WmoCode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Wttr.in API client implementing WeatherClient interface.
 * Provides geocoding via NominatimClient and weather data retrieval using wttr.in API.
 */
public class WttrInClient implements WeatherClient {

    private static final String WTTR_BASE_URL = "https://wttr.in";
    private final RestTemplate restTemplate;
    private final NominatimClient nominatimClient;

    public WttrInClient() {
        this.restTemplate = new RestTemplate();
        this.nominatimClient = new NominatimClient();
    }

    public WttrInClient(RestTemplate restTemplate, NominatimClient nominatimClient) {
        this.restTemplate = restTemplate;
        this.nominatimClient = nominatimClient;
    }

    @Override
    public Location searchCity(String query) throws WeatherApiException {
        try {
            Location location = nominatimClient.geocode(query);
            if (location == null) {
                throw WeatherApiException.notFound(query);
            }
            return location;
        } catch (WeatherApiException e) {
            throw e;
        } catch (Exception e) {
            throw WeatherApiException.apiError("Failed to search city: " + e.getMessage());
        }
    }

    @Override
    public List<Location> searchCities(String query) throws WeatherApiException {
        try {
            return nominatimClient.geocodeMany(query, 5);
        } catch (WeatherApiException e) {
            throw e;
        } catch (Exception e) {
            throw WeatherApiException.apiError("Failed to search cities: " + e.getMessage());
        }
    }

    @Override
    public CurrentWeather getCurrentWeather(String city) throws WeatherApiException {
        try {
            return RetryUtils.executeWithRetry(() -> {
                String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
                String locale = LanguageUtils.getLocale(city);
                String url = String.format("%s/%s?format=j1", WTTR_BASE_URL, encodedCity);

                HttpHeaders headers = new HttpHeaders();
                headers.set("Accept-Language", locale);

                ResponseEntity<WttrInResponse> response = restTemplate.getForEntity(url, WttrInResponse.class);

                if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                    throw WeatherApiException.apiError("No current weather data available for: " + city);
                }

                WttrInResponse wttrResponse = response.getBody();
                List<WttrInResponse.CurrentCondition> conditions = wttrResponse.getCurrentCondition();

                if (conditions == null || conditions.isEmpty()) {
                    throw WeatherApiException.apiError("No current condition data available for: " + city);
                }

                WttrInResponse.CurrentCondition condition = conditions.get(0);
                int weatherCode = condition.getWeatherCode();
                String description = extractDescription(condition);

                return new CurrentWeather(
                    condition.getTempC(),
                    condition.getFeelsLikeC(),
                    condition.getHumidity(),
                    condition.getPressure(),
                    condition.getWindSpeedKmph(),
                    condition.getWindDir16Point(),
                    weatherCode,
                    description
                );
            });
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
            return RetryUtils.executeWithRetry(() -> {
                String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
                String locale = LanguageUtils.getLocale(city);
                String url = String.format("%s/%s?format=j1", WTTR_BASE_URL, encodedCity);

                HttpHeaders headers = new HttpHeaders();
                headers.set("Accept-Language", locale);

                ResponseEntity<WttrInResponse> response = restTemplate.getForEntity(url, WttrInResponse.class);

                if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                    throw WeatherApiException.apiError("No forecast data available for: " + city);
                }

                WttrInResponse wttrResponse = response.getBody();
                List<WttrInResponse.Weather> weatherList = wttrResponse.getWeather();

                if (weatherList == null || weatherList.isEmpty()) {
                    throw WeatherApiException.apiError("No weather forecast data available for: " + city);
                }

                List<DailyForecast> forecasts = new ArrayList<>();
                int forecastCount = Math.min(days, weatherList.size());

                for (int i = 0; i < forecastCount; i++) {
                    WttrInResponse.Weather weather = weatherList.get(i);
                    int weatherCode = getDailyWeatherCode(weather);

                    forecasts.add(new DailyForecast(
                        weather.getDate(),
                        weather.getMaxTempC(),
                        weather.getMinTempC(),
                        getPrecipitationSum(weather),
                        weatherCode
                    ));
                }

                return forecasts;
            });
        } catch (WeatherApiException e) {
            throw e;
        } catch (Exception e) {
            throw WeatherApiException.apiError("Failed to get forecast for: " + city + ": " + e.getMessage());
        }
    }

    /**
     * Extract weather description from current condition.
     * Uses the first weather description if available, otherwise falls back to WMO code description.
     */
    private String extractDescription(WttrInResponse.CurrentCondition condition) {
        List<WttrInResponse.WeatherDesc> descs = condition.getWeatherDesc();
        if (descs != null && !descs.isEmpty() && descs.get(0).getValue() != null) {
            return descs.get(0).getValue();
        }
        return WmoCode.getDescription(condition.getWeatherCode());
    }

    /**
     * Get weather code for daily forecast.
     * Tries to get from hourly data (midday), falls back to 0 if unavailable.
     */
    private int getDailyWeatherCode(WttrInResponse.Weather weather) {
        List<WttrInResponse.Hourly> hourly = weather.getHourly();
        if (hourly != null && !hourly.isEmpty()) {
            // Use midday (12:00) weather code if available
            for (WttrInResponse.Hourly hour : hourly) {
                if ("1200".equals(hour.getTime())) {
                    return hour.getWeatherCode();
                }
            }
            // Fallback to first hourly entry
            return hourly.get(0).getWeatherCode();
        }
        return 0;
    }

    /**
     * Get precipitation sum for the day from hourly data.
     * Sums up hourly precipitation or returns 0 if unavailable.
     */
    private double getPrecipitationSum(WttrInResponse.Weather weather) {
        List<WttrInResponse.Hourly> hourly = weather.getHourly();
        if (hourly != null && !hourly.isEmpty()) {
            double sum = 0.0;
            for (WttrInResponse.Hourly hour : hourly) {
                sum += hour.getPrecipMm();
            }
            return sum;
        }
        return 0.0;
    }
}
