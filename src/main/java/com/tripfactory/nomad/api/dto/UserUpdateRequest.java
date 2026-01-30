package com.tripfactory.nomad.api.dto;

import com.tripfactory.nomad.domain.enums.InterestType;
import com.tripfactory.nomad.domain.enums.TravelPreference;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserUpdateRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String city;

    private String phoneNumber;

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;

    @NotNull
    private InterestType interestType;

    @NotNull
    private TravelPreference travelPreference;
}