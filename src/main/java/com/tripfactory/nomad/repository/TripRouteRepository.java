package com.tripfactory.nomad.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tripfactory.nomad.domain.entity.TripRoute;

public interface TripRouteRepository extends JpaRepository<TripRoute, Long> {

    Optional<TripRoute> findByTripRequestIdAndDayNumberAndModeIgnoreCase(Long tripRequestId, Integer dayNumber,
            String mode);

    // Cached routes become wrong as soon as a trip's stops change.
    void deleteByTripRequestId(Long tripRequestId);
}