package com.tripfactory.nomad.api.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TripRouteResponse {

    private Long tripRequestId;
    private Integer dayNumber;
    private String mode;
    private String geoJson;
}