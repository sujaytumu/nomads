package com.tripfactory.nomad.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PaymentCreateRequest {

    @NotNull
    private Long tripRequestId;

    @NotNull
    @DecimalMin("1.0")
    private BigDecimal amount;
}