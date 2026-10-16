package com.tripfactory.nomad.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tripfactory.nomad.api.dto.ItineraryOptionsResponse;
import com.tripfactory.nomad.api.dto.TripResponse;
import com.tripfactory.nomad.domain.entity.Place;
import com.tripfactory.nomad.domain.entity.TripPlan;
import com.tripfactory.nomad.domain.entity.TripRequest;
import com.tripfactory.nomad.domain.entity.User;
import com.tripfactory.nomad.domain.enums.InterestType;
import com.tripfactory.nomad.domain.enums.TripStatus;
import com.tripfactory.nomad.domain.enums.WeekendType;
import com.tripfactory.nomad.repository.PlaceRepository;
import com.tripfactory.nomad.repository.TripGroupRepository;
import com.tripfactory.nomad.repository.TripPlanRepository;
import com.tripfactory.nomad.repository.TripRequestRepository;
import com.tripfactory.nomad.repository.TripRouteRepository;
import com.tripfactory.nomad.repository.UserRepository;
import com.tripfactory.nomad.service.NotificationService;
import com.tripfactory.nomad.service.exception.BadRequestException;

@ExtendWith(MockitoExtension.class)
class TripServiceImplTest {

    private static final long TRIP_ID = 12L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private TripGroupRepository tripGroupRepository;

    @Mock
    private TripRequestRepository tripRequestRepository;

    @Mock
    private TripPlanRepository tripPlanRepository;

    @Mock
    private TripRouteRepository tripRouteRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ItineraryEmailService itineraryEmailService;

    @Captor
    private ArgumentCaptor<List<TripPlan>> plansCaptor;

    @InjectMocks
    private TripServiceImpl tripService;

    // ---- helpers --------------------------------------------------------

    private TripRequest trip(TripStatus status, WeekendType weekendType) {
        User user = new User();
        user.setId(7L);
        user.setEmail("a@b.com");
        user.setCity("Bengaluru");
        user.setLatitude(12.97);
        user.setLongitude(77.59);

        TripRequest trip = new TripRequest();
        trip.setId(TRIP_ID);
        trip.setUser(user);
        trip.setCity("Bengaluru");
        trip.setWeekendType(weekendType);
        trip.setInterest(InterestType.CULTURE);
        trip.setPickupRequired(Boolean.FALSE);
        trip.setStatus(status);
        trip.setEstimatedCost(new BigDecimal("2000"));
        return trip;
    }

    private Place place(long id, String name, String city, double lat, double lon) {
        Place place = new Place();
        place.setId(id);
        place.setName(name);
        place.setCity(city);
        place.setLatitude(lat);
        place.setLongitude(lon);
        place.setCategory(InterestType.CULTURE);
        place.setRating(4.0);
        return place;
    }

    private TripPlan planFor(Place place) {
        TripPlan plan = new TripPlan();
        plan.setDayNumber(1);
        plan.setPlace(place);
        plan.setStartTime(LocalTime.of(9, 0));
        plan.setEndTime(LocalTime.of(11, 0));
        plan.setDistanceFromPrevious(1.0);
        return plan;
    }

    // ---- price display --------------------------------------------------

