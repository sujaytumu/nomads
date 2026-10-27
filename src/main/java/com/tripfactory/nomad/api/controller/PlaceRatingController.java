package com.tripfactory.nomad.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tripfactory.nomad.api.dto.MyPlaceRatingResponse;
import com.tripfactory.nomad.api.dto.PlaceRatingRequest;
import com.tripfactory.nomad.api.dto.PlaceRatingsResponse;
import com.tripfactory.nomad.service.PlaceRatingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Traveller ratings of places. Reading the summary is public (like the rest of /api/places); everything that is
 * about the caller needs a login, which is enforced here because the /api/places/** path is open in the security config.
 */
@RestController
@RequestMapping("/api/places/{placeId}/ratings")
@RequiredArgsConstructor
public class PlaceRatingController {

    private final PlaceRatingService placeRatingService;

    @GetMapping
    public ResponseEntity<PlaceRatingsResponse> overview(@PathVariable Long placeId) {
        return ResponseEntity.ok(placeRatingService.overview(placeId));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MyPlaceRatingResponse> mine(@PathVariable Long placeId) {
        return ResponseEntity.ok(placeRatingService.mine(currentEmail(), placeId));
    }

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MyPlaceRatingResponse> rate(@PathVariable Long placeId, @Valid @RequestBody PlaceRatingRequest request) {
        return ResponseEntity.ok(placeRatingService.rate(currentEmail(), placeId, request.stars(), request.comment()));
    }

    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> remove(@PathVariable Long placeId) {
        placeRatingService.remove(currentEmail(), placeId);
        return ResponseEntity.noContent().build();
    }

    private String currentEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
