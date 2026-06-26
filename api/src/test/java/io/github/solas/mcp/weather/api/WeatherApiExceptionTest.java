package io.github.solas.mcp.weather.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class WeatherApiExceptionTest {

    @Test
    void testNotFound() {
        WeatherApiException exception = WeatherApiException.notFound("Moscow");
        
        assertThat(exception.getMessage()).isEqualTo("City not found: Moscow");
        assertThat(exception.getErrorCode()).isEqualTo("NOT_FOUND");
    }

    @Test
    void testApiError() {
        WeatherApiException exception = WeatherApiException.apiError("Connection failed");
        
        assertThat(exception.getMessage()).isEqualTo("API error: Connection failed");
        assertThat(exception.getErrorCode()).isEqualTo("API_ERROR");
    }

    @Test
    void testTimeout() {
        WeatherApiException exception = WeatherApiException.timeout("/forecast");
        
        assertThat(exception.getMessage()).isEqualTo("Request timeout: /forecast");
        assertThat(exception.getErrorCode()).isEqualTo("TIMEOUT");
    }

    @Test
    void testExceptionIsRuntimeException() {
        Throwable thrown = catchThrowable(() -> {
            throw WeatherApiException.notFound("TestCity");
        });
        
        assertThat(thrown).isInstanceOf(RuntimeException.class);
    }

    @Test
    void testExceptionWithCause() {
        Throwable cause = new IllegalArgumentException("Invalid input");
        WeatherApiException exception = new WeatherApiException("Test error", cause, "TEST_CODE");
        
        assertThat(exception.getMessage()).isEqualTo("Test error");
        assertThat(exception.getErrorCode()).isEqualTo("TEST_CODE");
        assertThat(exception.getCause()).isEqualTo(cause);
    }
}
