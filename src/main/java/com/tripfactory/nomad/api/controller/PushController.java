package com.tripfactory.nomad.api.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.tripfactory.nomad.api.dto.PushSubscribeRequest;
import com.tripfactory.nomad.domain.entity.PushSubscription;
import com.tripfactory.nomad.domain.entity.User;
import com.tripfactory.nomad.repository.PushSubscriptionRepository;
import com.tripfactory.nomad.repository.UserRepository;
import com.tripfactory.nomad.service.PushService;
import com.tripfactory.nomad.service.exception.BadRequestException;
import com.tripfactory.nomad.service.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

/** Lets a signed-in user turn notifications on or off for the device they are using. */
@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushController {

    private final PushService pushService;
    private final PushSubscriptionRepository subscriptions;
    private final UserRepository userRepository;

    @GetMapping("/key")
    public ResponseEntity<Map<String, Object>> key() {
        return ResponseEntity.ok(Map.of("enabled", pushService.isEnabled(), "key", pushService.publicKey()));
    }

    @PostMapping("/subscribe")
    @Transactional
    public ResponseEntity<Void> subscribe(@Valid @RequestBody PushSubscribeRequest request) {
        if (!pushService.isEnabled()) {
            throw new BadRequestException("Push notifications aren't set up on the server yet");
        }
        if (isBlank(request.getP256dh()) || isBlank(request.getAuth())) {
            throw new BadRequestException("Subscription is missing its keys");
        }
        if (!request.getEndpoint().startsWith("https://")) {
            throw new BadRequestException("Invalid subscription");
        }
        User user = currentUser();
        PushSubscription s = subscriptions.findByEndpoint(request.getEndpoint()).orElseGet(PushSubscription::new);
        s.setUser(user);
        s.setEndpoint(request.getEndpoint());
        s.setP256dh(request.getP256dh());
        s.setAuth(request.getAuth());
        subscriptions.save(s);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/unsubscribe")
    @Transactional
    public ResponseEntity<Void> unsubscribe(@Valid @RequestBody PushSubscribeRequest request) {
        User user = currentUser();
        subscriptions.findByEndpoint(request.getEndpoint())
                .filter(s -> s.getUser().getId().equals(user.getId()))
                .ifPresent(subscriptions::delete);
        return ResponseEntity.noContent().build();
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
