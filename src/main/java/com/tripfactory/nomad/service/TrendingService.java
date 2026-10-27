package com.tripfactory.nomad.service;

import java.util.List;

import com.tripfactory.nomad.api.dto.TrendingPlaceResponse;

public interface TrendingService {

    /** Places most often in recently booked trips, hottest first (empty when nothing has been booked lately). */
    List<TrendingPlaceResponse> trending(int limit);
}
