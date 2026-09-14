package com.smarttraffic.apigateway.monitoring;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Aggregate health: probes each downstream microservice's own
 * /actuator/health endpoint and folds their status into the gateway's
 * /actuator/health output. The gateway is only "UP" when every downstream
 * service reports UP - one place to answer "is the whole system healthy?".
 *
 * Disabled in the "test" profile via
 * management.health.downstream.enabled=false so unit tests don't need the
 * real services running.
 */
@Component
@ConditionalOnProperty(name = "management.health.downstream.enabled",
        havingValue = "true", matchIfMissing = true)
public class DownstreamHealthIndicator implements HealthIndicator {

    private static final Duration TIMEOUT = Duration.ofMillis(1000);

    private final Map<String, String> services;
    private final WebClient webClient;

    public DownstreamHealthIndicator(
            @Value("${app.downstream.auth-service:http://localhost:8091}") String authServiceUrl,
            @Value("${app.downstream.traffic-service:http://localhost:8082}") String trafficServiceUrl,
            @Value("${app.downstream.routing-service:http://localhost:8083}") String routingServiceUrl,
            WebClient.Builder webClientBuilder) {
        this.services = new LinkedHashMap<>();
        this.services.put("auth-service", authServiceUrl);
        this.services.put("traffic-service", trafficServiceUrl);
        this.services.put("routing-service", routingServiceUrl);
        this.webClient = webClientBuilder.build();
    }

    @Override
    public Health health() {
        Map<String, Object> details = new LinkedHashMap<>();
        boolean allUp = true;
        for (Map.Entry<String, String> service : services.entrySet()) {
            boolean up = probe(service.getKey(), service.getValue(), details);
            allUp = allUp && up;
        }
        return allUp ? Health.up().withDetails(details).build()
                : Health.down().withDetails(details).build();
    }

    private boolean probe(String name, String baseUrl, Map<String, Object> details) {
        try {
            Map<String, Object> body = webClient.get()
                    .uri(baseUrl + "/actuator/health")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(TIMEOUT)
                    .block();
            String status = body == null ? "DOWN" : String.valueOf(body.get("status"));
            details.put(name, Map.of("status", status));
            return "UP".equalsIgnoreCase(status);
        } catch (Exception e) {
            details.put(name, Map.of("status", "DOWN", "error", e.getClass().getSimpleName()));
            return false;
        }
    }

}