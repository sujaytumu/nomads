package com.tripfactory.nomad.api.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ItineraryUpdateRequest {

    // The places the traveller wants to visit, in any order - the server
    // schedules them into days and orders the stops by proximity.
    @NotEmpty
    private List<Long> placeIds;

    // The revision the editor started from (from the itinerary options). Optional: if it is sent and the trip
    // has changed since, the save is refused so nobody's change is silently overwritten.
    private Integer expectedRevision;
}
