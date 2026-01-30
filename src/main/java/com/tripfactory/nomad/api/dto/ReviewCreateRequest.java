package com.tripfactory.nomad.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReviewCreateRequest {

    @NotNull
    private Long tripRequestId;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;
    private String comment;
}