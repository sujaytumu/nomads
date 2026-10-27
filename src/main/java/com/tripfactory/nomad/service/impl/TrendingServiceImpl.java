package com.tripfactory.nomad.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tripfactory.nomad.api.dto.PlaceSearchResponse;
import com.tripfactory.nomad.api.dto.TrendingPlaceResponse;
import com.tripfactory.nomad.domain.entity.Place;
import com.tripfactory.nomad.repository.PlaceRepository;
import com.tripfactory.nomad.repository.TripPlanRepository;
import com.tripfactory.nomad.service.TrendingService;
import com.tripfactory.nomad.service.engine.TrendingEngine;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TrendingServiceImpl implements TrendingService {

    private static final int MAX_LIMIT = 20;

    private final TripPlanRepository tripPlanRepository;
    private final PlaceRepository placeRepository;

    @Value("${nomad.zone:Asia/Kolkata}")
    private String zone;

    @Override
    @Transactional(readOnly = true)
    public List<TrendingPlaceResponse> trending(int limit) {
        int n = Math.max(1, Math.min(limit, MAX_LIMIT));
        LocalDate today = LocalDate.now(ZoneId.of(zone));
        LocalDateTime since = today.minusDays(TrendingEngine.WINDOW_DAYS).atStartOfDay();

        List<TrendingEngine.Stop> stops = new ArrayList<>();
        for (Object[] row : tripPlanRepository.confirmedStopsSince(since)) {
            LocalDateTime created = (LocalDateTime) row[2];
            stops.add(new TrendingEngine.Stop((Long) row[0], (Long) row[1], created == null ? null : created.toLocalDate()));
        }
        List<TrendingEngine.Trending> ranked = TrendingEngine.rank(stops, today, n);
        if (ranked.isEmpty()) return List.of();

        Map<Long, Place> places = placeRepository.findAllById(ranked.stream().map(TrendingEngine.Trending::placeId).toList())
                .stream().collect(Collectors.toMap(Place::getId, Function.identity()));
        List<TrendingPlaceResponse> out = new ArrayList<>();
        for (TrendingEngine.Trending t : ranked) {
            Place p = places.get(t.placeId());
            if (p == null) continue;
            out.add(new TrendingPlaceResponse(new PlaceSearchResponse.Item(p.getId(), p.getName(), p.getCity(),
                    p.getLatitude() == null ? 0.0 : p.getLatitude(), p.getLongitude() == null ? 0.0 : p.getLongitude(),
                    p.getCategory(), p.getRating() == null ? 0.0 : p.getRating(), p.getImageUrl(), null), t.trips()));
        }
        return out;
    }
}
