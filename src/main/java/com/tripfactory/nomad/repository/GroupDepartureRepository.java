package com.tripfactory.nomad.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tripfactory.nomad.domain.entity.GroupDeparture;
import com.tripfactory.nomad.domain.enums.DepartureStatus;

import jakarta.persistence.LockModeType;

public interface GroupDepartureRepository extends JpaRepository<GroupDeparture, Long> {

    List<GroupDeparture> findByStatusAndStartDateAfterOrderByStartDateAsc(DepartureStatus status, LocalDate date);

    boolean existsByGroupId(Long groupId);

    /**
     * Row-locks the departure so two people booking the last seat at the same
     * moment are serialized instead of both succeeding (overbooking).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from GroupDeparture d where d.id = :id")
    Optional<GroupDeparture> findByIdForUpdate(@Param("id") Long id);
}
