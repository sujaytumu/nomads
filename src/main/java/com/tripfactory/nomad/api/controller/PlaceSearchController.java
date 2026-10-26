package com.tripfactory.nomad.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tripfactory.nomad.api.dto.PlaceSearchResponse;
import com.tripfactory.nomad.domain.enums.InterestType;
import com.tripfactory.nomad.service.PlaceSearchService;

import lombok.RequiredArgsConstructor;

/** Public place search with filters, sorting, paging and facet counts. */
@RestController
@RequestMapping("/api/places/search")
@RequiredArgsConstructor
public class PlaceSearchController {

    private final PlaceSearchService placeSearchService;

    @GetMapping
    public ResponseEntity<PlaceSearchResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) InterestType category,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(placeSearchService.search(q, city, category, minRating, sort, lat, lon, page, size));
    }
}
