package com.tripfactory.nomad.service.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Keeps the AI assistant affordable: each person gets a fixed number of questions per window. */
public class AssistantRateLimiter {

    private final int maxPerWindow;
    private final long windowMs;
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public AssistantRateLimiter(int maxPerWindow, long windowMs) {
        this.maxPerWindow = maxPerWindow;
        this.windowMs = windowMs;
    }

    /** @return true if this question may go ahead (and counts it), false if the person is over the limit */
    public boolean allow(String key, long nowMs) {
        Deque<Long> q = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && nowMs - q.peekFirst() >= windowMs) {
                q.pollFirst();
            }
            if (q.size() >= maxPerWindow) {
                return false;
            }
            q.addLast(nowMs);
            return true;
        }
    }
}
