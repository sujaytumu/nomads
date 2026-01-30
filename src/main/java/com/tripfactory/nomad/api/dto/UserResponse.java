package com.tripfactory.nomad.api.dto;

import java.time.LocalDateTime;

import com.tripfactory.nomad.domain.enums.InterestType;
import com.tripfactory.nomad.domain.enums.TravelPreference;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private String city;
    private Double latitude;
    private Double longitude;
    private InterestType interestType;
    private TravelPreference travelPreference;
    private com.tripfactory.nomad.domain.enums.UserRole role;
    private LocalDateTime createdAt;
}