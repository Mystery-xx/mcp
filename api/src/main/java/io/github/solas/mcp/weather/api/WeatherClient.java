package io.github.solas.mcp.weather.api;

import java.util.List;

/**
 * Client interface for weather API operations.
 * Provides synchronous access to weather data including location search,
 * current conditions, and forecasts.
 */
public interface WeatherClient {

    /**
     * Search for a location by name.
     *
     * @param query the city or location name to search for
     * @return the matching location information
     * @throws WeatherApiException if the search fails or no results are found
     */
    Location searchCity(String query) throws WeatherApiException;

    /**
     * Search for locations by name (returns multiple results).
     *
     * @param query the city or location name to search for
     * @return list of matching location information
     * @throws WeatherApiException if the search fails
     */
    List<Location> searchCities(String query) throws WeatherApiException;

    /**
     * Get current weather conditions for a city.
     *
     * @param city the city name to get weather for
     * @return current weather data
     * @throws WeatherApiException if the request fails or city is not found
     */
    CurrentWeather getCurrentWeather(String city) throws WeatherApiException;

    /**
     * Get daily weather forecast for a city.
     *
     * @param city the city name to get forecast for
     * @param days number of days to forecast (1-16)
     * @return list of daily forecast entries
     * @throws WeatherApiException if the request fails, city is not found,
     *                             or days parameter is outside valid range
     * @throws IllegalArgumentException if days is less than 1 or greater than 16
     */
    List<DailyForecast> getForecast(String city, int days) throws WeatherApiException;
}
