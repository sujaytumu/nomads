package com.tripfactory.nomad.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tripfactory.nomad.domain.entity.TripPlan;
import com.tripfactory.nomad.domain.entity.TripRequest;
import com.tripfactory.nomad.domain.enums.TripStatus;
import com.tripfactory.nomad.repository.ReviewRepository;
import com.tripfactory.nomad.repository.TripPlanRepository;
import com.tripfactory.nomad.repository.TripRequestRepository;
import com.tripfactory.nomad.service.InboxService;
import com.tripfactory.nomad.service.TripReminderService;
import com.tripfactory.nomad.service.engine.ReminderPlanner;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TripReminderServiceImpl implements TripReminderService {

    private static final Logger log = LoggerFactory.getLogger(TripReminderServiceImpl.class);

    private final TripRequestRepository tripRequestRepository;
    private final TripPlanRepository tripPlanRepository;
    private final ReviewRepository reviewRepository;
    private final InboxService inboxService;
    private final com.tripfactory.nomad.service.WeatherService weatherService;

    @Override
    @Transactional(readOnly = true)
    public int runOnce(LocalDate today) {
        int added = 0;
        List<TripRequest> trips = tripRequestRepository.findByStatusIn(
                List.of(TripStatus.CONFIRMED, TripStatus.PLANNED, TripStatus.PAYMENT_PENDING));

        for (TripRequest trip : trips) {
            try {
                int lastDay = 1;
                boolean hasReview = false;
                if (trip.getStatus() == TripStatus.CONFIRMED && trip.getTripDate() != null) {
                    for (TripPlan plan : tripPlanRepository.findByTripRequestIdOrderByDayNumberAscStartTimeAsc(trip.getId())) {
                        if (plan.getDayNumber() != null) {
                            lastDay = Math.max(lastDay, plan.getDayNumber());
                        }
                    }
                    hasReview = !reviewRepository.findByTripRequestId(trip.getId()).isEmpty();
                }
                LocalDate createdOn = trip.getCreatedAt() == null ? null : trip.getCreatedAt().toLocalDate();
                ReminderPlanner.TripFacts facts = new ReminderPlanner.TripFacts(trip.getId(), trip.getCity(),
                        trip.getStatus(), trip.getTripDate(), lastDay, createdOn, hasReview);

                List<ReminderPlanner.Reminder> due = new java.util.ArrayList<>(ReminderPlanner.plan(facts, today));
                ReminderPlanner.Reminder wet = weatherReminder(trip, facts, lastDay, today);
                if (wet != null) {
                    due.add(wet);
                }
                for (ReminderPlanner.Reminder r : due) {
                    if (inboxService.pushOnce(trip.getUser().getId(), r.dedupeKey(), r.type(), r.title(), r.body(), r.link())) {
                        added++;
                    }
                }
            } catch (Exception ex) {
                // One bad trip must not stop everyone else's reminders.
                log.warn("Reminder check failed for trip {}: {}", trip.getId(), ex.getMessage());
            }
        }
        if (added > 0) {
            log.info("Trip reminders: {} new notification(s) added", added);
        }
        return added;
    }

    /** Only trips starting within the next few days are checked, so the forecast service is not called for every trip. */
    private ReminderPlanner.Reminder weatherReminder(TripRequest trip, ReminderPlanner.TripFacts facts, int lastDay, LocalDate today) {
        if (trip.getStatus() != TripStatus.CONFIRMED || trip.getTripDate() == null) {
            return null;
        }
        long daysTo = java.time.temporal.ChronoUnit.DAYS.between(today, trip.getTripDate());
        if (daysTo < 0 || daysTo > 3) {
            return null;
        }
        try {
            com.tripfactory.nomad.api.dto.WeatherForecastResponse w = weatherService.getForecastForTrip(trip.getId());
            if (w == null || w.getDays() == null) {
                return null;
            }
            List<com.tripfactory.nomad.service.engine.ForecastAlign.Day> forecast = new java.util.ArrayList<>();
            w.getDays().forEach(d -> forecast.add(new com.tripfactory.nomad.service.engine.ForecastAlign.Day(d.getDate(), d.getPrecipitationChance())));
            return ReminderPlanner.weatherAlert(facts,
                    com.tripfactory.nomad.service.engine.ForecastAlign.rainByTripDay(trip.getTripDate(), forecast, lastDay), today);
        } catch (Exception ex) {
            log.debug("No weather check for trip {}: {}", trip.getId(), ex.getMessage());
            return null;
        }
    }
}
