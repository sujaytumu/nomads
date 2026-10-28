package com.tripfactory.nomad.service.impl;

import java.security.Security;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.tripfactory.nomad.domain.entity.PushSubscription;
import com.tripfactory.nomad.repository.PushSubscriptionRepository;
import com.tripfactory.nomad.service.PushService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import nl.martijndwars.webpush.Notification;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PushServiceImpl implements PushService {

    private static final Logger log = LoggerFactory.getLogger(PushServiceImpl.class);

    private final PushSubscriptionRepository subscriptions;

    @Value("${nomad.push.public-key:}")
    private String publicKey;

    @Value("${nomad.push.private-key:}")
    private String privateKey;

    @Value("${nomad.push.subject:mailto:admin@nomad.app}")
    private String subject;

    private nl.martijndwars.webpush.PushService sender;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

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

    @PreDestroy
    void stop() {
        executor.shutdown();
    }

    @Override
    public boolean isEnabled() {
        return sender != null;
    }

    @Override
    public String publicKey() {
        return sender == null ? "" : publicKey.trim();
    }

    @Override
    public void sendToUser(Long userId, String title, String body, String link) {
        if (sender == null || userId == null) {
            return;
        }
        executor.submit(() -> deliver(userId, title, body, link));
    }

    private void deliver(Long userId, String title, String body, String link) {
        try {
            List<PushSubscription> subs = subscriptions.findByUserId(userId);
            if (subs.isEmpty()) {
                return;
            }
            String payload = "{\"title\":" + quote(title) + ",\"body\":" + quote(body) + ",\"url\":" + quote(link == null ? "/" : link) + "}";
            for (PushSubscription s : subs) {
                try {
                    var response = sender.send(new Notification(s.getEndpoint(), s.getP256dh(), s.getAuth(), payload));
                    int status = response.getStatusLine().getStatusCode();
                    if (status == 404 || status == 410) {
                        subscriptions.deleteById(s.getId());
                    }
                } catch (Exception ex) {
                    log.debug("Push to subscription {} failed: {}", s.getId(), ex.getMessage());
                }
            }
        } catch (Exception ex) {
            log.warn("Push send failed for user {}: {}", userId, ex.getMessage());
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
