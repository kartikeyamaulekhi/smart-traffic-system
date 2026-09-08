package com.smarttraffic.authservice.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterTest {

    private RateLimiter limiter(int maxRequests, long windowSeconds) {
        return new RateLimiter(maxRequests, windowSeconds);
    }

    @Test
    void allow_underLimit_allowsAll() {
        RateLimiter limiter = limiter(3, 60);

        assertTrue(limiter.allow("10.0.0.1"));
        assertTrue(limiter.allow("10.0.0.1"));
        assertTrue(limiter.allow("10.0.0.1"));
    }

    @Test
    void allow_overLimit_rejects() {
        RateLimiter limiter = limiter(3, 60);

        assertTrue(limiter.allow("10.0.0.1"));
        assertTrue(limiter.allow("10.0.0.1"));
        assertTrue(limiter.allow("10.0.0.1"));
        assertFalse(limiter.allow("10.0.0.1"));
        assertFalse(limiter.allow("10.0.0.1"));
    }

    @Test
    void allow_differentKeys_areIndependent() {
        RateLimiter limiter = limiter(1, 60);

        assertTrue(limiter.allow("10.0.0.1"));
        assertFalse(limiter.allow("10.0.0.1"));
        assertTrue(limiter.allow("10.0.0.2"));
    }

    @Test
    void allow_blankOrNullKey_isAlwaysAllowed() {
        RateLimiter limiter = limiter(1, 60);

        assertTrue(limiter.allow(null));
        assertTrue(limiter.allow(" "));
    }

    @Test
    void reset_clearsAllBuckets() {
        RateLimiter limiter = limiter(1, 60);

        limiter.allow("10.0.0.1");
        assertFalse(limiter.allow("10.0.0.1"));

        limiter.reset();

        assertTrue(limiter.allow("10.0.0.1"));
    }

    @Test
    void windowExpiry_reallowsCalls() throws InterruptedException {
        RateLimiter limiter = limiter(1, 1);

        assertTrue(limiter.allow("10.0.0.1"));
        assertFalse(limiter.allow("10.0.0.1"));

        Thread.sleep(1100);

        assertTrue(limiter.allow("10.0.0.1"));
    }

    @Test
    void rejectedCalls_doNotConsumeExtraBudget() {
        RateLimiter limiter = limiter(3, 60);

        limiter.allow("10.0.0.1");
        limiter.allow("10.0.0.1");
        limiter.allow("10.0.0.1");

        for (int i = 0; i < 10; i++) {
            assertFalse(limiter.allow("10.0.0.1"));
        }

        // A fresh key still works immediately after.
        assertTrue(limiter.allow("10.0.0.2"));
    }
}