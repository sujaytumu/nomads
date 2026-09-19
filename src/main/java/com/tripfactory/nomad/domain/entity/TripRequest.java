package com.tripfactory.nomad.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.tripfactory.nomad.domain.enums.InterestType;
import com.tripfactory.nomad.domain.enums.TravelMode;
import com.tripfactory.nomad.domain.enums.TripStatus;
import com.tripfactory.nomad.domain.enums.WeekendType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "trip_requests")
public class TripRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private TripGroup group;

    @Column(nullable = false)
    private String city;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WeekendType weekendType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterestType interest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TravelMode travelMode;

    @Column(nullable = false)
    private Boolean pickupRequired;

    // How many people the user said they're personally bringing on this trip
    // (distinct from the auto-matched trip-group headcount tracked via `group`
    // below) - used to size vehicle/pickup assistance appropriately.
    private Integer requestedGroupSize;

    // The server-computed cost of this trip - source of truth for the
    // payment amount, never trusted from the client. Set once at creation
    // (Trip Planner) or when a package is enrolled in.
    private BigDecimal estimatedCost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TripStatus status;

    @Column(unique = true)
    private String shareToken;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = TripStatus.REQUESTED;
        }
        if (shareToken == null) {
            shareToken = java.util.UUID.randomUUID().toString();
        }
    }
}