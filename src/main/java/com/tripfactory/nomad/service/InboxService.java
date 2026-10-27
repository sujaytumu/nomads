package com.tripfactory.nomad.service;

import com.tripfactory.nomad.api.dto.InboxPageResponse;

/** The in-app notification inbox (the bell). Every read/write is scoped to one user. */
public interface InboxService {

    /** Adds an item to a user's inbox. Callers should treat this as best-effort and never let it break their own work. */
    void push(Long userId, String type, String title, String body, String link);

    /**
     * Like {@link #push} but stored at most once per {@code dedupeKey}.
     *
     * @return true if it was added, false if that key had already been used
     */
    boolean pushOnce(Long userId, String dedupeKey, String type, String title, String body, String link);

    InboxPageResponse list(String email, boolean unreadOnly, int page, int size);

    long unreadCount(String email);

    /** @return true if it existed (and belonged to the user), false otherwise. */
    boolean markRead(String email, Long id);

    /** @return how many items were newly marked as read. */
    int markAllRead(String email);
}
