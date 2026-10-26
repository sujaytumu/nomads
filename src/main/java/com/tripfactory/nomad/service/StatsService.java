package com.tripfactory.nomad.service;

import com.tripfactory.nomad.api.dto.TravelStatsResponse;

public interface StatsService {

    /** The caller's travel stats, XP level and badges, worked out from their trips. */
    TravelStatsResponse statsFor(String email);
}
