package com.smarttraffic.trafficservice.integration.ml;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MlPredictionClientTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private MlPredictionClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
        // Deliberately tiny retry/breaker windows so unit tests stay fast.
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofMillis(50))
                .build();
        client = new MlPredictionClient(restTemplate, "http://ml-service:8010",
                CircuitBreakerRegistry.ofDefaults(), RetryRegistry.of(retryConfig));
    }

    @Test
    void predict_mlResponds_returnsResponse() {
        String body = "{\"road_segment_id\":1,\"timestamp\":\"2026-09-05T09:30:00\","
                + "\"predicted_congestion_level\":\"HIGH\",\"confidence\":0.82}";

        mockServer.expect(requestTo("http://ml-service:8010/predict"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        MlPredictionApiResponse response = client.predict(1L, LocalDateTime.of(2026, 9, 5, 9, 30)).orElseThrow();

        assertEquals(1L, response.getRoadSegmentId());
        assertEquals("HIGH", response.getPredictedCongestionLevel());
        assertEquals(0.82, response.getConfidence(), 0.001);
    }

    @Test
    void predict_mlDown_retriesThenReturnsEmpty() {
        // With maxAttempts=2 the client hits ml-service twice before giving up.
        mockServer.expect(requestTo("http://ml-service:8010/predict"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withServerError());
        mockServer.expect(requestTo("http://ml-service:8010/predict"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withServerError());

        assertTrue(client.predict(1L, LocalDateTime.of(2026, 9, 5, 9, 30)).isEmpty());
    }

    @Test
    void predict_mlNullBody_returnsEmpty() {
        mockServer.expect(requestTo("http://ml-service:8010/predict"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("null", MediaType.APPLICATION_JSON));

        assertTrue(client.predict(1L, LocalDateTime.of(2026, 9, 5, 9, 30)).isEmpty());
    }

    @Test
    void predict_mlMalformedBody_returnsEmpty() {
        mockServer.expect(requestTo("http://ml-service:8010/predict"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        mockServer.expect(requestTo("http://ml-service:8010/predict"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertTrue(client.predict(1L, LocalDateTime.of(2026, 9, 5, 9, 30)).isEmpty());
    }

    @Test
    void predict_mlKeepsFailing_circuitOpensAndFailsFast() {
        CircuitBreakerConfig breakerConfig = CircuitBreakerConfig.custom()
                .slidingWindowSize(4)
                .minimumNumberOfCalls(4)
                .failureRateThreshold(50)
                .permittedNumberOfCallsInHalfOpenState(1)
                .waitDurationInOpenState(Duration.ofMinutes(1))
                .build();
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(1)
                .build();
        client = new MlPredictionClient(restTemplate, "http://ml-service:8010",
                CircuitBreakerRegistry.of(breakerConfig), RetryRegistry.of(retryConfig));

        for (int i = 0; i < 4; i++) {
            mockServer.expect(requestTo("http://ml-service:8010/predict"))
                    .andExpect(method(org.springframework.http.HttpMethod.POST))
                    .andRespond(withServerError());
        }

        for (int i = 0; i < 4; i++) {
            assertTrue(client.predict(1L, LocalDateTime.of(2026, 9, 5, 9, 30)).isEmpty());
        }

        // Circuit is OPEN now - the next call must short-circuit without
        // touching ml-service (mockServer.verify() enforces no extra requests).
        assertTrue(client.predict(1L, LocalDateTime.of(2026, 9, 5, 9, 30)).isEmpty());
        mockServer.verify();
    }
}