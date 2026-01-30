package com.tripfactory.nomad.api.dto;

import java.time.LocalDateTime;

import com.tripfactory.nomad.domain.enums.AssistanceStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PickupUpdateRequest {

    @NotNull
    private LocalDateTime pickupTime;

    @NotNull
    private AssistanceStatus status;
}