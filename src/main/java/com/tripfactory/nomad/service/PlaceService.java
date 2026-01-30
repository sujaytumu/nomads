package com.tripfactory.nomad.service;

import java.util.List;

import com.tripfactory.nomad.api.dto.PlaceNearbyResponse;
import com.tripfactory.nomad.domain.enums.InterestType;

public interface PlaceService {

    List<PlaceNearbyResponse> getNearbyPlaces(String city, double userLat, double userLon, InterestType interest,
            double radiusKm, int limit);
}