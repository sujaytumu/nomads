package com.tripfactory.nomad.service.impl;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tripfactory.nomad.api.dto.TravelStatsResponse;
import com.tripfactory.nomad.domain.entity.TripPlan;
import com.tripfactory.nomad.domain.entity.TripRequest;
import com.tripfactory.nomad.domain.entity.User;
import com.tripfactory.nomad.domain.enums.InterestType;
import com.tripfactory.nomad.domain.enums.TripStatus;
import com.tripfactory.nomad.repository.ReviewRepository;
import com.tripfactory.nomad.repository.TripPlanRepository;
import com.tripfactory.nomad.repository.TripRequestRepository;
import com.tripfactory.nomad.repository.UserRepository;
import com.tripfactory.nomad.service.StatsService;
import com.tripfactory.nomad.service.engine.Achievements;
import com.tripfactory.nomad.service.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final UserRepository userRepository;
    private final TripRequestRepository tripRequestRepository;
    private final TripPlanRepository tripPlanRepository;
    private final ReviewRepository reviewRepository;

    @Override
    @Transactional(readOnly = true)
    public TravelStatsResponse statsFor(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        List<TripRequest> trips = tripRequestRepository.findByUserId(user.getId());

        int booked = 0, cancelled = 0, pending = 0, groupTrips = 0, reviews = 0;
        BigDecimal spent = BigDecimal.ZERO;
        double km = 0;
        Set<Long> places = new HashSet<>();
        Set<String> cities = new HashSet<>();
        Map<InterestType, Integer> byInterest = new EnumMap<>(InterestType.class);

        for (TripRequest trip : trips) {
            TripStatus status = trip.getStatus();
            if (status == TripStatus.CANCELLED) {
                cancelled++;
                continue;
            }
            if (status != TripStatus.CONFIRMED) {
                pending++;
                continue;
            }
            booked++;
            if (trip.getEstimatedCost() != null) {
                spent = spent.add(trip.getEstimatedCost());
            }
            if (trip.getGroup() != null) {
                groupTrips++;
            }
            if (trip.getCity() != null && !trip.getCity().isBlank()) {
                cities.add(trip.getCity().trim().toLowerCase(Locale.ROOT));
            }
            byInterest.merge(trip.getInterest(), 1, Integer::sum);
            reviews += reviewRepository.findByTripRequestId(trip.getId()).size();

            for (TripPlan plan : tripPlanRepository.findByTripRequestIdOrderByDayNumberAscStartTimeAsc(trip.getId())) {
                places.add(plan.getPlace().getId());
                if (plan.getDistanceFromPrevious() != null) {
                    km += plan.getDistanceFromPrevious();
                }
            }
        }

        double roundedKm = Math.round(km * 10.0) / 10.0;
        Achievements.Input input = new Achievements.Input(booked, places.size(), cities.size(), roundedKm,
                groupTrips, reviews, byInterest);
        List<Achievements.Badge> badges = Achievements.badges(input);
        int earned = (int) badges.stream().filter(Achievements.Badge::earned).count();

        // The badge the traveller is closest to earning (by share of the target already done).
        Achievements.Badge next = badges.stream()
                .filter(b -> !b.earned())
                .max(Comparator.comparingDouble((Achievements.Badge b) -> (double) b.current() / b.target()))
                .orElse(null);

        String favourite = byInterest.entrySet().stream()
                .max(Map.Entry.<InterestType, Integer>comparingByValue())
                .map(e -> e.getKey().name())
                .orElse(null);
        Map<String, Integer> byInterestOut = new LinkedHashMap<>();
        byInterest.forEach((k, v) -> byInterestOut.put(k.name(), v));

        return new TravelStatsResponse(booked, cancelled, pending, places.size(), cities.size(), roundedKm, spent,
                groupTrips, reviews, favourite, byInterestOut,
                Achievements.level(Achievements.xp(input)), earned, badges.size(), badges, next);
    }
}
