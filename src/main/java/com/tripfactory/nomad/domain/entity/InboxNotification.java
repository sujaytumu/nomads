package com.tripfactory.nomad.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One item in a user's in-app inbox (the bell icon): booking confirmed, trip cancelled, and so on. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "inbox_notifications")
public class InboxNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Machine-readable kind, e.g. BOOKING_CONFIRMED. The app uses it to pick an icon. */
    @Column(nullable = false, length = 40)
    private String type;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 500)
    private String body;

    /** In-app path to open when tapped, e.g. /bookings. */
    @Column(length = 200)
    private String link;

    /**
     * Optional. When set it is stored only once, so a scheduled reminder can run as often as it likes without
     * ever notifying twice. Null for one-off notifications.
     */
    @Column(length = 80, unique = true)
    private String dedupeKey;

    /** Null until the user has seen it. */
    @Column
    private LocalDateTime readAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
