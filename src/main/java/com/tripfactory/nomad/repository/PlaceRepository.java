package com.tripfactory.nomad.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tripfactory.nomad.domain.entity.Place;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    List<Place> findByCityIgnoreCase(String city);
}