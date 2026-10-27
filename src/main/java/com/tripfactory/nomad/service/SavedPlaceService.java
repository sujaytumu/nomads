package com.tripfactory.nomad.service;

import java.util.List;

import com.tripfactory.nomad.api.dto.PlaceSearchResponse;

public interface SavedPlaceService {

    /** The caller's saved places, most recently saved first. */
    List<PlaceSearchResponse.Item> list(String email);

    /** Ids only, so a grid can fill its hearts with one call. */
    List<Long> ids(String email);

    /** Idempotent. */
    void add(String email, Long placeId);

    /** Idempotent. */
    void remove(String email, Long placeId);
}
