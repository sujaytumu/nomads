package com.tripfactory.nomad.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.tripfactory.nomad.domain.entity.MemberLocation;

public interface MemberLocationRepository extends JpaRepository<MemberLocation, Long> {

    Optional<MemberLocation> findByGroupIdAndUserId(Long groupId, Long userId);

    List<MemberLocation> findByGroupIdAndUpdatedAtAfter(Long groupId, LocalDateTime after);

    @Modifying
    @Transactional
    @Query("delete from MemberLocation m where m.groupId = :groupId and m.userId = :userId")
    int stopSharing(@Param("groupId") Long groupId, @Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("delete from MemberLocation m where m.updatedAt < :before")
    int deleteOlderThan(@Param("before") LocalDateTime before);
}
