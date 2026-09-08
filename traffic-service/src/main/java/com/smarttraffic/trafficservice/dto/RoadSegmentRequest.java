package com.smarttraffic.trafficservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoadSegmentRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "city is required")
    private String city;

    @NotNull(message = "startLat is required")
    private Double startLat;

    @NotNull(message = "startLng is required")
    private Double startLng;

    @NotNull(message = "endLat is required")
    private Double endLat;

    @NotNull(message = "endLng is required")
    private Double endLng;

}