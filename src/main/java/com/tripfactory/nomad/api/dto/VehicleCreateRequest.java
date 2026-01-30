package com.tripfactory.nomad.api.dto;

import com.tripfactory.nomad.domain.enums.AvailabilityStatus;
import com.tripfactory.nomad.domain.enums.VehicleType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VehicleCreateRequest {

    @NotNull
    private VehicleType vehicleType;

    @NotNull
    private Integer capacity;

    @NotBlank
    private String driverName;

    @NotBlank
    private String vehicleNumber;

    @NotNull
    private AvailabilityStatus availabilityStatus;
}