package com.smarttraffic.routingservice.messaging;

import com.smarttraffic.routingservice.cache.LocalTrafficCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TrafficReadingConsumerTest {

    @Mock
    private LocalTrafficCache localTrafficCache;

    @InjectMocks
    private TrafficReadingConsumer consumer;

    private TrafficReadingEvent event() {
        TrafficReadingEvent event = new TrafficReadingEvent();
        event.setRoadSegmentId(1L);
        event.setAvgSpeedKmh(34.5);
        return event;
    }

    @Test
    void consume_validEvent_updatesCache() {
        consumer.consume(event());

        verify(localTrafficCache).put(anyLong(), anyDouble(), any(Instant.class));
    }

    @Test
    void consume_missingSegmentId_isIgnored() {
        TrafficReadingEvent event = event();
        event.setRoadSegmentId(null);

        consumer.consume(event);

        verify(localTrafficCache, never()).put(anyLong(), anyDouble(), any(Instant.class));
    }

    @Test
    void consume_missingSpeed_isIgnored() {
        TrafficReadingEvent event = event();
        event.setAvgSpeedKmh(null);

        consumer.consume(event);

        verify(localTrafficCache, never()).put(anyLong(), anyDouble(), any(Instant.class));
    }
}