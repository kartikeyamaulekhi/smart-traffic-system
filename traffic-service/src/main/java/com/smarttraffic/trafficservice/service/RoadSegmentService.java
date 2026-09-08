package com.smarttraffic.trafficservice.service;

import com.smarttraffic.trafficservice.dto.RoadSegmentRequest;
import com.smarttraffic.trafficservice.dto.RoadSegmentResponse;
import com.smarttraffic.trafficservice.exception.ResourceNotFoundException;
import com.smarttraffic.trafficservice.model.RoadSegment;
import com.smarttraffic.trafficservice.repository.RoadSegmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RoadSegmentService {

    private final RoadSegmentRepository roadSegmentRepository;

    public RoadSegmentResponse create(RoadSegmentRequest request) {
        RoadSegment segment = RoadSegment.builder()
                .name(request.getName())
                .city(request.getCity())
                .startLat(request.getStartLat())
                .startLng(request.getStartLng())
                .endLat(request.getEndLat())
                .endLng(request.getEndLng())
                .build();

        return toResponse(roadSegmentRepository.save(segment));
    }

    @Transactional(readOnly = true)
    public List<RoadSegmentResponse> findAll() {
        return roadSegmentRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoadSegmentResponse findById(Long id) {
        return toResponse(getEntityOrThrow(id));
    }

    public RoadSegmentResponse update(Long id, RoadSegmentRequest request) {
        RoadSegment segment = getEntityOrThrow(id);

        segment.setName(request.getName());
        segment.setCity(request.getCity());
        segment.setStartLat(request.getStartLat());
        segment.setStartLng(request.getStartLng());
        segment.setEndLat(request.getEndLat());
        segment.setEndLng(request.getEndLng());

        return toResponse(roadSegmentRepository.save(segment));
    }

    public void delete(Long id) {
        RoadSegment segment = getEntityOrThrow(id);
        roadSegmentRepository.delete(segment);
    }

    RoadSegment getEntityOrThrow(Long id) {
        return roadSegmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RoadSegment not found with id: " + id));
    }

    private RoadSegmentResponse toResponse(RoadSegment segment) {
        return RoadSegmentResponse.builder()
                .id(segment.getId())
                .name(segment.getName())
                .city(segment.getCity())
                .startLat(segment.getStartLat())
                .startLng(segment.getStartLng())
                .endLat(segment.getEndLat())
                .endLng(segment.getEndLng())
                .createdAt(segment.getCreatedAt())
                .build();
    }

}