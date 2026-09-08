package com.smarttraffic.routingservice.routing;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RoadEdge {

    private Long roadSegmentId;
    private String roadSegmentName;
    private String fromNodeKey;
    private String toNodeKey;
    private double distanceKm;
    private double effectiveSpeedKmh;
    private double travelTimeMinutes; // this is the Dijkstra edge weight

}