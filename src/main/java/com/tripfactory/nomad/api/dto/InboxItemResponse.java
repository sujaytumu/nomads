package com.tripfactory.nomad.api.dto;

import java.time.LocalDateTime;

public record InboxItemResponse(Long id, String type, String title, String body, String link,
                                boolean read, LocalDateTime createdAt) {
}
