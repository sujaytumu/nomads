package com.tripfactory.nomad.service.util;

import java.util.Map;

/**
 * Normalizes user-supplied city names to the canonical name used in the seed data.
 * People overwhelmingly type common/former names (Bangalore, Bombay, Calcutta,
 * Madras) rather than the official renamed versions (Bengaluru, Mumbai, Kolkata,
 * Chennai) - a strict city match on the official name alone rejects real users
 * typing the name they actually know, so this resolves the common aliases first.
 */
public final class CityAliasResolver {

    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("bangalore", "Bengaluru"),
            Map.entry("bengaluru", "Bengaluru"),
            Map.entry("bombay", "Mumbai"),
            Map.entry("mumbai", "Mumbai"),
            Map.entry("calcutta", "Kolkata"),
            Map.entry("kolkata", "Kolkata"),
            Map.entry("madras", "Chennai"),
            Map.entry("chennai", "Chennai"),
            Map.entry("delhi", "Delhi"),
            Map.entry("new delhi", "Delhi"),
            Map.entry("pune", "Pune"),
            Map.entry("poona", "Pune")
    );

    private CityAliasResolver() {
    }

    /**
     * Returns the canonical city name if a known alias matches (case/whitespace
     * insensitive), otherwise returns the original input trimmed and unchanged.
     */
    public static String canonicalize(String city) {
        if (city == null) {
            return null;
        }
        String key = city.trim().toLowerCase();
        return ALIASES.getOrDefault(key, city.trim());
    }
}
