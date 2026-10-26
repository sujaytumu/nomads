package com.tripfactory.nomad.service.impl;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tripfactory.nomad.api.dto.RecommendedPlaceResponse;
import com.tripfactory.nomad.domain.entity.Place;
import com.tripfactory.nomad.domain.entity.TripPlan;
import com.tripfactory.nomad.domain.entity.TripRequest;
import com.tripfactory.nomad.domain.entity.User;
import com.tripfactory.nomad.domain.enums.InterestType;
import com.tripfactory.nomad.domain.enums.TripStatus;
import com.tripfactory.nomad.repository.PlaceRepository;
import com.tripfactory.nomad.repository.TripPlanRepository;
import com.tripfactory.nomad.repository.TripRequestRepository;
import com.tripfactory.nomad.repository.UserRepository;
import com.tripfactory.nomad.service.RecommendationService;
import com.tripfactory.nomad.service.engine.PlaceScorer;
import com.tripfactory.nomad.service.exception.ResourceNotFoundException;
import com.tripfactory.nomad.service.util.GeoUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    static final int MAX_LIMIT = 30;

    private final UserRepository userRepository;
    private final PlaceRepository placeRepository;
    private final TripRequestRepository tripRequestRepository;
    private final TripPlanRepository tripPlanRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RecommendedPlaceResponse> recommendFor(String email, int limit, InterestType interest, Double maxKm) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        int safeLimit = Math.max(1, Math.min(limit <= 0 ? 10 : limit, MAX_LIMIT));

        // What the traveller has actually done: interests booked and places already seen.
        Map<InterestType, Integer> history = new EnumMap<>(InterestType.class);
        Set<Long> visited = new HashSet<>();
        for (TripRequest trip : tripRequestRepository.findByUserId(user.getId())) {
            if (trip.getStatus() != TripStatus.CONFIRMED) continue;
            history.merge(trip.getInterest(), 1, Integer::sum);
            for (TripPlan plan : tripPlanRepository.findByTripRequestIdOrderByDayNumberAscStartTimeAsc(trip.getId())) {
                visited.add(plan.getPlace().getId());
            }
        }

        List<PlaceScorer.Candidate> candidates = new ArrayList<>();
        for (Place p : placeRepository.findAll()) {
            if (interest != null && p.getCategory() != interest) continue;
            if (maxKm != null && GeoUtils.haversineKm(user.getLatitude(), user.getLongitude(), p.getLatitude(), p.getLongitude()) > maxKm) continue;
            candidates.add(new PlaceScorer.Candidate(p.getId(), p.getName(), p.getCity(), p.getLatitude(), p.getLongitude(),
                    p.getCategory(), p.getRating() == null ? 0.0 : p.getRating(), p.getImageUrl(), visited.contains(p.getId())));
        }

        PlaceScorer.Taste taste = new PlaceScorer.Taste(user.getInterestType(), history, user.getLatitude(), user.getLongitude());
        List<RecommendedPlaceResponse> out = new ArrayList<>();
        for (PlaceScorer.Scored s : PlaceScorer.rank(candidates, taste, safeLimit)) {
            PlaceScorer.Candidate c = s.place();
            out.add(new RecommendedPlaceResponse(c.id(), c.name(), c.city(), c.latitude(), c.longitude(), c.category(),
                    c.rating(), c.imageUrl(), s.distanceKm(), s.score(), s.reasons()));
        }
        return out;
    }
}
