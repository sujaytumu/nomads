package com.tripfactory.nomad.api.dto;

import java.time.LocalTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TripPlanItemResponse {

    private Integer dayNumber;
    private Long placeId;
    private String placeName;
    private LocalTime startTime;
    private LocalTime endTime;
    private Double distanceFromPrevious;
}