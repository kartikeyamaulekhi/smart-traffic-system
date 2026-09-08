package com.smarttraffic.trafficservice.repository;

import com.smarttraffic.trafficservice.model.TrafficData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrafficDataRepository extends JpaRepository<TrafficData, Long> {

    List<TrafficData> findByRoadSegmentIdOrderByRecordedAtDesc(Long roadSegmentId);

    Optional<TrafficData> findTopByRoadSegmentIdOrderByRecordedAtDesc(Long roadSegmentId);

}