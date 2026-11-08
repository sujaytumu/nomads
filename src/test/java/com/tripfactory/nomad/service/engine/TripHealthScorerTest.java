package com.tripfactory.nomad.service.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.tripfactory.nomad.service.engine.TripHealthScorer.Stop;

class TripHealthScorerTest {

    private static Stop stop(int day, String cat, double rating, double km) {
        return new Stop(day, cat, rating, km, "Place " + day + cat);
    }

    @Test
    void aRelaxedVariedWellRatedPlanScoresHigh() {
        var r = TripHealthScorer.score(2, List.of(
                stop(1, "CULTURE", 4.6, 6), stop(1, "FOOD", 4.5, 3), stop(1, "NATURE", 4.6, 4),
                stop(2, "RELAXATION", 4.5, 8), stop(2, "SHOPPING", 4.4, 3), stop(2, "FOOD", 4.6, 2)), List.of(10, 5));
        assertThat(r.score()).isGreaterThanOrEqualTo(85);
        assertThat(r.label()).isEqualTo("Excellent");
        assertThat(r.tips()).isEmpty();
    }

    @Test
    void anOverpackedFarFlungRainyPlanScoresLowAndGivesTips() {
        var r = TripHealthScorer.score(1, List.of(
                stop(1, "NATURE", 3.6, 70), stop(1, "NATURE", 3.6, 60), stop(1, "NATURE", 3.7, 55),
                stop(1, "NATURE", 3.6, 50), stop(1, "NATURE", 3.6, 45), stop(1, "NATURE", 3.6, 40)), List.of(90));
        assertThat(r.score()).isLessThan(50);
        assertThat(r.tips()).isNotEmpty();
        assertThat(r.factors()).extracting(TripHealthScorer.Factor::key).contains("pace", "road", "quality", "variety", "weather");
    }

    @Test
    void worksWithoutAForecastAndWithNoStops() {
        var noForecast = TripHealthScorer.score(1, List.of(stop(1, "FOOD", 4.5, 5)), List.of());
        assertThat(noForecast.factors()).extracting(TripHealthScorer.Factor::key).doesNotContain("weather");
        assertThat(noForecast.score()).isBetween(0, 100);
        var empty = TripHealthScorer.score(2, List.of(), List.of());
        assertThat(empty.score()).isZero();
        assertThat(empty.tips()).hasSize(1);
    }
}
