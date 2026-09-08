package com.smarttraffic.trafficservice.service;

import com.smarttraffic.trafficservice.dto.TrafficDataRequest;
import com.smarttraffic.trafficservice.dto.TrafficDataResponse;
import com.smarttraffic.trafficservice.exception.ResourceNotFoundException;
import com.smarttraffic.trafficservice.model.RoadSegment;
import com.smarttraffic.trafficservice.model.TrafficData;
import com.smarttraffic.trafficservice.repository.TrafficDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TrafficDataService {

    private final TrafficDataRepository trafficDataRepository;
    private final RoadSegmentService roadSegmentService;

    public TrafficDataResponse create(TrafficDataRequest request) {
        RoadSegment segment = roadSegmentService.getEntityOrThrow(request.getRoadSegmentId());

        TrafficData data = TrafficData.builder()
                .roadSegment(segment)
                .vehicleCount(request.getVehicleCount())
                .avgSpeedKmh(request.getAvgSpeedKmh())
                .congestionLevel(request.getCongestionLevel())
                .source(request.getSource())
                .build();

        return toResponse(trafficDataRepository.save(data));
    }

    @Transactional(readOnly = true)
    public List<TrafficDataResponse> findHistoryForSegment(Long roadSegmentId) {
        // Confirms the segment exists before returning (possibly empty) history
        roadSegmentService.getEntityOrThrow(roadSegmentId);
        return trafficDataRepository.findByRoadSegmentIdOrderByRecordedAtDesc(roadSegmentId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TrafficDataResponse findLatestForSegment(Long roadSegmentId) {
        roadSegmentService.getEntityOrThrow(roadSegmentId);
        return trafficDataRepository.findTopByRoadSegmentIdOrderByRecordedAtDesc(roadSegmentId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No traffic data recorded yet for road segment id: " + roadSegmentId));
    }

    @Transactional(readOnly = true)
    public TrafficDataResponse findById(Long id) {
        return toResponse(getEntityOrThrow(id));
    }

    public void delete(Long id) {
        TrafficData data = getEntityOrThrow(id);
        trafficDataRepository.delete(data);
    }

    private TrafficData getEntityOrThrow(Long id) {
        return trafficDataRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TrafficData not found with id: " + id));
    }

    private TrafficDataResponse toResponse(TrafficData data) {
        return TrafficDataResponse.builder()
                .id(data.getId())
                .roadSegmentId(data.getRoadSegment().getId())
                .roadSegmentName(data.getRoadSegment().getName())
                .vehicleCount(data.getVehicleCount())
                .avgSpeedKmh(data.getAvgSpeedKmh())
                .congestionLevel(data.getCongestionLevel())
                .source(data.getSource())
                .recordedAt(data.getRecordedAt())
                .build();
    }

}