package com.smarttraffic.trafficservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "traffic_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrafficData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "road_segment_id", nullable = false)
    private RoadSegment roadSegment;

    @Min(0)
    @Column(nullable = false)
    private Integer vehicleCount;

    @Min(0)
    @Column(nullable = false)
    private Double avgSpeedKmh;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CongestionLevel congestionLevel;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrafficSource source;

    @Builder.Default
    @Column(nullable = false)
    private Instant recordedAt = Instant.now();

}