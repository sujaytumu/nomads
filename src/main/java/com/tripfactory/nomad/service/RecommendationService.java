package com.tripfactory.nomad.service;

import java.util.List;

import com.tripfactory.nomad.api.dto.RecommendedPlaceResponse;
import com.tripfactory.nomad.domain.enums.InterestType;

public interface RecommendationService {

    /**
     * Places picked for this traveller, best first, with the reasons each one was chosen.
     *
     * @param interest optional: only places of this kind
     * @param maxKm optional: only places within this distance of the traveller's home
     */
    List<RecommendedPlaceResponse> recommendFor(String email, int limit, InterestType interest, Double maxKm);
}