    @Test
    void getTrip_packageTrip_showsTheSavedPackagePrice() {
        // A package booking has no stops, so recomputing "500 x stops" would
        // show 0 while the customer is actually charged the package price.
        TripRequest trip = trip(TripStatus.PAYMENT_PENDING, WeekendType.TWO_DAY);
        trip.setEstimatedCost(new BigDecimal("3500"));
        when(tripRequestRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(tripPlanRepository.findByTripRequestIdOrderByDayNumberAscStartTimeAsc(TRIP_ID)).thenReturn(List.of());

        TripResponse response = tripService.getTrip(TRIP_ID);

        assertThat(response.getEstimatedCost()).isEqualByComparingTo("3500");
    }

    // ---- choosing places ------------------------------------------------

    @Test
    void updateItinerary_rebuildsTheStopsAndRecalculatesThePrice() {
        TripRequest trip = trip(TripStatus.PLANNED, WeekendType.TWO_DAY);
        Place a = place(1L, "Lalbagh", "Bengaluru", 12.95, 77.58);
        Place b = place(2L, "Cubbon Park", "Bengaluru", 12.98, 77.59);
        when(tripRequestRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(tripPlanRepository.findByTripRequestIdOrderByDayNumberAscStartTimeAsc(TRIP_ID))
                .thenReturn(List.of(planFor(a), planFor(b)));
        when(placeRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(a, b));
        when(tripRequestRepository.save(any(TripRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        TripResponse response = tripService.updateItinerary(TRIP_ID, List.of(1L, 2L));

        verify(tripRouteRepository).deleteByTripRequestId(TRIP_ID);
        verify(tripPlanRepository).deleteByTripRequestId(TRIP_ID);
        verify(tripPlanRepository).saveAll(plansCaptor.capture());
        assertThat(plansCaptor.getValue()).hasSize(2);
        assertThat(plansCaptor.getValue()).extracting(plan -> plan.getPlace().getId())
                .containsExactlyInAnyOrder(1L, 2L);
        assertThat(response.getPlans()).hasSize(2);
        // 2 places x 500, no pickup
        assertThat(response.getEstimatedCost()).isEqualByComparingTo("1000");
        assertThat(trip.getEstimatedCost()).isEqualByComparingTo("1000");
    }

    @Test
    void updateItinerary_addsThePickupFeeWhenPickupWasRequested() {
        TripRequest trip = trip(TripStatus.PLANNED, WeekendType.ONE_DAY);
        trip.setPickupRequired(Boolean.TRUE);
        Place a = place(1L, "Lalbagh", "Bengaluru", 12.95, 77.58);
        when(tripRequestRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(tripPlanRepository.findByTripRequestIdOrderByDayNumberAscStartTimeAsc(TRIP_ID))
                .thenReturn(List.of(planFor(a)));
        when(placeRepository.findAllById(List.of(1L))).thenReturn(List.of(a));
        when(tripRequestRepository.save(any(TripRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        TripResponse response = tripService.updateItinerary(TRIP_ID, List.of(1L));

        // 1 place x 500 + 300 pickup
        assertThat(response.getEstimatedCost()).isEqualByComparingTo("800");
    }

    @Test
    void updateItinerary_ignoresDuplicateIds() {
        TripRequest trip = trip(TripStatus.PLANNED, WeekendType.ONE_DAY);
        Place a = place(1L, "Lalbagh", "Bengaluru", 12.95, 77.58);
        when(tripRequestRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(tripPlanRepository.findByTripRequestIdOrderByDayNumberAscStartTimeAsc(TRIP_ID))
                .thenReturn(List.of(planFor(a)));
        when(placeRepository.findAllById(List.of(1L))).thenReturn(List.of(a));
        when(tripRequestRepository.save(any(TripRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        TripResponse response = tripService.updateItinerary(TRIP_ID, List.of(1L, 1L, 1L));

        assertThat(response.getPlans()).hasSize(1);
        assertThat(response.getEstimatedCost()).isEqualByComparingTo("500");
    }

    @Test
    void updateItinerary_isRejectedOncePaymentHasStarted() {
        TripRequest trip = trip(TripStatus.PAYMENT_PENDING, WeekendType.TWO_DAY);
        when(tripRequestRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));

        assertThrows(BadRequestException.class, () -> tripService.updateItinerary(TRIP_ID, List.of(1L)));

        verify(tripPlanRepository, never()).deleteByTripRequestId(any());
        verify(tripPlanRepository, never()).saveAll(any());
        assertThat(trip.getEstimatedCost()).isEqualByComparingTo("2000");
    }

    @Test
    void updateItinerary_isRejectedForAPackageTripWithNoItinerary() {
        TripRequest trip = trip(TripStatus.PLANNED, WeekendType.TWO_DAY);
        when(tripRequestRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(tripPlanRepository.findByTripRequestIdOrderByDayNumberAscStartTimeAsc(TRIP_ID)).thenReturn(List.of());

        assertThrows(BadRequestException.class, () -> tripService.updateItinerary(TRIP_ID, List.of(1L)));

        verify(tripPlanRepository, never()).deleteByTripRequestId(any());
    }

    @Test
    void updateItinerary_requiresAtLeastOnePlace() {
        TripRequest trip = trip(TripStatus.PLANNED, WeekendType.TWO_DAY);
        when(tripRequestRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(tripPlanRepository.findByTripRequestIdOrderByDayNumberAscStartTimeAsc(TRIP_ID))
                .thenReturn(List.of(planFor(place(1L, "Lalbagh", "Bengaluru", 12.95, 77.58))));

        assertThrows(BadRequestException.class, () -> tripService.updateItinerary(TRIP_ID, List.of()));

        verify(tripPlanRepository, never()).deleteByTripRequestId(any());
    }

    @Test
    void updateItinerary_rejectsMoreStopsThanTheTripLengthAllows() {
        // A one-day trip fits 4 stops.
        TripRequest trip = trip(TripStatus.PLANNED, WeekendType.ONE_DAY);
        when(tripRequestRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(tripPlanRepository.findByTripRequestIdOrderByDayNumberAscStartTimeAsc(TRIP_ID))
                .thenReturn(List.of(planFor(place(1L, "Lalbagh", "Bengaluru", 12.95, 77.58))));

        assertThrows(BadRequestException.class,
                () -> tripService.updateItinerary(TRIP_ID, List.of(1L, 2L, 3L, 4L, 5L)));

        verify(tripPlanRepository, never()).deleteByTripRequestId(any());
    }

    @Test
    void updateItinerary_rejectsAPlaceFromAnotherCity() {
        TripRequest trip = trip(TripStatus.PLANNED, WeekendType.TWO_DAY);
        Place elsewhere = place(9L, "Gateway of India", "Mumbai", 18.92, 72.83);
        when(tripRequestRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(tripPlanRepository.findByTripRequestIdOrderByDayNumberAscStartTimeAsc(TRIP_ID))
                .thenReturn(List.of(planFor(place(1L, "Lalbagh", "Bengaluru", 12.95, 77.58))));
        when(placeRepository.findAllById(List.of(9L))).thenReturn(List.of(elsewhere));

        assertThrows(BadRequestException.class, () -> tripService.updateItinerary(TRIP_ID, List.of(9L)));

        verify(tripPlanRepository, never()).deleteByTripRequestId(any());
    }

    @Test
    void updateItinerary_rejectsAnUnknownPlace() {
        TripRequest trip = trip(TripStatus.PLANNED, WeekendType.TWO_DAY);
        when(tripRequestRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(tripPlanRepository.findByTripRequestIdOrderByDayNumberAscStartTimeAsc(TRIP_ID))
                .thenReturn(List.of(planFor(place(1L, "Lalbagh", "Bengaluru", 12.95, 77.58))));
        when(placeRepository.findAllById(List.of(99L))).thenReturn(List.of());

        assertThrows(BadRequestException.class, () -> tripService.updateItinerary(TRIP_ID, List.of(99L)));

        verify(tripPlanRepository, never()).deleteByTripRequestId(any());
    }

    // ---- options for the editor ----------------------------------------

    @Test
    void getItineraryOptions_marksTheCurrentStopsAsSelected() {
        TripRequest trip = trip(TripStatus.PLANNED, WeekendType.TWO_DAY);
        Place a = place(1L, "Lalbagh", "Bengaluru", 12.95, 77.58);
        Place b = place(2L, "Cubbon Park", "Bengaluru", 12.98, 77.59);
        when(tripRequestRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(tripPlanRepository.findByTripRequestIdOrderByDayNumberAscStartTimeAsc(TRIP_ID))
                .thenReturn(List.of(planFor(a)));
        when(placeRepository.findByCityIgnoreCase("Bengaluru")).thenReturn(List.of(a, b));

        ItineraryOptionsResponse response = tripService.getItineraryOptions(TRIP_ID);

        assertThat(response.isEditable()).isTrue();
        assertThat(response.getMaxPlaces()).isEqualTo(8);
        assertThat(response.getMinPlaces()).isEqualTo(1);
        assertThat(response.getCostPerPlace()).isEqualByComparingTo("500");
        assertThat(response.getOptions()).hasSize(2);
        assertThat(response.getOptions()).filteredOn(o -> o.getPlaceId() == 1L)
                .allMatch(o -> o.isSelected());
        assertThat(response.getOptions()).filteredOn(o -> o.getPlaceId() == 2L)
                .noneMatch(o -> o.isSelected());
    }

    @Test
    void getItineraryOptions_isNotEditableAfterBooking() {
        TripRequest trip = trip(TripStatus.CONFIRMED, WeekendType.TWO_DAY);
        when(tripRequestRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(tripPlanRepository.findByTripRequestIdOrderByDayNumberAscStartTimeAsc(TRIP_ID))
                .thenReturn(List.of(planFor(place(1L, "Lalbagh", "Bengaluru", 12.95, 77.58))));

        ItineraryOptionsResponse response = tripService.getItineraryOptions(TRIP_ID);

        assertThat(response.isEditable()).isFalse();
        assertThat(response.getOptions()).isEmpty();
        verify(placeRepository, never()).findByCityIgnoreCase(any());
    }

    // ---- PDF ------------------------------------------------------------

    @Test
    void generateItineraryPdf_usesTheSavedPrice() {
        TripRequest trip = trip(TripStatus.CONFIRMED, WeekendType.TWO_DAY);
        List<TripPlan> plans = List.of(planFor(place(1L, "Lalbagh", "Bengaluru", 12.95, 77.58)));
        byte[] pdf = new byte[] { 1, 2, 3 };
        when(tripRequestRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(tripPlanRepository.findByTripRequestIdOrderByDayNumberAscStartTimeAsc(TRIP_ID)).thenReturn(plans);
        when(itineraryEmailService.buildPdf(trip, trip.getEstimatedCost())).thenReturn(pdf);

        assertThat(tripService.generateItineraryPdf(TRIP_ID)).isSameAs(pdf);
    }

    @Test
    void generateItineraryPdf_packageTripWithoutPlansStillProducesPdf() {
        TripRequest trip = trip(TripStatus.CONFIRMED, WeekendType.TWO_DAY);
        byte[] pdf = new byte[] { 1 };
        when(tripRequestRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(tripPlanRepository.findByTripRequestIdOrderByDayNumberAscStartTimeAsc(TRIP_ID)).thenReturn(List.of());
        when(itineraryEmailService.buildPdf(trip, trip.getEstimatedCost())).thenReturn(pdf);

        assertThat(tripService.generateItineraryPdf(TRIP_ID)).isSameAs(pdf);
    }

    @Test
    void emailItinerary_sendsOnlyForBookedTripsAndReportsResult() {
        TripRequest booked = trip(TripStatus.CONFIRMED, WeekendType.TWO_DAY);
        when(tripRequestRepository.findById(TRIP_ID)).thenReturn(Optional.of(booked));
        when(itineraryEmailService.sendBookingEmail(booked)).thenReturn(false);

        assertThat(tripService.emailItinerary(TRIP_ID)).isFalse();
    }

    @Test
    void emailItinerary_isRejectedForUnbookedTrips() {
        TripRequest planned = trip(TripStatus.PLANNED, WeekendType.TWO_DAY);
        when(tripRequestRepository.findById(TRIP_ID)).thenReturn(Optional.of(planned));

        assertThrows(BadRequestException.class, () -> tripService.emailItinerary(TRIP_ID));
    }

    @Test
    void updateTripDate_savesAFutureDate() {
        TripRequest trip = trip(TripStatus.CONFIRMED, WeekendType.TWO_DAY);
        when(tripRequestRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(tripPlanRepository.findByTripRequestIdOrderByDayNumberAscStartTimeAsc(TRIP_ID)).thenReturn(List.of());
        java.time.LocalDate date = java.time.LocalDate.now().plusDays(10);

        tripService.updateTripDate(TRIP_ID, date);

        assertThat(trip.getTripDate()).isEqualTo(date);
        verify(tripRequestRepository).save(trip);
    }

    @Test
    void updateTripDate_rejectsPastDatesAndCancelledTrips() {
        TripRequest booked = trip(TripStatus.CONFIRMED, WeekendType.TWO_DAY);
        when(tripRequestRepository.findById(TRIP_ID)).thenReturn(Optional.of(booked));
        assertThrows(BadRequestException.class,
                () -> tripService.updateTripDate(TRIP_ID, java.time.LocalDate.now().minusDays(5)));

        TripRequest cancelled = trip(TripStatus.CANCELLED, WeekendType.TWO_DAY);
        when(tripRequestRepository.findById(TRIP_ID)).thenReturn(Optional.of(cancelled));
        assertThrows(BadRequestException.class,
                () -> tripService.updateTripDate(TRIP_ID, java.time.LocalDate.now().plusDays(5)));
        verify(tripRequestRepository, never()).save(any());
    }
}
