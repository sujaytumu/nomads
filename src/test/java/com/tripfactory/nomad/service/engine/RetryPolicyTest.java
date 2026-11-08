package com.tripfactory.nomad.service.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class RetryPolicyTest {

    @Test
    void delaysDoubleThenStopAtOneHour() {
        assertThat(RetryPolicy.delayAfter(1)).isEqualTo(Duration.ofSeconds(30));
        assertThat(RetryPolicy.delayAfter(2)).isEqualTo(Duration.ofSeconds(60));
        assertThat(RetryPolicy.delayAfter(4)).isEqualTo(Duration.ofMinutes(4));
        assertThat(RetryPolicy.delayAfter(50)).isEqualTo(Duration.ofHours(1));
        assertThat(RetryPolicy.delayAfter(0)).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void givesUpAfterTheMaximumAttempts() {
        assertThat(RetryPolicy.giveUp(RetryPolicy.MAX_ATTEMPTS - 1)).isFalse();
        assertThat(RetryPolicy.giveUp(RetryPolicy.MAX_ATTEMPTS)).isTrue();
    }
}
