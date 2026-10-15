package com.tripfactory.nomad.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.tripfactory.nomad.domain.enums.InterestType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DepartureCreateRequest {

    @NotBlank
    @Size(max = 120)
    private String title;

    @NotBlank
    private String city;

    @Size(max = 2000)
    private String description;

    @NotNull
    private InterestType interest;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @NotNull
    @DecimalMin("1.00")
    private BigDecimal pricePerSeat;

    @NotNull
    @Min(2)
    private Integer minTravellers;

    @NotNull
    @Min(2)
    @Max(50)
    private Integer maxTravellers;

    @Size(max = 255)
    private String meetingPoint;
}
