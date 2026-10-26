package com.tripfactory.nomad.api.dto;

import java.util.List;

public record InboxPageResponse(List<InboxItemResponse> items, long unread, int page, int size,
                                long totalItems, boolean hasMore) {
}
