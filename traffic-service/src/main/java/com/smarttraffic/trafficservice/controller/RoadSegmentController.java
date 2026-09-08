package com.smarttraffic.trafficservice.controller;

import com.smarttraffic.trafficservice.dto.RoadSegmentRequest;
import com.smarttraffic.trafficservice.dto.RoadSegmentResponse;
import com.smarttraffic.trafficservice.service.RoadSegmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/road-segments")
@RequiredArgsConstructor
public class RoadSegmentController {

    private final RoadSegmentService roadSegmentService;

    @PostMapping
    public ResponseEntity<RoadSegmentResponse> create(@Valid @RequestBody RoadSegmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roadSegmentService.create(request));
    }

    @GetMapping
    public List<RoadSegmentResponse> findAll() {
        return roadSegmentService.findAll();
    }

    @GetMapping("/{id}")
    public RoadSegmentResponse findById(@PathVariable Long id) {
        return roadSegmentService.findById(id);
    }

    @PutMapping("/{id}")
    public RoadSegmentResponse update(@PathVariable Long id, @Valid @RequestBody RoadSegmentRequest request) {
        return roadSegmentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roadSegmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

}