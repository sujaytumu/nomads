package com.tripfactory.nomad.api.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tripfactory.nomad.domain.entity.TripRequest;
import com.tripfactory.nomad.repository.TripPlanRepository;
import com.tripfactory.nomad.repository.TripRequestRepository;
import com.tripfactory.nomad.service.exception.ResourceNotFoundException;
import com.tripfactory.nomad.service.impl.ItineraryPdfGenerator;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class ItineraryController {

    private final TripRequestRepository tripRequestRepository;
    private final TripPlanRepository tripPlanRepository;
    private final ItineraryPdfGenerator pdfGenerator;

    @GetMapping("/{id}/itinerary.pdf")
    @PreAuthorize("@authz.canAccessTrip(#id)")
    public ResponseEntity<byte[]> itinerary(@PathVariable Long id) {
        TripRequest trip = tripRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        byte[] pdf = pdfGenerator.generate(trip,
                tripPlanRepository.findByTripRequestIdOrderByDayNumberAscStartTimeAsc(id));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"itinerary-" + id + ".pdf\"")
                .body(pdf);
    }
}
