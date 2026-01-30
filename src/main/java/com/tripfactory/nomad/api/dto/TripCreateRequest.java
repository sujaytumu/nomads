package com.tripfactory.nomad.api.dto;

import com.tripfactory.nomad.domain.enums.InterestType;
import com.tripfactory.nomad.domain.enums.TravelMode;
import com.tripfactory.nomad.domain.enums.WeekendType;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TripCreateRequest {

    @NotNull
    private Long userId;
    private String city;
    private WeekendType weekendType;
    private InterestType interest;
    private TravelMode travelMode;
    private Boolean pickupRequired;
    private Integer groupSize;
    private Double userLatitude;
    private Double userLongitude;
}