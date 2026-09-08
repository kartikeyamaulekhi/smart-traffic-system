package com.smarttraffic.routingservice.routing;

import com.smarttraffic.routingservice.cache.LocalTrafficCache;
import com.smarttraffic.routingservice.integration.RoadSegment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoadNetworkBuilderTest {

    private LocalTrafficCache cache;
    private RoadNetworkBuilder builder;

    @BeforeEach
    void setUp() {
        cache = mock(LocalTrafficCache.class);
        when(cache.effectiveSpeed(1L)).thenReturn(Optional.of(60.0));
        builder = new RoadNetworkBuilder(cache);
    }

    private RoadSegment segment1() {
        RoadSegment r = new RoadSegment();
        r.setId(1L);
        r.setName("MG Road");
        r.setStartLat(22.7196);
        r.setStartLng(75.8577);
        r.setEndLat(22.7182);
        r.setEndLng(75.8641);
        return r;
    }

    @Test
    void build_singleSegment_createsBidirectionalEdges() {
        Map<String, List<RoadEdge>> graph = builder.build(List.of(segment1()));

        assertEquals(2, graph.size());
        for (var entry : graph.values()) {
            assertEquals(1, entry.size());
        }

        RoadEdge forward = graph.values().stream().flatMap(List::stream)
                .filter(e -> e.getFromNodeKey().equals("22.71960,75.85770")).findFirst().orElseThrow();
        assertEquals("22.71820,75.86410", forward.getToNodeKey());
        assertEquals(60.0, forward.getEffectiveSpeedKmh());
        assertTrue(forward.getDistanceKm() > 0);
        assertTrue(forward.getTravelTimeMinutes() > 0);
    }
}