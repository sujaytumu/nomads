package com.tripfactory.nomad.api.controller;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tripfactory.nomad.api.dto.WeatherForecastResponse;
import com.tripfactory.nomad.domain.entity.TripPlan;
import com.tripfactory.nomad.domain.entity.TripRequest;
import com.tripfactory.nomad.repository.TripPlanRepository;
import com.tripfactory.nomad.repository.TripRequestRepository;
import com.tripfactory.nomad.service.WeatherService;
import com.tripfactory.nomad.service.engine.TripHealthScorer;
import com.tripfactory.nomad.service.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

/** A 0-100 health score for a trip plan, with the reasons and tips. */
@RestController
@RequestMapping("/api/trips/{tripId}/health")
@RequiredArgsConstructor
public class TripHealthController {

    private static final Logger log = LoggerFactory.getLogger(TripHealthController.class);

    private final TripRequestRepository tripRequestRepository;
    private final TripPlanRepository tripPlanRepository;
    private final WeatherService weatherService;

    @GetMapping
    @PreAuthorize("@authz.canAccessTrip(#tripId)")
    @Transactional(readOnly = true)
    public ResponseEntity<TripHealthScorer.Result> get(@PathVariable Long tripId) {
        TripRequest trip = tripRequestRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        List<TripPlan> plans = tripPlanRepository.findByTripRequestIdOrderByDayNumberAscStartTimeAsc(tripId);
        List<TripHealthScorer.Stop> stops = new ArrayList<>();
        for (TripPlan p : plans) {
            stops.add(new TripHealthScorer.Stop(p.getDayNumber(), p.getPlace().getCategory().name(),
                    p.getPlace().getRating() == null ? 0 : p.getPlace().getRating(),
                    p.getDistanceFromPrevious() == null ? 0 : p.getDistanceFromPrevious(), p.getPlace().getName()));
        }
        int days = trip.getTripDays() != null ? trip.getTripDays()
                : (trip.getWeekendType() != null && "TWO_DAY".equals(trip.getWeekendType().name()) ? 2 : 1);

        // The forecast is a nice extra. If it can't be fetched, the score is worked out without it.
        List<Integer> rain = new ArrayList<>();
        try {
            WeatherForecastResponse w = weatherService.getForecastForTrip(tripId);
            if (w != null && w.getDays() != null) {
                w.getDays().forEach(d -> {
                    if (d.getPrecipitationChance() != null) {
                        rain.add(d.getPrecipitationChance());
                    }
                });
            }
        } catch (Exception ex) {
            log.debug("No forecast for trip {} health score: {}", tripId, ex.getMessage());
        }
        return ResponseEntity.ok(TripHealthScorer.score(days, stops, rain));
    }
}
