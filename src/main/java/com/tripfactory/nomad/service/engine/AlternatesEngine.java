package com.tripfactory.nomad.service.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Plan B for every stop of a trip. When rain is likely on an outdoor stop's day, the options are indoor places
 * nearby; otherwise they are similar places nearby (for a stop that is closed or too crowded). Pure: no database.
 *
 * <p>Option score = 50% rating + 40% closeness (full marks at the stop, none at 15 km) + 10% same kind as the stop.
 * Places already in the trip are never offered.
 */
public final class AlternatesEngine {

    public static final int RAIN_THRESHOLD = 50;
    public static final double MAX_KM = 15.0;
    public static final int OPTIONS_PER_STOP = 3;

    private static final Set<String> OUTDOOR = Set.of("NATURE", "ADVENTURE");

    private AlternatesEngine() {
    }

    public record Spot(long placeId, String name, String category, double rating, double lat, double lon) {
    }

    public record Stop(int day, Spot spot) {
    }

    public record Option(long placeId, String name, String category, double rating, double distanceKm, String why) {
    }

    public record StopAlternates(int day, long placeId, String name, String category, Integer rainChance,
                                 boolean needsBackup, String headline, List<Option> options) {
    }

    /** {@code forecastKnown} is false when no forecast covers the trip days, so the screen can say so instead of claiming dry weather. */
    public record Result(List<Integer> rainyDays, boolean forecastKnown, List<StopAlternates> stops) {
    }

    public static boolean isOutdoor(String category) {
        return category != null && OUTDOOR.contains(category);
    }

    public static Result compute(List<Stop> stops, List<Spot> candidates, Map<Integer, Integer> rainChanceByDay) {
        Set<Long> inTrip = new HashSet<>();
        for (Stop s : stops) inTrip.add(s.spot().placeId());

        List<Integer> rainy = new ArrayList<>();
        rainChanceByDay.forEach((d, c) -> {
            if (c != null && c >= RAIN_THRESHOLD) rainy.add(d);
        });
        rainy.sort(Integer::compare);

        List<StopAlternates> out = new ArrayList<>();
        for (Stop s : stops) {
            Integer chance = rainChanceByDay.get(s.day());
            boolean rainRisk = chance != null && chance >= RAIN_THRESHOLD;
            boolean needsBackup = rainRisk && isOutdoor(s.spot().category());

            List<Option> options = new ArrayList<>();
            Map<Long, Double> scores = new java.util.HashMap<>();
            for (Spot c : candidates) {
                if (inTrip.contains(c.placeId())) continue;
                if (needsBackup && isOutdoor(c.category())) continue;           // need a roof
                if (!needsBackup && !sameKind(s.spot(), c)) continue;           // a like-for-like swap
                double km = haversineKm(s.spot().lat(), s.spot().lon(), c.lat(), c.lon());
                if (km > MAX_KM) continue;
                double score = 0.5 * (clamp(c.rating(), 0, 5) / 5.0) + 0.4 * (1 - km / MAX_KM) + (sameKind(s.spot(), c) ? 0.1 : 0.0);
                options.add(new Option(c.placeId(), c.name(), c.category(), c.rating(), Math.round(km * 10.0) / 10.0, why(needsBackup, c, km)));
                scores.put(c.placeId(), score);
            }
            options.sort(Comparator.<Option>comparingDouble(o -> -scores.getOrDefault(o.placeId(), 0.0)).thenComparingLong(Option::placeId));
            if (options.size() > OPTIONS_PER_STOP) options = new ArrayList<>(options.subList(0, OPTIONS_PER_STOP));

            String headline = needsBackup ? "Rain likely (" + chance + "%): have an indoor backup"
                    : rainRisk ? "Rain likely (" + chance + "%), but this stop is indoors"
                    : "If it's closed or crowded";
            out.add(new StopAlternates(s.day(), s.spot().placeId(), s.spot().name(), s.spot().category(),
                    chance, needsBackup, headline, options));
        }
        return new Result(rainy, !rainChanceByDay.isEmpty(), out);
    }

    private static boolean sameKind(Spot a, Spot b) {
        return a.category() != null && a.category().equals(b.category());
    }

    private static String why(boolean indoorBackup, Spot c, double km) {
        String d = km < 1 ? "under 1 km away" : String.format(java.util.Locale.ROOT, "%.1f km away", km);
        return (indoorBackup ? "Indoors, " : "Similar, ") + d + (c.rating() >= 4.5 ? ", top rated" : "");
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * r * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }
}
