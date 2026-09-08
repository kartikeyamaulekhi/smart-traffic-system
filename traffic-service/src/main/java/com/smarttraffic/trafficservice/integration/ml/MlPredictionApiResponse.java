package com.smarttraffic.trafficservice.integration.ml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Maps the response of ml-service's POST /predict endpoint.
 * Field names use snake_case there (Python/FastAPI convention),
 * so each is mapped explicitly to its camelCase Java equivalent.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MlPredictionApiResponse {

    @JsonProperty("road_segment_id")
    private Long roadSegmentId;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @JsonProperty("predicted_congestion_level")
    private String predictedCongestionLevel;

    @JsonProperty("confidence")
    private Double confidence;

}