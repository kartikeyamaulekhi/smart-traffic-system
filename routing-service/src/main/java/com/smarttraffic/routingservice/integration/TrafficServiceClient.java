package com.smarttraffic.routingservice.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

/**
 * Fetches the road network from traffic-service. This is the one synchronous
 * dependency routing-service has on another service - it's fine because the
 * graph is rebuilt on demand for each route request and the segments change
 * rarely compared to traffic readings (which come through Kafka).
 *
 * The URL is statically configured (TRAFFIC_SERVICE_URL); in docker-compose
 * this becomes the service's DNS name.
 */
@Component
@Slf4j
public class TrafficServiceClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public TrafficServiceClient(RestTemplate restTemplate, @Value("${app.traffic-service.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public List<RoadSegment> fetchRoadSegments(String callToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            if (callToken != null && !callToken.isBlank()) {
                headers.setBearerAuth(callToken);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            List<RoadSegment> segments = restTemplate.exchange(
                    baseUrl + "/api/road-segments",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<RoadSegment>>() {
                    }
            ).getBody();
            return segments != null ? segments : Collections.emptyList();
        } catch (RestClientException ex) {
            log.error("Failed to fetch road segments from traffic-service at {}: {}", baseUrl, ex.getMessage());
            throw new IllegalStateException(
                    "Unable to reach traffic-service for the road network. Is it running at " + baseUrl + "?"
            );
        }
    }

}