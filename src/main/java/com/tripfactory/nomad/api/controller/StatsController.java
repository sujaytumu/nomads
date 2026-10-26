package com.tripfactory.nomad.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tripfactory.nomad.api.dto.TravelStatsResponse;
import com.tripfactory.nomad.service.StatsService;

import lombok.RequiredArgsConstructor;

/** The caller's own travel stats, level and badges. */
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/stats")
    public ResponseEntity<TravelStatsResponse> stats() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(statsService.statsFor(email));
    }
}
