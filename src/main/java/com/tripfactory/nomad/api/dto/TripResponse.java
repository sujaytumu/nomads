package com.tripfactory.nomad.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.tripfactory.nomad.domain.enums.TripStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TripResponse {

    private Long tripRequestId;
    private Long userId;
    private Long groupId;
    private Long groupSize;
    private String city;
    private String shareToken;
    private TripStatus status;
    private LocalDateTime createdAt;
    private BigDecimal estimatedCost;
    private List<TripPlanItemResponse> plans;
}