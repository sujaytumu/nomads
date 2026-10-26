package com.tripfactory.nomad.service;

import com.tripfactory.nomad.api.dto.PlaceResponse;
import com.tripfactory.nomad.api.dto.PlaceSearchResponse;
import com.tripfactory.nomad.domain.enums.InterestType;

public interface PlaceSearchService {

    PlaceSearchResponse search(String text, String city, InterestType category, Double minRating, String sort,
                               Double lat, Double lon, int page, int size);

    /** One place by id. Throws ResourceNotFoundException when there is none. */
    PlaceResponse getPlace(Long id);
}
