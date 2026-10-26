package com.tripfactory.nomad.api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tripfactory.nomad.api.dto.RecommendedPlaceResponse;
import com.tripfactory.nomad.domain.enums.InterestType;
import com.tripfactory.nomad.service.RecommendationService;

import lombok.RequiredArgsConstructor;

/** Personalised place suggestions for the signed-in traveller. */
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/places")
    public ResponseEntity<List<RecommendedPlaceResponse>> places(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) InterestType interest,
            @RequestParam(required = false) Double maxKm) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(recommendationService.recommendFor(email, limit, interest, maxKm));
    }
}
