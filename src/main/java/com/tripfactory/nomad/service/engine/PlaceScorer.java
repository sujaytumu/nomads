package com.tripfactory.nomad.service.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.tripfactory.nomad.domain.enums.InterestType;
import com.tripfactory.nomad.service.util.GeoUtils;

/**
 * Ranks places for one traveller. No Spring, no database: everything it needs is passed in, so the
 * maths can be tested on its own.
 *
 * <p>Each place gets a score between 0 and 1 from four signals:
 * <ul>
 *   <li><b>taste</b> (35%): the interest the user picked, plus what they actually booked before</li>
 *   <li><b>quality</b> (25%): the place's rating, where 3 stars or less counts as nothing and 5 as full marks</li>
 *   <li><b>closeness</b> (25%): fades smoothly with distance from the user (half-way at about 28 km)</li>
 *   <li><b>novelty</b> (15%): places the user has not been to yet</li>
 * </ul>
 * The final list is picked one at a time with a small penalty for repeating a category or city, so the
 * top results are a mix instead of ten of the same thing.
 */
public final class PlaceScorer {

    static final double W_TASTE = 0.35;
    static final double W_QUALITY = 0.25;
    static final double W_CLOSE = 0.25;
    static final double W_NOVELTY = 0.15;
    /** Distance scale of the closeness curve: score = e^(-km / 40). */
    static final double CLOSE_SCALE_KM = 40.0;
    static final double REPEAT_CATEGORY_PENALTY = 0.12;
    static final double REPEAT_CITY_PENALTY = 0.04;

    private PlaceScorer() {
    }

    public record Candidate(Long id, String name, String city, double latitude, double longitude,
                            InterestType category, double rating, String imageUrl, boolean visited) {
    }

    /** What we know about the traveller. {@code history} counts booked trips per interest. */
    public record Taste(InterestType declared, Map<InterestType, Integer> history, double homeLat, double homeLon) {
        public Taste {
            history = history == null ? new EnumMap<>(InterestType.class) : history;
        }
    }

    public record Scored(Candidate place, double score, double distanceKm, List<String> reasons) {
    }

    public static List<Scored> rank(List<Candidate> candidates, Taste taste, int limit) {
        if (candidates == null || candidates.isEmpty() || limit <= 0) {
            return List.of();
        }
        List<Scored> pool = new ArrayList<>(candidates.size());
        for (Candidate c : candidates) {
            pool.add(score(c, taste));
        }

        List<Scored> picked = new ArrayList<>();
        Map<InterestType, Integer> categoryPicked = new EnumMap<>(InterestType.class);
        Map<String, Integer> cityPicked = new java.util.HashMap<>();

        while (picked.size() < limit && !pool.isEmpty()) {
            Scored best = null;
            double bestAdjusted = -1;
            for (Scored s : pool) {
                double adjusted = s.score()
                        - REPEAT_CATEGORY_PENALTY * categoryPicked.getOrDefault(s.place().category(), 0)
                        - REPEAT_CITY_PENALTY * cityPicked.getOrDefault(cityKey(s.place().city()), 0);
                boolean better = best == null || adjusted > bestAdjusted + 1e-12
                        || (Math.abs(adjusted - bestAdjusted) <= 1e-12 && s.place().id() != null && best.place().id() != null
                            && s.place().id() < best.place().id());
                if (better) {
                    best = s;
                    bestAdjusted = adjusted;
                }
            }
            pool.remove(best);
            picked.add(best);
            categoryPicked.merge(best.place().category(), 1, Integer::sum);
            cityPicked.merge(cityKey(best.place().city()), 1, Integer::sum);
        }
        return picked;
    }

    static Scored score(Candidate c, Taste t) {
        double taste = tasteScore(c.category(), t);
        double quality = clamp((c.rating() - 3.0) / 2.0);
        double km = GeoUtils.haversineKm(t.homeLat(), t.homeLon(), c.latitude(), c.longitude());
        double close = Math.exp(-km / CLOSE_SCALE_KM);
        double novelty = c.visited() ? 0.0 : 1.0;

        double total = W_TASTE * taste + W_QUALITY * quality + W_CLOSE * close + W_NOVELTY * novelty;

        List<String> reasons = new ArrayList<>();
        if (t.declared() == c.category()) {
            reasons.add("Matches your favourite: " + pretty(c.category()));
        } else if (t.history().getOrDefault(c.category(), 0) > 0) {
            reasons.add("You have enjoyed " + pretty(c.category()) + " trips before");
        }
        if (c.rating() >= 4.5) {
            reasons.add(String.format("Top rated (%.1f stars)", c.rating()));
        } else if (c.rating() >= 4.0) {
            reasons.add(String.format("Highly rated (%.1f stars)", c.rating()));
        }
        if (km <= 15) {
            reasons.add(km < 1 ? "Right next to you" : String.format("Only %.0f km away", km));
        }
        if (!c.visited()) {
            reasons.add("New to you");
        }
        return new Scored(c, round3(total), round1(km), List.copyOf(reasons));
    }

    /** 0..1. The picked interest is worth 0.6, past bookings fill the rest (most-booked interest = 0.4). */
    static double tasteScore(InterestType category, Taste t) {
        double declared = t.declared() == category ? 0.6 : 0.0;
        int max = t.history().values().stream().mapToInt(Integer::intValue).max().orElse(0);
        double history = max <= 0 ? 0.0 : 0.4 * t.history().getOrDefault(category, 0) / max;
        return clamp(declared + history);
    }

    private static String cityKey(String city) {
        return city == null ? "" : city.trim().toLowerCase();
    }

    private static String pretty(InterestType type) {
        String s = type.name().toLowerCase();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private static double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    /** Highest score first; ties by id. Handy for callers and tests that want a plain sorted list. */
    public static final Comparator<Scored> BY_SCORE = Comparator
            .comparingDouble(Scored::score).reversed()
            .thenComparing(s -> s.place().id(), Comparator.nullsLast(Comparator.naturalOrder()));
}
