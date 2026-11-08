package com.tripfactory.nomad.api.dto;

/** One line in the search suggestions: either a city (type CITY) or a place (type PLACE, with its id). */
public record PlaceSuggestion(String type, String label, String subLabel, Long placeId, String city) {
}
