package com.tripfactory.nomad.service.impl;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripfactory.nomad.api.dto.TripRouteResponse;
import com.tripfactory.nomad.domain.entity.TripPlan;
import com.tripfactory.nomad.domain.entity.TripRequest;
import com.tripfactory.nomad.domain.entity.TripRoute;
import com.tripfactory.nomad.repository.TripPlanRepository;
import com.tripfactory.nomad.repository.TripRequestRepository;
import com.tripfactory.nomad.repository.TripRouteRepository;
import com.tripfactory.nomad.service.TripRouteService;
import com.tripfactory.nomad.service.exception.BadRequestException;
import com.tripfactory.nomad.service.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TripRouteServiceImpl implements TripRouteService {

    private final TripRouteRepository tripRouteRepository;
    private final TripRequestRepository tripRequestRepository;
    private final TripPlanRepository tripPlanRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${mapbox.access-token:}")
    private String mapboxAccessToken;

    @Override
    public TripRouteResponse getOrCreateRoute(Long tripRequestId, Integer dayNumber, String mode) {
        int day = dayNumber == null ? 1 : dayNumber;
        TripRoute existing = tripRouteRepository.findByTripRequestIdAndDayNumberAndModeIgnoreCase(tripRequestId, day,
                mode).orElse(null);
        if (existing != null) {
            return toResponse(existing);
        }

        TripRequest tripRequest = tripRequestRepository.findById(tripRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        List<TripPlan> plans = tripPlanRepository.findByTripRequestIdOrderByDayNumberAscStartTimeAsc(tripRequestId)
            .stream()
            .filter(plan -> plan.getDayNumber().equals(day))
            .collect(Collectors.toList());
        if (plans.size() < 2) {
            throw new BadRequestException("Not enough places to build a route");
        }

        if (mapboxAccessToken == null || mapboxAccessToken.isBlank()) {
            throw new BadRequestException("Mapbox access token not configured");
        }

        String coordinates = plans.stream()
                .map(plan -> plan.getPlace().getLongitude() + "," + plan.getPlace().getLatitude())
                .collect(Collectors.joining(";"));

        String url = String.format(
                "https://api.mapbox.com/directions/v5/mapbox/%s/%s?geometries=geojson&access_token=%s",
                mode, coordinates, mapboxAccessToken);

        try {
            RequestEntity<Void> request = RequestEntity.get(URI.create(url)).build();
            ResponseEntity<String> response = restTemplate.exchange(request, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode routes = root.get("routes");
            if (routes == null || !routes.isArray() || routes.isEmpty()) {
                throw new BadRequestException("No routes returned from Mapbox");
            }

            JsonNode geometry = routes.get(0).get("geometry");
            TripRoute tripRoute = existing != null ? existing : new TripRoute();
            tripRoute.setTripRequest(tripRequest);
            tripRoute.setDayNumber(day);
            tripRoute.setMode(mode);
            tripRoute.setGeoJson(geometry.toString());
            TripRoute saved = tripRouteRepository.save(tripRoute);
            return toResponse(saved);
        } catch (Exception ex) {
            throw new BadRequestException("Failed to fetch route from Mapbox");
        }
    }

    private TripRouteResponse toResponse(TripRoute route) {
        TripRouteResponse response = new TripRouteResponse();
        response.setTripRequestId(route.getTripRequest().getId());
        response.setDayNumber(route.getDayNumber());
        response.setMode(route.getMode());
        response.setGeoJson(route.getGeoJson());
        return response;
    }
}