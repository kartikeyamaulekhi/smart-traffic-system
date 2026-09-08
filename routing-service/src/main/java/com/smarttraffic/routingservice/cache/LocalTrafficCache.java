package com.smarttraffic.routingservice.cache;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local, in-memory cache of the most recent traffic speed per road segment.
 *
 * This is the "event-carried state transfer" half of Phase 11c: routing-service
 * has no database and never calls back into traffic-service's DB for live
 * traffic. Instead a Kafka consumer (TrafficReadingConsumer) keeps this cache
 * up to date as traffic-service publishes readings. Route weighting then reads
 * straight from here - fast, and fully decoupled from traffic-service.
 */
@Component
public class LocalTrafficCache {

    private record CachedReading(double avgSpeedKmh, Instant recordedAt) {
    }

    private final Map<Long, CachedReading> latestBySegment = new ConcurrentHashMap<>();

    private final long freshnessMinutes;

    public LocalTrafficCache(@Value("${app.routing.traffic-freshness-minutes:30}") long freshnessMinutes) {
        this.freshnessMinutes = freshnessMinutes;
    }

    /**
     * Refreshes the cache entry for a segment. Called by the Kafka consumer.
     */
    public void put(Long roadSegmentId, double avgSpeedKmh, Instant recordedAt) {
        if (roadSegmentId == null) {
            return;
        }
        latestBySegment.put(roadSegmentId, new CachedReading(avgSpeedKmh, recordedAt != null ? recordedAt : Instant.now()));
    }

    /**
     * @return the effective speed for a segment, or Optional.empty() if there
     *         is no fresh reading cached (caller falls back to the default).
     */
    public Optional<Double> effectiveSpeed(Long roadSegmentId) {
        CachedReading reading = latestBySegment.get(roadSegmentId);
        if (reading == null) {
            return Optional.empty();
        }
        if (reading.recordedAt().isBefore(Instant.now().minus(freshnessMinutes, ChronoUnit.MINUTES))) {
            // Stale - treat as if we have no reading, but keep it around in case
            // a future call needs a rough answer before the next Kafka event.
            latestBySegment.remove(roadSegmentId, reading);
            return Optional.empty();
        }
        if (reading.avgSpeedKmh() <= 0) {
            return Optional.empty();
        }
        return Optional.of(reading.avgSpeedKmh());
    }

    public int size() {
        return latestBySegment.size();
    }

}