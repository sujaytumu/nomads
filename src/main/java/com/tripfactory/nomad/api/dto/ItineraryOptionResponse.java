package com.tripfactory.nomad.api.dto;

import com.tripfactory.nomad.domain.enums.InterestType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ItineraryOptionResponse {
    private Long placeId;
    private String name;
    private InterestType category;
    private Double rating;
    private String imageUrl;
    private Double distanceFromHomeKm;
    private boolean selected;
}
