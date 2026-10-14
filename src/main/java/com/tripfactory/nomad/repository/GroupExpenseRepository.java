package com.tripfactory.nomad.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tripfactory.nomad.domain.entity.GroupExpense;

public interface GroupExpenseRepository extends JpaRepository<GroupExpense, Long> {

    @Query("select e from GroupExpense e join fetch e.paidBy where e.group.id = :groupId order by e.createdAt desc")
    List<GroupExpense> findByGroupIdWithPayer(@Param("groupId") Long groupId);
}
