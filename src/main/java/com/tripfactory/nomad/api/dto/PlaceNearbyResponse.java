package com.tripfactory.nomad.api.dto;

import com.tripfactory.nomad.domain.enums.InterestType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PlaceNearbyResponse {

    private Long id;
    private String name;
    private String city;
    private Double latitude;
    private Double longitude;
    private InterestType category;
    private Double rating;
    private Double distanceKm;
    private String imageUrl;
}