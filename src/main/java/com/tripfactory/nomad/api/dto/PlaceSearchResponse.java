package com.tripfactory.nomad.api.dto;

import java.util.List;
import java.util.Map;

import com.tripfactory.nomad.domain.enums.InterestType;

public record PlaceSearchResponse(List<Item> items, int page, int size, long totalItems, int totalPages,
                                  Facets facets) {

    public record Item(Long id, String name, String city, double latitude, double longitude,
                       InterestType category, double rating, String imageUrl, Double distanceKm) {
    }

    /** How many results each choice would give, so a filter panel can show the counts next to its options. */
    public record Facets(Map<String, Long> categories, Map<String, Long> cities) {
    }
}
