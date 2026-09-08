package com.smarttraffic.trafficservice.service;

import com.smarttraffic.trafficservice.dto.PredictionResponse;
import com.smarttraffic.trafficservice.dto.RoadSegmentResponse;
import com.smarttraffic.trafficservice.exception.MlServiceUnavailableException;
import com.smarttraffic.trafficservice.integration.ml.MlPredictionApiResponse;
import com.smarttraffic.trafficservice.integration.ml.MlPredictionClient;
import com.smarttraffic.trafficservice.model.CongestionLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PredictionService {

    private final RoadSegmentService roadSegmentService;
    private final MlPredictionClient mlPredictionClient;

    /**
     * Cached in Redis under "predictions::{roadSegmentId}_{timestamp}".
     * A repeated call with the same arguments within the TTL window (see
     * RedisConfig) returns instantly from cache without calling ml-service
     * again - you'll only see the log line below on an actual cache miss.
     */
    @Cacheable(cacheNames = "predictions", key = "#roadSegmentId + '_' + #timestamp")
    public PredictionResponse predict(Long roadSegmentId, LocalDateTime timestamp) {
        log.info("Cache miss - calling ML service for segment {} at {}", roadSegmentId, timestamp);

        // Confirms the segment actually exists before bothering the ML service
        RoadSegmentResponse segment = roadSegmentService.findById(roadSegmentId);

        MlPredictionApiResponse apiResponse = mlPredictionClient.predict(roadSegmentId, timestamp)
                .orElseThrow(() -> new MlServiceUnavailableException(
                        "Prediction service is unavailable or has no trained model yet. " +
                        "Make sure ml-service is running and 'python train.py' has been run at least once."
                ));

        return PredictionResponse.builder()
                .roadSegmentId(segment.getId())
                .roadSegmentName(segment.getName())
                .forTimestamp(apiResponse.getTimestamp() != null ? apiResponse.getTimestamp() : LocalDateTime.now())
                .predictedCongestionLevel(CongestionLevel.valueOf(apiResponse.getPredictedCongestionLevel()))
                .confidence(apiResponse.getConfidence() != null ? apiResponse.getConfidence() : 0.0)
                .build();
    }

}