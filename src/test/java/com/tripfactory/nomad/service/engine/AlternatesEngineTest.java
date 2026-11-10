package com.tripfactory.nomad.service.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.tripfactory.nomad.service.engine.AlternatesEngine.Option;
import com.tripfactory.nomad.service.engine.AlternatesEngine.Result;
import com.tripfactory.nomad.service.engine.AlternatesEngine.Spot;
import com.tripfactory.nomad.service.engine.AlternatesEngine.Stop;
import com.tripfactory.nomad.service.engine.AlternatesEngine.StopAlternates;

class AlternatesEngineTest {

    // Roughly Goa: 0.01 degrees of latitude is about 1.1 km.
    private static Spot spot(long id, String name, String cat, double rating, double dLat) {
        return new Spot(id, name, cat, rating, 15.50 + dLat, 73.80);
    }

    private static final Spot BEACH = spot(1, "Beach", "NATURE", 4.5, 0);
    private static final Spot FORT = spot(2, "Fort", "CULTURE", 4.2, 0.02);

    private static final List<Spot> POOL = List.of(
        spot(10, "Museum", "CULTURE", 4.6, 0.01),
        spot(11, "Mall", "SHOPPING", 4.0, 0.03),
        spot(12, "Trek", "ADVENTURE", 4.9, 0.01),     // outdoor: never a rain backup
        spot(13, "Cafe", "FOOD", 4.4, 0.02),
        spot(14, "Faraway Gallery", "CULTURE", 5.0, 0.5),   // ~55 km: out of range
        spot(1, "Beach", "NATURE", 4.5, 0),                 // already in the trip
        spot(15, "Park", "NATURE", 4.1, 0.015));

    @Test
    void rainOnAnOutdoorStopOffersIndoorBackupsOnly() {
        Result r = AlternatesEngine.compute(List.of(new Stop(1, BEACH)), POOL, Map.of(1, 80));
        StopAlternates s = r.stops().get(0);
        assertTrue(s.needsBackup());
        assertEquals(List.of(1), r.rainyDays());
        assertEquals(3, s.options().size());
        for (Option o : s.options()) {
            assertFalse(o.category().equals("NATURE") || o.category().equals("ADVENTURE"));
            assertTrue(o.why().startsWith("Indoors"));
        }
        assertEquals("Museum", s.options().get(0).name());   // best rated and closest
    }

    @Test
    void tripPlacesAndFarPlacesAreNeverOffered() {
        Result r = AlternatesEngine.compute(List.of(new Stop(1, BEACH)), POOL, Map.of(1, 90));
        for (Option o : r.stops().get(0).options()) {
            assertTrue(o.placeId() != 1 && o.placeId() != 14);
        }
    }

    @Test
    void dryDayGivesLikeForLikeSwaps() {
        Result r = AlternatesEngine.compute(List.of(new Stop(1, BEACH)), POOL, Map.of(1, 10));
        StopAlternates s = r.stops().get(0);
        assertFalse(s.needsBackup());
        assertTrue(r.rainyDays().isEmpty());
        assertEquals(1, s.options().size());                 // only Park is NATURE, in range and not in the trip
        assertEquals("Park", s.options().get(0).name());
        assertTrue(s.options().get(0).why().startsWith("Similar"));
    }

    @Test
    void rainOnAnIndoorStopNeedsNoBackupButSaysSo() {
        Result r = AlternatesEngine.compute(List.of(new Stop(2, FORT)), POOL, Map.of(2, 70));
        StopAlternates s = r.stops().get(0);
        assertFalse(s.needsBackup());
        assertTrue(s.headline().contains("indoors"));
        assertEquals("Museum", s.options().get(0).name());
    }

    @Test
    void unknownForecastIsTreatedAsDry() {
        Result r = AlternatesEngine.compute(List.of(new Stop(1, BEACH)), POOL, Map.of());
        assertFalse(r.stops().get(0).needsBackup());
        assertEquals(null, r.stops().get(0).rainChance());
    }

    @Test
    void nothingNearbyGivesEmptyOptionsNotErrors() {
        Result r = AlternatesEngine.compute(List.of(new Stop(1, BEACH)), List.of(), Map.of(1, 90));
        assertTrue(r.stops().get(0).options().isEmpty());
    }

    @Test
    void thresholdIsInclusive() {
        assertTrue(AlternatesEngine.compute(List.of(new Stop(1, BEACH)), POOL, Map.of(1, AlternatesEngine.RAIN_THRESHOLD)).stops().get(0).needsBackup());
        assertFalse(AlternatesEngine.compute(List.of(new Stop(1, BEACH)), POOL, Map.of(1, AlternatesEngine.RAIN_THRESHOLD - 1)).stops().get(0).needsBackup());
    }
}
