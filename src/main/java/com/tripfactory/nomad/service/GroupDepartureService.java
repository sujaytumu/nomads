package com.tripfactory.nomad.service;

import java.util.List;
import java.util.Optional;

import com.tripfactory.nomad.api.dto.DepartureBookingResponse;
import com.tripfactory.nomad.api.dto.DepartureCreateRequest;
import com.tripfactory.nomad.api.dto.DepartureResponse;
import com.tripfactory.nomad.api.dto.DepartureTravellerResponse;
import com.tripfactory.nomad.domain.entity.TripRequest;
import com.tripfactory.nomad.domain.enums.InterestType;

/**
 * Scheduled group trips that strangers book seats on. Seats are limited, so
 * every booking path goes through here to keep capacity honest.
 */
public interface GroupDepartureService {

    List<DepartureResponse> listOpen(String city, InterestType interest);

    DepartureResponse get(Long departureId);

    DepartureResponse create(DepartureCreateRequest request);

    /**
     * Reserves seats for a user (unpaid, held for a limited time) and returns
     * the booking to pay for. Fails if the trip is closed, full or already
     * booked by this user.
     */
    DepartureBookingResponse join(Long departureId, Long userId, int seats);

    /**
     * Called before a payment order is created for a booking. For departure
     * bookings it re-checks that the seats are still available and refreshes
     * the seat hold; for any other trip it does nothing.
     */
    void assertCanPay(TripRequest trip);

    Optional<DepartureBookingResponse> myBooking(Long departureId, Long userId);

    /** Paid co-travellers, visible only to other paid travellers (and admins). */
    List<DepartureTravellerResponse> travellers(Long departureId, Long viewerUserId);
}
