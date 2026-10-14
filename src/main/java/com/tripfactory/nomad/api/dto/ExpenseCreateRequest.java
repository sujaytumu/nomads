package com.tripfactory.nomad.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ExpenseCreateRequest {

    @NotBlank
    @Size(max = 200)
    private String description;

    /** Rupees, e.g. 1250.50 */
    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;
}
