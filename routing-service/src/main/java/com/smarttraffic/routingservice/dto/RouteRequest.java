package com.smarttraffic.routingservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RouteRequest {

    @NotNull(message = "originLat is required")
    private Double originLat;

    @NotNull(message = "originLng is required")
    private Double originLng;

    @NotNull(message = "destinationLat is required")
    private Double destinationLat;

    @NotNull(message = "destinationLng is required")
    private Double destinationLng;

}