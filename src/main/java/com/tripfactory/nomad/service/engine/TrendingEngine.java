package com.tripfactory.nomad.service.engine;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Which places are being booked right now. Each booked trip that stops at a place counts once for that place
 * (a trip visiting a place on two days is still one trip), and newer trips count more: a trip's weight halves
 * every {@link #HALF_LIFE_DAYS} days. Trips older than the window, or dated in the future, are ignored.
 * Pure: no database, so the ranking can be unit tested.
 */
public final class TrendingEngine {

    public static final int WINDOW_DAYS = 45;
    public static final double HALF_LIFE_DAYS = 14.0;

    private TrendingEngine() {
    }

    /** One stop of a booked trip. */
    public record Stop(long placeId, long tripId, LocalDate tripCreatedOn) {
    }

    public record Trending(long placeId, int trips, double score) {
    }

    public static List<Trending> rank(List<Stop> stops, LocalDate today, int limit) {
        Map<Long, Set<Long>> tripsByPlace = new HashMap<>();
        Map<Long, Double> scoreByPlace = new HashMap<>();
        for (Stop s : stops) {
            if (s.tripCreatedOn() == null || s.tripCreatedOn().isAfter(today)) continue;
            long age = ChronoUnit.DAYS.between(s.tripCreatedOn(), today);
            if (age >= WINDOW_DAYS) continue;
            Set<Long> seen = tripsByPlace.computeIfAbsent(s.placeId(), k -> new HashSet<>());
            if (!seen.add(s.tripId())) continue;          // this trip already counted for this place
            scoreByPlace.merge(s.placeId(), Math.pow(0.5, age / HALF_LIFE_DAYS), Double::sum);
        }
        List<Trending> out = new ArrayList<>();
        scoreByPlace.forEach((id, score) -> out.add(new Trending(id, tripsByPlace.get(id).size(), Math.round(score * 1000.0) / 1000.0)));
        out.sort(Comparator.comparingDouble(Trending::score).reversed().thenComparingLong(Trending::placeId));
        int n = Math.max(0, limit);
        return out.size() > n ? new ArrayList<>(out.subList(0, n)) : out;
    }
}
