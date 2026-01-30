package com.tripfactory.nomad.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TravelAssignRequest {

    @NotNull
    private Long tripRequestId;
}