package com.smarttraffic.trafficservice.integration.tomtom;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Locale;
import java.util.Optional;

@Component
@Slf4j
public class TomTomTrafficClient {

    private static final String PLACEHOLDER_KEY = "demo-key-replace-me";

    private static final String FLOW_URL =
            "https://api.tomtom.com/traffic/services/4/flowSegmentData/absolute/10/json?point={point}&key={key}";

    private final RestTemplate restTemplate;
    private final String apiKey;

    public TomTomTrafficClient(RestTemplate restTemplate, @Value("${app.tomtom.api-key}") String apiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
    }

    /**
     * Fetches live flow data for the given coordinate.
     * Returns Optional.empty() if no API key is configured yet, or if the
     * call fails for any reason - callers should treat that as "skip this reading",
     * not as a fatal error.
     */
    public Optional<TomTomFlowResponse.FlowSegmentData> fetchFlow(double lat, double lng) {
        if (isPlaceholderKey()) {
            log.debug("No TomTom API key configured yet - skipping live traffic fetch.");
            return Optional.empty();
        }

        String point = String.format(Locale.US, "%.6f,%.6f", lat, lng);

        try {
            TomTomFlowResponse response = restTemplate.getForObject(FLOW_URL, TomTomFlowResponse.class, point, apiKey);
            return Optional.ofNullable(response).map(TomTomFlowResponse::getFlowSegmentData);
        } catch (RestClientException ex) {
            log.warn("TomTom API call failed for point {}: {}", point, ex.getMessage());
            return Optional.empty();
        }
    }

    private boolean isPlaceholderKey() {
        return apiKey == null || apiKey.isBlank() || PLACEHOLDER_KEY.equals(apiKey);
    }

}