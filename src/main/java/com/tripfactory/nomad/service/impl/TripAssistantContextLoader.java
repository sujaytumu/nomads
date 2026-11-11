package com.tripfactory.nomad.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tripfactory.nomad.api.dto.WeatherForecastResponse;
import com.tripfactory.nomad.domain.entity.TripPlan;
import com.tripfactory.nomad.domain.entity.TripRequest;
import com.tripfactory.nomad.repository.TripPlanRepository;
import com.tripfactory.nomad.repository.TripRequestRepository;
import com.tripfactory.nomad.service.WeatherService;
import com.tripfactory.nomad.service.engine.TripAssistant;
import com.tripfactory.nomad.service.exception.ResourceNotFoundException;

/** Reads the trip from the database in its own short transaction, so the slow AI call happens outside it. */
@Component
public class TripAssistantContextLoader {

    private static final Logger log = LoggerFactory.getLogger(TripAssistantContextLoader.class);

    private final TripRequestRepository tripRequestRepository;
    private final TripPlanRepository tripPlanRepository;
    private final WeatherService weatherService;

    public TripAssistantContextLoader(TripRequestRepository tripRequestRepository, TripPlanRepository tripPlanRepository,
            WeatherService weatherService) {
        this.tripRequestRepository = tripRequestRepository;
        this.tripPlanRepository = tripPlanRepository;
        this.weatherService = weatherService;
    }

    @Transactional(readOnly = true)
    public TripAssistant.Trip load(Long tripId) {
        TripRequest trip = tripRequestRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        List<TripPlan> plans = tripPlanRepository.findByTripRequestIdOrderByDayNumberAscStartTimeAsc(tripId);
        List<TripAssistant.Stop> stops = new ArrayList<>();
        for (TripPlan p : plans) {
            stops.add(new TripAssistant.Stop(
                    p.getDayNumber() == null ? 1 : p.getDayNumber(),
                    p.getPlace().getName(),
                    p.getPlace().getCategory().name(),
                    p.getStartTime() == null ? null : p.getStartTime().toString(),
                    p.getDistanceFromPrevious() == null ? 0 : p.getDistanceFromPrevious()));
        }
        int days = trip.getTripDays() != null ? trip.getTripDays()
                : (trip.getWeekendType() != null && "TWO_DAY".equals(trip.getWeekendType().name()) ? 2 : 1);

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
            log.debug("No forecast for assistant context of trip {}: {}", tripId, ex.getMessage());
        }

        return new TripAssistant.Trip(
                trip.getCity(),
                days,
                trip.getTripDate() == null ? null : trip.getTripDate().toString(),
                trip.getRequestedGroupSize() == null ? 1 : trip.getRequestedGroupSize(),
                trip.getTravelMode() == null ? null : trip.getTravelMode().name(),
                trip.getEstimatedCost() == null ? null : trip.getEstimatedCost().toPlainString(),
                stops,
                rain);
    }
}
