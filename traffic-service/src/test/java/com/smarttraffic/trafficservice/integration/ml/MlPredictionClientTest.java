package com.smarttraffic.trafficservice.integration.ml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

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
        client = new MlPredictionClient(restTemplate, "http://ml-service:8010");
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
    void predict_mlDown_returnsEmpty() {
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

        assertTrue(client.predict(1L, LocalDateTime.of(2026, 9, 5, 9, 30)).isEmpty());
    }
}