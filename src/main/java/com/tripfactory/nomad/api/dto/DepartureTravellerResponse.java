package com.tripfactory.nomad.api.dto;

import java.time.LocalDateTime;

import com.tripfactory.nomad.domain.enums.InterestType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * What one traveller may see about another on the same trip. Deliberately
 * minimal for people who have never met: first name only, no email/phone.
 */
@Getter
@Setter
@NoArgsConstructor
public class DepartureTravellerResponse {

    private String firstName;
    private String city;
    private InterestType interestType;
    private Integer seats;
    private LocalDateTime joinedAt;
}
