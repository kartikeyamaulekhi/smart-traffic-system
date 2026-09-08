package com.smarttraffic.trafficservice.dto;

import com.smarttraffic.trafficservice.model.CongestionLevel;
import com.smarttraffic.trafficservice.model.TrafficSource;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TrafficDataRequest {

    @NotNull(message = "roadSegmentId is required")
    private Long roadSegmentId;

    @NotNull(message = "vehicleCount is required")
    @Min(value = 0, message = "vehicleCount cannot be negative")
    private Integer vehicleCount;

    @NotNull(message = "avgSpeedKmh is required")
    @Min(value = 0, message = "avgSpeedKmh cannot be negative")
    private Double avgSpeedKmh;

    @NotNull(message = "congestionLevel is required")
    private CongestionLevel congestionLevel;

    @NotNull(message = "source is required")
    private TrafficSource source;

}