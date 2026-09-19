package com.tripfactory.nomad.api.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PackageEnrollRequest {

    // No longer trusted - the server derives the real amount from the
    // package's own price server-side. Kept only for backward compatibility
    // with older clients that still send it.
    private BigDecimal amount;
}
