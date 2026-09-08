package com.smarttraffic.trafficservice.service;

import com.smarttraffic.trafficservice.dto.RoadSegmentResponse;
import com.smarttraffic.trafficservice.dto.TrafficDataRequest;
import com.smarttraffic.trafficservice.integration.tomtom.TomTomFlowResponse;
import com.smarttraffic.trafficservice.integration.tomtom.TomTomTrafficClient;
import com.smarttraffic.trafficservice.messaging.KafkaTopics;
import com.smarttraffic.trafficservice.model.CongestionLevel;
import com.smarttraffic.trafficservice.model.TrafficSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Periodically polls live traffic conditions for every known road segment
 * and PUBLISHES each reading onto Kafka. The routing-service consumes these
 * events (event-carried state transfer) so it can weight its route graph
 * without calling back into traffic-service's database every time.
 *
 * Publishing to Kafka (instead of writing to Postgres directly) decouples
 * "receiving data" from "consuming data" - the same event stream feeds any
 * consumer (routing, analytics, alerts) without changing this class.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrafficIngestionService {

    private final RoadSegmentService roadSegmentService;
    private final TomTomTrafficClient tomTomTrafficClient;
    private final KafkaTemplate<String, TrafficDataRequest> kafkaTemplate;

    @Scheduled(fixedDelayString = "${app.tomtom.poll-interval-ms}", initialDelay = 5000)
    public void pollAllSegments() {
        List<RoadSegmentResponse> segments = roadSegmentService.findAll();

        if (segments.isEmpty()) {
            log.debug("No road segments exist yet - nothing to poll.");
            return;
        }

        for (RoadSegmentResponse segment : segments) {
            pollSegment(segment);
        }
    }

    private void pollSegment(RoadSegmentResponse segment) {
        double midLat = (segment.getStartLat() + segment.getEndLat()) / 2.0;
        double midLng = (segment.getStartLng() + segment.getEndLng()) / 2.0;

        tomTomTrafficClient.fetchFlow(midLat, midLng).ifPresentOrElse(
                flow -> saveReading(segment, flow),
                () -> log.debug("No live traffic data returned for segment {} ({})", segment.getId(), segment.getName())
        );
    }

    private void saveReading(RoadSegmentResponse segment, TomTomFlowResponse.FlowSegmentData flow) {
        if (flow.getCurrentSpeed() == null || flow.getFreeFlowSpeed() == null || flow.getFreeFlowSpeed() == 0) {
            log.debug("Incomplete flow data for segment {} - skipping.", segment.getId());
            return;
        }

        double ratio = flow.getCurrentSpeed() / (double) flow.getFreeFlowSpeed();
        CongestionLevel level = classify(ratio, Boolean.TRUE.equals(flow.getRoadClosure()));

        TrafficDataRequest event = new TrafficDataRequest();
        event.setRoadSegmentId(segment.getId());
        // TomTom's flow endpoint doesn't report vehicle counts, only speed -
        // default to 0 for this source. congestionLevel/avgSpeedKmh carry the real signal.
        event.setVehicleCount(0);
        event.setAvgSpeedKmh(flow.getCurrentSpeed().doubleValue());
        event.setCongestionLevel(level);
        event.setSource(TrafficSource.API);

        // Key by road segment id so all readings for the same segment land on the
        // same partition and are processed in order relative to each other.
        kafkaTemplate.send(KafkaTopics.TRAFFIC_READINGS, segment.getId().toString(), event);

        log.info("Published traffic reading to Kafka for segment {} ({}): {} km/h ({}% of free-flow) -> {}",
                segment.getId(), segment.getName(), flow.getCurrentSpeed(),
                Math.round(ratio * 100), level);
    }

    private CongestionLevel classify(double currentToFreeFlowRatio, boolean roadClosure) {
        if (roadClosure) {
            return CongestionLevel.SEVERE;
        }
        if (currentToFreeFlowRatio >= 0.85) {
            return CongestionLevel.LOW;
        }
        if (currentToFreeFlowRatio >= 0.60) {
            return CongestionLevel.MEDIUM;
        }
        if (currentToFreeFlowRatio >= 0.35) {
            return CongestionLevel.HIGH;
        }
        return CongestionLevel.SEVERE;
    }

}