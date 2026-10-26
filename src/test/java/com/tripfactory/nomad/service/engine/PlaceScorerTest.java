package com.tripfactory.nomad.service.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.tripfactory.nomad.domain.enums.InterestType;
import com.tripfactory.nomad.service.engine.PlaceScorer.Candidate;
import com.tripfactory.nomad.service.engine.PlaceScorer.Scored;
import com.tripfactory.nomad.service.engine.PlaceScorer.Taste;

class PlaceScorerTest {

    // Home: central Pune.
    private static final double LAT = 18.5204, LON = 73.8567;

    private static Candidate place(long id, String city, double lat, double lon, InterestType cat, double rating, boolean visited) {
        return new Candidate(id, "Place " + id, city, lat, lon, cat, rating, null, visited);
    }

    private static Taste taste(InterestType declared) {
        return new Taste(declared, new EnumMap<>(InterestType.class), LAT, LON);
    }

    @Test
    void favouriteInterestOutranksOtherwiseIdenticalPlace() {
        Candidate food = place(1, "Pune", LAT, LON, InterestType.FOOD, 4.5, false);
        Candidate nature = place(2, "Pune", LAT, LON, InterestType.NATURE, 4.5, false);
        List<Scored> ranked = PlaceScorer.rank(List.of(nature, food), taste(InterestType.FOOD), 2);
        assertEquals(1L, ranked.get(0).place().id());
        assertTrue(ranked.get(0).score() > ranked.get(1).score());
    }

    @Test
    void nearbyBeatsFarAwayWhenEverythingElseIsEqual() {
        Candidate near = place(1, "Pune", LAT + 0.01, LON, InterestType.FOOD, 4.0, false);
        Candidate far = place(2, "Goa", 15.2993, 74.1240, InterestType.FOOD, 4.0, false);
        List<Scored> ranked = PlaceScorer.rank(List.of(far, near), taste(InterestType.FOOD), 2);
        assertEquals(1L, ranked.get(0).place().id());
        assertTrue(ranked.get(1).distanceKm() > 300);
    }

    @Test
    void placesAlreadyVisitedScoreLowerThanNewOnes() {
        Candidate seen = place(1, "Pune", LAT, LON, InterestType.FOOD, 4.5, true);
        Candidate fresh = place(2, "Pune", LAT, LON, InterestType.FOOD, 4.5, false);
        List<Scored> ranked = PlaceScorer.rank(List.of(seen, fresh), taste(InterestType.FOOD), 2);
        assertEquals(2L, ranked.get(0).place().id());
    }

    @Test
    void bookingHistoryShiftsTasteTowardsWhatTheUserActuallyBooks() {
        Map<InterestType, Integer> history = new EnumMap<>(InterestType.class);
        history.put(InterestType.ADVENTURE, 4);
        Taste t = new Taste(InterestType.FOOD, history, LAT, LON);
        // Declared FOOD = 0.6, ADVENTURE history = 0.4: both strong, others zero.
        assertEquals(0.6, PlaceScorer.tasteScore(InterestType.FOOD, t), 1e-9);
        assertEquals(0.4, PlaceScorer.tasteScore(InterestType.ADVENTURE, t), 1e-9);
        assertEquals(0.0, PlaceScorer.tasteScore(InterestType.SHOPPING, t), 1e-9);
    }

    @Test
    void resultsAreDiversifiedInsteadOfTenOfTheSameCategory() {
        // Four slightly better FOOD places and one slightly worse NATURE place: the nature one should
        // not be pushed all the way to the bottom.
        Candidate f1 = place(1, "Pune", LAT, LON, InterestType.FOOD, 4.9, false);
        Candidate f2 = place(2, "Pune", LAT, LON, InterestType.FOOD, 4.9, false);
        Candidate f3 = place(3, "Pune", LAT, LON, InterestType.FOOD, 4.9, false);
        Candidate n1 = place(4, "Pune", LAT, LON, InterestType.NATURE, 4.9, false);
        List<Scored> ranked = PlaceScorer.rank(List.of(f1, f2, f3, n1), taste(InterestType.FOOD), 4);
        int natureAt = -1;
        for (int i = 0; i < ranked.size(); i++) {
            if (ranked.get(i).place().id() == 4L) natureAt = i;
        }
        assertTrue(natureAt >= 0 && natureAt < 3, "nature place should rank above the last food repeat, was at " + natureAt);
    }

    @Test
    void limitIsRespectedAndEmptyInputIsSafe() {
        assertTrue(PlaceScorer.rank(List.of(), taste(InterestType.FOOD), 5).isEmpty());
        assertTrue(PlaceScorer.rank(null, taste(InterestType.FOOD), 5).isEmpty());
        Candidate a = place(1, "Pune", LAT, LON, InterestType.FOOD, 4.0, false);
        Candidate b = place(2, "Pune", LAT, LON, InterestType.FOOD, 4.0, false);
        assertEquals(1, PlaceScorer.rank(List.of(a, b), taste(InterestType.FOOD), 1).size());
        assertTrue(PlaceScorer.rank(List.of(a, b), taste(InterestType.FOOD), 0).isEmpty());
    }

    @Test
    void reasonsExplainTheScore() {
        Candidate c = place(1, "Pune", LAT, LON, InterestType.FOOD, 4.8, false);
        Scored s = PlaceScorer.rank(List.of(c), taste(InterestType.FOOD), 1).get(0);
        assertTrue(s.reasons().stream().anyMatch(r -> r.contains("favourite")), s.reasons().toString());
        assertTrue(s.reasons().stream().anyMatch(r -> r.contains("Top rated")), s.reasons().toString());
        assertTrue(s.reasons().stream().anyMatch(r -> r.contains("New to you")), s.reasons().toString());
        assertTrue(s.score() >= 0.0 && s.score() <= 1.0);
    }

    @Test
    void scoreIsAlwaysBetweenZeroAndOne() {
        Candidate worst = place(1, "Far", -33.86, 151.21, InterestType.NIGHTLIFE, 1.0, true);
        Candidate best = place(2, "Pune", LAT, LON, InterestType.FOOD, 5.0, false);
        Taste t = taste(InterestType.FOOD);
        for (Scored s : PlaceScorer.rank(List.of(worst, best), t, 2)) {
            assertTrue(s.score() >= 0.0 && s.score() <= 1.0, "score out of range: " + s.score());
        }
    }
}
