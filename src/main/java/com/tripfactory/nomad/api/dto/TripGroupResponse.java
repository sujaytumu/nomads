package com.tripfactory.nomad.api.dto;

import java.time.LocalDateTime;

import com.tripfactory.nomad.domain.enums.GroupStatus;
import com.tripfactory.nomad.domain.enums.InterestType;
import com.tripfactory.nomad.domain.enums.WeekendType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TripGroupResponse {

    private Long id;
    private String city;
    private InterestType interest;
    private WeekendType weekendType;
    private GroupStatus status;
    private Long size;
    private LocalDateTime createdAt;
}