package com.tripfactory.nomad.api.dto;

import java.time.LocalDateTime;

import com.tripfactory.nomad.domain.enums.InterestType;
import com.tripfactory.nomad.domain.enums.TripStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TripGroupMemberResponse {

    private Long tripRequestId;
    private Long userId;
    private String name;
    private String city;
    private InterestType interestType;
    private TripStatus tripStatus;
    private LocalDateTime joinedAt;
}
