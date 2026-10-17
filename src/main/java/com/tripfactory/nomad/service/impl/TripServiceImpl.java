package com.tripfactory.nomad.service.impl;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tripfactory.nomad.api.dto.ItineraryOptionResponse;
import com.tripfactory.nomad.api.dto.ItineraryOptionsResponse;
import com.tripfactory.nomad.api.dto.TripCreateRequest;
import com.tripfactory.nomad.api.dto.TripPlanItemResponse;
import com.tripfactory.nomad.api.dto.TripResponse;
import com.tripfactory.nomad.domain.entity.Place;
import com.tripfactory.nomad.domain.entity.TripGroup;
import com.tripfactory.nomad.domain.entity.TripPlan;
import com.tripfactory.nomad.domain.entity.TripRequest;
import com.tripfactory.nomad.domain.entity.User;
import com.tripfactory.nomad.domain.enums.GroupStatus;
import com.tripfactory.nomad.domain.enums.TravelMode;
import com.tripfactory.nomad.domain.enums.TripStatus;
import com.tripfactory.nomad.domain.enums.WeekendType;
import com.tripfactory.nomad.repository.PlaceRepository;
import com.tripfactory.nomad.repository.TripGroupRepository;
import com.tripfactory.nomad.repository.TripPlanRepository;
import com.tripfactory.nomad.repository.TripRouteRepository;
import com.tripfactory.nomad.repository.TripRequestRepository;
import com.tripfactory.nomad.repository.UserRepository;
import com.tripfactory.nomad.service.NotificationService;
import com.tripfactory.nomad.service.TripService;
import com.tripfactory.nomad.service.exception.BadRequestException;
import com.tripfactory.nomad.service.exception.ResourceNotFoundException;
import com.tripfactory.nomad.service.util.GeoUtils;
import com.tripfactory.nomad.service.util.CityAliasResolver;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {

    private static final BigDecimal BASE_COST_PER_PLACE = new BigDecimal("500");
    private static final BigDecimal PICKUP_COST = new BigDecimal("300");
    private static final int MIN_PLACES = 1;
    private static final int MAX_PLACES_PER_DAY = 4;

    private final UserRepository userRepository;
    private final PlaceRepository placeRepository;
    private final TripGroupRepository tripGroupRepository;
    private final TripRequestRepository tripRequestRepository;
    private final TripPlanRepository tripPlanRepository;
    private final TripRouteRepository tripRouteRepository;
    private final ItineraryEmailService itineraryEmailService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public TripResponse createTrip(TripCreateRequest request) {
        if (request.getUserId() == null) {
            throw new BadRequestException("userId is required");
        }
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getCity() != null && user.getCity() != null
                && !request.getCity().equalsIgnoreCase(user.getCity())) {
            throw new BadRequestException("Trips are limited to the user's city only");
        }

        String city = CityAliasResolver.canonicalize(user.getCity());
        Double userLat = request.getUserLatitude() != null ? request.getUserLatitude() : user.getLatitude();
        Double userLon = request.getUserLongitude() != null ? request.getUserLongitude() : user.getLongitude();

        if (city == null || userLat == null || userLon == null) {
            throw new BadRequestException("City and user coordinates are required");
        }

        TripRequest tripRequest = new TripRequest();
        tripRequest.setUser(user);
        tripRequest.setCity(city);
        tripRequest.setWeekendType(Objects.requireNonNullElse(request.getWeekendType(), WeekendType.ONE_DAY));
        tripRequest.setInterest(Objects.requireNonNullElse(request.getInterest(), user.getInterestType()));
        tripRequest.setTravelMode(Objects.requireNonNullElseGet(request.getTravelMode(),
            () -> com.tripfactory.nomad.domain.enums.TravelMode.valueOf(user.getTravelPreference().name())));
        tripRequest.setPickupRequired(Boolean.TRUE.equals(request.getPickupRequired()));
        tripRequest.setRequestedGroupSize(request.getGroupSize());
        if (request.getTripDate() != null) {
            requireFutureDate(request.getTripDate());
            tripRequest.setTripDate(request.getTripDate());
        }
        tripRequest.setStatus(TripStatus.REQUESTED);

        if (tripRequest.getTravelMode() == TravelMode.GROUP) {
            TripGroup group = assignGroup(city, tripRequest.getInterest(), tripRequest.getWeekendType());
            tripRequest.setGroup(group);
        }

        TripRequest savedRequest = tripRequestRepository.save(tripRequest);

        List<Place> places = placeRepository.findByCityIgnoreCase(city);
        List<Place> rankedPlaces = rankPlaces(places, userLat, userLon, request.getInterest());
        if (rankedPlaces.isEmpty()) {
            throw new BadRequestException("No places found for the city");
        }

        int totalSlots = savedRequest.getWeekendType() == WeekendType.TWO_DAY ? 6 : 4;
        int dayCount = savedRequest.getWeekendType() == WeekendType.TWO_DAY ? 2 : 1;
        int perDay = (int) Math.ceil((double) totalSlots / dayCount);

        List<TripPlan> plans = buildPlans(savedRequest, rankedPlaces, totalSlots, perDay);
        tripPlanRepository.saveAll(plans);

        savedRequest.setStatus(TripStatus.PLANNED);
        savedRequest.setEstimatedCost(estimateCost(plans.size(), Boolean.TRUE.equals(request.getPickupRequired())));
        TripRequest updated = tripRequestRepository.save(savedRequest);

        TripResponse response = toResponse(updated, plans);
        response.setEstimatedCost(updated.getEstimatedCost());

        if (user.getPhoneNumber() != null && !user.getPhoneNumber().isBlank()) {
            notificationService.sendSms(user.getPhoneNumber(),
                "NOMAD: Trip planned for " + updated.getCity() + ". Trip ID: " + updated.getId());
        }
        return response;
    }

    @Override
    public TripResponse getTrip(Long tripRequestId) {
        TripRequest tripRequest = tripRequestRepository.findById(tripRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        List<TripPlan> plans = tripPlanRepository.findByTripRequestIdOrderByDayNumberAscStartTimeAsc(tripRequestId);
        TripResponse response = toResponse(tripRequest, plans);
        response.setEstimatedCost(effectiveCost(tripRequest, plans));
        return response;
    }

    @Override
    public List<TripResponse> getTripsByUser(Long userId) {
        return tripRequestRepository.findByUserId(userId).stream()
                .map(tripRequest -> {
                    List<TripPlan> plans = tripPlanRepository
                            .findByTripRequestIdOrderByDayNumberAscStartTimeAsc(tripRequest.getId());
                        TripResponse response = toResponse(tripRequest, plans);
                        response.setEstimatedCost(effectiveCost(tripRequest, plans));
                        return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ItineraryOptionsResponse getItineraryOptions(Long tripRequestId) {
        TripRequest trip = findTrip(tripRequestId);
        List<TripPlan> plans = tripPlanRepository.findByTripRequestIdOrderByDayNumberAscStartTimeAsc(tripRequestId);

        ItineraryOptionsResponse response = new ItineraryOptionsResponse();
        response.setTripRequestId(trip.getId());
        response.setMinPlaces(MIN_PLACES);
        response.setMaxPlaces(dayCount(trip) * MAX_PLACES_PER_DAY);
        response.setCostPerPlace(BASE_COST_PER_PLACE);
        response.setPickupCost(PICKUP_COST);
        response.setPickupRequired(Boolean.TRUE.equals(trip.getPickupRequired()));

        // Once payment has started the price is fixed, and package trips have
        // no day-by-day itinerary at all - so there is nothing to choose.
        boolean editable = trip.getStatus() == TripStatus.PLANNED && !plans.isEmpty();
        response.setEditable(editable);
        if (!editable) {
            response.setOptions(List.of());
            return response;
        }

        Set<Long> selected = plans.stream()
                .map(plan -> plan.getPlace().getId())
                .collect(Collectors.toSet());
        User user = trip.getUser();
        boolean hasHome = user.getLatitude() != null && user.getLongitude() != null;
        double homeLat = hasHome ? user.getLatitude() : 0d;
        double homeLon = hasHome ? user.getLongitude() : 0d;

        List<Place> ranked = rankPlaces(placeRepository.findByCityIgnoreCase(trip.getCity()), homeLat, homeLon,
                trip.getInterest());
        List<ItineraryOptionResponse> options = ranked.stream().map(place -> {
            ItineraryOptionResponse option = new ItineraryOptionResponse();
            option.setPlaceId(place.getId());
            option.setName(place.getName());
            option.setCategory(place.getCategory());
            option.setRating(place.getRating());
            option.setImageUrl(place.getImageUrl());
            if (hasHome) {
                option.setDistanceFromHomeKm(
                        GeoUtils.haversineKm(homeLat, homeLon, place.getLatitude(), place.getLongitude()));
            }
            option.setSelected(selected.contains(place.getId()));
            return option;
        }).collect(Collectors.toList());
        response.setOptions(options);
        return response;
    }

    @Override
    @Transactional
    public TripResponse updateItinerary(Long tripRequestId, List<Long> placeIds) {
        TripRequest trip = findTrip(tripRequestId);

        // Changing the stops changes the price. Once an order exists for the
        // old price it could still be paid, so only unpaid, not-yet-started
        // trips can be edited.
        if (trip.getStatus() != TripStatus.PLANNED) {
            throw new BadRequestException("The itinerary can only be changed before payment has started");
        }
        if (tripPlanRepository.findByTripRequestIdOrderByDayNumberAscStartTimeAsc(tripRequestId).isEmpty()) {
            throw new BadRequestException("This trip has no day-by-day itinerary to change");
        }

        List<Long> ids = placeIds == null
                ? List.of()
                : placeIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        int max = dayCount(trip) * MAX_PLACES_PER_DAY;
        if (ids.size() < MIN_PLACES) {
            throw new BadRequestException("Choose at least " + MIN_PLACES + " place");
        }
        if (ids.size() > max) {
            throw new BadRequestException("You can choose at most " + max + " places for this trip");
        }

        Map<Long, Place> found = placeRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Place::getId, place -> place));
        List<Place> chosen = new ArrayList<>();
        for (Long id : ids) {
            Place place = found.get(id);
            if (place == null) {
                throw new BadRequestException("Place " + id + " does not exist");
            }
            if (trip.getCity() == null || !trip.getCity().equalsIgnoreCase(place.getCity())) {
                throw new BadRequestException(place.getName() + " is not in " + trip.getCity());
            }
            chosen.add(place);
        }

        User user = trip.getUser();
        if (user.getLatitude() == null || user.getLongitude() == null) {
            throw new BadRequestException("Your profile location is needed to plan the route");
        }

        int perDay = (int) Math.ceil((double) chosen.size() / dayCount(trip));
        // Cached routes belong to the old stops, so they go too.
        tripRouteRepository.deleteByTripRequestId(tripRequestId);
        tripPlanRepository.deleteByTripRequestId(tripRequestId);
        List<TripPlan> plans = buildPlans(trip, chosen, chosen.size(), perDay);
        tripPlanRepository.saveAll(plans);

        trip.setEstimatedCost(estimateCost(plans.size(), Boolean.TRUE.equals(trip.getPickupRequired())));
        TripRequest saved = tripRequestRepository.save(trip);

        TripResponse response = toResponse(saved, plans);
        response.setEstimatedCost(saved.getEstimatedCost());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateItineraryPdf(Long tripRequestId) {
        TripRequest trip = findTrip(tripRequestId);
        List<TripPlan> plans = tripPlanRepository.findByTripRequestIdOrderByDayNumberAscStartTimeAsc(tripRequestId);
        // Package bookings have no day-by-day plan; the PDF then shows a booking summary.
        return itineraryEmailService.buildPdf(trip, effectiveCost(trip, plans));
    }

    @Override
    public boolean emailItinerary(Long tripRequestId) {
        TripRequest trip = findTrip(tripRequestId);
        if (trip.getStatus() != TripStatus.CONFIRMED) {
            throw new BadRequestException("The itinerary email is sent once a trip is booked and paid");
        }
        return itineraryEmailService.sendBookingEmail(trip);
    }

    @Override
    @Transactional
    public TripResponse updateTripDate(Long tripRequestId, java.time.LocalDate tripDate) {
        TripRequest trip = findTrip(tripRequestId);
        if (trip.getStatus() == TripStatus.CANCELLED) {
            throw new BadRequestException("A cancelled trip can't be given a date");
        }
        requireFutureDate(tripDate);
        trip.setTripDate(tripDate);
        tripRequestRepository.save(trip);
        return toResponse(trip, tripPlanRepository.findByTripRequestIdOrderByDayNumberAscStartTimeAsc(tripRequestId));
    }

    private void requireFutureDate(java.time.LocalDate date) {
        if (date.isBefore(java.time.LocalDate.now().minusDays(1))) {
            throw new BadRequestException("Choose a date that hasn't passed yet");
        }
    }

    private TripRequest findTrip(Long tripRequestId) {
        return tripRequestRepository.findById(tripRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
    }

    private int dayCount(TripRequest trip) {
        return trip.getWeekendType() == WeekendType.TWO_DAY ? 2 : 1;
    }

    /**
     * The price saved on the trip is what the customer is actually charged
     * (package trips have no stops to count, so recomputing from stops would
     * show the wrong amount). Only fall back to the formula if nothing is saved.
     */
    private BigDecimal effectiveCost(TripRequest trip, List<TripPlan> plans) {
        if (trip.getEstimatedCost() != null) {
            return trip.getEstimatedCost();
        }
        return estimateCost(plans.size(), Boolean.TRUE.equals(trip.getPickupRequired()));
    }

    private List<Place> rankPlaces(List<Place> places, double userLat, double userLon,
            com.tripfactory.nomad.domain.enums.InterestType interest) {
        return places.stream()
                .sorted(Comparator
                        .comparing((Place p) -> interest != null && interest == p.getCategory() ? 0 : 1)
                        .thenComparing(p -> GeoUtils.haversineKm(userLat, userLon, p.getLatitude(), p.getLongitude()))
                        .thenComparing(Place::getRating, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    private List<TripPlan> buildPlans(TripRequest tripRequest, List<Place> rankedPlaces, int totalSlots, int perDay) {
        List<TripPlan> plans = new ArrayList<>();
        List<Place> candidates = rankedPlaces.subList(0, Math.min(totalSlots, rankedPlaces.size()));
        List<Place> unassigned = new ArrayList<>(candidates);

        int dayCount = (int) Math.ceil((double) totalSlots / perDay);
        for (int day = 1; day <= dayCount; day++) {
            List<Place> dayPlaces = new ArrayList<>();
            double currentLat = tripRequest.getUser().getLatitude();
            double currentLon = tripRequest.getUser().getLongitude();

            while (dayPlaces.size() < perDay && !unassigned.isEmpty()) {
                Place next = findNearestPlace(currentLat, currentLon, unassigned);
                dayPlaces.add(next);
                unassigned.remove(next);
                currentLat = next.getLatitude();
                currentLon = next.getLongitude();
            }

            dayPlaces = reorderForTimeWindows(dayPlaces);

            LocalTime start = LocalTime.of(9, 0);
            Place previous = null;
            for (Place place : dayPlaces) {
                TripPlan plan = new TripPlan();
                plan.setTripRequest(tripRequest);
                plan.setDayNumber(day);
                plan.setPlace(place);
                plan.setStartTime(start);
                plan.setEndTime(start.plusHours(2));
                double distance = previous == null
                        ? GeoUtils.haversineKm(tripRequest.getUser().getLatitude(),
                                tripRequest.getUser().getLongitude(),
                                place.getLatitude(), place.getLongitude())
                        : GeoUtils.haversineKm(previous.getLatitude(), previous.getLongitude(),
                                place.getLatitude(), place.getLongitude());
                plan.setDistanceFromPrevious(distance);
                plans.add(plan);
                start = start.plusHours(2);
                previous = place;
            }
        }
        return plans;
    }

    private Place findNearestPlace(double currentLat, double currentLon, List<Place> places) {
        return places.stream()
                .min(Comparator.comparing(place -> GeoUtils.haversineKm(currentLat, currentLon,
                        place.getLatitude(), place.getLongitude())))
                .orElseThrow();
    }

    private List<Place> reorderForTimeWindows(List<Place> places) {
        if (places.size() <= 1) {
            return places;
        }
        int nightlifeIndex = -1;
        for (int i = 0; i < places.size(); i++) {
            if (places.get(i).getCategory() == com.tripfactory.nomad.domain.enums.InterestType.NIGHTLIFE) {
                nightlifeIndex = i;
                break;
            }
        }
        if (nightlifeIndex >= 0 && nightlifeIndex != places.size() - 1) {
            Place nightlife = places.remove(nightlifeIndex);
            places.add(nightlife);
        }
        return places;
    }

    private TripResponse toResponse(TripRequest tripRequest, List<TripPlan> plans) {
        TripResponse response = new TripResponse();
        response.setTripRequestId(tripRequest.getId());
        response.setUserId(tripRequest.getUser().getId());
        response.setCity(tripRequest.getCity());
        response.setShareToken(tripRequest.getShareToken());
        response.setWeekendType(tripRequest.getWeekendType() == null ? null : tripRequest.getWeekendType().name());
        response.setInterest(tripRequest.getInterest() == null ? null : tripRequest.getInterest().name());
        response.setTravelMode(tripRequest.getTravelMode() == null ? null : tripRequest.getTravelMode().name());
        response.setPickupRequired(tripRequest.getPickupRequired());
        response.setTripDate(tripRequest.getTripDate());
        if (tripRequest.getGroup() != null) {
            response.setGroupId(tripRequest.getGroup().getId());
            response.setGroupSize(tripRequestRepository.countByGroupId(tripRequest.getGroup().getId()));
        }
        response.setStatus(tripRequest.getStatus());
        response.setCreatedAt(tripRequest.getCreatedAt());

        List<TripPlanItemResponse> planResponses = plans.stream().map(plan -> {
            TripPlanItemResponse item = new TripPlanItemResponse();
            item.setDayNumber(plan.getDayNumber());
            item.setPlaceId(plan.getPlace().getId());
            item.setPlaceName(plan.getPlace().getName());
            item.setStartTime(plan.getStartTime());
            item.setEndTime(plan.getEndTime());
            item.setDistanceFromPrevious(plan.getDistanceFromPrevious());
            return item;
        }).collect(Collectors.toList());
        response.setPlans(planResponses);
        return response;
    }

    private BigDecimal estimateCost(int totalSlots, boolean pickupRequired) {
        BigDecimal total = BASE_COST_PER_PLACE.multiply(BigDecimal.valueOf(totalSlots));
        if (pickupRequired) {
            total = total.add(PICKUP_COST);
        }
        return total;
    }

    private TripGroup assignGroup(String city, com.tripfactory.nomad.domain.enums.InterestType interest,
            WeekendType weekendType) {
        TripGroup group = tripGroupRepository
                .findFirstByCityIgnoreCaseAndInterestAndWeekendTypeAndStatusOrderByCreatedAtAsc(
                        city, interest, weekendType, GroupStatus.OPEN)
                .orElseGet(() -> {
                    TripGroup created = new TripGroup();
                    created.setCity(city);
                    created.setInterest(interest);
                    created.setWeekendType(weekendType);
                    created.setStatus(GroupStatus.OPEN);
                    return tripGroupRepository.save(created);
                });

        long size = tripRequestRepository.countByGroupId(group.getId());
        if (size >= 6) {
            group.setStatus(GroupStatus.CLOSED);
            return tripGroupRepository.save(group);
        }
        if (size + 1 >= 4) {
            group.setStatus(GroupStatus.READY);
        }
        return tripGroupRepository.save(group);
    }
}