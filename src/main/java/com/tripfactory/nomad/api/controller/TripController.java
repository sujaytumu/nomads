package com.tripfactory.nomad.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;

import com.tripfactory.nomad.repository.UserRepository;
import com.tripfactory.nomad.domain.entity.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.tripfactory.nomad.api.dto.TripCreateRequest;
import com.tripfactory.nomad.api.dto.TripResponse;
import com.tripfactory.nomad.service.TripService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;
    private final UserRepository userRepository;

    @PostMapping("/create")
    public ResponseEntity<TripResponse> createTrip(@Valid @RequestBody TripCreateRequest request) {
        return new ResponseEntity<>(tripService.createTrip(request), HttpStatus.CREATED);
    }

    @GetMapping("/me")
    public ResponseEntity<List<TripResponse>> getMyTrips() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(tripService.getTripsByUser(user.getId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.canAccessTrip(#id)")
    public ResponseEntity<TripResponse> getTrip(@PathVariable Long id) {
        return ResponseEntity.ok(tripService.getTrip(id));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("@authz.canAccessUser(#userId)")
    public ResponseEntity<List<TripResponse>> getTripsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(tripService.getTripsByUser(userId));
    }
}