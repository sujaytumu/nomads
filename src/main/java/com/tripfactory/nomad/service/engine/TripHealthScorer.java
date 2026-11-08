package com.tripfactory.nomad.service.engine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Scores a plan from 0 to 100 on things travellers actually feel: is the pace relaxed, how much time goes on the
 * road, are the places well rated, is there variety, and will the weather behave. Each part has a score, a
 * one-line explanation and, when it is weak, a tip.
 */
public final class TripHealthScorer {

    public record Stop(int day, String category, double rating, double legKm, String name) {
    }

    public record Factor(String key, String label, int score, int weightPercent, String detail) {
    }

    public record Result(int score, String label, List<Factor> factors, List<String> tips) {
    }

    private TripHealthScorer() {
    }

    /** @param rainChances chance of rain (0-100) for each day of the trip, empty if the forecast is not available */
    public static Result score(int days, List<Stop> stops, List<Integer> rainChances) {
        List<Factor> factors = new ArrayList<>();
        List<String> tips = new ArrayList<>();
        if (stops.isEmpty()) {
            return new Result(0, "No stops yet", List.of(), List.of("Add a few places to see how your plan looks."));
        }
        int dayCount = Math.max(1, days);

        // Pace: 2 to 3 stops a day feels relaxed; a single day can take 4.
        Map<Integer, Integer> perDay = new TreeMap<>();
        for (Stop s : stops) {
            perDay.merge(s.day(), 1, Integer::sum);
        }
        int busiest = perDay.values().stream().max(Integer::compare).orElse(0);
        int comfortable = dayCount == 1 ? 4 : 3;
        int pace = clamp(100 - Math.max(0, busiest - comfortable) * 25);
        if (dayCount > 1 && perDay.size() > 0 && perDay.values().stream().anyMatch(n -> n == 1) && stops.size() > dayCount) {
            pace = Math.min(pace, 90);
        }
        factors.add(new Factor("pace", "Pace", pace, 30,
                busiest + (busiest == 1 ? " stop" : " stops") + " on your busiest day"));
        if (pace < 75) {
            tips.add("Your busiest day has " + busiest + " stops. Move one to a lighter day.");
        }

        // Road time: total distance per day. 25 km a day or less is easy; 120 km a day is exhausting.
        double totalKm = stops.stream().mapToDouble(Stop::legKm).sum();
        double perDayKm = totalKm / dayCount;
        int road = clamp((int) Math.round(100 - Math.max(0, perDayKm - 25) / 95.0 * 100));
        Stop longest = stops.stream().max((a, b) -> Double.compare(a.legKm(), b.legKm())).orElse(stops.get(0));
        factors.add(new Factor("road", "Time on the road", road, 25,
                "About " + Math.round(perDayKm) + " km a day"));
        if (longest.legKm() > 40) {
            tips.add("The drive to " + longest.name() + " is about " + Math.round(longest.legKm()) + " km. Start that day early.");
        } else if (road < 70) {
            tips.add("Lots of driving. Swap a far place for one closer to the others.");
        }

        // Quality: average rating, 4.5+ is excellent, 3.5 or less is weak.
        double avg = stops.stream().mapToDouble(Stop::rating).average().orElse(0);
        int quality = clamp((int) Math.round((avg - 3.5) / 1.0 * 100));
        factors.add(new Factor("quality", "Place quality", quality, 20, "Average rating " + String.format(java.util.Locale.ROOT, "%.1f", avg)));
        if (quality < 60) {
            tips.add("Some places are rated low. Swap one for a top-rated place in the same area.");
        }

        // Variety: different kinds of places, so days do not feel repeated.
        Set<String> kinds = new HashSet<>();
        stops.forEach(s -> kinds.add(s.category()));
        int target = Math.min(stops.size(), 4);
        int variety = clamp((int) Math.round(100.0 * Math.min(kinds.size(), target) / target));
        factors.add(new Factor("variety", "Variety", variety, 10, kinds.size() + (kinds.size() == 1 ? " kind" : " kinds") + " of places"));
        if (variety < 60) {
            tips.add("All similar places. Add something different, like food or nature.");
        }

        // Weather: the average chance of rain across the trip.
        if (rainChances != null && !rainChances.isEmpty()) {
            double rain = rainChances.stream().mapToInt(Integer::intValue).average().orElse(0);
            int weather = clamp((int) Math.round(100 - rain * 1.1));
            factors.add(new Factor("weather", "Weather", weather, 15, Math.round(rain) + "% average chance of rain"));
            if (weather < 60) {
                tips.add("Rain is likely. Keep an indoor option, like a museum or a food stop, in reserve.");
            }
        }

        int weightTotal = factors.stream().mapToInt(Factor::weightPercent).sum();
        double sum = factors.stream().mapToDouble(f -> f.score() * f.weightPercent()).sum();
        int overall = clamp((int) Math.round(sum / weightTotal));
        String label = overall >= 85 ? "Excellent" : overall >= 70 ? "Good" : overall >= 50 ? "Could be better" : "Needs changes";
        return new Result(overall, label, factors, tips);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(100, v));
    }
}
