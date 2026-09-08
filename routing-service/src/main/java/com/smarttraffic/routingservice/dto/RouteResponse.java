package com.smarttraffic.routingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteResponse {

    private double totalDistanceKm;
    private double totalTravelTimeMinutes;
    private List<RouteSegmentInfo> segments;

}