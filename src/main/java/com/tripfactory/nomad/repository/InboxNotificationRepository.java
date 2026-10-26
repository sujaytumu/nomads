package com.tripfactory.nomad.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tripfactory.nomad.domain.entity.InboxNotification;

public interface InboxNotificationRepository extends JpaRepository<InboxNotification, Long> {

    Page<InboxNotification> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

    Page<InboxNotification> findByUserIdAndReadAtIsNullOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

    long countByUserIdAndReadAtIsNull(Long userId);

    Optional<InboxNotification> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("update InboxNotification n set n.readAt = :now where n.user.id = :userId and n.readAt is null")
    int markAllRead(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
