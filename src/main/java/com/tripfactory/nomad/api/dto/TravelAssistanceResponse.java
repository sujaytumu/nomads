package com.tripfactory.nomad.api.dto;

import java.time.LocalDateTime;

import com.tripfactory.nomad.domain.enums.AssistanceStatus;
import com.tripfactory.nomad.domain.enums.VehicleType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TravelAssistanceResponse {

    private Long tripRequestId;
    private String pickupLocation;
    private LocalDateTime pickupTime;
    private Long vehicleId;
    private VehicleType vehicleType;
    private String driverName;
    private String vehicleNumber;
    private String routeMapUrl;
    private AssistanceStatus status;
}