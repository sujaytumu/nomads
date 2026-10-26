package com.tripfactory.nomad.api.dto;

import java.util.List;

import com.tripfactory.nomad.domain.enums.InterestType;

public record RecommendedPlaceResponse(Long id, String name, String city, double latitude, double longitude,
                                       InterestType category, double rating, String imageUrl,
                                       double distanceKm, double score, List<String> reasons) {
}
