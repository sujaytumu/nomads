package com.tripfactory.nomad.service.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class LiveLocationRulesTest {

    @Test
    void rejectsImpossiblePositions() {
        assertThat(LiveLocationRules.validPosition(12.97, 77.59)).isTrue();
        assertThat(LiveLocationRules.validPosition(91, 10)).isFalse();
        assertThat(LiveLocationRules.validPosition(10, 181)).isFalse();
        assertThat(LiveLocationRules.validPosition(Double.NaN, 10)).isFalse();
        assertThat(LiveLocationRules.validPosition(0, 0)).isFalse();
    }

    @Test
    void positionsGoStaleAfterFifteenMinutes() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 10, 12, 0);
        assertThat(LiveLocationRules.fresh(now.minusMinutes(14), now)).isTrue();
        assertThat(LiveLocationRules.fresh(now.minusMinutes(16), now)).isFalse();
        assertThat(LiveLocationRules.fresh(null, now)).isFalse();
    }

    @Test
    void measuresDistanceInMetres() {
        // Bengaluru to Mysuru is roughly 126 km in a straight line.
        double d = LiveLocationRules.metres(12.9716, 77.5946, 12.2958, 76.6394);
        assertThat(d).isBetween(120000.0, 135000.0);
        assertThat(LiveLocationRules.metres(10, 10, 10, 10)).isZero();
    }
}
