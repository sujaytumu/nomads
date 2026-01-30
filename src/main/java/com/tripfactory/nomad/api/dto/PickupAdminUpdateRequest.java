package com.tripfactory.nomad.api.dto;

import java.time.LocalDateTime;

import com.tripfactory.nomad.domain.enums.AssistanceStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PickupAdminUpdateRequest {

    private LocalDateTime pickupTime;
    private AssistanceStatus status;
    private String routeMapUrl;
    private Long vehicleId;
}
