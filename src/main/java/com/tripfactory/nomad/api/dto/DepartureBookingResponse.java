package com.tripfactory.nomad.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.tripfactory.nomad.domain.enums.TripStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DepartureBookingResponse {

    private Long tripRequestId;
    private Long departureId;
    private TripStatus status;
    private Integer seats;
    private BigDecimal amount;
    // While payment is pending, the seats are reserved until this moment.
    private LocalDateTime seatHoldUntil;
    // Only set once the booking is paid - unlocks the chat and co-traveller list.
    private Long groupId;
}
