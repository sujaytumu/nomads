package com.tripfactory.nomad.service.engine;

import java.time.Duration;
import java.time.LocalDateTime;

/** Rules for shared positions: what counts as a real position, and when a shared position is too old to show. */
public final class LiveLocationRules {

    /** A position older than this is not shown, because the person is probably no longer there. */
    public static final Duration SHOW_FOR = Duration.ofMinutes(15);
    /** Rows older than this are deleted, so old positions do not sit in the database. */
    public static final Duration KEEP_FOR = Duration.ofHours(12);

    private LiveLocationRules() {
    }

    public static boolean validPosition(double lat, double lng) {
        return Double.isFinite(lat) && Double.isFinite(lng) && lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180
                && !(lat == 0 && lng == 0);
    }

    public static boolean fresh(LocalDateTime updatedAt, LocalDateTime now) {
        return updatedAt != null && !updatedAt.isBefore(now.minus(SHOW_FOR));
    }

    /** Straight-line distance in metres. */
    public static double metres(double lat1, double lng1, double lat2, double lng2) {
        double r = 6371000;
        double p1 = Math.toRadians(lat1), p2 = Math.toRadians(lat2);
        double dp = p2 - p1, dl = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dp / 2) * Math.sin(dp / 2) + Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2);
        return 2 * r * Math.asin(Math.sqrt(a));
    }
}
