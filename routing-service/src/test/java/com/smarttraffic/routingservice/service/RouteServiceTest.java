package com.smarttraffic.routingservice.service;

import com.smarttraffic.routingservice.cache.LocalTrafficCache;
import com.smarttraffic.routingservice.dto.RouteRequest;
import com.smarttraffic.routingservice.dto.RouteResponse;
import com.smarttraffic.routingservice.exception.NoRouteFoundException;
import com.smarttraffic.routingservice.integration.RoadSegment;
import com.smarttraffic.routingservice.integration.TrafficServiceClient;
import com.smarttraffic.routingservice.routing.DijkstraRouter;
import com.smarttraffic.routingservice.routing.RoadNetworkBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteServiceTest {

    @Mock
    private TrafficServiceClient trafficServiceClient;

    @Mock
    private LocalTrafficCache localTrafficCache;

    private RouteService routeService;

    @BeforeEach
    void setUp() {
        RoadNetworkBuilder builder = new RoadNetworkBuilder(localTrafficCache);
        routeService = new RouteService(trafficServiceClient, builder, new DijkstraRouter());
    }

    private RoadSegment segment1() {
        RoadSegment r = new RoadSegment();
        r.setId(1L);
        r.setName("MG Road");
        r.setStartLat(22.7196);
        r.setStartLng(75.8577);
        r.setEndLat(22.7182);
        r.setEndLng(75.8641);
        return r;
    }

    private RoadSegment segment2() {
        RoadSegment r = new RoadSegment();
        r.setId(2L);
        r.setName("Civil Lines Road");
        r.setStartLat(22.7182);
        r.setStartLng(75.8641);
        r.setEndLat(22.7170);
        r.setEndLng(75.8710);
        return r;
    }

    private RouteRequest request(double oLat, double oLng, double dLat, double dLng) {
        RouteRequest request = new RouteRequest();
        request.setOriginLat(oLat);
        request.setOriginLng(oLng);
        request.setDestinationLat(dLat);
        request.setDestinationLng(dLng);
        return request;
    }

    @Test
    void findRoute_connectedGraph_returnsRoute() {
        when(trafficServiceClient.fetchRoadSegments("tok")).thenReturn(List.of(segment1(), segment2()));
        when(localTrafficCache.effectiveSpeed(1L)).thenReturn(Optional.of(40.0));
        when(localTrafficCache.effectiveSpeed(2L)).thenReturn(Optional.of(40.0));

        RouteResponse response = routeService.findRoute(
                request(22.7196, 75.8577, 22.7170, 75.8710), "tok");

        assertNotNull(response);
        assertEquals(2, response.getSegments().size());
        assertEquals(1L, response.getSegments().get(0).getRoadSegmentId());
        assertEquals(2L, response.getSegments().get(1).getRoadSegmentId());
        assertEquals("MG Road", response.getSegments().get(0).getRoadSegmentName());
        assertEquals("Civil Lines Road", response.getSegments().get(1).getRoadSegmentName());
    }

    @Test
    void findRoute_noSegments_throws() {
        when(trafficServiceClient.fetchRoadSegments("tok")).thenReturn(List.of());

        assertThrows(NoRouteFoundException.class,
                () -> routeService.findRoute(request(22.7196, 75.8577, 22.7170, 75.8710), "tok"));
    }

    @Test
    void findRoute_sameSnapPoint_throws() {
        when(trafficServiceClient.fetchRoadSegments("tok")).thenReturn(List.of(segment1()));
        when(localTrafficCache.effectiveSpeed(1L)).thenReturn(Optional.of(40.0));

        assertThrows(NoRouteFoundException.class,
                () -> routeService.findRoute(request(22.7196, 75.8577, 22.7196, 75.8577), "tok"));
    }

    @Test
    void findRoute_tooFarFromNetwork_throws() {
        when(trafficServiceClient.fetchRoadSegments("tok")).thenReturn(List.of(segment1()));

        assertThrows(NoRouteFoundException.class,
                () -> routeService.findRoute(request(10.0, 20.0, 22.7170, 75.8710), "tok"));
    }
}