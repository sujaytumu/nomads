package com.tripfactory.nomad.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tripfactory.nomad.api.dto.PlaceResponse;
import com.tripfactory.nomad.service.PlaceSearchService;

import lombok.RequiredArgsConstructor;

/** One place by id (the app's place page and the live trip card use this). Public, like the rest of /api/places. */
@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceDetailController {

    private final PlaceSearchService placeSearchService;

    @GetMapping("/{id}")
    public ResponseEntity<PlaceResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(placeSearchService.getPlace(id));
    }
}
