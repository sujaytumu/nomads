package com.tripfactory.nomad.service.impl;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

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

    // OSRM's free public demo server (FOSSGIS-sponsored) - no API key, no signup,
    // no payment method ever required. Rate-limited to reasonable/non-commercial
    // use (~1 req/sec) with no uptime guarantee, which is fine for a demo app.
    // Only the "driving" profile is reliably supported on this shared server.
    private static final String OSRM_BASE_URL = "https://router.project-osrm.org/route/v1/driving/";

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

        String coordinates = plans.stream()
                .map(plan -> plan.getPlace().getLongitude() + "," + plan.getPlace().getLatitude())
                .collect(Collectors.joining(";"));

        String url = OSRM_BASE_URL + coordinates + "?geometries=geojson&overview=full";

        try {
            RequestEntity<Void> request = RequestEntity.get(URI.create(url)).build();
            ResponseEntity<String> response = restTemplate.exchange(request, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode routes = root.get("routes");
            if (routes == null || !routes.isArray() || routes.isEmpty()) {
                throw new BadRequestException("No routes returned from OSRM");
            }

            JsonNode geometry = routes.get(0).get("geometry");
            TripRoute tripRoute = existing != null ? existing : new TripRoute();
            tripRoute.setTripRequest(tripRequest);
            tripRoute.setDayNumber(day);
            tripRoute.setMode(mode);
            tripRoute.setGeoJson(geometry.toString());
            TripRoute saved = tripRouteRepository.save(tripRoute);
            return toResponse(saved);
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("Failed to fetch route from OSRM");
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