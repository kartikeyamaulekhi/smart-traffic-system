package com.smarttraffic.routingservice.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalTrafficCacheTest {

    private LocalTrafficCache cache;

    @BeforeEach
    void setUp() {
        cache = new LocalTrafficCache(30);
    }

    @Test
    void put_nullSegmentId_isIgnored() {
        cache.put(null, 30.0, Instant.now());

        assertEquals(0, cache.size());
    }

    @Test
    void effectiveSpeed_freshReading_returnsSpeed() {
        cache.put(1L, 42.5, Instant.now());

        assertTrue(cache.effectiveSpeed(1L).isPresent());
        assertEquals(42.5, cache.effectiveSpeed(1L).get());
    }

    @Test
    void effectiveSpeed_noReading_returnsEmpty() {
        assertFalse(cache.effectiveSpeed(1L).isPresent());
    }

    @Test
    void effectiveSpeed_nonPositiveSpeed_returnsEmpty() {
        cache.put(1L, 0.0, Instant.now());

        assertFalse(cache.effectiveSpeed(1L).isPresent());
    }

    @Test
    void effectiveSpeed_staleReading_returnsEmpty() {
        cache.put(1L, 30.0, Instant.now().minus(60, ChronoUnit.MINUTES));

        assertFalse(cache.effectiveSpeed(1L).isPresent());
        assertEquals(0, cache.size()); // stale entry is evicted
    }

    @Test
    void effectiveSpeed_nullRecordedAt_usesNowAndCountsAsFresh() {
        cache.put(1L, 30.0, null);

        assertTrue(cache.effectiveSpeed(1L).isPresent());
    }
}