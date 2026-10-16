package com.tripfactory.nomad.service;

import java.util.List;

import com.tripfactory.nomad.api.dto.ItineraryOptionsResponse;
import com.tripfactory.nomad.api.dto.TripCreateRequest;
import com.tripfactory.nomad.api.dto.TripResponse;

public interface TripService {

    TripResponse createTrip(TripCreateRequest request);

    TripResponse getTrip(Long tripRequestId);

    List<TripResponse> getTripsByUser(Long userId);

    ItineraryOptionsResponse getItineraryOptions(Long tripRequestId);

    TripResponse updateItinerary(Long tripRequestId, List<Long> placeIds);

    TripResponse updateTripDate(Long tripRequestId, java.time.LocalDate tripDate);

    byte[] generateItineraryPdf(Long tripRequestId);

    /** Re-sends the booking email with the itinerary PDF. @return whether it was actually sent. */
    boolean emailItinerary(Long tripRequestId);
}