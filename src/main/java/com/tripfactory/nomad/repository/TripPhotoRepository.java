package com.tripfactory.nomad.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tripfactory.nomad.domain.entity.TripPhoto;

public interface TripPhotoRepository extends JpaRepository<TripPhoto, Long> {

    @Query("select p from TripPhoto p where p.tripRequest.id = :tripId order by p.createdAt asc, p.id asc")
    List<TripPhoto> findByTripId(@Param("tripId") Long tripId);

    @Query("select count(p) from TripPhoto p where p.tripRequest.id = :tripId")
    long countByTripId(@Param("tripId") Long tripId);
}
