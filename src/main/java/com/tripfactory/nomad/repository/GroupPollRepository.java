package com.tripfactory.nomad.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tripfactory.nomad.domain.entity.GroupPoll;

public interface GroupPollRepository extends JpaRepository<GroupPoll, Long> {

    @Query("select p from GroupPoll p join fetch p.createdBy where p.group.id = :groupId order by p.createdAt desc")
    List<GroupPoll> findByGroupIdWithCreator(@Param("groupId") Long groupId);
}
