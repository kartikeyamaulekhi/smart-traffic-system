package com.smarttraffic.trafficservice.integration.ml;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MlPredictionApiRequest {

    @JsonProperty("road_segment_id")
    private Long roadSegmentId;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp; // null means "let the ML service default to now"

}