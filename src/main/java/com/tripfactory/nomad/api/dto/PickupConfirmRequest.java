package com.tripfactory.nomad.api.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PickupConfirmRequest {

    @NotNull
    private LocalDateTime pickupTime;
}
