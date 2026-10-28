package com.tripfactory.nomad.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tripfactory.nomad.domain.entity.PushSubscription;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    Optional<PushSubscription> findByEndpoint(String endpoint);

    @Query("select s from PushSubscription s where s.user.id = :userId")
    List<PushSubscription> findByUserId(@Param("userId") Long userId);
}
