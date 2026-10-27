package com.tripfactory.nomad.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tripfactory.nomad.domain.entity.SavedPlace;

public interface SavedPlaceRepository extends JpaRepository<SavedPlace, Long> {

    List<SavedPlace> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByUserIdAndPlaceId(Long userId, Long placeId);

    long deleteByUserIdAndPlaceId(Long userId, Long placeId);
}
