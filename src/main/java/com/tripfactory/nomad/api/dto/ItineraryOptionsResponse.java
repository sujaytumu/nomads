package com.tripfactory.nomad.api.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Everything the itinerary editor needs: which places can be picked, how many
 * are allowed, and the pricing rule so the UI can preview the cost live. The
 * server stays authoritative - the real price is recalculated on save.
 */
@Getter
@Setter
@NoArgsConstructor
public class ItineraryOptionsResponse {
    private Long tripRequestId;
    // Only a planned (unpaid) trip with a day-by-day itinerary can be edited.
    private boolean editable;
    private int minPlaces;
    private int maxPlaces;
    private BigDecimal costPerPlace;
    private BigDecimal pickupCost;
    private boolean pickupRequired;
    private List<ItineraryOptionResponse> options;
}
