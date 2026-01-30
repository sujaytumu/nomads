package com.tripfactory.nomad.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tripfactory.nomad.domain.entity.TripPlan;

public interface TripPlanRepository extends JpaRepository<TripPlan, Long> {

    List<TripPlan> findByTripRequestIdOrderByDayNumberAscStartTimeAsc(Long tripRequestId);
}