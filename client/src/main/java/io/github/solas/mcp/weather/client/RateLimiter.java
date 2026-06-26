package io.github.solas.mcp.weather.client;

import java.util.concurrent.TimeUnit;

/**
 * Thread-safe rate limiter enforcing 1 request per second.
 * Uses synchronized block with timestamp tracking.
 * 
 * Nominatim policy: maximum 1 request per second
 * https://operations.osmfoundation.org/policies/nominatim/
 */
public final class RateLimiter {

    private static final long MIN_DELAY_MS = 1000; // 1 second

    private long lastRequestTimestamp = 0;

    /**
     * Blocks until a request is allowed under the rate limit.
     * Thread-safe via synchronized method.
     */
    public synchronized void acquire() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRequestTimestamp;
        
        if (elapsed < MIN_DELAY_MS && lastRequestTimestamp != 0) {
            long sleepTime = MIN_DELAY_MS - elapsed;
            try {
                TimeUnit.MILLISECONDS.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Rate limiter interrupted", e);
            }
        }
        
        lastRequestTimestamp = System.currentTimeMillis();
    }
}
