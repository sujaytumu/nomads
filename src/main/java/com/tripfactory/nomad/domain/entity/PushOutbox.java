package com.tripfactory.nomad.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A push message waiting to be delivered. It is saved in the same transaction as the inbox item that caused it,
 * so a message is never lost if the server restarts or the push service is down: a background worker keeps
 * retrying with growing delays until it is delivered or the attempts run out.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "push_outbox", indexes = @Index(name = "idx_push_outbox_due", columnList = "status,nextAttemptAt"))
public class PushOutbox {

    public static final String PENDING = "PENDING";
    public static final String SENT = "SENT";
    public static final String FAILED = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 500)
    private String body;

    @Column(length = 200)
    private String link;

    @Column(nullable = false, length = 10)
    private String status = PENDING;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(length = 200)
    private String lastError;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (nextAttemptAt == null) {
            nextAttemptAt = createdAt;
        }
    }
}
