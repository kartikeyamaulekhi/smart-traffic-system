package com.smarttraffic.routingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteSegmentInfo {

    private Long roadSegmentId;
    private String roadSegmentName;
    private double distanceKm;
    private double effectiveSpeedKmh;
    private double travelTimeMinutes;

}