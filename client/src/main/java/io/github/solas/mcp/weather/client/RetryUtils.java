package io.github.solas.mcp.weather.client;

import io.github.solas.mcp.weather.api.WeatherApiException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for retrying HTTP requests with configurable attempts and delay.
 * Retries on: IOException, TimeoutException, HTTP 5xx errors
 * Does NOT retry on: HTTP 4xx errors
 */
public final class RetryUtils {

    private static final int MAX_ATTEMPTS = 3;
    private static final long DELAY_MS = 1000; // 1 second fixed delay

    private RetryUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Execute a retryable HTTP operation with 3 attempts and 1-second delay.
     *
     * @param operation The retryable operation to execute
     * @return Result of the operation
     * @throws WeatherApiException After all retries exhausted or on non-retryable errors
     * @throws IOException If IO error occurs on final attempt
     */
    public static <T> T executeWithRetry(RetryableOperation<T> operation) throws WeatherApiException, IOException {
        IOException lastIOException = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return operation.execute();
            } catch (WeatherApiException e) {
                // Do NOT retry on 4xx errors (client errors like NOT_FOUND)
                if (isClientError(e)) {
                    throw e;
                }
                // For 5xx errors, retry if attempts remain
                if (attempt == MAX_ATTEMPTS) {
                    throw e;
                }
                sleepBeforeRetry(attempt);
            } catch (IOException e) {
                // Retry on IOException (network errors, connection issues)
                lastIOException = e;
                if (attempt == MAX_ATTEMPTS) {
                    throw e;
                }
                sleepBeforeRetry(attempt);
            } catch (Exception e) {
                // Wrap unexpected exceptions
                if (attempt == MAX_ATTEMPTS) {
                    throw new WeatherApiException("Unexpected error: " + e.getMessage(), e, "INTERNAL_ERROR");
                }
                sleepBeforeRetry(attempt);
            }
        }

        // Should not reach here, but throw last exception if we do
        if (lastIOException != null) {
            throw lastIOException;
        }
        throw new WeatherApiException("Retry failed after " + MAX_ATTEMPTS + " attempts", null, "RETRY_EXHAUSTED");
    }

    /**
     * Check if the exception represents a client error (4xx) that should NOT be retried.
     *
     * @param e The exception to check
     * @return true if it's a 4xx client error
     */
    private static boolean isClientError(WeatherApiException e) {
        String errorCode = e.getErrorCode();
        // NOT_FOUND, BAD_REQUEST, etc. are 4xx errors - do not retry
        return "NOT_FOUND".equals(errorCode) ||
               "BAD_REQUEST".equals(errorCode) ||
               "UNAUTHORIZED".equals(errorCode) ||
               "FORBIDDEN".equals(errorCode);
    }

    /**
     * Sleep for the configured delay before retrying.
     *
     * @param attempt Current attempt number (1-based)
     */
    private static void sleepBeforeRetry(int attempt) {
        try {
            TimeUnit.MILLISECONDS.sleep(DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Retry interrupted on attempt " + attempt, e);
        }
    }

    /**
     * Functional interface for retryable operations.
     *
     * @param <T> Return type of the operation
     */
    @FunctionalInterface
    public interface RetryableOperation<T> {
        T execute() throws WeatherApiException, IOException;
    }
}
