package com.smarttraffic.trafficservice.service;

import com.smarttraffic.trafficservice.dto.PredictionResponse;
import com.smarttraffic.trafficservice.dto.RoadSegmentResponse;
import com.smarttraffic.trafficservice.exception.MlServiceUnavailableException;
import com.smarttraffic.trafficservice.integration.ml.MlPredictionApiResponse;
import com.smarttraffic.trafficservice.integration.ml.MlPredictionClient;
import com.smarttraffic.trafficservice.model.CongestionLevel;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PredictionServiceTest {

    @Mock
    private RoadSegmentService roadSegmentService;

    @Mock
    private MlPredictionClient mlPredictionClient;

    @Spy
    private SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    @InjectMocks
    private PredictionService predictionService;

    private RoadSegmentResponse segment() {
        return RoadSegmentResponse.builder()
                .id(1L)
                .name("MG Road")
                .city("Indore")
                .build();
    }

    @Test
    void predict_mlResponds_returnsMappedPrediction() {
        LocalDateTime ts = LocalDateTime.of(2026, 9, 5, 9, 30);
        when(roadSegmentService.findById(1L)).thenReturn(segment());

        MlPredictionApiResponse apiResponse = new MlPredictionApiResponse();
        apiResponse.setRoadSegmentId(1L);
        apiResponse.setTimestamp(ts);
        apiResponse.setPredictedCongestionLevel("HIGH");
        apiResponse.setConfidence(0.82);
        when(mlPredictionClient.predict(1L, ts)).thenReturn(Optional.of(apiResponse));

        PredictionResponse response = predictionService.predict(1L, ts);

        assertEquals(1L, response.getRoadSegmentId());
        assertEquals("MG Road", response.getRoadSegmentName());
        assertEquals(CongestionLevel.HIGH, response.getPredictedCongestionLevel());
        assertEquals(0.82, response.getConfidence());
        assertEquals(ts, response.getForTimestamp());
    }

    @Test
    void predict_mlEmpty_throwsUnavailable() {
        LocalDateTime ts = LocalDateTime.of(2026, 9, 5, 9, 30);
        when(roadSegmentService.findById(1L)).thenReturn(segment());
        when(mlPredictionClient.predict(1L, ts)).thenReturn(Optional.empty());

        assertThrows(MlServiceUnavailableException.class, () -> predictionService.predict(1L, ts));
    }
}