package com.tripfactory.nomad.service;

import com.tripfactory.nomad.api.dto.PickupAdminUpdateRequest;
import com.tripfactory.nomad.api.dto.PickupConfirmRequest;
import com.tripfactory.nomad.api.dto.TravelAssistanceResponse;

public interface TravelAssistanceService {

    TravelAssistanceResponse assignAssistance(Long tripRequestId);

    TravelAssistanceResponse getAssistance(Long tripRequestId);

    TravelAssistanceResponse confirmPickup(Long tripRequestId, PickupConfirmRequest request);

    TravelAssistanceResponse updatePickupAdmin(Long tripRequestId, PickupAdminUpdateRequest request);
}