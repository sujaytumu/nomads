package com.tripfactory.nomad.service.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tripfactory.nomad.domain.enums.TripStatus;
import com.tripfactory.nomad.service.engine.ReminderPlanner.Reminder;
import com.tripfactory.nomad.service.engine.ReminderPlanner.TripFacts;

class ReminderPlannerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);

    private static TripFacts confirmed(LocalDate date, int lastDay, boolean reviewed) {
        return new TripFacts(42, "Goa", TripStatus.CONFIRMED, date, lastDay, TODAY.minusDays(20), reviewed);
    }

    private static List<String> keys(List<Reminder> rs) {
        return rs.stream().map(Reminder::dedupeKey).collect(java.util.stream.Collectors.toList());
    }

    @Test
    void tripTomorrowAsksForPickup() {
        List<Reminder> rs = ReminderPlanner.plan(confirmed(TODAY.plusDays(1), 2, false), TODAY);
        assertEquals(List.of("TRIP_TOMORROW:42"), keys(rs));
        assertEquals("/pickup?tripId=42", rs.get(0).link());
        assertTrue(rs.get(0).title().contains("Goa"));
    }

    @Test
    void tripTodayPointsAtTheItinerary() {
        List<Reminder> rs = ReminderPlanner.plan(confirmed(TODAY, 2, false), TODAY);
        assertEquals(List.of("TRIP_TODAY:42"), keys(rs));
        assertEquals("/trip-summary?tripId=42", rs.get(0).link());
    }

    @Test
    void anywhereFromTwoToSevenDaysOutGivesOneWeekReminderWithTheRealCount() {
        for (int d = 2; d <= 7; d++) {
            List<Reminder> rs = ReminderPlanner.plan(confirmed(TODAY.plusDays(d), 2, false), TODAY);
            assertEquals(List.of("TRIP_WEEK:42"), keys(rs), "day " + d);
            assertTrue(rs.get(0).title().contains(d + " days"), rs.get(0).title());
        }
    }

    @Test
    void farAwayTripsAreLeftAlone() {
        assertTrue(ReminderPlanner.plan(confirmed(TODAY.plusDays(8), 2, false), TODAY).isEmpty());
        assertTrue(ReminderPlanner.plan(confirmed(TODAY.plusDays(60), 2, false), TODAY).isEmpty());
    }

    @Test
    void midTripThereIsNothingToSay() {
        // Started yesterday, lasts 3 days: today is day 2.
        assertTrue(ReminderPlanner.plan(confirmed(TODAY.minusDays(1), 3, false), TODAY).isEmpty());
    }

    @Test
    void reviewPromptComesAfterTheLastDayAndOnlyIfNotReviewed() {
        // 2-day trip that started 2 days ago ended yesterday.
        TripFacts t = confirmed(TODAY.minusDays(2), 2, false);
        assertEquals(List.of("REVIEW_PROMPT:42"), keys(ReminderPlanner.plan(t, TODAY)));
        assertTrue(ReminderPlanner.plan(confirmed(TODAY.minusDays(2), 2, true), TODAY).isEmpty());
        // On the last day itself: too early.
        assertTrue(ReminderPlanner.plan(confirmed(TODAY.minusDays(1), 2, false), TODAY).isEmpty());
        // Long after: stop asking.
        assertTrue(ReminderPlanner.plan(confirmed(TODAY.minusDays(40), 2, false), TODAY).isEmpty());
    }

    @Test
    void bookedWithoutADateIsNudgedAfterTwoDays() {
        TripFacts fresh = new TripFacts(7, "Pune", TripStatus.CONFIRMED, null, 1, TODAY.minusDays(1), false);
        TripFacts old = new TripFacts(7, "Pune", TripStatus.CONFIRMED, null, 1, TODAY.minusDays(2), false);
        assertTrue(ReminderPlanner.plan(fresh, TODAY).isEmpty());
        assertEquals(List.of("SET_DATE:7"), keys(ReminderPlanner.plan(old, TODAY)));
    }

    @Test
    void unpaidTripsGetOneNudgeInTheFirstWeek() {
        TripFacts day0 = new TripFacts(9, "Goa", TripStatus.PLANNED, null, 1, TODAY, false);
        TripFacts day1 = new TripFacts(9, "Goa", TripStatus.PAYMENT_PENDING, null, 1, TODAY.minusDays(1), false);
        TripFacts day8 = new TripFacts(9, "Goa", TripStatus.PLANNED, null, 1, TODAY.minusDays(8), false);
        assertTrue(ReminderPlanner.plan(day0, TODAY).isEmpty());
        assertEquals(List.of("PAY_NUDGE:9"), keys(ReminderPlanner.plan(day1, TODAY)));
        assertEquals("/payment?tripRequestId=9", ReminderPlanner.plan(day1, TODAY).get(0).link());
        assertTrue(ReminderPlanner.plan(day8, TODAY).isEmpty());
    }

    @Test
    void cancelledAndRequestedTripsNeverGetReminders() {
        assertTrue(ReminderPlanner.plan(new TripFacts(1, "Goa", TripStatus.CANCELLED, TODAY.plusDays(1), 1, TODAY.minusDays(5), false), TODAY).isEmpty());
        assertTrue(ReminderPlanner.plan(new TripFacts(1, "Goa", TripStatus.REQUESTED, null, 1, TODAY.minusDays(5), false), TODAY).isEmpty());
    }

    @Test
    void missingCityStillReadsNaturally() {
        TripFacts t = new TripFacts(5, null, TripStatus.CONFIRMED, TODAY.plusDays(1), 1, TODAY.minusDays(9), false);
        List<Reminder> rs = ReminderPlanner.plan(t, TODAY);
        assertEquals("Tomorrow: your trip", rs.get(0).title());
    }

    @Test
    void keysAreStablePerTripSoTheInboxCanDeduplicate() {
        List<Reminder> a = ReminderPlanner.plan(confirmed(TODAY.plusDays(1), 2, false), TODAY);
        List<Reminder> b = ReminderPlanner.plan(confirmed(TODAY.plusDays(1), 2, false), TODAY);
        assertEquals(keys(a), keys(b));
    }
}
