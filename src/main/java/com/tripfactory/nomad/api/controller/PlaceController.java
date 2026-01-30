package com.tripfactory.nomad.api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tripfactory.nomad.api.dto.PlaceNearbyResponse;
import com.tripfactory.nomad.domain.enums.InterestType;
import com.tripfactory.nomad.service.PlaceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;

    @GetMapping("/nearby")
    public ResponseEntity<List<PlaceNearbyResponse>> getNearbyPlaces(
            @RequestParam(required = false) String city,
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(required = false) InterestType interest,
            @RequestParam(defaultValue = "15") double radiusKm,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(placeService.getNearbyPlaces(city, latitude, longitude, interest, radiusKm, limit));
    }
}