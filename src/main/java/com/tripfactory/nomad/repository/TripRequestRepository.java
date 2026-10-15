package com.tripfactory.nomad.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tripfactory.nomad.domain.entity.TripRequest;
import com.tripfactory.nomad.domain.enums.TripStatus;

public interface TripRequestRepository extends JpaRepository<TripRequest, Long> {

    List<TripRequest> findByUserId(Long userId);

    Optional<TripRequest> findByShareToken(String shareToken);

    long countByGroupId(Long groupId);

    long countByGroupIdAndStatus(Long groupId, TripStatus status);

    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    boolean existsByGroupIdAndUserIdAndStatus(Long groupId, Long userId, TripStatus status);

    List<TripRequest> findByDepartureId(Long departureId);

    List<TripRequest> findByDepartureIdIn(Collection<Long> departureIds);

    List<TripRequest> findByDepartureIdAndUserId(Long departureId, Long userId);

    @Query("select tr from TripRequest tr join fetch tr.user where tr.group.id = :groupId order by tr.createdAt asc")
    List<TripRequest> findGroupMembers(@Param("groupId") Long groupId);
}