package com.tripfactory.nomad.api.dto;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WeatherForecastResponse {

    private String city;
    private List<DailyForecast> days;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class DailyForecast {
        private String date;
        private Double maxTempC;
        private Double minTempC;
        private Integer precipitationChance;
        private String description;
    }
}
