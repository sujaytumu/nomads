package com.tripfactory.nomad.service;

import com.tripfactory.nomad.service.engine.AnalyticsEngine;

public interface AdminAnalyticsService {

    /** Business numbers for the admin dashboard over the last {@code days} days (clamped to 1..90). */
    AnalyticsEngine.Snapshot snapshot(int days);
}
