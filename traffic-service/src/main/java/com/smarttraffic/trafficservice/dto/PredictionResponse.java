package com.smarttraffic.trafficservice.dto;

import com.smarttraffic.trafficservice.model.CongestionLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictionResponse {

    private Long roadSegmentId;
    private String roadSegmentName;
    private LocalDateTime forTimestamp;
    private CongestionLevel predictedCongestionLevel;
    private double confidence;

}