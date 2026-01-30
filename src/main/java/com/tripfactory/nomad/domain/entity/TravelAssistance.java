package com.tripfactory.nomad.domain.entity;

import java.time.LocalDateTime;

import com.tripfactory.nomad.domain.enums.AssistanceStatus;

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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "travel_assistance")
public class TravelAssistance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_request_id", nullable = false)
    private TripRequest tripRequest;

    @Column(nullable = false)
    private String pickupLocation;

    @Column(nullable = false)
    private LocalDateTime pickupTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column
    private String routeMapUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssistanceStatus status;
}