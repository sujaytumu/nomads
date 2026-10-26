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
public class SettlementCreateRequest {

    /** The member you paid. */
    @NotNull
    private Long toUserId;

    /** Rupees, e.g. 250.00 */
    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;
}
