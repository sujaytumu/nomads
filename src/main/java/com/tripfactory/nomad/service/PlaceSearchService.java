package com.tripfactory.nomad.service;

import com.tripfactory.nomad.api.dto.PlaceResponse;
import com.tripfactory.nomad.api.dto.PlaceSearchResponse;
import com.tripfactory.nomad.domain.enums.InterestType;

public interface PlaceSearchService {

    PlaceSearchResponse search(String text, String city, InterestType category, Double minRating, String sort,
                               Double lat, Double lon, int page, int size);

    /** Up to 8 quick suggestions (cities first, then places) for what has been typed so far. */
    java.util.List<com.tripfactory.nomad.api.dto.PlaceSuggestion> suggest(String text);

    /** One place by id. Throws ResourceNotFoundException when there is none. */
    PlaceResponse getPlace(Long id);
}
