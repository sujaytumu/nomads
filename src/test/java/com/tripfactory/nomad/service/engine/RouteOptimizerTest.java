package com.tripfactory.nomad.service.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.tripfactory.nomad.domain.entity.Place;

class RouteOptimizerTest {

    private static Place place(String name, double lat, double lon) {
        Place p = new Place();
        p.setName(name);
        p.setLatitude(lat);
        p.setLongitude(lon);
        return p;
    }

    @Test
    void neverLongerThanTheOrderGivenAndKeepsEveryPlaceOnce() {
        Random r = new Random(42);
        for (int round = 0; round < 50; round++) {
            List<Place> places = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                places.add(place("p" + i, 12.9 + r.nextDouble() * 0.2, 77.5 + r.nextDouble() * 0.2));
            }
            List<Place> route = RouteOptimizer.optimize(12.95, 77.6, places);
            assertThat(route).hasSameSizeAs(places).containsExactlyInAnyOrderElementsOf(places);
            assertThat(RouteOptimizer.pathKm(12.95, 77.6, route)).isLessThanOrEqualTo(RouteOptimizer.pathKm(12.95, 77.6, places) + 1e-9);
        }
    }

    @Test
    void fixesAZigZag() {
        // Home at 0, places on a line: 1, 5, 2, 4, 3 (degrees of longitude). The best order walks outwards 1,2,3,4,5.
        List<Place> zigzag = List.of(place("a", 0, 1), place("b", 0, 5), place("c", 0, 2), place("d", 0, 4), place("e", 0, 3));
        List<Place> route = RouteOptimizer.optimize(0, 0, zigzag);
        assertThat(route.stream().map(Place::getName)).containsExactly("a", "c", "e", "d", "b");
    }

    @Test
    void handlesTinyInputs() {
        assertThat(RouteOptimizer.optimize(0, 0, List.of())).isEmpty();
        Place only = place("x", 1, 1);
        assertThat(RouteOptimizer.optimize(0, 0, List.of(only))).containsExactly(only);
    }
}
