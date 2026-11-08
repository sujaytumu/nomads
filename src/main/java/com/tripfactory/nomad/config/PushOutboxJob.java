package com.tripfactory.nomad.config;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tripfactory.nomad.service.PushService;

import lombok.RequiredArgsConstructor;

/** Delivers queued push messages every 20 seconds and tidies up old ones about once an hour. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "nomad.push.worker.enabled", havingValue = "true", matchIfMissing = true)
public class PushOutboxJob {

    private static final Logger log = LoggerFactory.getLogger(PushOutboxJob.class);
    private static final int CLEAN_EVERY = 180;

    private final PushService pushService;
    private final AtomicInteger ticks = new AtomicInteger();

    @Scheduled(initialDelayString = "${nomad.push.worker.initial-delay-ms:30000}",
               fixedDelayString = "${nomad.push.worker.interval-ms:20000}")
    public void run() {
        try {
            int n = pushService.deliverDue();
            if (n > 0) {
                log.debug("Handled {} queued push messages", n);
            }
            if (ticks.incrementAndGet() % CLEAN_EVERY == 0) {
                pushService.cleanUp();
            }
        } catch (Exception ex) {
            log.warn("Push worker run failed: {}", ex.getMessage());
        }
    }
}
