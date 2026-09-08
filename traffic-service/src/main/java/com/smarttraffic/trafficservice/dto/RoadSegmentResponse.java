package com.smarttraffic.trafficservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoadSegmentResponse {

    private Long id;
    private String name;
    private String city;
    private Double startLat;
    private Double startLng;
    private Double endLat;
    private Double endLng;
    private Instant createdAt;

}