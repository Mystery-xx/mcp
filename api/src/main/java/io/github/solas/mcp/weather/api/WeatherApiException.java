package io.github.solas.mcp.weather.api;

/**
 * Custom exception for weather API errors.
 * Contains message, cause, and error code for detailed error reporting.
 */
public class WeatherApiException extends RuntimeException {

    private final String errorCode;

    /**
     * Create a new WeatherApiException.
     *
     * @param message Error message
     * @param cause   Root cause (may be null)
     * @param errorCode Error code identifier
     */
    public WeatherApiException(String message, Throwable cause, String errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Get the error code.
     *
     * @return Error code identifier
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Create exception for city not found errors.
     *
     * @param city City name that was not found
     * @return WeatherApiException with NOT_FOUND error code
     */
    public static WeatherApiException notFound(String city) {
        return new WeatherApiException("City not found: " + city, null, "NOT_FOUND");
    }

    /**
     * Create exception for general API errors.
     *
     * @param message Error message from API
     * @return WeatherApiException with API_ERROR error code
     */
    public static WeatherApiException apiError(String message) {
        return new WeatherApiException("API error: " + message, null, "API_ERROR");
    }

    /**
     * Create exception for API timeout errors.
     *
     * @param endpoint Endpoint that timed out
     * @return WeatherApiException with TIMEOUT error code
     */
    public static WeatherApiException timeout(String endpoint) {
        return new WeatherApiException("Request timeout: " + endpoint, null, "TIMEOUT");
    }
}
