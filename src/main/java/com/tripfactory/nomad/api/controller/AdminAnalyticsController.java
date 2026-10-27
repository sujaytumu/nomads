package com.tripfactory.nomad.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tripfactory.nomad.service.AdminAnalyticsService;
import com.tripfactory.nomad.service.engine.AnalyticsEngine;

import lombok.RequiredArgsConstructor;

/** Business numbers for admins: users, trips, bookings, revenue, funnel, top cities. */
@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsController {

    private final AdminAnalyticsService analyticsService;

    @GetMapping
    public ResponseEntity<AnalyticsEngine.Snapshot> analytics(@RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(analyticsService.snapshot(days));
    }
}
