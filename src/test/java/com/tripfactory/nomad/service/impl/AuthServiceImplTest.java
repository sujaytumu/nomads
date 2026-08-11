package com.tripfactory.nomad.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.tripfactory.nomad.api.dto.AuthLoginRequest;
import com.tripfactory.nomad.api.dto.AuthRegisterRequest;
import com.tripfactory.nomad.api.dto.AuthResponse;
import com.tripfactory.nomad.domain.entity.User;
import com.tripfactory.nomad.domain.enums.InterestType;
import com.tripfactory.nomad.domain.enums.TravelPreference;
import com.tripfactory.nomad.repository.UserRepository;
import com.tripfactory.nomad.service.exception.BadRequestException;
import com.tripfactory.nomad.service.jwt.JwtService;

class AuthServiceImplTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AuthenticationManager authenticationManager;
    private JwtService jwtService;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        authenticationManager = mock(AuthenticationManager.class);
        jwtService = mock(JwtService.class);
        authService = new AuthServiceImpl(userRepository, passwordEncoder, authenticationManager, jwtService);
    }

    private AuthRegisterRequest validRegisterRequest() {
        AuthRegisterRequest req = new AuthRegisterRequest();
        req.setName("Test User");
        req.setEmail("test@example.com");
        req.setPassword("password123");
        req.setCity("Bengaluru");
        req.setLatitude(12.97);
        req.setLongitude(77.59);
        req.setInterestType(InterestType.FOOD);
        req.setTravelPreference(TravelPreference.SOLO);
        return req;
    }

    @Test
    void register_savesUserAndReturnsToken() {
        AuthRegisterRequest req = validRegisterRequest();
        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(req.getPassword())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtService.generateToken(req.getEmail())).thenReturn("jwt-token");

        AuthResponse response = authService.register(req);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUser().getEmail()).isEqualTo(req.getEmail());
        assertThat(response.getUser().getId()).isEqualTo(1L);
    }

    @Test
    void register_rejectsDuplicateEmail() {
        AuthRegisterRequest req = validRegisterRequest();
        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void login_returnsTokenOnValidCredentials() {
        AuthLoginRequest req = new AuthLoginRequest();
        req.setEmail("test@example.com");
        req.setPassword("password123");

        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);

        User user = new User();
        user.setId(1L);
        user.setEmail(req.getEmail());
        user.setPasswordHash("hashed-password");
        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(user));
        when(jwtService.generateToken(req.getEmail())).thenReturn("jwt-token");

        AuthResponse response = authService.login(req);

        assertThat(response.getToken()).isEqualTo("jwt-token");
    }

    @Test
    void login_propagatesBadCredentials() {
        AuthLoginRequest req = new AuthLoginRequest();
        req.setEmail("test@example.com");
        req.setPassword("wrong-password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(req)).isInstanceOf(BadCredentialsException.class);
    }
}
