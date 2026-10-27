package com.tripfactory.nomad.service;

import java.time.LocalDate;

public interface TripReminderService {

    /**
     * Looks at every active trip and adds whatever reminders are due on {@code today} to the owners' inboxes.
     * Safe to run as often as you like: each reminder is stored once.
     *
     * @return how many new reminders were added
     */
    int runOnce(LocalDate today);
}
