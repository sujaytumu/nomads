package com.tripfactory.nomad.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tripfactory.nomad.domain.entity.PlaceRating;

public interface PlaceRatingRepository extends JpaRepository<PlaceRating, Long> {

    List<PlaceRating> findByPlaceIdOrderByUpdatedAtDesc(Long placeId);

    Optional<PlaceRating> findByUserIdAndPlaceId(Long userId, Long placeId);

    long deleteByUserIdAndPlaceId(Long userId, Long placeId);
}
