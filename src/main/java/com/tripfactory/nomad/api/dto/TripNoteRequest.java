package com.tripfactory.nomad.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TripNoteRequest(
        @NotBlank @Size(max = 1000) String text,
        @Min(1) @Max(30) Integer dayNumber,
        @Size(max = 8) String mood) {
}
