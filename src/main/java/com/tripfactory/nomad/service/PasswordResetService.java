package com.tripfactory.nomad.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.tripfactory.nomad.domain.entity.User;
import com.tripfactory.nomad.repository.UserRepository;
import com.tripfactory.nomad.service.exception.BadRequestException;
import com.tripfactory.nomad.service.jwt.JwtService;

/**
 * "Forgot password" without any new database column: the emailed link carries a
 * signed, 30-minute token that also embeds a fingerprint of the CURRENT password hash,
 * so the link stops working as soon as the password is changed (one-time use).
 */
@Service
public class PasswordResetService {

    static final String INVALID = "This reset link is invalid or has expired. Please request a new one.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final NotificationService notificationService;
    private final String frontendUrl;

    public PasswordResetService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            JwtService jwtService, NotificationService notificationService,
            @Value("${nomad.frontend-url}") String frontendUrl) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.notificationService = notificationService;
        this.frontendUrl = frontendUrl;
    }

    /** Never reveals whether the email exists (the caller always answers the same way). */
    public void requestReset(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        Optional<User> found = userRepository.findByEmail(email.trim());
        if (found.isEmpty() || found.get().getPasswordHash() == null) {
            return;
        }
        User user = found.get();
        String token = jwtService.generateResetToken(user.getEmail(), fingerprint(user.getPasswordHash()));
        String base = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        String link = base + "/auth?reset=" + token;
        String name = user.getName() == null || user.getName().isBlank() ? "traveller" : user.getName();
        String body = "Hi " + name + ",\n\n"
                + "Someone asked to reset your NOMAD password. Open this link to choose a new one "
                + "(valid for 30 minutes):\n\n" + link + "\n\n"
                + "If this wasn't you, ignore this email - your password stays the same.\n\nNOMAD";
        notificationService.sendEmail(user.getEmail(), "Reset your NOMAD password", body);
    }

    public void resetPassword(String token, String newPassword) {
        String email;
        String fp;
        try {
            email = jwtService.extractResetEmail(token);
            fp = jwtService.extractResetFingerprint(token);
        } catch (Exception ex) {
            throw new BadRequestException(INVALID);
        }
        User user = userRepository.findByEmail(email).orElseThrow(() -> new BadRequestException(INVALID));
        if (user.getPasswordHash() == null || !fingerprint(user.getPasswordHash()).equals(fp)) {
            throw new BadRequestException(INVALID);
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    static String fingerprint(String passwordHash) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(passwordHash.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 16);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
