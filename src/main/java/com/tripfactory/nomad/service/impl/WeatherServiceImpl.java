package com.tripfactory.nomad.service.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripfactory.nomad.api.dto.WeatherForecastResponse;
import com.tripfactory.nomad.domain.entity.TripRequest;
import com.tripfactory.nomad.repository.TripRequestRepository;
import com.tripfactory.nomad.service.WeatherService;
import com.tripfactory.nomad.service.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

/**
 * Uses Open-Meteo (open-meteo.com) - a genuinely free, keyless weather API
 * with no signup and no rate-limit wall for non-commercial use. Unlike every
 * other integration in this app, there is no account, no API key, and no
 * trial credit to run out here.
 */
@Service
@RequiredArgsConstructor
public class WeatherServiceImpl implements WeatherService {

    private static final String OPEN_METEO_URL = "https://api.open-meteo.com/v1/forecast";

    // Central coordinates for the three cities this app actually has data for.
    // Matches the coordinates already used elsewhere (map view defaults, seed data).
    private static final Map<String, double[]> CITY_COORDINATES = Map.of(
            "Bengaluru", new double[] { 12.9716, 77.5946 },
            "Mumbai", new double[] { 19.0760, 72.8777 },
            "Delhi", new double[] { 28.6139, 77.2090 });

    private final TripRequestRepository tripRequestRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public WeatherForecastResponse getForecastForTrip(Long tripRequestId) {
        TripRequest tripRequest = tripRequestRepository.findById(tripRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        double[] coords = CITY_COORDINATES.getOrDefault(tripRequest.getCity(), CITY_COORDINATES.get("Bengaluru"));

        String url = String.format(
                "%s?latitude=%s&longitude=%s&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max,weathercode&timezone=auto&forecast_days=3",
                OPEN_METEO_URL, coords[0], coords[1]);

        WeatherForecastResponse response = new WeatherForecastResponse();
        response.setCity(tripRequest.getCity());
        response.setDays(new ArrayList<>());

        try {
            RequestEntity<Void> request = RequestEntity.get(URI.create(url)).build();
            ResponseEntity<String> raw = restTemplate.exchange(request, String.class);
            JsonNode root = objectMapper.readTree(raw.getBody());
            JsonNode daily = root.get("daily");
            if (daily == null) {
                return response;
            }

            JsonNode dates = daily.get("time");
            JsonNode maxTemps = daily.get("temperature_2m_max");
            JsonNode minTemps = daily.get("temperature_2m_min");
            JsonNode precipitation = daily.get("precipitation_probability_max");
            JsonNode codes = daily.get("weathercode");

            for (int i = 0; i < dates.size(); i++) {
                WeatherForecastResponse.DailyForecast day = new WeatherForecastResponse.DailyForecast();
                day.setDate(dates.get(i).asText());
                day.setMaxTempC(maxTemps.get(i).asDouble());
                day.setMinTempC(minTemps.get(i).asDouble());
                day.setPrecipitationChance(precipitation.get(i).asInt());
                day.setDescription(describeWeatherCode(codes.get(i).asInt()));
                response.getDays().add(day);
            }
        } catch (Exception ex) {
            // Weather is a nice-to-have, not core to booking a trip - fail
            // quietly with an empty forecast rather than breaking trip summary.
        }

        return response;
    }

    // Maps Open-Meteo's WMO weather codes to plain descriptions.
    // https://open-meteo.com/en/docs (see "WMO Weather interpretation codes")
    private String describeWeatherCode(int code) {
        if (code == 0) return "Clear sky";
        if (code <= 3) return "Partly cloudy";
        if (code == 45 || code == 48) return "Fog";
        if (code >= 51 && code <= 57) return "Drizzle";
        if (code >= 61 && code <= 67) return "Rain";
        if (code >= 71 && code <= 77) return "Snow";
        if (code >= 80 && code <= 82) return "Rain showers";
        if (code >= 95) return "Thunderstorm";
        return "Unknown";
    }
}
