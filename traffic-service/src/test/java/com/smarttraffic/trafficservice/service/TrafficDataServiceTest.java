package com.smarttraffic.trafficservice.service;

import com.smarttraffic.trafficservice.dto.TrafficDataRequest;
import com.smarttraffic.trafficservice.dto.TrafficDataResponse;
import com.smarttraffic.trafficservice.exception.ResourceNotFoundException;
import com.smarttraffic.trafficservice.model.CongestionLevel;
import com.smarttraffic.trafficservice.model.RoadSegment;
import com.smarttraffic.trafficservice.model.TrafficData;
import com.smarttraffic.trafficservice.model.TrafficSource;
import com.smarttraffic.trafficservice.repository.TrafficDataRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrafficDataServiceTest {

    @Mock
    private TrafficDataRepository trafficDataRepository;

    @Mock
    private RoadSegmentService roadSegmentService;

    @InjectMocks
    private TrafficDataService trafficDataService;

    private RoadSegment segment() {
        return RoadSegment.builder()
                .id(1L)
                .name("MG Road")
                .city("Indore")
                .startLat(22.7196)
                .startLng(75.8577)
                .endLat(22.7182)
                .endLng(75.8641)
                .build();
    }

    private TrafficData trafficData(Long id) {
        return TrafficData.builder()
                .id(id)
                .roadSegment(segment())
                .vehicleCount(120)
                .avgSpeedKmh(18.5)
                .congestionLevel(CongestionLevel.SEVERE)
                .source(TrafficSource.SENSOR)
                .build();
    }

    private TrafficDataRequest request() {
        TrafficDataRequest request = new TrafficDataRequest();
        request.setRoadSegmentId(1L);
        request.setVehicleCount(120);
        request.setAvgSpeedKmh(18.5);
        request.setCongestionLevel(CongestionLevel.SEVERE);
        request.setSource(TrafficSource.SENSOR);
        return request;
    }

    @Test
    void create_existingSegment_savesAndReturnsResponse() {
        doReturn(segment()).when(roadSegmentService).getEntityOrThrow(1L);
        when(trafficDataRepository.save(any(TrafficData.class))).thenAnswer(invocation -> {
            TrafficData td = invocation.getArgument(0);
            td.setId(10L);
            return td;
        });

        TrafficDataResponse response = trafficDataService.create(request());

        assertEquals(10L, response.getId());
        assertEquals(1L, response.getRoadSegmentId());
        assertEquals("MG Road", response.getRoadSegmentName());
        assertEquals(CongestionLevel.SEVERE, response.getCongestionLevel());
        assertEquals(120, response.getVehicleCount());
        assertEquals(18.5, response.getAvgSpeedKmh());
    }

    @Test
    void create_missingSegment_throws() {
        doThrow(new ResourceNotFoundException("RoadSegment not found with id: 99"))
                .when(roadSegmentService).getEntityOrThrow(99L);

        TrafficDataRequest badRequest = request();
        badRequest.setRoadSegmentId(99L);

        assertThrows(ResourceNotFoundException.class, () -> trafficDataService.create(badRequest));
        verify(trafficDataRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void findHistoryForSegment_returnsOrderedHistory() {
        doReturn(segment()).when(roadSegmentService).getEntityOrThrow(1L);
        when(trafficDataRepository.findByRoadSegmentIdOrderByRecordedAtDesc(1L))
                .thenReturn(List.of(trafficData(2L), trafficData(1L)));

        List<TrafficDataResponse> history = trafficDataService.findHistoryForSegment(1L);

        assertEquals(2, history.size());
        assertEquals(2L, history.get(0).getId());
    }

    @Test
    void findHistoryForSegment_missingSegment_throws() {
        doThrow(new ResourceNotFoundException("RoadSegment not found with id: 99"))
                .when(roadSegmentService).getEntityOrThrow(99L);

        assertThrows(ResourceNotFoundException.class, () -> trafficDataService.findHistoryForSegment(99L));
    }

    @Test
    void findLatestForSegment_returnsLatestReading() {
        doReturn(segment()).when(roadSegmentService).getEntityOrThrow(1L);
        when(trafficDataRepository.findTopByRoadSegmentIdOrderByRecordedAtDesc(1L))
                .thenReturn(Optional.of(trafficData(7L)));

        TrafficDataResponse latest = trafficDataService.findLatestForSegment(1L);

        assertEquals(7L, latest.getId());
    }

    @Test
    void findLatestForSegment_noReading_throws() {
        doReturn(segment()).when(roadSegmentService).getEntityOrThrow(1L);
        when(trafficDataRepository.findTopByRoadSegmentIdOrderByRecordedAtDesc(1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> trafficDataService.findLatestForSegment(1L));
    }

    @Test
    void findById_missingData_throws() {
        when(trafficDataRepository.findById(55L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> trafficDataService.findById(55L));
    }

    @Test
    void delete_existingData_deletes() {
        when(trafficDataRepository.findById(3L)).thenReturn(Optional.of(trafficData(3L)));

        trafficDataService.delete(3L);

        verify(trafficDataRepository).delete(any(TrafficData.class));
    }
}