package com.smarttraffic.trafficservice.dto;

import com.smarttraffic.trafficservice.model.CongestionLevel;
import com.smarttraffic.trafficservice.model.TrafficSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrafficDataResponse {

    private Long id;
    private Long roadSegmentId;
    private String roadSegmentName;
    private Integer vehicleCount;
    private Double avgSpeedKmh;
    private CongestionLevel congestionLevel;
    private TrafficSource source;
    private Instant recordedAt;

}