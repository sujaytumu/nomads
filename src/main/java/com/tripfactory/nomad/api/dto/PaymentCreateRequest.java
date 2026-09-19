package com.tripfactory.nomad.api.dto;

import java.math.BigDecimal;

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

    // No longer trusted - the server always derives the real amount from
    // the trip's own estimatedCost (see PaymentServiceImpl.createOrder).
    // Kept only so older clients that still send it don't fail validation.
    private BigDecimal amount;
}