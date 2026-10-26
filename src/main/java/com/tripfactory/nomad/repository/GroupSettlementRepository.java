package com.tripfactory.nomad.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tripfactory.nomad.domain.entity.GroupSettlement;

public interface GroupSettlementRepository extends JpaRepository<GroupSettlement, Long> {

    @Query("select s from GroupSettlement s join fetch s.fromUser join fetch s.toUser where s.group.id = :groupId order by s.createdAt desc")
    List<GroupSettlement> findByGroupIdWithUsers(@Param("groupId") Long groupId);
}
