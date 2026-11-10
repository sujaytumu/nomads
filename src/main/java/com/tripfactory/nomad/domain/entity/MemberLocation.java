package com.tripfactory.nomad.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** The last position a group member chose to share. One row per member per group; deleted when they stop sharing. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "member_locations", uniqueConstraints = @UniqueConstraint(columnNames = {"groupId", "userId"}))
public class MemberLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long groupId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private double lat;

    @Column(nullable = false)
    private double lng;

    @Column
    private Double accuracyM;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
