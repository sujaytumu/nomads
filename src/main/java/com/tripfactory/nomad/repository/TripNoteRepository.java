package com.tripfactory.nomad.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tripfactory.nomad.domain.entity.TripNote;

public interface TripNoteRepository extends JpaRepository<TripNote, Long> {

    List<TripNote> findByTripRequestIdOrderByDayNumberAscCreatedAtAsc(Long tripRequestId);

    Optional<TripNote> findByIdAndTripRequestId(Long id, Long tripRequestId);

    long countByTripRequestId(Long tripRequestId);
}
