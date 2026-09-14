package com.smarttraffic.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Boots the full gateway context and checks Spring Boot Actuator is wired so
 * its readiness endpoint reports UP. Downstream service probing is disabled in
 * the "test" profile (management.health.downstream.enabled=false) - the real
 * services don't run in CI. Phase 19 observability.
 */
@SpringBootTest
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@AutoConfigureObservability
class ActuatorHealthTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void actuatorHealth_reportsUp() {
        webTestClient.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    void prometheusEndpoint_exposesMetrics() {
        webTestClient.get().uri("/actuator/prometheus")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null && body.contains("jvm_memory_used_bytes");
                });
    }
}