package com.tripfactory.nomad.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tripfactory.nomad.domain.entity.TravelAssistance;

public interface TravelAssistanceRepository extends JpaRepository<TravelAssistance, Long> {

    Optional<TravelAssistance> findByTripRequestId(Long tripRequestId);
}