package com.tripfactory.nomad.service.engine;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lines a date-stamped forecast up with the days of a trip. The forecast covers the next few days from today, so
 * "first forecast day" is not "trip day 1" unless the trip starts today. Trip day N is {@code tripDate + N - 1}.
 * Days the forecast does not cover are simply absent, and callers treat absent as "unknown", never as "dry".
 */
public final class ForecastAlign {

    private ForecastAlign() {
    }

    public record Day(String date, Integer rainChance) {
    }

    /** Trip day (1-based) to chance of rain, for trip days that fall inside the forecast. Empty without a trip date. */
    public static Map<Integer, Integer> rainByTripDay(LocalDate tripDate, List<Day> forecast, int tripDays) {
        Map<Integer, Integer> out = new HashMap<>();
        if (tripDate == null || forecast == null) return out;
        int days = Math.max(1, tripDays);
        for (Day d : forecast) {
            if (d == null || d.rainChance() == null || d.date() == null) continue;
            try {
                long idx = java.time.temporal.ChronoUnit.DAYS.between(tripDate, LocalDate.parse(d.date()));
                if (idx >= 0 && idx < days) out.put((int) idx + 1, d.rainChance());
            } catch (DateTimeParseException ignored) {
                // a malformed date in the forecast is skipped
            }
        }
        return out;
    }
}
