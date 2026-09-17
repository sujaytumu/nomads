package com.tripfactory.nomad.service;

import com.tripfactory.nomad.api.dto.WeatherForecastResponse;

public interface WeatherService {

    WeatherForecastResponse getForecastForTrip(Long tripRequestId);
}
