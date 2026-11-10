package com.tripfactory.nomad.service.engine;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import com.tripfactory.nomad.domain.enums.TripStatus;

/**
 * Decides which reminders a trip needs today. Pure logic: the caller supplies "today", so it is easy to test.
 *
 * <p>Every reminder has a dedupe key such as {@code TRIP_TOMORROW:42}, and the inbox stores each key only once.
 * That is why the rules use windows (for example "2 to 7 days before") instead of exact days: if the server was
 * asleep on the exact day, the reminder still goes out the next time the job runs, and never twice.
 */
public final class ReminderPlanner {

    private ReminderPlanner() {
    }

    /** What the planner needs to know about one trip. {@code lastDay} is the number of days the itinerary spans (at least 1). */
    public record TripFacts(long tripId, String city, TripStatus status, LocalDate tripDate, int lastDay,
                            LocalDate createdOn, boolean hasReview) {
    }

    public record Reminder(String dedupeKey, String type, String title, String body, String link) {
    }

    public static List<Reminder> plan(TripFacts t, LocalDate today) {
        List<Reminder> out = new ArrayList<>();
        String where = t.city() == null || t.city().isBlank() ? "your weekend trip" : "your " + t.city() + " trip";
        String id = String.valueOf(t.tripId());

        if (t.status() == TripStatus.CONFIRMED) {
            if (t.tripDate() != null) {
                long daysTo = ChronoUnit.DAYS.between(today, t.tripDate());
                LocalDate endDate = t.tripDate().plusDays(Math.max(1, t.lastDay()) - 1L);
                long sinceEnd = ChronoUnit.DAYS.between(endDate, today);

                if (daysTo == 0) {
                    out.add(new Reminder("TRIP_TODAY:" + id, "TRIP_TODAY", "Today's the day 🌄",
                            "Your trip starts today. Open your itinerary for the stops and directions.",
                            "/trip-summary?tripId=" + id));
                } else if (daysTo == 1) {
                    out.add(new Reminder("TRIP_TOMORROW:" + id, "TRIP_TOMORROW", "Tomorrow: " + (t.city() == null ? "your trip" : t.city()),
                            "Check your pickup and pack your bag. " + cap(where) + " starts tomorrow.",
                            "/pickup?tripId=" + id));
                } else if (daysTo >= 2 && daysTo <= 7) {
                    out.add(new Reminder("TRIP_WEEK:" + id, "TRIP_WEEK", cap(where) + " is in " + daysTo + " days",
                            "Time to look over the itinerary and arrange your pickup.",
                            "/trip-summary?tripId=" + id));
                }

                if (sinceEnd >= 1 && sinceEnd <= 14 && !t.hasReview()) {
                    out.add(new Reminder("REVIEW_PROMPT:" + id, "REVIEW_PROMPT", "How was " + (t.city() == null ? "your trip" : t.city()) + "?",
                            "Rate your trip. It takes ten seconds and earns you XP.",
                            "/review?tripId=" + id));
                }
            } else if (t.createdOn() != null && ChronoUnit.DAYS.between(t.createdOn(), today) >= 2) {
                out.add(new Reminder("SET_DATE:" + id, "SET_DATE", "Pick your trip date",
                        cap(where) + " is booked. Choose a date and we will count down and remind you.",
                        "/bookings"));
            }
        } else if (t.status() == TripStatus.PLANNED || t.status() == TripStatus.PAYMENT_PENDING) {
            if (t.createdOn() != null) {
                long age = ChronoUnit.DAYS.between(t.createdOn(), today);
                if (age >= 1 && age <= 7) {
                    out.add(new Reminder("PAY_NUDGE:" + id, "PAY_NUDGE", cap(where) + " is waiting",
                            "Your itinerary is ready. Pay to lock it in.",
                            "/payment?tripRequestId=" + id));
                }
            }
        }
        return out;
    }

    /** Rain chance (percent) at which a booked trip gets a heads-up. */
    public static final int WEATHER_ALERT_CHANCE = 70;

    /**
     * A one-off heads-up when the forecast for a booked trip's own days turns wet. Called only when a forecast
     * covers the trip. The key includes the trip date, so changing the date can warn again, but a trip never
     * gets the same warning twice.
     *
     * @param rainByTripDay trip day (1-based) to chance of rain, only for days the forecast covers
     */
    public static Reminder weatherAlert(TripFacts t, java.util.Map<Integer, Integer> rainByTripDay, LocalDate today) {
        if (t.status() != TripStatus.CONFIRMED || t.tripDate() == null || rainByTripDay == null || rainByTripDay.isEmpty()) {
            return null;
        }
        long daysTo = ChronoUnit.DAYS.between(today, t.tripDate());
        if (daysTo < 0 || daysTo > 3) return null;
        int worstDay = -1;
        int worst = -1;
        for (java.util.Map.Entry<Integer, Integer> e : new java.util.TreeMap<>(rainByTripDay).entrySet()) {
            if (e.getValue() != null && e.getValue() > worst) {
                worst = e.getValue();
                worstDay = e.getKey();
            }
        }
        if (worst < WEATHER_ALERT_CHANCE) return null;
        String id = String.valueOf(t.tripId());
        String day = t.tripDate().plusDays(worstDay - 1L).getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH);
        String where = t.city() == null || t.city().isBlank() ? "your trip" : "your " + t.city() + " trip";
        return new Reminder("WEATHER_ALERT:" + id + ":" + t.tripDate(), "WEATHER_ALERT",
                "Rain likely on " + day + " (" + worst + "%)",
                "Heads-up for " + where + ". Open Plan B for indoor backups to your outdoor stops.",
                "/trip-summary?tripId=" + id);
    }

    private static String cap(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
