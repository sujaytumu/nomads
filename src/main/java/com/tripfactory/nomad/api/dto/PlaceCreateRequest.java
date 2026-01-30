package com.tripfactory.nomad.api.dto;

import com.tripfactory.nomad.domain.enums.InterestType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PlaceCreateRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String city;

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;

    @NotNull
    private InterestType category;

    @NotNull
    private Double rating;
}