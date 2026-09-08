package com.smarttraffic.routingservice.routing;

import com.smarttraffic.routingservice.cache.LocalTrafficCache;
import com.smarttraffic.routingservice.integration.RoadSegment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns the list of RoadSegments (fetched from traffic-service) into a graph:
 * each distinct coordinate (rounded to ~1m) is a node, each road segment is
 * an edge between two nodes. Roads are treated as bidirectional (an edge is
 * added in both directions).
 *
 * Effective speed for each edge is resolved from the local in-memory cache
 * (kept fresh by the Kafka consumer), NOT from a database. This is the
 * "event-carried state transfer" part of Phase 11c.
 */
@Component
@RequiredArgsConstructor
public class RoadNetworkBuilder {

    private static final double DEFAULT_SPEED_KMH = 40.0;

    private final LocalTrafficCache localTrafficCache;

    public Map<String, List<RoadEdge>> build(List<RoadSegment> segments) {
        Map<String, List<RoadEdge>> graph = new HashMap<>();

        for (RoadSegment segment : segments) {
            String fromKey = GeoUtils.nodeKey(segment.getStartLat(), segment.getStartLng());
            String toKey = GeoUtils.nodeKey(segment.getEndLat(), segment.getEndLng());

            double distanceKm = GeoUtils.distanceKm(
                    segment.getStartLat(), segment.getStartLng(),
                    segment.getEndLat(), segment.getEndLng()
            );

            double speedKmh = localTrafficCache.effectiveSpeed(segment.getId()).orElse(DEFAULT_SPEED_KMH);
            double travelTimeMinutes = (distanceKm / speedKmh) * 60.0;

            RoadEdge forward = new RoadEdge(
                    segment.getId(), segment.getName(), fromKey, toKey,
                    distanceKm, speedKmh, travelTimeMinutes
            );
            RoadEdge backward = new RoadEdge(
                    segment.getId(), segment.getName(), toKey, fromKey,
                    distanceKm, speedKmh, travelTimeMinutes
            );

            graph.computeIfAbsent(fromKey, k -> new ArrayList<>()).add(forward);
            graph.computeIfAbsent(toKey, k -> new ArrayList<>()).add(backward);
        }

        return graph;
    }

}