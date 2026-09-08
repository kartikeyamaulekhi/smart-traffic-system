package com.smarttraffic.routingservice.routing;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DijkstraRouterTest {

    private final DijkstraRouter router = new DijkstraRouter();

    private RoadEdge edge(Long id, String from, String to, double travelTime) {
        return new RoadEdge(id, "road-" + id, from, to, 1.0, 40.0, travelTime);
    }

    private Map<String, List<RoadEdge>> lineGraph() {
        return Map.of(
                "A", List.of(edge(1L, "A", "B", 10.0)),
                "B", List.of(
                        edge(1L, "B", "A", 10.0),
                        edge(2L, "B", "C", 20.0)),
                "C", List.of(edge(2L, "C", "B", 20.0))
        );
    }

    @Test
    void findShortestPath_directEdge_returnsIt() {
        Optional<List<RoadEdge>> path = router.findShortestPath(lineGraph(), "A", "C");

        assertTrue(path.isPresent());
        assertEquals(2, path.get().size());
        assertEquals(1L, path.get().get(0).getRoadSegmentId());
        assertEquals(2L, path.get().get(1).getRoadSegmentId());
    }

    @Test
    void findShortestPath_picksLowerTimePath() {
        // A -> B is 5 min; A -> C is 15; C -> B is 5. Shortest A->B should go A->C->B? No - A->B direct = 5 beats 20.
        Map<String, List<RoadEdge>> graph = Map.of(
                "A", List.of(edge(1L, "A", "B", 5.0), edge(3L, "A", "C", 15.0)),
                "B", List.of(edge(1L, "B", "A", 5.0), edge(2L, "B", "C", 5.0)),
                "C", List.of(edge(3L, "C", "A", 15.0), edge(2L, "C", "B", 5.0))
        );

        Optional<List<RoadEdge>> path = router.findShortestPath(graph, "A", "B");

        assertTrue(path.isPresent());
        assertEquals(1, path.get().size());
        assertEquals(1L, path.get().get(0).getRoadSegmentId());
    }

    @Test
    void findShortestPath_startEqualsEnd_returnsEmptyPath() {
        Optional<List<RoadEdge>> path = router.findShortestPath(lineGraph(), "A", "A");

        assertTrue(path.isPresent());
        assertTrue(path.get().isEmpty());
    }

    @Test
    void findShortestPath_disconnected_returnsEmpty() {
        Optional<List<RoadEdge>> path = router.findShortestPath(lineGraph(), "A", "Z");

        assertFalse(path.isPresent());
    }
}