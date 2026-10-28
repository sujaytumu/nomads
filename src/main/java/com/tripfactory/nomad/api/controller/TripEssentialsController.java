package com.tripfactory.nomad.api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tripfactory.nomad.api.dto.TripEssentialsResponse;
import com.tripfactory.nomad.domain.entity.TripPlan;
import com.tripfactory.nomad.domain.entity.TripRequest;
import com.tripfactory.nomad.repository.TripPlanRepository;
import com.tripfactory.nomad.repository.TripRequestRepository;
import com.tripfactory.nomad.service.engine.EssentialsCatalog;
import com.tripfactory.nomad.service.engine.EssentialsCatalog.Item;
import com.tripfactory.nomad.service.engine.EssentialsCatalog.Kind;
import com.tripfactory.nomad.service.engine.EssentialsPlanner;
import com.tripfactory.nomad.service.engine.EssentialsPlanner.Point;
import com.tripfactory.nomad.service.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

/** Stay areas, scenic spots and hospitals near a trip. */
@RestController
@RequestMapping("/api/trips/{tripId}/essentials")
@RequiredArgsConstructor
public class TripEssentialsController {

    private static final List<Item> CATALOG = EssentialsCatalog.items();

    private final TripRequestRepository tripRequestRepository;
    private final TripPlanRepository tripPlanRepository;

    @GetMapping
    @PreAuthorize("@authz.canAccessTrip(#tripId)")
    @Transactional(readOnly = true)
    public ResponseEntity<TripEssentialsResponse> get(@PathVariable Long tripId) {
        TripRequest trip = tripRequestRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        List<Point> stops = tripPlanRepository.findByTripRequestIdOrderByDayNumberAscStartTimeAsc(tripId).stream()
                .map(TripPlan::getPlace)
                .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
                .map(p -> new Point(p.getLatitude(), p.getLongitude()))
                .toList();
        Point home = trip.getUser().getLatitude() != null && trip.getUser().getLongitude() != null
                ? new Point(trip.getUser().getLatitude(), trip.getUser().getLongitude())
                : null;
        String city = trip.getCity();

        // Without stops or a home location there is nothing to measure from: use the city's own catalog centre.
        Point fallback = home != null ? home : cityCentre(city);
        if (fallback == null) {
            return ResponseEntity.ok(new TripEssentialsResponse(city, List.of(), List.of(), List.of()));
        }
        Point centre = EssentialsPlanner.centre(stops, fallback);

        var stay = EssentialsPlanner.stays(CATALOG, city, stops, centre, 4).stream()
                .map(p -> new TripEssentialsResponse.Stay(p.item().name(), p.item().extra1(), p.item().price(),
                        round(p.avgKmToStops()), p.item().note()))
                .toList();
        var scenic = EssentialsPlanner.nearest(CATALOG, city, Kind.SCENIC, stops, centre, 5).stream()
                .map(p -> new TripEssentialsResponse.Scenic(p.item().name(), p.item().extra1(),
                        round(p.kmFromTrip()), p.item().note()))
                .toList();
        var medical = EssentialsPlanner.nearest(CATALOG, city, Kind.MEDICAL, stops, centre, 4).stream()
                .map(p -> new TripEssentialsResponse.Medical(p.item().name(), p.item().extra1(),
                        round(p.kmFromTrip()), p.item().note()))
                .toList();
        return ResponseEntity.ok(new TripEssentialsResponse(city, stay, scenic, medical));
    }

    private static Point cityCentre(String city) {
        List<Item> inCity = CATALOG.stream().filter(i -> i.city().equalsIgnoreCase(city)).toList();
        if (inCity.isEmpty()) {
            return null;
        }
        return EssentialsPlanner.centre(inCity.stream().map(i -> new Point(i.lat(), i.lon())).toList(), null);
    }

    private static double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
