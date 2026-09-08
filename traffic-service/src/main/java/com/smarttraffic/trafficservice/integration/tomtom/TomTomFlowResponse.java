package com.smarttraffic.trafficservice.integration.tomtom;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Maps the response of TomTom's "Flow Segment Data" endpoint:
 * https://api.tomtom.com/traffic/services/4/flowSegmentData/absolute/10/json
 *
 * Only the fields we actually use are mapped; everything else in the
 * response is ignored.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TomTomFlowResponse {

    private FlowSegmentData flowSegmentData;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FlowSegmentData {
        private Integer currentSpeed;
        private Integer freeFlowSpeed;
        private Double confidence;
        private Boolean roadClosure;
    }

}