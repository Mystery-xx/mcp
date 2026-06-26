package io.github.solas.mcp.weather.client;

import io.github.solas.mcp.weather.api.WeatherApiException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetryUtilsTest {

    @Test
    void testRetryOnIOException() throws IOException, WeatherApiException {
        AtomicInteger attemptCount = new AtomicInteger(0);

        RetryUtils.RetryableOperation<String> operation = () -> {
            int attempt = attemptCount.incrementAndGet();
            if (attempt < 3) {
                throw new IOException("Connection timeout - attempt " + attempt);
            }
            return "Success on attempt " + attempt;
        };

        String result = RetryUtils.executeWithRetry(operation);

        assertThat(result).isEqualTo("Success on attempt 3");
        assertThat(attemptCount.get()).isEqualTo(3);
    }

    @Test
    void testNoRetryOn4xxError() throws IOException, WeatherApiException {
        AtomicInteger attemptCount = new AtomicInteger(0);

        RetryUtils.RetryableOperation<String> operation = () -> {
            attemptCount.incrementAndGet();
            throw WeatherApiException.notFound("TestCity");
        };

        assertThatThrownBy(() -> RetryUtils.executeWithRetry(operation))
            .isInstanceOf(WeatherApiException.class)
            .hasMessageContaining("City not found");

        assertThat(attemptCount.get()).isEqualTo(1);
    }

    @Test
    void testRetryOn5xxError() throws IOException, WeatherApiException {
        AtomicInteger attemptCount = new AtomicInteger(0);

        RetryUtils.RetryableOperation<String> operation = () -> {
            int attempt = attemptCount.incrementAndGet();
            if (attempt < 3) {
                throw new WeatherApiException("Internal server error - attempt " + attempt, null, "INTERNAL_ERROR");
            }
            return "Success on attempt " + attempt;
        };

        String result = RetryUtils.executeWithRetry(operation);

        assertThat(result).isEqualTo("Success on attempt 3");
        assertThat(attemptCount.get()).isEqualTo(3);
    }

    @Test
    void testThrowAfter3FailedAttempts() throws IOException, WeatherApiException {
        AtomicInteger attemptCount = new AtomicInteger(0);

        RetryUtils.RetryableOperation<String> operation = () -> {
            attemptCount.incrementAndGet();
            throw new IOException("Persistent network failure");
        };

        assertThatThrownBy(() -> RetryUtils.executeWithRetry(operation))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("Persistent network failure");

        assertThat(attemptCount.get()).isEqualTo(3);
    }

    @Test
    void testThrowAfter3Failed5xxAttempts() throws IOException, WeatherApiException {
        AtomicInteger attemptCount = new AtomicInteger(0);

        RetryUtils.RetryableOperation<String> operation = () -> {
            attemptCount.incrementAndGet();
            throw new WeatherApiException("Service unavailable", null, "SERVICE_UNAVAILABLE");
        };

        assertThatThrownBy(() -> RetryUtils.executeWithRetry(operation))
            .isInstanceOf(WeatherApiException.class)
            .hasMessageContaining("Service unavailable");

        assertThat(attemptCount.get()).isEqualTo(3);
    }

    @Test
    void testNoRetryOnBadRequest() throws IOException, WeatherApiException {
        AtomicInteger attemptCount = new AtomicInteger(0);

        RetryUtils.RetryableOperation<String> operation = () -> {
            attemptCount.incrementAndGet();
            throw new WeatherApiException("Bad request", null, "BAD_REQUEST");
        };

        assertThatThrownBy(() -> RetryUtils.executeWithRetry(operation))
            .isInstanceOf(WeatherApiException.class)
            .hasMessageContaining("Bad request");

        assertThat(attemptCount.get()).isEqualTo(1);
    }

    @Test
    void testSuccessOnFirstAttempt() throws IOException, WeatherApiException {
        AtomicInteger attemptCount = new AtomicInteger(0);

        RetryUtils.RetryableOperation<String> operation = () -> {
            attemptCount.incrementAndGet();
            return "Immediate success";
        };

        String result = RetryUtils.executeWithRetry(operation);

        assertThat(result).isEqualTo("Immediate success");
        assertThat(attemptCount.get()).isEqualTo(1);
    }
}
