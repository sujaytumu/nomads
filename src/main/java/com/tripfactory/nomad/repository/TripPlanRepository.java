package com.tripfactory.nomad.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tripfactory.nomad.domain.entity.TripPlan;

public interface TripPlanRepository extends JpaRepository<TripPlan, Long> {

    List<TripPlan> findByTripRequestIdOrderByDayNumberAscStartTimeAsc(Long tripRequestId);

    void deleteByTripRequestId(Long tripRequestId);

    /** How many stops in this user's trips with the given status are at this place (0 = they have not been there). */
    @org.springframework.data.jpa.repository.Query("select count(tp) from TripPlan tp "
            + "where tp.tripRequest.user.id = :userId and tp.tripRequest.status = :status and tp.place.id = :placeId")
    long countVisits(@org.springframework.data.repository.query.Param("userId") Long userId,
                     @org.springframework.data.repository.query.Param("status") com.tripfactory.nomad.domain.enums.TripStatus status,
                     @org.springframework.data.repository.query.Param("placeId") Long placeId);
}