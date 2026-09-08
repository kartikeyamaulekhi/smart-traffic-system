package com.smarttraffic.routingservice.routing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeoUtilsTest {

    @Test
    void distanceKm_samePoint_isZero() {
        assertEquals(0.0, GeoUtils.distanceKm(22.7196, 75.8577, 22.7196, 75.8577), 0.0001);
    }

    @Test
    void distanceKm_knownPair_matchesHaversine() {
        double d = GeoUtils.distanceKm(22.7196, 75.8577, 22.7182, 75.8641);
        // Roughly 650m in Indore coordinates
        assertTrue(d > 0.5 && d < 0.8, "expected ~0.65km but was " + d);
    }

    @Test
    void nodeKey_roundsTo5Decimals() {
        assertEquals("22.71960,75.85770", GeoUtils.nodeKey(22.7196, 75.8577));
        assertEquals("22.71960,75.85770", GeoUtils.nodeKey(22.719604, 75.857704));
    }
}