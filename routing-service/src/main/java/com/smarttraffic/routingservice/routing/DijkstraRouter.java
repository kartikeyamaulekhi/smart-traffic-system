package com.smarttraffic.routingservice.routing;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DijkstraRouter {

    /**
     * Finds the lowest-total-travel-time path from startNode to endNode.
     * Returns Optional.empty() if no path exists between them at all.
     */
    public Optional<List<RoadEdge>> findShortestPath(
            Map<String, List<RoadEdge>> graph, String startNode, String endNode
    ) {
        Map<String, Double> bestTime = new HashMap<>();
        Map<String, RoadEdge> edgeUsedToReach = new HashMap<>();
        PriorityQueue<String> queue = new PriorityQueue<>(Comparator.comparingDouble(
                node -> bestTime.getOrDefault(node, Double.MAX_VALUE)
        ));

        bestTime.put(startNode, 0.0);
        queue.add(startNode);

        Set<String> visited = new HashSet<>();

        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (!visited.add(current)) {
                continue; // already finalized via a shorter path
            }
            if (current.equals(endNode)) {
                break;
            }

            for (RoadEdge edge : graph.getOrDefault(current, List.of())) {
                double candidateTime = bestTime.get(current) + edge.getTravelTimeMinutes();
                if (candidateTime < bestTime.getOrDefault(edge.getToNodeKey(), Double.MAX_VALUE)) {
                    bestTime.put(edge.getToNodeKey(), candidateTime);
                    edgeUsedToReach.put(edge.getToNodeKey(), edge);
                    queue.add(edge.getToNodeKey());
                }
            }
        }

        if (!bestTime.containsKey(endNode)) {
            return Optional.empty(); // endNode is unreachable from startNode
        }

        // Walk backwards from endNode to startNode using edgeUsedToReach, then reverse it
        LinkedList<RoadEdge> path = new LinkedList<>();
        String node = endNode;
        while (!node.equals(startNode)) {
            RoadEdge edge = edgeUsedToReach.get(node);
            path.addFirst(edge);
            node = edge.getFromNodeKey();
        }

        return Optional.of(path);
    }

}