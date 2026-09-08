package com.smarttraffic.routingservice.controller;

import com.smarttraffic.routingservice.dto.RouteRequest;
import com.smarttraffic.routingservice.dto.RouteResponse;
import com.smarttraffic.routingservice.service.RouteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    @PostMapping
    public RouteResponse findRoute(@Valid @RequestBody RouteRequest request, HttpServletRequest httpRequest) {
        // The caller's bearer token is forwarded to traffic-service when
        // routing-service fetches the road network, so every hop in the chain
        // performs its own authentication/authorization.
        String authorization = httpRequest.getHeader("Authorization");
        return routeService.findRoute(request, authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring("Bearer ".length())
                : null);
    }

}