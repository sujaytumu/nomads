package com.tripfactory.nomad.service;

import com.tripfactory.nomad.api.dto.TripRouteResponse;

public interface TripRouteService {

    TripRouteResponse getOrCreateRoute(Long tripRequestId, Integer dayNumber, String mode);
}