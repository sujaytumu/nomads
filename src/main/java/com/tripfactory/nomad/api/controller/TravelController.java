package com.tripfactory.nomad.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.tripfactory.nomad.api.dto.PickupAdminUpdateRequest;
import com.tripfactory.nomad.api.dto.PickupConfirmRequest;
import com.tripfactory.nomad.api.dto.TravelAssignRequest;
import com.tripfactory.nomad.api.dto.TravelAssistanceResponse;
import com.tripfactory.nomad.service.TravelAssistanceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/travel")
@RequiredArgsConstructor
public class TravelController {

    private final TravelAssistanceService travelAssistanceService;

    @PostMapping("/assign")
    @PreAuthorize("@authz.canAccessTrip(#request.tripRequestId)")
    public ResponseEntity<TravelAssistanceResponse> assign(@Valid @RequestBody TravelAssignRequest request) {
        return new ResponseEntity<>(travelAssistanceService.assignAssistance(request.getTripRequestId()),
                HttpStatus.CREATED);
    }

    @GetMapping("/{tripId}")
    @PreAuthorize("@authz.canAccessTrip(#tripId)")
    public ResponseEntity<TravelAssistanceResponse> getAssistance(@PathVariable Long tripId) {
        return ResponseEntity.ok(travelAssistanceService.getAssistance(tripId));
    }

    @PutMapping("/{tripId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TravelAssistanceResponse> updatePickup(@PathVariable Long tripId,
            @RequestBody PickupAdminUpdateRequest request) {
        return ResponseEntity.ok(travelAssistanceService.updatePickupAdmin(tripId, request));
    }

    @PutMapping("/{tripId}/confirm")
    @PreAuthorize("@authz.canAccessTrip(#tripId)")
    public ResponseEntity<TravelAssistanceResponse> confirmPickup(@PathVariable Long tripId,
            @Valid @RequestBody PickupConfirmRequest request) {
        return ResponseEntity.ok(travelAssistanceService.confirmPickup(tripId, request));
    }
}