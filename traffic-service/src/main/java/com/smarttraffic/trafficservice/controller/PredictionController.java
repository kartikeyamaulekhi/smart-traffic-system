package com.smarttraffic.trafficservice.controller;

import com.smarttraffic.trafficservice.dto.PredictionResponse;
import com.smarttraffic.trafficservice.service.PredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/api/predictions")
@RequiredArgsConstructor
public class PredictionController {

    private final PredictionService predictionService;

    /**
     * GET /api/predictions/1                              -> predicts for right now
     * GET /api/predictions/1?timestamp=2026-08-30T09:00:00 -> predicts for a specific time
     */
    @GetMapping("/{roadSegmentId}")
    public PredictionResponse predict(
            @PathVariable Long roadSegmentId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime timestamp
    ) {
        // The model's features are hour-of-day + day-of-week, not minute-level -
        // rounding to the hour here means requests within the same hour hit the
        // Redis cache instead of re-calling the ML service for an identical result.
        LocalDateTime effectiveTimestamp = (timestamp != null ? timestamp : LocalDateTime.now())
                .truncatedTo(ChronoUnit.HOURS);

        return predictionService.predict(roadSegmentId, effectiveTimestamp);
    }

}