package com.smarttraffic.routingservice.service;

import com.smarttraffic.routingservice.dto.RouteRequest;
import com.smarttraffic.routingservice.dto.RouteResponse;
import com.smarttraffic.routingservice.dto.RouteSegmentInfo;
import com.smarttraffic.routingservice.exception.NoRouteFoundException;
import com.smarttraffic.routingservice.integration.RoadSegment;
import com.smarttraffic.routingservice.integration.TrafficServiceClient;
import com.smarttraffic.routingservice.routing.DijkstraRouter;
import com.smarttraffic.routingservice.routing.GeoUtils;
import com.smarttraffic.routingservice.routing.RoadEdge;
import com.smarttraffic.routingservice.routing.RoadNetworkBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RouteService {

    // How far (in km) an origin/destination point is allowed to be from the
    // nearest known road network node before we give up on it. Prevents
    // silently "snapping" a request to a road segment that's nowhere near
    // where the person actually meant.
    private static final double MAX_SNAP_DISTANCE_KM = 2.0;

    private final TrafficServiceClient trafficServiceClient;
    private final RoadNetworkBuilder roadNetworkBuilder;
    private final DijkstraRouter dijkstraRouter;

    public RouteResponse findRoute(RouteRequest request, String callerToken) {
        List<RoadSegment> segments = trafficServiceClient.fetchRoadSegments(callerToken);
        if (segments.isEmpty()) {
            throw new NoRouteFoundException("No road segments exist yet - nothing to route through.");
        }

        Map<String, List<RoadEdge>> graph = roadNetworkBuilder.build(segments);

        String startNode = snapToNearestNode(segments, request.getOriginLat(), request.getOriginLng());
        String endNode = snapToNearestNode(segments, request.getDestinationLat(), request.getDestinationLng());

        if (startNode.equals(endNode)) {
            throw new NoRouteFoundException("Origin and destination snap to the same road network point.");
        }

        List<RoadEdge> path = dijkstraRouter.findShortestPath(graph, startNode, endNode)
                .orElseThrow(() -> new NoRouteFoundException(
                        "No connected path exists between the origin and destination in the current road network."
                ));

        List<RouteSegmentInfo> segmentInfos = path.stream()
                .map(edge -> RouteSegmentInfo.builder()
                        .roadSegmentId(edge.getRoadSegmentId())
                        .roadSegmentName(edge.getRoadSegmentName())
                        .distanceKm(round(edge.getDistanceKm()))
                        .effectiveSpeedKmh(round(edge.getEffectiveSpeedKmh()))
                        .travelTimeMinutes(round(edge.getTravelTimeMinutes()))
                        .build())
                .toList();

        double totalDistance = segmentInfos.stream().mapToDouble(RouteSegmentInfo::getDistanceKm).sum();
        double totalTime = segmentInfos.stream().mapToDouble(RouteSegmentInfo::getTravelTimeMinutes).sum();

        return RouteResponse.builder()
                .totalDistanceKm(round(totalDistance))
                .totalTravelTimeMinutes(round(totalTime))
                .segments(segmentInfos)
                .build();
    }

    /**
     * Finds the closest existing road segment endpoint to the given point.
     * This is what lets a request use "real" coordinates instead of having
     * to know the exact node keys of the graph.
     */
    private String snapToNearestNode(List<RoadSegment> segments, double lat, double lng) {
        String nearestKey = null;
        double nearestDistance = Double.MAX_VALUE;

        for (RoadSegment segment : segments) {
            String startKey = GeoUtils.nodeKey(segment.getStartLat(), segment.getStartLng());
            double startDist = GeoUtils.distanceKm(lat, lng, segment.getStartLat(), segment.getStartLng());
            if (startDist < nearestDistance) {
                nearestDistance = startDist;
                nearestKey = startKey;
            }

            String endKey = GeoUtils.nodeKey(segment.getEndLat(), segment.getEndLng());
            double endDist = GeoUtils.distanceKm(lat, lng, segment.getEndLat(), segment.getEndLng());
            if (endDist < nearestDistance) {
                nearestDistance = endDist;
                nearestKey = endKey;
            }
        }

        if (nearestKey == null || nearestDistance > MAX_SNAP_DISTANCE_KM) {
            throw new NoRouteFoundException(String.format(
                    "No road segment endpoint found within %.1f km of (%.5f, %.5f).",
                    MAX_SNAP_DISTANCE_KM, lat, lng
            ));
        }

        return nearestKey;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

}