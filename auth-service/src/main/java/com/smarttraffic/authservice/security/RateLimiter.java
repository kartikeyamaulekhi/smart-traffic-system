package com.smarttraffic.authservice.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory fixed-window rate limiter keyed by client identifier (the
 * request's IP address). Used to slow down brute-force attempts against the
 * public /auth/login and /auth/register endpoints.
 *
 * It is intentionally dependency-free and single-instance: good enough for
 * this system's threat model. A distributed deployment (multiple replicas)
 * should move this to a shared store (e.g. Redis) - see Phase 20 notes.
 */
@Component
public class RateLimiter {

    private record Window(long windowStartMillis, int count) {
    }

    private final Map<String, Window> buckets = new ConcurrentHashMap<>();

    private final int maxRequests;
    private final long windowMillis;

    public RateLimiter(
            @Value("${app.rate-limit.max-requests:20}") int maxRequests,
            @Value("${app.rate-limit.window-seconds:60}") long windowSeconds
    ) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowSeconds * 1000L;
    }

    /**
     * @return true if the key may proceed, false if it has exceeded its quota.
     */
    public boolean allow(String key) {
        if (key == null || key.isBlank()) {
            return true; // can't rate-limit an empty key; don't break unknown calls
        }

        long now = System.currentTimeMillis();
        return buckets.compute(key, (k, window) -> {
            if (window == null || now - window.windowStartMillis() >= windowMillis) {
                return new Window(now, 1);
            }
            if (window.count() >= maxRequests) {
                // Over the limit. Pin the count just above the limit so the
                // final check below evaluates to false for this and every
                // subsequent call until the window rolls over.
                return new Window(window.windowStartMillis(), maxRequests + 1);
            }
            return new Window(window.windowStartMillis(), window.count() + 1);
        }).count() <= maxRequests;
    }

    public void reset() {
        buckets.clear();
    }
}