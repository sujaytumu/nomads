package com.tripfactory.nomad.service.impl;

import java.security.Security;
import java.time.LocalDateTime;
import java.util.List;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tripfactory.nomad.domain.entity.PushOutbox;
import com.tripfactory.nomad.domain.entity.PushSubscription;
import com.tripfactory.nomad.repository.PushOutboxRepository;
import com.tripfactory.nomad.repository.PushSubscriptionRepository;
import com.tripfactory.nomad.service.PushService;
import com.tripfactory.nomad.service.engine.RetryPolicy;

import jakarta.annotation.PostConstruct;
import nl.martijndwars.webpush.Notification;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PushServiceImpl implements PushService {

    private static final Logger log = LoggerFactory.getLogger(PushServiceImpl.class);

    private static final int BATCH = 50;

    private final PushSubscriptionRepository subscriptions;
    private final PushOutboxRepository outbox;

    @Value("${nomad.push.public-key:}")
    private String publicKey;

    @Value("${nomad.push.private-key:}")
    private String privateKey;

    @Value("${nomad.push.subject:mailto:admin@nomad.app}")
    private String subject;

    private nl.martijndwars.webpush.PushService sender;

    @PostConstruct
    void init() {
        if (publicKey == null || publicKey.isBlank() || privateKey == null || privateKey.isBlank()) {
            log.info("Push notifications are off (set VAPID_PUBLIC_KEY and VAPID_PRIVATE_KEY to turn them on)");
            return;
        }
        try {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
            sender = new nl.martijndwars.webpush.PushService(publicKey.trim(), privateKey.trim(), subject);
            log.info("Push notifications are on");
        } catch (Exception ex) {
            sender = null;
            log.warn("Push keys are invalid, push is off: {}", ex.getMessage());
        }
    }

    @Override
    public boolean isEnabled() {
        return sender != null;
    }

    @Override
    public String publicKey() {
        return sender == null ? "" : publicKey.trim();
    }

    /** Joins the caller's transaction, so the queued message is saved or lost together with the inbox item. */
    @Override
    @Transactional
    public void sendToUser(Long userId, String title, String body, String link) {
        if (sender == null || userId == null) {
            return;
        }
        try {
            PushOutbox o = new PushOutbox();
            o.setUserId(userId);
            o.setTitle(title == null ? "" : title);
            o.setBody(body);
            o.setLink(link);
            outbox.save(o);
        } catch (Exception ex) {
            log.warn("Could not queue push for user {}: {}", userId, ex.getMessage());
        }
    }

    @Override
    public int deliverDue() {
        if (sender == null) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        int handled = 0;
        for (PushOutbox o : outbox.findDue(now, PageRequest.of(0, BATCH))) {
            if (outbox.claim(o.getId(), now, now.plusMinutes(2)) != 1) {
                continue; // another server took it
            }
            handled++;
            String error = null;
            try {
                error = deliver(o);
            } catch (Exception ex) {
                error = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            }
            finish(o, error);
        }
        return handled;
    }

    private void finish(PushOutbox o, String error) {
        try {
            if (error == null) {
                o.setStatus(PushOutbox.SENT);
                o.setLastError(null);
            } else {
                o.setAttempts(o.getAttempts() + 1);
                o.setLastError(error.length() > 200 ? error.substring(0, 200) : error);
                if (RetryPolicy.giveUp(o.getAttempts())) {
                    o.setStatus(PushOutbox.FAILED);
                } else {
                    o.setNextAttemptAt(LocalDateTime.now().plus(RetryPolicy.delayAfter(o.getAttempts())));
                }
            }
            outbox.save(o);
        } catch (Exception ex) {
            log.warn("Could not update push outbox {}: {}", o.getId(), ex.getMessage());
        }
    }

    /** @return null when delivered (or when there is nobody to deliver to), otherwise a reason to retry */
    private String deliver(PushOutbox o) {
        List<PushSubscription> subs = subscriptions.findByUserId(o.getUserId());
        if (subs.isEmpty()) {
            return null;
        }
        String payload = "{\"title\":" + quote(o.getTitle()) + ",\"body\":" + quote(o.getBody()) + ",\"url\":" + quote(o.getLink() == null ? "/" : o.getLink()) + "}";
        int delivered = 0;
        String lastFailure = null;
        for (PushSubscription s : subs) {
            try {
                var response = sender.send(new Notification(s.getEndpoint(), s.getP256dh(), s.getAuth(), payload));
                int status = response.getStatusLine().getStatusCode();
                if (status == 404 || status == 410) {
                    subscriptions.deleteById(s.getId()); // that device is gone for good
                } else if (status >= 200 && status < 300) {
                    delivered++;
                } else {
                    lastFailure = "Push service answered " + status;
                }
            } catch (Exception ex) {
                lastFailure = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            }
        }
        // One device getting it is enough; only retry when nobody did and something was a real failure.
        return delivered > 0 ? null : lastFailure;
    }

    @Override
    public long pending() {
        return outbox.countByStatus(PushOutbox.PENDING);
    }

    @Override
    public long failed() {
        return outbox.countByStatus(PushOutbox.FAILED);
    }

    @Override
    public void cleanUp() {
        try {
            outbox.deleteFinishedBefore(LocalDateTime.now().minusDays(7));
        } catch (Exception ex) {
            log.debug("Push outbox clean-up skipped: {}", ex.getMessage());
        }
    }

    static String quote(String s) {
        if (s == null) {
            return "\"\"";
        }
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> {
                    if (c < 0x20) {
                        b.append(String.format("\\u%04x", (int) c));
                    } else {
                        b.append(c);
                    }
                }
            }
        }
        return b.append('"').toString();
    }
}
