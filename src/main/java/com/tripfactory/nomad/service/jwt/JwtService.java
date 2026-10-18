package com.tripfactory.nomad.service.jwt;

import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMs;
    // Reset-link tokens are signed with a DIFFERENT key derived from the secret, so a
    // reset token can never be accepted as a login token by the auth filter.
    private final SecretKey resetKey;

    private static final long RESET_EXPIRATION_MS = 30L * 60L * 1000L;

    public JwtService(@Value("${nomad.jwt.secret}") String secret,
            @Value("${nomad.jwt.expiration-ms}") long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(toBase64(secret)));
        this.expirationMs = expirationMs;
        this.resetKey = deriveResetKey(secret);
    }

    private static SecretKey deriveResetKey(String secret) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] raw = ((secret == null ? "" : secret) + ":password-reset")
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);
            return Keys.hmacShaKeyFor(md.digest(raw));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    /** Short-lived (30 min) token for the "forgot password" email link. */
    public String generateResetToken(String email, String fingerprint) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(email)
                .claim("fp", fingerprint)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + RESET_EXPIRATION_MS))
                .signWith(resetKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /** Throws if the token is invalid, tampered with or expired. */
    public String extractResetEmail(String token) {
        return parseResetClaims(token).getSubject();
    }

    /** Throws if the token is invalid, tampered with or expired. */
    public String extractResetFingerprint(String token) {
        Object fp = parseResetClaims(token).get("fp");
        return fp == null ? "" : fp.toString();
    }

    private Claims parseResetClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(resetKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String generateToken(String subject) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractSubject(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private String toBase64(String raw) {
        if (raw == null) {
            return "";
        }
        return java.util.Base64.getEncoder().encodeToString(raw.getBytes());
    }
}