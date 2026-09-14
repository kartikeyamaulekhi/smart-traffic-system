package com.smarttraffic.routingservice.integration;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.core.functions.CheckedSupplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
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
 *
 * The HTTP call is wrapped in a Resilience4j retry + circuit breaker: brief
 * outages are retried a few times, and a sustained outage opens the breaker so
 * routing fails fast with a clean 503 instead of piling up timeout requests
 * onto a dead traffic-service.
 */
@Component
@Slf4j
public class TrafficServiceClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public TrafficServiceClient(RestTemplate restTemplate,
                                @Value("${app.traffic-service.base-url}") String baseUrl,
                                CircuitBreakerRegistry circuitBreakerRegistry,
                                RetryRegistry retryRegistry) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("traffic");
        this.retry = retryRegistry.retry("traffic");
    }

    public List<RoadSegment> fetchRoadSegments(String callToken) {
        CheckedSupplier<List<RoadSegment>> decoratedCall =
                CircuitBreaker.decorateCheckedSupplier(circuitBreaker,
                        Retry.decorateCheckedSupplier(retry, () -> doFetch(callToken)));

        try {
            return decoratedCall.get();
        } catch (Throwable t) {
            log.error("Failed to fetch road segments from traffic-service at {} after retries/circuit breaker: {}",
                    baseUrl, t.getMessage());
            throw new IllegalStateException(
                    "Unable to reach traffic-service for the road network. Is it running at " + baseUrl + "?",
                    t
            );
        }
    }

    private List<RoadSegment> doFetch(String callToken) {
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
    }

}