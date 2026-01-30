package com.tripfactory.nomad.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tripfactory.nomad.domain.entity.Vehicle;
import com.tripfactory.nomad.domain.enums.AvailabilityStatus;
import com.tripfactory.nomad.domain.enums.VehicleType;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByAvailabilityStatusAndVehicleTypeIn(AvailabilityStatus status, List<VehicleType> types);
}