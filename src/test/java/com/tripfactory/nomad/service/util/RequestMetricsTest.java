package com.tripfactory.nomad.service.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RequestMetricsTest {

    @Test
    void percentilesUseNearestRank() {
        long[] v = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
        assertThat(RequestMetrics.percentile(v, 50)).isEqualTo(50);
        assertThat(RequestMetrics.percentile(v, 95)).isEqualTo(100);
        assertThat(RequestMetrics.percentile(new long[0], 95)).isZero();
        assertThat(RequestMetrics.percentile(new long[] {7}, 50)).isEqualTo(7);
    }

    @Test
    void countsErrorsAndRanksSlowRoutes() {
        RequestMetrics m = new RequestMetrics();
        for (int i = 0; i < 10; i++) {
            m.record("GET /api/fast", 200, 5);
        }
        m.record("GET /api/slow", 200, 900);
        m.record("GET /api/slow", 500, 1200);
        m.record("GET /api/missing", 404, 3);
        var s = m.snapshot();
        assertThat(s.total()).isEqualTo(13);
        assertThat(s.serverErrors()).isEqualTo(1);
        assertThat(s.clientErrors()).isEqualTo(1);
        assertThat(s.slowest().get(0).route()).isEqualTo("GET /api/slow");
        assertThat(s.slowest().get(0).errors()).isEqualTo(1);
    }
}
