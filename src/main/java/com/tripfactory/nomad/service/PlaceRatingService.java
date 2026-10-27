package com.tripfactory.nomad.service;

import com.tripfactory.nomad.api.dto.MyPlaceRatingResponse;
import com.tripfactory.nomad.api.dto.PlaceRatingsResponse;

public interface PlaceRatingService {

    /** Public summary plus the latest written reviews. */
    PlaceRatingsResponse overview(Long placeId);

    MyPlaceRatingResponse mine(String email, Long placeId);

    /** Adds or changes the caller's rating. Only people with a booked trip that includes the place may rate it. */
    MyPlaceRatingResponse rate(String email, Long placeId, int stars, String comment);

    void remove(String email, Long placeId);
}
