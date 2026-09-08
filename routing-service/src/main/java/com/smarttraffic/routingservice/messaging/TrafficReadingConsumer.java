package com.smarttraffic.routingservice.messaging;

import com.smarttraffic.routingservice.cache.LocalTrafficCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Consumes "traffic-readings" events published by traffic-service and applies
 * them to the local in-memory cache. This is the ONLY thing that updates live
 * traffic state in routing-service - no database, no REST call back to
 * traffic-service for current speed.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TrafficReadingConsumer {

    private final LocalTrafficCache localTrafficCache;

    @KafkaListener(topics = KafkaTopics.TRAFFIC_READINGS, groupId = "${spring.kafka.consumer.group-id}")
    public void consume(TrafficReadingEvent event) {
        if (event.getRoadSegmentId() == null || event.getAvgSpeedKmh() == null) {
            log.debug("Ignoring incomplete traffic-reading event (missing segment id or speed).");
            return;
        }

        localTrafficCache.put(event.getRoadSegmentId(), event.getAvgSpeedKmh(), Instant.now());
        log.info("Cached traffic reading for segment {}: {} km/h", event.getRoadSegmentId(), event.getAvgSpeedKmh());
    }

}