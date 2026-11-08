package com.tripfactory.nomad.service.engine;

import java.time.Duration;

/** How long to wait before trying a failed delivery again: 30s, 1m, 2m, 4m, 8m... capped at one hour. */
public final class RetryPolicy {

    public static final int MAX_ATTEMPTS = 6;
    private static final long BASE_SECONDS = 30;
    private static final long CAP_SECONDS = 3600;

    private RetryPolicy() {
    }

    /** @param attemptsSoFar how many tries have already failed (1 after the first failure) */
    public static Duration delayAfter(int attemptsSoFar) {
        int n = Math.max(1, Math.min(attemptsSoFar, 20));
        long seconds = Math.min(CAP_SECONDS, BASE_SECONDS << (n - 1));
        return Duration.ofSeconds(seconds);
    }

    public static boolean giveUp(int attemptsSoFar) {
        return attemptsSoFar >= MAX_ATTEMPTS;
    }
}
