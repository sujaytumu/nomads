package com.tripfactory.nomad.api.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Everything the place page shows about traveller ratings. Reviewer names are first names only. */
public record PlaceRatingsResponse(Long placeId, int count, double average, double blended,
                                   List<Integer> distribution, List<Review> reviews) {

    public record Review(int stars, String comment, String reviewer, LocalDateTime when) {
    }
}
