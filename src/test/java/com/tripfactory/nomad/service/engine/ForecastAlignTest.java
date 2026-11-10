package com.tripfactory.nomad.service.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.tripfactory.nomad.service.engine.ForecastAlign.Day;

class ForecastAlignTest {

    private static final List<Day> FORECAST = List.of(new Day("2026-11-10", 10), new Day("2026-11-11", 80), new Day("2026-11-12", 30));

    @Test
    void tripStartingTodayMapsDayOneToFirstForecastDay() {
        Map<Integer, Integer> m = ForecastAlign.rainByTripDay(LocalDate.of(2026, 11, 10), FORECAST, 2);
        assertEquals(10, (int) m.get(1));
        assertEquals(80, (int) m.get(2));
        assertEquals(2, m.size());
    }

    @Test
    void laterTripPicksTheRightForecastDays() {
        Map<Integer, Integer> m = ForecastAlign.rainByTripDay(LocalDate.of(2026, 11, 11), FORECAST, 2);
        assertEquals(80, (int) m.get(1));
        assertEquals(30, (int) m.get(2));
    }

    @Test
    void tripBeyondTheForecastHasNoData() {
        assertTrue(ForecastAlign.rainByTripDay(LocalDate.of(2026, 11, 20), FORECAST, 2).isEmpty());
    }

    @Test
    void forecastDaysOutsideTheTripAreIgnoredAndTripDaysCapAtLength() {
        Map<Integer, Integer> m = ForecastAlign.rainByTripDay(LocalDate.of(2026, 11, 10), FORECAST, 1);
        assertEquals(1, m.size());
        assertEquals(10, (int) m.get(1));
    }

    @Test
    void noTripDateOrBadDataGivesEmpty() {
        assertTrue(ForecastAlign.rainByTripDay(null, FORECAST, 2).isEmpty());
        assertTrue(ForecastAlign.rainByTripDay(LocalDate.of(2026, 11, 10), null, 2).isEmpty());
        assertTrue(ForecastAlign.rainByTripDay(LocalDate.of(2026, 11, 10), List.of(new Day("nope", 50), new Day(null, 50), new Day("2026-11-10", null)), 2).isEmpty());
    }
}
