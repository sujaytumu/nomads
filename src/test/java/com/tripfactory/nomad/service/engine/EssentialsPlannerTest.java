package com.tripfactory.nomad.service.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.tripfactory.nomad.service.engine.EssentialsCatalog.Item;
import com.tripfactory.nomad.service.engine.EssentialsCatalog.Kind;
import com.tripfactory.nomad.service.engine.EssentialsPlanner.Point;

class EssentialsPlannerTest {

    private static final List<Item> ALL = EssentialsCatalog.items();
    private static final List<String> CITIES = List.of("Bengaluru", "Mumbai", "Delhi", "Hyderabad", "Vishakapatnam", "Chennai", "Kolkata", "Pune");

    @Test
    void everyCityHasStayScenicAndMedicalEntries() {
        for (String city : CITIES) {
            for (Kind k : Kind.values()) {
                long n = ALL.stream().filter(i -> i.city().equals(city) && i.kind() == k).count();
                assertThat(n).as(city + " " + k).isGreaterThanOrEqualTo(3);
            }
        }
        ALL.stream().filter(i -> i.kind() == Kind.STAY).forEach(i -> {
            assertThat(i.extra1()).isIn("BUDGET", "COMFORT", "PREMIUM");
            assertThat(i.price()).isPositive();
        });
    }

    @Test
    void hospitalsAreSortedClosestToTheTripFirst() {
        // Stops around Lalbagh (south Bengaluru)
        List<Point> stops = List.of(new Point(12.9507, 77.5848), new Point(12.9453, 77.5741));
        Point centre = EssentialsPlanner.centre(stops, null);
        var picks = EssentialsPlanner.nearest(ALL, "Bengaluru", Kind.MEDICAL, stops, centre, 4);
        assertThat(picks).hasSize(4);
        assertThat(picks.get(0).kmFromTrip()).isLessThanOrEqualTo(picks.get(3).kmFromTrip());
        assertThat(picks.get(0).item().name()).isEqualTo("Victoria Hospital");
    }

    @Test
    void staysOfferABudgetAComfortableAndAPremiumChoiceSortedByPrice() {
        List<Point> stops = List.of(new Point(12.9756, 77.6068));
        Point centre = EssentialsPlanner.centre(stops, null);
        var picks = EssentialsPlanner.stays(ALL, "Bengaluru", stops, centre, 4);
        assertThat(picks.stream().map(p -> p.item().extra1())).contains("BUDGET", "COMFORT", "PREMIUM");
        for (int i = 1; i < picks.size(); i++) {
            assertThat(picks.get(i).item().price()).isGreaterThanOrEqualTo(picks.get(i - 1).item().price());
        }
    }
}
