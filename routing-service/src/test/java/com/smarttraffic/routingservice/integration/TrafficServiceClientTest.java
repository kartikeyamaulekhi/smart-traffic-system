package com.smarttraffic.routingservice.integration;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TrafficServiceClientTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private TrafficServiceClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofMillis(50))
                .build();
        client = new TrafficServiceClient(restTemplate, "http://traffic-service:8082",
                CircuitBreakerRegistry.ofDefaults(), RetryRegistry.of(retryConfig));
    }

    @Test
    void fetchRoadSegments_trafficResponds_returnsSegments() {
        String body = """
                [{"id":1,"name":"MG Road","startLat":22.7196,"startLng":75.8577,
                  "endLat":22.7182,"endLng":75.8641,"speedLimitKmh":50}]
                """;

        mockServer.expect(requestTo("http://traffic-service:8082/api/road-segments"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        List<RoadSegment> segments = client.fetchRoadSegments("some-token");

        assertEquals(1, segments.size());
        assertEquals(1L, segments.get(0).getId());
        assertEquals("MG Road", segments.get(0).getName());
    }

    @Test
    void fetchRoadSegments_noBody_returnsEmptyList() {
        mockServer.expect(requestTo("http://traffic-service:8082/api/road-segments"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<RoadSegment> segments = client.fetchRoadSegments("some-token");

        assertTrue(segments.isEmpty());
    }

    @Test
    void fetchRoadSegments_trafficDown_retriesThenThrows() {
        mockServer.expect(requestTo("http://traffic-service:8082/api/road-segments"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());
        mockServer.expect(requestTo("http://traffic-service:8082/api/road-segments"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        assertThrows(IllegalStateException.class, () -> client.fetchRoadSegments("some-token"));
    }

    @Test
    void fetchRoadSegments_trafficDown_circuitOpensAndFailsFast() {
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
        client = new TrafficServiceClient(restTemplate, "http://traffic-service:8082",
                CircuitBreakerRegistry.of(breakerConfig), RetryRegistry.of(retryConfig));

        for (int i = 0; i < 4; i++) {
            mockServer.expect(requestTo("http://traffic-service:8082/api/road-segments"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withServerError());
        }

        for (int i = 0; i < 4; i++) {
            assertThrows(IllegalStateException.class, () -> client.fetchRoadSegments("some-token"));
        }

        // Circuit OPEN - next call fails fast without touching traffic-service.
        assertThrows(IllegalStateException.class, () -> client.fetchRoadSegments("some-token"));
        mockServer.verify();
    }
}