package com.tripfactory.nomad.service.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

class ReadCacheTest {

    @Test
    void servesFromMemoryUntilTheEntryExpires() {
        AtomicLong now = new AtomicLong(1_000);
        ReadCache cache = new ReadCache(now::get, 10);
        AtomicInteger loads = new AtomicInteger();

        assertThat(cache.<String>get("k", 100, () -> "v" + loads.incrementAndGet())).isEqualTo("v1");
        assertThat(cache.<String>get("k", 100, () -> "v" + loads.incrementAndGet())).isEqualTo("v1");
        assertThat(loads.get()).isEqualTo(1);
        assertThat(cache.hits()).isEqualTo(1);
        assertThat(cache.misses()).isEqualTo(1);

        now.addAndGet(101);
        assertThat(cache.<String>get("k", 100, () -> "v" + loads.incrementAndGet())).isEqualTo("v2");
    }

    @Test
    void neverStoresNullAndStaysWithinItsSizeLimit() {
        AtomicLong now = new AtomicLong(0);
        ReadCache cache = new ReadCache(now::get, 4);
        assertThat(cache.<String>get("none", 100, () -> null)).isNull();
        assertThat(cache.size()).isZero();
        for (int i = 0; i < 20; i++) {
            int n = i;
            cache.get("key" + i, 1000 + i, () -> "v" + n);
        }
        assertThat(cache.size()).isLessThanOrEqualTo(4);
    }

    @Test
    void clearForgetsEverything() {
        ReadCache cache = new ReadCache(() -> 0, 10);
        cache.get("a", 100, () -> "x");
        cache.clear();
        assertThat(cache.size()).isZero();
    }
}
