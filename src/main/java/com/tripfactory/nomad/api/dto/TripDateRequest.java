package com.tripfactory.nomad.api.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TripDateRequest {

    @NotNull
    private LocalDate tripDate;
}
