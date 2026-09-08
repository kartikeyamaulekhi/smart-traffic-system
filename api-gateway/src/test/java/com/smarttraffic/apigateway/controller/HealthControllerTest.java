package com.smarttraffic.apigateway.controller;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HealthControllerTest {

    private final HealthController controller = new HealthController();

    @Test
    void health_returnsUpWithServiceName() {
        Map<String, Object> body = controller.health().block();

        assertNotNull(body);
        assertEquals("UP", body.get("status"));
        assertEquals("api-gateway", body.get("service"));
        assertNotNull(body.get("timestamp"));
    }
}