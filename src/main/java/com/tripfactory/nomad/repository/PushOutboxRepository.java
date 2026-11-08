package com.tripfactory.nomad.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.tripfactory.nomad.domain.entity.PushOutbox;

public interface PushOutboxRepository extends JpaRepository<PushOutbox, Long> {

    @Query("select o from PushOutbox o where o.status = 'PENDING' and o.nextAttemptAt <= :now order by o.id")
    List<PushOutbox> findDue(@Param("now") LocalDateTime now, Pageable page);

    /**
     * Takes a message for this worker by pushing its next attempt into the future. If two servers race, only one
     * update matches the old time, so a message is never sent twice at once.
     */
    @Modifying
    @Transactional
    @Query("update PushOutbox o set o.nextAttemptAt = :lease where o.id = :id and o.status = 'PENDING' and o.nextAttemptAt <= :now")
    int claim(@Param("id") Long id, @Param("now") LocalDateTime now, @Param("lease") LocalDateTime lease);

    long countByStatus(String status);

    @Modifying
    @Transactional
    @Query("delete from PushOutbox o where o.status <> 'PENDING' and o.createdAt < :before")
    int deleteFinishedBefore(@Param("before") LocalDateTime before);
}
