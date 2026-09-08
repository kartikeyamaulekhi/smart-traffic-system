package com.smarttraffic.trafficservice.integration.ml;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@Slf4j
public class MlPredictionClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public MlPredictionClient(RestTemplate restTemplate, @Value("${app.ml.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * Calls the ML service for a prediction.
     * Returns Optional.empty() if the service is unreachable, has no model
     * trained yet, or returns an error - callers decide how to surface that.
     */
    public Optional<MlPredictionApiResponse> predict(Long roadSegmentId, LocalDateTime timestamp) {
        MlPredictionApiRequest request = new MlPredictionApiRequest(roadSegmentId, timestamp);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<MlPredictionApiRequest> entity = new HttpEntity<>(request, headers);

        try {
            MlPredictionApiResponse response = restTemplate.postForObject(
                    baseUrl + "/predict", entity, MlPredictionApiResponse.class);
            return Optional.ofNullable(response);
        } catch (RestClientException ex) {
            log.warn("ML prediction service call failed for segment {}: {}", roadSegmentId, ex.getMessage());
            return Optional.empty();
        }
    }

}