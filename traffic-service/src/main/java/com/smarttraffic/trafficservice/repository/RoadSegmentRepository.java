package com.smarttraffic.trafficservice.repository;

import com.smarttraffic.trafficservice.model.RoadSegment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoadSegmentRepository extends JpaRepository<RoadSegment, Long> {

    List<RoadSegment> findByCityIgnoreCase(String city);

}