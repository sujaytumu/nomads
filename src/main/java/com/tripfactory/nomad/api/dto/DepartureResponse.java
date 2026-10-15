package com.tripfactory.nomad.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.tripfactory.nomad.domain.enums.DepartureStatus;
import com.tripfactory.nomad.domain.enums.InterestType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DepartureResponse {

    private Long id;
    private String title;
    private String city;
    private String description;
    private InterestType interest;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer durationDays;
    private BigDecimal pricePerSeat;
    private Integer minTravellers;
    private Integer maxTravellers;
    private String meetingPoint;
    private DepartureStatus status;

    // Seats already paid for.
    private Integer confirmedSeats;
    // Seats still bookable right now (excludes paid seats and seats someone is
    // currently holding while they complete payment).
    private Integer seatsLeft;
    // True once enough seats are paid for that the trip will definitely run.
    private Boolean guaranteedToRun;
    private Boolean bookingOpen;
}
