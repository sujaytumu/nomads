package com.tripfactory.nomad.api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.tripfactory.nomad.service.engine.Achievements;

public record TravelStatsResponse(
        int bookedTrips,
        int cancelledTrips,
        int pendingTrips,
        int placesVisited,
        int citiesVisited,
        double kmTravelled,
        BigDecimal totalSpent,
        int groupTrips,
        int reviewsWritten,
        String favouriteInterest,
        Map<String, Integer> tripsByInterest,
        Achievements.Level level,
        int badgesEarned,
        int badgesTotal,
        List<Achievements.Badge> badges,
        Achievements.Badge nextBadge) {
}
