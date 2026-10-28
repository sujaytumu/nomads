package com.tripfactory.nomad.api.dto;

import java.util.List;

/** Where to stay, scenic spots and hospitals for one trip. */
public record TripEssentialsResponse(String city, List<Stay> stay, List<Scenic> scenic, List<Medical> medical) {

    public record Stay(String area, String tier, int pricePerNight, double avgKmToStops, String why) {
    }

    public record Scenic(String name, String bestTime, double kmFromTrip, String why) {
    }

    public record Medical(String name, String type, double kmFromTrip, String note) {
    }
}
