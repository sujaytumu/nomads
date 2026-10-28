package com.tripfactory.nomad.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class PlaceCatalogTest {

    @Test
    void everyCityHasEnoughPlacesForAWeekLongTrip() {
        Map<String, Long> perCity = PlaceCatalog.entries().stream()
                .collect(Collectors.groupingBy(PlaceCatalog.Entry::city, Collectors.counting()));
        assertThat(perCity.keySet()).contains("Bengaluru", "Mumbai", "Delhi", "Hyderabad", "Vishakapatnam",
                "Chennai", "Kolkata", "Pune");
        perCity.values().forEach(n -> assertThat(n).isGreaterThanOrEqualTo(19));
    }

    @Test
    void namesAreUniquePerCityAndRatingsAndCoordinatesAreSane() {
        List<PlaceCatalog.Entry> all = PlaceCatalog.entries();
        Set<String> seen = new HashSet<>();
        for (PlaceCatalog.Entry e : all) {
            assertThat(seen.add(e.city() + "|" + e.name())).as("duplicate " + e.name()).isTrue();
            assertThat(e.rating()).isBetween(3.5, 5.0);
            assertThat(e.lat()).isBetween(8.0, 37.0);
            assertThat(e.lon()).isBetween(68.0, 98.0);
        }
    }
}
