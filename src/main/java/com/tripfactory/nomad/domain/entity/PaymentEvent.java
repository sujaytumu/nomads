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

/** An append-only record of what happened to a payment, so any booking can be traced afterwards. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payment_events", indexes = @Index(name = "idx_payment_events_trip", columnList = "tripRequestId"))
public class PaymentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tripRequestId;

    private Long paymentId;

    @Column(nullable = false, length = 40)
    private String type;

    @Column(length = 300)
    private String detail;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
