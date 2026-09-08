package com.smarttraffic.trafficservice.service;

import com.smarttraffic.trafficservice.dto.RoadSegmentRequest;
import com.smarttraffic.trafficservice.dto.RoadSegmentResponse;
import com.smarttraffic.trafficservice.exception.ResourceNotFoundException;
import com.smarttraffic.trafficservice.model.RoadSegment;
import com.smarttraffic.trafficservice.repository.RoadSegmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoadSegmentServiceTest {

    @Mock
    private RoadSegmentRepository roadSegmentRepository;

    @InjectMocks
    private RoadSegmentService roadSegmentService;

    private RoadSegmentRequest request() {
        RoadSegmentRequest request = new RoadSegmentRequest();
        request.setName("MG Road");
        request.setCity("Indore");
        request.setStartLat(22.7196);
        request.setStartLng(75.8577);
        request.setEndLat(22.7182);
        request.setEndLng(75.8641);
        return request;
    }

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

    @Test
    void create_savesAndReturnsResponse() {
        when(roadSegmentRepository.save(any(RoadSegment.class))).thenAnswer(invocation -> {
            RoadSegment rs = invocation.getArgument(0);
            rs.setId(1L);
            return rs;
        });

        RoadSegmentResponse response = roadSegmentService.create(request());

        assertEquals(1L, response.getId());
        assertEquals("MG Road", response.getName());
        assertEquals("Indore", response.getCity());
        assertNotNull(response.getCreatedAt());
    }

    @Test
    void findAll_returnsMappedSegments() {
        when(roadSegmentRepository.findAll()).thenReturn(List.of(segment()));

        List<RoadSegmentResponse> response = roadSegmentService.findAll();

        assertEquals(1, response.size());
        assertEquals("MG Road", response.get(0).getName());
    }

    @Test
    void findById_existingSegment_returnsResponse() {
        when(roadSegmentRepository.findById(1L)).thenReturn(Optional.of(segment()));

        assertEquals("MG Road", roadSegmentService.findById(1L).getName());
    }

    @Test
    void findById_missingSegment_throws() {
        when(roadSegmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> roadSegmentService.findById(99L));
    }

    @Test
    void update_existingSegment_updatesFields() {
        when(roadSegmentRepository.findById(1L)).thenReturn(Optional.of(segment()));
        when(roadSegmentRepository.save(any(RoadSegment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoadSegmentRequest update = request();
        update.setName("MG Road Extension");

        RoadSegmentResponse response = roadSegmentService.update(1L, update);

        assertEquals("MG Road Extension", response.getName());
        verify(roadSegmentRepository).save(any(RoadSegment.class));
    }

    @Test
    void delete_existingSegment_deletes() {
        when(roadSegmentRepository.findById(1L)).thenReturn(Optional.of(segment()));

        roadSegmentService.delete(1L);

        verify(roadSegmentRepository).delete(any(RoadSegment.class));
    }
}