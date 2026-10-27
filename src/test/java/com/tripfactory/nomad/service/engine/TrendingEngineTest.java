package com.tripfactory.nomad.service.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tripfactory.nomad.service.engine.TrendingEngine.Stop;
import com.tripfactory.nomad.service.engine.TrendingEngine.Trending;

class TrendingEngineTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);

    private static Stop stop(long place, long trip, int daysAgo) {
        return new Stop(place, trip, TODAY.minusDays(daysAgo));
    }

    @Test
    void morePlaceVisitsRankHigher() {
        List<Trending> r = TrendingEngine.rank(List.of(stop(1, 10, 1), stop(2, 11, 1), stop(2, 12, 1)), TODAY, 5);
        assertEquals(2L, r.get(0).placeId());
        assertEquals(2, r.get(0).trips());
        assertEquals(1L, r.get(1).placeId());
    }

    @Test
    void sameTripVisitingTwiceCountsOnce() {
        List<Trending> r = TrendingEngine.rank(List.of(stop(1, 10, 0), stop(1, 10, 0), stop(1, 10, 0)), TODAY, 5);
        assertEquals(1, r.get(0).trips());
        assertEquals(1.0, r.get(0).score(), 1e-9);
    }

    @Test
    void newerTripsCountMore() {
        // Place 1: one trip today. Place 2: two trips 28 days ago (each worth 1/4), so 0.5 in total.
        List<Trending> r = TrendingEngine.rank(List.of(stop(1, 10, 0), stop(2, 11, 28), stop(2, 12, 28)), TODAY, 5);
        assertEquals(1L, r.get(0).placeId());
        assertEquals(0.5, r.get(1).score(), 1e-9);
    }

    @Test
    void oldAndFutureTripsAreIgnored() {
        List<Trending> r = TrendingEngine.rank(List.of(stop(1, 10, TrendingEngine.WINDOW_DAYS), stop(2, 11, -3),
                new Stop(3, 12, null), stop(4, 13, TrendingEngine.WINDOW_DAYS - 1)), TODAY, 5);
        assertEquals(1, r.size());
        assertEquals(4L, r.get(0).placeId());
    }

    @Test
    void tiesBreakByLowerPlaceIdAndLimitApplies() {
        List<Trending> r = TrendingEngine.rank(List.of(stop(9, 1, 0), stop(3, 2, 0), stop(5, 3, 0)), TODAY, 2);
        assertEquals(2, r.size());
        assertEquals(3L, r.get(0).placeId());
        assertEquals(5L, r.get(1).placeId());
    }

    @Test
    void nothingBookedGivesNothing() {
        assertTrue(TrendingEngine.rank(List.of(), TODAY, 5).isEmpty());
        assertTrue(TrendingEngine.rank(List.of(stop(1, 1, 0)), TODAY, 0).isEmpty());
    }
}
