package com.tripfactory.nomad.api.controller;

import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tripfactory.nomad.repository.PaymentEventRepository;
import com.tripfactory.nomad.service.PushService;
import com.tripfactory.nomad.service.util.ReadCache;
import com.tripfactory.nomad.service.util.RequestMetrics;

import lombok.RequiredArgsConstructor;

/** How the server itself is doing: speed, errors, cache, queues, database and memory. Admins only. */
@RestController
@RequestMapping("/api/admin/system")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSystemController {

    private final RequestMetrics metrics;
    private final ReadCache cache;
    private final PushService pushService;
    private final PaymentEventRepository paymentEvents;
    private final DataSource dataSource;

    @GetMapping
    public ResponseEntity<Map<String, Object>> system() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("checkedAt", LocalDateTime.now().toString());
        out.put("uptimeSeconds", ManagementFactory.getRuntimeMXBean().getUptime() / 1000);

        Runtime rt = Runtime.getRuntime();
        long used = rt.totalMemory() - rt.freeMemory();
        out.put("memoryUsedMb", used / (1024 * 1024));
        out.put("memoryMaxMb", rt.maxMemory() / (1024 * 1024));

        out.put("requests", metrics.snapshot());

        long hits = cache.hits();
        long misses = cache.misses();
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("hits", hits);
        c.put("misses", misses);
        c.put("hitRatePercent", hits + misses == 0 ? 0 : Math.round(100.0 * hits / (hits + misses)));
        c.put("entries", cache.size());
        out.put("cache", c);

        Map<String, Object> push = new LinkedHashMap<>();
        push.put("enabled", pushService.isEnabled());
        push.put("pending", safe(pushService::pending));
        push.put("failed", safe(pushService::failed));
        out.put("push", push);

        out.put("paymentEvents", safe(paymentEvents::count));
        out.put("database", database());
        return ResponseEntity.ok(out);
    }

    private Map<String, Object> database() {
        Map<String, Object> db = new LinkedHashMap<>();
        long start = System.nanoTime();
        try (var conn = dataSource.getConnection()) {
            boolean ok = conn.isValid(2);
            db.put("ok", ok);
        } catch (Exception ex) {
            db.put("ok", false);
        }
        db.put("pingMs", (System.nanoTime() - start) / 1_000_000);
        return db;
    }

    private static long safe(java.util.function.LongSupplier s) {
        try {
            return s.getAsLong();
        } catch (Exception ex) {
            return -1;
        }
    }
}
