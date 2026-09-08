package com.smarttraffic.trafficservice.controller;

import com.smarttraffic.trafficservice.dto.TrafficDataRequest;
import com.smarttraffic.trafficservice.dto.TrafficDataResponse;
import com.smarttraffic.trafficservice.service.TrafficDataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/traffic")
@RequiredArgsConstructor
public class TrafficDataController {

    private final TrafficDataService trafficDataService;

    @PostMapping
    public ResponseEntity<TrafficDataResponse> ingest(@Valid @RequestBody TrafficDataRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(trafficDataService.create(request));
    }

    @GetMapping("/{id}")
    public TrafficDataResponse findById(@PathVariable Long id) {
        return trafficDataService.findById(id);
    }

    @GetMapping("/segment/{roadSegmentId}")
    public List<TrafficDataResponse> historyForSegment(@PathVariable Long roadSegmentId) {
        return trafficDataService.findHistoryForSegment(roadSegmentId);
    }

    @GetMapping("/segment/{roadSegmentId}/latest")
    public TrafficDataResponse latestForSegment(@PathVariable Long roadSegmentId) {
        return trafficDataService.findLatestForSegment(roadSegmentId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        trafficDataService.delete(id);
        return ResponseEntity.noContent().build();
    }

}