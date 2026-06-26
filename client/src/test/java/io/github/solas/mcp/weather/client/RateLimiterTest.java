package io.github.solas.mcp.weather.client;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {

    @Test
    void testFirstAcquireDoesNotBlock() {
        RateLimiter limiter = new RateLimiter();
        
        long start = System.currentTimeMillis();
        limiter.acquire();
        long elapsed = System.currentTimeMillis() - start;
        
        // First call should return immediately
        assertThat(elapsed).isLessThan(100);
    }

    @Test
    void testSecondAcquireBlocksForOneSecond() throws InterruptedException {
        RateLimiter limiter = new RateLimiter();
        
        // First acquire
        limiter.acquire();
        
        // Second acquire should block for ~1 second
        long start = System.currentTimeMillis();
        limiter.acquire();
        long elapsed = System.currentTimeMillis() - start;
        
        assertThat(elapsed).isGreaterThanOrEqualTo(950); // Allow 50ms tolerance
        assertThat(elapsed).isLessThan(1500); // Should not block too long
    }

    @Test
    void testTenRapidCallsTakeAtLeastNineSeconds() throws InterruptedException {
        RateLimiter limiter = new RateLimiter();
        
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            limiter.acquire();
        }
        long elapsed = System.currentTimeMillis() - start;
        
        // 10 calls = 9 delays of 1 second each = ~9 seconds minimum
        assertThat(elapsed).isGreaterThanOrEqualTo(8500); // 8.5s with tolerance
    }

    @Test
    void testThreadSafetyWithConcurrentThreads() throws InterruptedException {
        RateLimiter limiter = new RateLimiter();
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicLong totalTime = new AtomicLong(0);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    long start = System.currentTimeMillis();
                    limiter.acquire();
                    long elapsed = System.currentTimeMillis() - start;
                    totalTime.addAndGet(elapsed);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for all threads to complete (with timeout)
        boolean completed = latch.await(10, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        
        // Total accumulated wait time should be significant
        // With 5 threads competing for 1 req/sec, some must wait
        assertThat(totalTime.get()).isGreaterThan(2000);
        
        executor.shutdown();
    }

    @Test
    void testAcquireAfterLongPauseDoesNotBlock() throws InterruptedException {
        RateLimiter limiter = new RateLimiter();
        
        // First acquire
        limiter.acquire();
        
        // Wait 2 seconds (longer than rate limit)
        Thread.sleep(2000);
        
        // Should not block since enough time passed
        long start = System.currentTimeMillis();
        limiter.acquire();
        long elapsed = System.currentTimeMillis() - start;
        
        assertThat(elapsed).isLessThan(100);
    }
}
