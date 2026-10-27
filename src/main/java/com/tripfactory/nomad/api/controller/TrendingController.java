package com.tripfactory.nomad.api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tripfactory.nomad.api.dto.TrendingPlaceResponse;
import com.tripfactory.nomad.service.TrendingService;

import lombok.RequiredArgsConstructor;

/** Public: places that recent bookings are heading to. Carries no user data. */
@RestController
@RequestMapping("/api/places/trending")
@RequiredArgsConstructor
public class TrendingController {

    private final TrendingService trendingService;

    @GetMapping
    public ResponseEntity<List<TrendingPlaceResponse>> trending(@RequestParam(defaultValue = "8") int limit) {
        return ResponseEntity.ok(trendingService.trending(limit));
    }
}
