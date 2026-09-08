package com.smarttraffic.routingservice.integration;

import lombok.Data;

import java.time.Instant;

/**
 * Consumer-side view of traffic-service's RoadSegmentResponse. routing-service
 * does not share traffic-service's class - it just needs the geometry to build
 * the route graph.
 */
@Data
public class RoadSegment {

    private Long id;
    private String name;
    private String city;
    private Double startLat;
    private Double startLng;
    private Double endLat;
    private Double endLng;
    private Instant createdAt;

}