package com.smarttraffic.routingservice.messaging;

import lombok.Data;

/**
 * Consumer-side view of the "traffic-readings" Kafka event published by
 * traffic-service. The JSON shape matches traffic-service's
 * TrafficDataRequest - routing-service does NOT share that class, it only
 * needs the fields relevant to route weighting (segment + speed). Keeping its
 * own event type here is what decouples the two services' builds.
 */
@Data
public class TrafficReadingEvent {

    private Long roadSegmentId;
    private Integer vehicleCount;
    private Double avgSpeedKmh;
    private String congestionLevel;
    private String source;

}