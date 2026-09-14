package com.smarttraffic.trafficservice.integration.ml;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.core.functions.CheckedSupplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Calls the ML service for congestion predictions.
 *
 * The call is wrapped in a Resilience4j retry + circuit breaker: a flaky/absent
 * ML service gets three quick attempts (backing off briefly) before the call
 * fails here, and once enough consecutive failures are seen the breaker opens
 * and requests short-circuit immediately without touching ml-service at all.
 * Either way the caller sees Optional.empty() here - the same graceful
 * degradation the old try/catch gave, but without hammering a down service.
 */
@Component
@Slf4j
public class MlPredictionClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public MlPredictionClient(RestTemplate restTemplate,
                              @Value("${app.ml.base-url}") String baseUrl,
                              CircuitBreakerRegistry circuitBreakerRegistry,
                              RetryRegistry retryRegistry) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        // Each service keeps its own breaker/retry instances. The resilience4j
        // "ml" config from application.yml drives these registries (thresholds,
        // wait durations, etc.); metrics are published to Prometheus via Micrometer.
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("ml");
        this.retry = retryRegistry.retry("ml");
    }

    /**
     * Calls the ML service for a prediction.
     * Returns Optional.empty() if the service is unreachable, has no model
     * trained yet, or keeps failing after retries/while the circuit is open -
     * callers decide how to surface that.
     */
    public Optional<MlPredictionApiResponse> predict(Long roadSegmentId, LocalDateTime timestamp) {
        // Circuit breaker on the outside, retry on the inside: a single call
        // gets its retries, then the outcome counts once against the breaker.
        CheckedSupplier<Optional<MlPredictionApiResponse>> decoratedCall =
                CircuitBreaker.decorateCheckedSupplier(circuitBreaker,
                        Retry.decorateCheckedSupplier(retry, () -> doPredict(roadSegmentId, timestamp)));

        try {
            return decoratedCall.get();
        } catch (Throwable t) {
            // Cover the last-resort case (e.g. circuit open, or all retries
            // exhausted): degrade gracefully, exactly like the old catch-all.
            log.warn("ML prediction call failed for segment {} after retries/circuit breaker: {}",
                    roadSegmentId, t.getMessage());
            return Optional.empty();
        }
    }

    private Optional<MlPredictionApiResponse> doPredict(Long roadSegmentId, LocalDateTime timestamp) {
        MlPredictionApiRequest request = new MlPredictionApiRequest(roadSegmentId, timestamp);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<MlPredictionApiRequest> entity = new HttpEntity<>(request, headers);

        MlPredictionApiResponse response = restTemplate.postForObject(
                baseUrl + "/predict", entity, MlPredictionApiResponse.class);
        return Optional.ofNullable(response);
    }

}