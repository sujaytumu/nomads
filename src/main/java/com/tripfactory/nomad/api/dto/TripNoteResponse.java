package com.tripfactory.nomad.api.dto;

import java.time.LocalDateTime;

public record TripNoteResponse(Long id, int dayNumber, String mood, String text,
                               LocalDateTime createdAt, LocalDateTime updatedAt) {
}
