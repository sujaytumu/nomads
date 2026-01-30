package com.tripfactory.nomad.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tripfactory.nomad.api.dto.TripRouteResponse;
import com.tripfactory.nomad.service.TripRouteService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class TripRouteController {

    private final TripRouteService tripRouteService;

    @GetMapping("/{tripRequestId}")
    @PreAuthorize("@authz.canAccessTrip(#tripRequestId)")
    public ResponseEntity<TripRouteResponse> getRoute(@PathVariable Long tripRequestId,
            @RequestParam(defaultValue = "1") Integer dayNumber,
            @RequestParam(defaultValue = "driving") String mode) {
        return ResponseEntity.ok(tripRouteService.getOrCreateRoute(tripRequestId, dayNumber, mode));
    }
}