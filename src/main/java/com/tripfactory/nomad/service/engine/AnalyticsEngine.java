package com.tripfactory.nomad.service.engine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.tripfactory.nomad.domain.enums.InterestType;
import com.tripfactory.nomad.domain.enums.PaymentStatus;
import com.tripfactory.nomad.domain.enums.TripStatus;

/**
 * The numbers behind the admin dashboard. Pure: the caller passes plain rows and "today", nothing here touches
 * a database, so every figure can be checked in a unit test.
 *
 * <p>Definitions (kept simple on purpose, and shown on the dashboard):
 * <ul>
 *   <li><b>Booked trip</b>: a trip whose status is CONFIRMED.</li>
 *   <li><b>Revenue</b>: payments that were CAPTURED. <b>Refunds</b>: payments that were REFUNDED. Net = revenue + refunds
 *       is NOT used; a refunded payment is no longer CAPTURED, so net revenue is simply the CAPTURED total.</li>
 *   <li><b>Booking rate</b>: booked trips divided by all trips (0 when there are none).</li>
 *   <li><b>Funnel</b>: trips planned, then trips with a payment attempt, then booked trips.</li>
 * </ul>
 */
public final class AnalyticsEngine {

    public static final int MAX_DAYS = 90;

    private AnalyticsEngine() {
    }

    public record UserRow(LocalDate createdOn) {
    }

    public record TripRow(long id, String city, TripStatus status, InterestType interest, LocalDate createdOn) {
    }

    public record PaymentRow(long tripId, PaymentStatus status, BigDecimal amount, LocalDate createdOn) {
    }

    public record Totals(int users, int trips, int bookedTrips, int cancelledTrips, double bookingRate,
                         double netRevenue, double refunded, double averageBookingValue) {
    }

    public record FunnelStep(String label, int count, double percentOfFirst) {
    }

    public record Ranked(String label, int count) {
    }

    public record DayPoint(String date, int signups, int tripsPlanned, int bookings, double revenue) {
    }

    public record Snapshot(String from, String to, int days, Totals totals, List<FunnelStep> funnel,
                           Map<String, Integer> tripsByStatus, List<Ranked> topCities, List<Ranked> interestMix,
                           List<DayPoint> daily) {
    }

    public static Snapshot compute(List<UserRow> users, List<TripRow> trips, List<PaymentRow> payments,
                                   LocalDate today, int days) {
        int window = Math.max(1, Math.min(days <= 0 ? 30 : days, MAX_DAYS));
        LocalDate from = today.minusDays(window - 1L);

        // ---- totals
        int booked = 0, cancelled = 0;
        Map<String, Integer> byStatus = new LinkedHashMap<>();
        for (TripStatus s : TripStatus.values()) byStatus.put(s.name(), 0);
        Map<String, Integer> cityCounts = new HashMap<>();
        Map<String, String> cityLabel = new HashMap<>();
        Map<InterestType, Integer> interestCounts = new EnumMap<>(InterestType.class);
        for (TripRow t : trips) {
            byStatus.merge(t.status().name(), 1, Integer::sum);
            if (t.status() == TripStatus.CONFIRMED) {
                booked++;
                if (t.city() != null && !t.city().isBlank()) {
                    String key = t.city().trim().toLowerCase(Locale.ROOT);
                    cityCounts.merge(key, 1, Integer::sum);
                    cityLabel.putIfAbsent(key, t.city().trim());
                }
                if (t.interest() != null) interestCounts.merge(t.interest(), 1, Integer::sum);
            } else if (t.status() == TripStatus.CANCELLED) {
                cancelled++;
            }
        }

        double revenue = 0, refunded = 0;
        int captured = 0;
        Set<Long> tripsWithPayment = new HashSet<>();
        for (PaymentRow p : payments) {
            double amount = p.amount() == null ? 0 : p.amount().doubleValue();
            tripsWithPayment.add(p.tripId());
            if (p.status() == PaymentStatus.CAPTURED) {
                revenue += amount;
                captured++;
            } else if (p.status() == PaymentStatus.REFUNDED) {
                refunded += amount;
            }
        }

        int totalTrips = trips.size();
        Totals totals = new Totals(users.size(), totalTrips, booked, cancelled,
                totalTrips == 0 ? 0.0 : round3((double) booked / totalTrips),
                round2(revenue), round2(refunded), captured == 0 ? 0.0 : round2(revenue / captured));

        // ---- funnel
        int attempted = tripsWithPayment.size();
        List<FunnelStep> funnel = List.of(
                step("Trips planned", totalTrips, totalTrips),
                step("Payment started", attempted, totalTrips),
                step("Booked", booked, totalTrips));

        // ---- day-by-day for the window
        Map<LocalDate, int[]> counts = new HashMap<>();   // [signups, planned, bookings]
        Map<LocalDate, Double> money = new HashMap<>();
        for (UserRow u : users) bump(counts, u.createdOn(), from, today, 0);
        for (TripRow t : trips) bump(counts, t.createdOn(), from, today, 1);
        for (PaymentRow p : payments) {
            if (p.status() != PaymentStatus.CAPTURED || p.createdOn() == null) continue;
            if (p.createdOn().isBefore(from) || p.createdOn().isAfter(today)) continue;
            bump(counts, p.createdOn(), from, today, 2);
            money.merge(p.createdOn(), p.amount() == null ? 0.0 : p.amount().doubleValue(), Double::sum);
        }
        List<DayPoint> daily = new ArrayList<>(window);
        for (int i = 0; i < window; i++) {
            LocalDate d = from.plusDays(i);
            int[] c = counts.getOrDefault(d, new int[3]);
            daily.add(new DayPoint(d.toString(), c[0], c[1], c[2], round2(money.getOrDefault(d, 0.0))));
        }

        return new Snapshot(from.toString(), today.toString(), window, totals, funnel, byStatus,
                top(cityCounts, cityLabel, 5), interestMix(interestCounts), daily);
    }

    private static FunnelStep step(String label, int count, int first) {
        return new FunnelStep(label, count, first == 0 ? 0.0 : round1(100.0 * count / first));
    }

    private static void bump(Map<LocalDate, int[]> counts, LocalDate day, LocalDate from, LocalDate to, int slot) {
        if (day == null || day.isBefore(from) || day.isAfter(to)) return;
        counts.computeIfAbsent(day, k -> new int[3])[slot]++;
    }

    /** Biggest first; ties alphabetical so the order never flickers between loads. */
    private static List<Ranked> top(Map<String, Integer> counts, Map<String, String> labels, int limit) {
        List<Ranked> out = new ArrayList<>();
        counts.forEach((k, v) -> out.add(new Ranked(labels.getOrDefault(k, k), v)));
        out.sort(Comparator.comparingInt(Ranked::count).reversed().thenComparing(Ranked::label, String.CASE_INSENSITIVE_ORDER));
        return out.size() > limit ? new ArrayList<>(out.subList(0, limit)) : out;
    }

    private static List<Ranked> interestMix(Map<InterestType, Integer> counts) {
        List<Ranked> out = new ArrayList<>();
        counts.forEach((k, v) -> out.add(new Ranked(k.name(), v)));
        out.sort(Comparator.comparingInt(Ranked::count).reversed().thenComparing(Ranked::label));
        return out;
    }

    /** Whole days between two dates, for callers that want "since" figures. */
    public static long daysBetween(LocalDate a, LocalDate b) {
        return ChronoUnit.DAYS.between(a, b);
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
