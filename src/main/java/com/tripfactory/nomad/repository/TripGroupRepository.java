package com.tripfactory.nomad.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tripfactory.nomad.domain.entity.TripGroup;
import com.tripfactory.nomad.domain.enums.GroupStatus;
import com.tripfactory.nomad.domain.enums.InterestType;
import com.tripfactory.nomad.domain.enums.WeekendType;

public interface TripGroupRepository extends JpaRepository<TripGroup, Long> {

    Optional<TripGroup> findFirstByCityIgnoreCaseAndInterestAndWeekendTypeAndStatusOrderByCreatedAtAsc(
            String city, InterestType interest, WeekendType weekendType, GroupStatus status);
}