package com.tripfactory.nomad.service.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

/** Keeps simple live numbers about API requests: how many, how many failed, and how long they take (p50/p95). */
@Component
public class RequestMetrics {

    private static final int WINDOW = 1000;
    private static final int MAX_ROUTES = 200;

    public record Route(String route, long count, long errors, long p95Ms) {
    }

    public record Snapshot(long total, long clientErrors, long serverErrors, long p50Ms, long p95Ms, long windowSize, List<Route> slowest) {
    }

    private static final class RouteStats {
        final AtomicLong count = new AtomicLong();
        final AtomicLong errors = new AtomicLong();
        final long[] ring = new long[100];
        int pos;
        int filled;

        synchronized void add(long ms) {
            ring[pos] = ms;
            pos = (pos + 1) % ring.length;
            filled = Math.min(filled + 1, ring.length);
        }

        synchronized long p95() {
            return percentile(Arrays.copyOf(ring, filled), 95);
        }
    }

    private final AtomicLong total = new AtomicLong();
    private final AtomicLong client = new AtomicLong();
    private final AtomicLong server = new AtomicLong();
    private final long[] ring = new long[WINDOW];
    private int pos;
    private int filled;
    private final Map<String, RouteStats> routes = new ConcurrentHashMap<>();

    public void record(String route, int status, long ms) {
        total.incrementAndGet();
        if (status >= 500) {
            server.incrementAndGet();
        } else if (status >= 400) {
            client.incrementAndGet();
        }
        synchronized (this) {
            ring[pos] = ms;
            pos = (pos + 1) % WINDOW;
            filled = Math.min(filled + 1, WINDOW);
        }
        if (route != null && (routes.containsKey(route) || routes.size() < MAX_ROUTES)) {
            RouteStats rs = routes.computeIfAbsent(route, k -> new RouteStats());
            rs.count.incrementAndGet();
            if (status >= 500) {
                rs.errors.incrementAndGet();
            }
            rs.add(ms);
        }
    }

    public Snapshot snapshot() {
        long[] copy;
        synchronized (this) {
            copy = Arrays.copyOf(ring, filled);
        }
        List<Route> list = new ArrayList<>();
        routes.forEach((k, v) -> list.add(new Route(k, v.count.get(), v.errors.get(), v.p95())));
        list.sort(Comparator.comparingLong(Route::p95Ms).reversed());
        return new Snapshot(total.get(), client.get(), server.get(), percentile(copy, 50), percentile(copy, 95), copy.length,
                list.size() > 5 ? list.subList(0, 5) : list);
    }

    /** Nearest-rank percentile of the values; 0 when there are none. */
    public static long percentile(long[] values, int p) {
        if (values.length == 0) {
            return 0;
        }
        long[] sorted = values.clone();
        Arrays.sort(sorted);
        int rank = (int) Math.ceil(p / 100.0 * sorted.length);
        return sorted[Math.max(0, Math.min(sorted.length - 1, rank - 1))];
    }
}
