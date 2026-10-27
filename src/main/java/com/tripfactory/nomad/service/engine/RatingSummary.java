package com.tripfactory.nomad.service.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a pile of 1-5 star ratings into what the app shows: the average, a bar for each star count, and a
 * "blended" score that starts from the place's own rating and moves towards what travellers say as more of them
 * rate it. Without the blending, one single 5-star rating would put a place above one with fifty 4.8s.
 */
public final class RatingSummary {

    /** How many traveller ratings it takes to count as much as the place's starting rating. */
    static final int PRIOR_WEIGHT = 5;

    private RatingSummary() {
    }

    /** {@code distribution.get(0)} is the number of 1-star ratings, {@code get(4)} the number of 5-star ones. */
    public record Summary(int count, double average, double blended, List<Integer> distribution) {
    }

    public static Summary compute(List<Integer> stars, double baseRating) {
        int[] counts = new int[5];
        int n = 0;
        long sum = 0;
        if (stars != null) {
            for (Integer s : stars) {
                if (s == null || s < 1 || s > 5) continue;
                counts[s - 1]++;
                n++;
                sum += s;
            }
        }
        List<Integer> dist = new ArrayList<>(5);
        for (int c : counts) dist.add(c);

        double average = n == 0 ? 0.0 : round1((double) sum / n);
        double base = Math.max(0.0, Math.min(5.0, baseRating));
        double blended = round1((PRIOR_WEIGHT * base + sum) / (PRIOR_WEIGHT + n));
        return new Summary(n, average, blended, dist);
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
