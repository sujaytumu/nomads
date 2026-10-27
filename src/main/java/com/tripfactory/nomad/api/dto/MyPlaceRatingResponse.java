package com.tripfactory.nomad.api.dto;

/** The caller's own rating of a place, and whether they are allowed to rate it (they have a booked trip that includes it). */
public record MyPlaceRatingResponse(boolean canRate, Integer stars, String comment) {
}
