package com.tripfactory.nomad.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tripfactory.nomad.domain.entity.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByTripRequestId(Long tripRequestId);
}