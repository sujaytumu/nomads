package com.tripfactory.nomad.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.tripfactory.nomad.domain.enums.DepartureStatus;
import com.tripfactory.nomad.domain.enums.InterestType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A scheduled group trip that strangers can each book seats on. Everyone who
 * pays for a seat lands in the same {@link TripGroup} (chat + co-traveller
 * list), so people who have never met travel together on a fixed date at a
 * fixed per-seat price.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "group_departures")
public class GroupDeparture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String city;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterestType interest;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private BigDecimal pricePerSeat;

    // The trip is "guaranteed to run" once this many seats are paid for.
    @Column(nullable = false)
    private Integer minTravellers;

    @Column(nullable = false)
    private Integer maxTravellers;

    private String meetingPoint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DepartureStatus status;

    // Backing group: holds the chat room and the member list for this trip.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false, unique = true)
    private TripGroup group;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = DepartureStatus.OPEN;
        }
    }
}
