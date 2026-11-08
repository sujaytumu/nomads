package com.tripfactory.nomad.service.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

/**
 * A small in-memory cache for data everyone sees the same (package lists, trending places).
 * Entries expire after a time-to-live and the cache never holds more than {@code maxEntries}, so memory stays bounded.
 * Hits and misses are counted so the admin health page can show how well it works.
 */
@Component
public class ReadCache {

    private record Entry(Object value, long expiresAtMs) {
    }

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();
    private final LongSupplier clock;
    private final int maxEntries;

    public ReadCache() {
        this(System::currentTimeMillis, 500);
    }

    ReadCache(LongSupplier clock, int maxEntries) {
        this.clock = clock;
        this.maxEntries = maxEntries;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, long ttlMs, Supplier<T> loader) {
        long now = clock.getAsLong();
        Entry e = entries.get(key);
        if (e != null && e.expiresAtMs() > now) {
            hits.incrementAndGet();
            return (T) e.value();
        }
        misses.incrementAndGet();
        T value = loader.get();
        if (value != null) {
            if (entries.size() >= maxEntries) {
                evict(now);
            }
            entries.put(key, new Entry(value, now + ttlMs));
        }
        return value;
    }

    /** Drops everything, for use after data changes. */
    public void clear() {
        entries.clear();
    }

    public long hits() {
        return hits.get();
    }

    public long misses() {
        return misses.get();
    }

    public int size() {
        return entries.size();
    }

    private void evict(long now) {
        entries.entrySet().removeIf(en -> en.getValue().expiresAtMs() <= now);
        if (entries.size() >= maxEntries) {
            // Still full: remove the entries that would expire first.
            entries.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(java.util.Comparator.comparingLong(Entry::expiresAtMs)))
                    .limit(Math.max(1, maxEntries / 4))
                    .map(Map.Entry::getKey)
                    .toList()
                    .forEach(entries::remove);
        }
    }
}
