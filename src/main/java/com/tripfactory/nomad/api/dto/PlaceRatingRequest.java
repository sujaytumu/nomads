package com.tripfactory.nomad.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlaceRatingRequest(
        @NotNull @Min(1) @Max(5) Integer stars,
        @Size(max = 500) String comment) {
}
