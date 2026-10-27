package com.tripfactory.nomad.config;

import java.time.LocalDate;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tripfactory.nomad.service.TripReminderService;

import lombok.RequiredArgsConstructor;

/**
 * Checks for due trip reminders a couple of minutes after start-up and then every few hours. It runs often
 * (instead of once a day at a fixed time) because a free-tier server can be asleep at any given time; each
 * reminder is stored only once, so the extra runs are harmless.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "nomad.reminders.enabled", havingValue = "true", matchIfMissing = true)
public class TripReminderJob {

    private static final Logger log = LoggerFactory.getLogger(TripReminderJob.class);

    private final TripReminderService tripReminderService;

    @Value("${nomad.zone:Asia/Kolkata}")
    private String zone;

    @Scheduled(initialDelayString = "${nomad.reminders.initial-delay-ms:120000}",
               fixedDelayString = "${nomad.reminders.interval-ms:10800000}")
    public void run() {
        try {
            tripReminderService.runOnce(LocalDate.now(ZoneId.of(zone)));
        } catch (Exception ex) {
            log.warn("Trip reminder run failed: {}", ex.getMessage());
        }
    }
}
