package com.tripfactory.nomad.api.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.tripfactory.nomad.domain.entity.Place;
import com.tripfactory.nomad.domain.entity.TripPlan;
import com.tripfactory.nomad.domain.entity.TripRequest;
import com.tripfactory.nomad.repository.PlaceRepository;
import com.tripfactory.nomad.repository.TripPlanRepository;
import com.tripfactory.nomad.repository.TripRequestRepository;
import com.tripfactory.nomad.service.WeatherService;
import com.tripfactory.nomad.service.engine.AlternatesEngine;
import com.tripfactory.nomad.service.engine.ForecastAlign;
import com.tripfactory.nomad.service.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

/** Plan B for each stop of a trip: indoor backups when rain is likely, like-for-like swaps otherwise. */
@RestController
@RequestMapping("/api/trips/{tripId}/alternates")
@RequiredArgsConstructor
public class TripAlternatesController {

    private static final Logger log = LoggerFactory.getLogger(TripAlternatesController.class);

    private final TripRequestRepository tripRequestRepository;
    private final TripPlanRepository tripPlanRepository;
    private final PlaceRepository placeRepository;
    private final WeatherService weatherService;

    @GetMapping
    @PreAuthorize("@authz.canAccessTrip(#tripId)")
    @Transactional(readOnly = true)
    public ResponseEntity<AlternatesEngine.Result> get(@PathVariable Long tripId) {
        TripRequest trip = tripRequestRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        List<TripPlan> plans = tripPlanRepository.findByTripRequestIdOrderByDayNumberAscStartTimeAsc(tripId);

        List<AlternatesEngine.Stop> stops = new ArrayList<>();
        for (TripPlan p : plans) {
            Place pl = p.getPlace();
            if (pl == null || pl.getLatitude() == null || pl.getLongitude() == null || pl.getCategory() == null) continue;
            stops.add(new AlternatesEngine.Stop(p.getDayNumber(), spot(pl)));
        }

        List<AlternatesEngine.Spot> candidates = new ArrayList<>();
        if (trip.getCity() != null) {
            for (Place pl : placeRepository.findByCityIgnoreCase(trip.getCity())) {
                if (pl.getLatitude() == null || pl.getLongitude() == null || pl.getCategory() == null) continue;
                candidates.add(spot(pl));
            }
        }

        // The forecast is an extra, and it only covers the next few days: it is matched to the trip's own dates.
        // Without a forecast for the trip days, no day is called rainy and the screen says the forecast is unknown.
        int tripDays = Math.max(1, plans.stream().map(TripPlan::getDayNumber).filter(java.util.Objects::nonNull).max(Integer::compare).orElse(1));
        Map<Integer, Integer> rain = new HashMap<>();
        try {
            WeatherForecastResponse w = weatherService.getForecastForTrip(tripId);
            if (w != null && w.getDays() != null) {
                List<ForecastAlign.Day> forecast = new ArrayList<>();
                w.getDays().forEach(d -> forecast.add(new ForecastAlign.Day(d.getDate(), d.getPrecipitationChance())));
                rain = ForecastAlign.rainByTripDay(trip.getTripDate(), forecast, tripDays);
            }
        } catch (Exception ex) {
            log.debug("No forecast for trip {} alternates: {}", tripId, ex.getMessage());
        }
        return ResponseEntity.ok(AlternatesEngine.compute(stops, candidates, rain));
    }

    private static AlternatesEngine.Spot spot(Place pl) {
        return new AlternatesEngine.Spot(pl.getId(), pl.getName(), pl.getCategory().name(),
                pl.getRating() == null ? 0.0 : pl.getRating(), pl.getLatitude(), pl.getLongitude());
    }
}
