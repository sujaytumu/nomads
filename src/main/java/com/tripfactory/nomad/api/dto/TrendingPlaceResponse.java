package com.tripfactory.nomad.api.dto;

/** A place that is being booked a lot lately, with how many recent booked trips include it. */
public record TrendingPlaceResponse(PlaceSearchResponse.Item place, int trips) {
}
