package com.tripfactory.nomad.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.tripfactory.nomad.domain.entity.User;
import com.tripfactory.nomad.repository.UserRepository;
import com.tripfactory.nomad.service.exception.BadRequestException;
import com.tripfactory.nomad.service.jwt.JwtService;

class PasswordResetServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private NotificationService notificationService;
    private JwtService jwtService;
    private PasswordResetService service;
    private User user;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        notificationService = mock(NotificationService.class);
        jwtService = new JwtService("test-secret-test-secret-test-secret-123", 86400000L);
        service = new PasswordResetService(userRepository, passwordEncoder, jwtService, notificationService,
                "https://app.example.com/");
        user = new User();
        user.setName("Asha");
        user.setEmail("asha@example.com");
        user.setPasswordHash("old-hash");
        when(userRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpassword1")).thenReturn("new-hash");
    }

    private String sentToken() {
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(notificationService).sendEmail(eq("asha@example.com"), anyString(), body.capture());
        String text = body.getValue();
        int start = text.indexOf("/auth?reset=") + "/auth?reset=".length();
        int end = text.indexOf('\n', start);
        return text.substring(start, end).trim();
    }

    @Test
    void requestReset_unknownEmail_sendsNothing() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());
        service.requestReset("nobody@example.com");
        verify(notificationService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void requestReset_knownEmail_emailsLinkToFrontend() {
        service.requestReset("asha@example.com");
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(notificationService).sendEmail(eq("asha@example.com"), anyString(), body.capture());
        assertThat(body.getValue()).contains("https://app.example.com/auth?reset=");
    }

    @Test
    void resetToken_isNotAValidLoginToken() {
        service.requestReset("asha@example.com");
        assertThat(jwtService.isTokenValid(sentToken())).isFalse();
    }

    @Test
    void resetPassword_validToken_updatesHash() {
        service.requestReset("asha@example.com");
        service.resetPassword(sentToken(), "newpassword1");
        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        verify(userRepository).save(user);
    }

    @Test
    void resetPassword_linkStopsWorkingOnceUsed() {
        service.requestReset("asha@example.com");
        String token = sentToken();
        service.resetPassword(token, "newpassword1");
        // password hash changed above, so the same link must now be rejected
        assertThatThrownBy(() -> service.resetPassword(token, "newpassword1"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void resetPassword_garbageToken_rejected() {
        assertThatThrownBy(() -> service.resetPassword("not-a-token", "newpassword1"))
                .isInstanceOf(BadRequestException.class);
        verify(userRepository, never()).save(user);
    }
}
