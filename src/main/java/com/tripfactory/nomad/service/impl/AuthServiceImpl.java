package com.tripfactory.nomad.service.impl;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.tripfactory.nomad.api.dto.AuthLoginRequest;
import com.tripfactory.nomad.api.dto.AuthRegisterRequest;
import com.tripfactory.nomad.api.dto.AuthResponse;
import com.tripfactory.nomad.api.dto.UserResponse;
import com.tripfactory.nomad.domain.entity.User;
import com.tripfactory.nomad.repository.UserRepository;
import com.tripfactory.nomad.service.AuthService;
import com.tripfactory.nomad.service.exception.BadRequestException;
import com.tripfactory.nomad.service.exception.ResourceNotFoundException;
import com.tripfactory.nomad.service.jwt.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Override
    public AuthResponse register(AuthRegisterRequest request) {
        if (request.getEmail() == null || request.getPassword() == null) {
            throw new BadRequestException("Email and password are required");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException("Email already registered");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setCity(request.getCity());
        user.setLatitude(request.getLatitude());
        user.setLongitude(request.getLongitude());
        user.setInterestType(request.getInterestType());
        user.setTravelPreference(request.getTravelPreference());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        User saved = userRepository.save(user);
        String token = jwtService.generateToken(saved.getEmail());

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setUser(toResponse(saved));
        return response;
    }

    @Override
    public AuthResponse login(AuthLoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        if (!authentication.isAuthenticated()) {
            throw new BadRequestException("Invalid credentials");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getPasswordHash() == null) {
            throw new BadRequestException("Password not set for user");
        }

        AuthResponse response = new AuthResponse();
        response.setToken(jwtService.generateToken(user.getEmail()));
        response.setUser(toResponse(user));
        return response;
    }

    @Override
    public UserResponse me(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toResponse(user);
    }

    private UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setCity(user.getCity());
        response.setLatitude(user.getLatitude());
        response.setLongitude(user.getLongitude());
        response.setInterestType(user.getInterestType());
        response.setTravelPreference(user.getTravelPreference());
        response.setRole(user.getRole());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}