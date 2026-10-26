package com.tripfactory.nomad.api.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tripfactory.nomad.api.dto.InboxPageResponse;
import com.tripfactory.nomad.service.InboxService;

import lombok.RequiredArgsConstructor;

/** The caller's own notification inbox. Requires authentication; every route is scoped to the caller. */
@RestController
@RequestMapping("/api/inbox")
@RequiredArgsConstructor
public class InboxController {

    private final InboxService inboxService;

    @GetMapping
    public ResponseEntity<InboxPageResponse> list(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(inboxService.list(currentEmail(), unreadOnly, page, size));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        return ResponseEntity.ok(Map.of("unread", inboxService.unreadCount(currentEmail())));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id) {
        return inboxService.markRead(currentEmail(), id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllRead() {
        return ResponseEntity.ok(Map.of("updated", inboxService.markAllRead(currentEmail())));
    }

    private String currentEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
