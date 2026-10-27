package com.tripfactory.nomad.service.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tripfactory.nomad.service.engine.RatingSummary.Summary;

class RatingSummaryTest {

    @Test
    void noRatingsKeepsThePlacesOwnRating() {
        Summary s = RatingSummary.compute(List.of(), 4.3);
        assertEquals(0, s.count());
        assertEquals(0.0, s.average(), 1e-9);
        assertEquals(4.3, s.blended(), 1e-9);
        assertEquals(List.of(0, 0, 0, 0, 0), s.distribution());
    }

    @Test
    void nullListIsTreatedAsEmpty() {
        assertEquals(0, RatingSummary.compute(null, 4.0).count());
    }

    @Test
    void averageAndDistributionAreCounted() {
        Summary s = RatingSummary.compute(List.of(5, 5, 4, 3, 1), 4.0);
        assertEquals(5, s.count());
        assertEquals(3.6, s.average(), 1e-9);
        assertEquals(List.of(1, 0, 1, 1, 2), s.distribution());
    }

    @Test
    void oneFiveStarRatingBarelyMovesAWellRatedPlace() {
        Summary s = RatingSummary.compute(List.of(5), 4.0);
        // (5*4.0 + 5) / 6 = 4.1666...
        assertEquals(4.2, s.blended(), 1e-9);
        assertTrue(s.blended() < 4.5);
    }

    @Test
    void manyRatingsOutweighTheStartingRating() {
        Integer[] fifty = new Integer[50];
        Arrays.fill(fifty, 5);
        Summary s = RatingSummary.compute(Arrays.asList(fifty), 3.0);
        // (5*3.0 + 250) / 55 = 4.8181...
        assertEquals(4.8, s.blended(), 1e-9);
    }

    @Test
    void outOfRangeAndNullRatingsAreIgnored() {
        Summary s = RatingSummary.compute(Arrays.asList(5, 0, 6, null, -1, 3), 4.0);
        assertEquals(2, s.count());
        assertEquals(4.0, s.average(), 1e-9);
    }

    @Test
    void startingRatingIsClamped() {
        assertEquals(5.0, RatingSummary.compute(List.of(), 9.0).blended(), 1e-9);
        assertEquals(0.0, RatingSummary.compute(List.of(), -2.0).blended(), 1e-9);
    }
}
