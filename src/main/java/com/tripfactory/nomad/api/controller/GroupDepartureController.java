package com.tripfactory.nomad.api.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tripfactory.nomad.api.dto.DepartureBookingResponse;
import com.tripfactory.nomad.api.dto.DepartureCreateRequest;
import com.tripfactory.nomad.api.dto.DepartureJoinRequest;
import com.tripfactory.nomad.api.dto.DepartureResponse;
import com.tripfactory.nomad.api.dto.DepartureTravellerResponse;
import com.tripfactory.nomad.api.dto.PaymentCreateRequest;
import com.tripfactory.nomad.api.dto.PaymentCreateResponse;
import com.tripfactory.nomad.domain.entity.User;
import com.tripfactory.nomad.domain.enums.InterestType;
import com.tripfactory.nomad.repository.UserRepository;
import com.tripfactory.nomad.service.GroupDepartureService;
import com.tripfactory.nomad.service.PaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Scheduled group trips that people who have never met book seats on.
 * Browsing (GET list / GET one) is public; everything else needs a login.
 */
@RestController
@RequestMapping("/api/departures")
@RequiredArgsConstructor
public class GroupDepartureController {

    private final GroupDepartureService departureService;
    private final PaymentService paymentService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<DepartureResponse>> list(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) InterestType interest) {
        return ResponseEntity.ok(departureService.listOpen(city, interest));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepartureResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(departureService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepartureResponse> create(@Valid @RequestBody DepartureCreateRequest request) {
        return new ResponseEntity<>(departureService.create(request), HttpStatus.CREATED);
    }

    /**
     * Reserves seats and opens a payment order for them in one step, so the
     * client can go straight to checkout (same flow as package enrolment).
     * The amount is computed server-side from the per-seat price.
     */
    @PostMapping("/{id}/join")
    public ResponseEntity<PaymentCreateResponse> join(@PathVariable Long id,
            @Valid @RequestBody DepartureJoinRequest request) {
        User user = currentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        DepartureBookingResponse booking = departureService.join(id, user.getId(), request.getSeats());

        PaymentCreateRequest paymentRequest = new PaymentCreateRequest();
        paymentRequest.setTripRequestId(booking.getTripRequestId());
        PaymentCreateResponse payment = paymentService.createOrder(paymentRequest);
        return new ResponseEntity<>(payment, HttpStatus.CREATED);
    }

    /** The caller's own booking on this trip, or 204 if they have none. */
    @GetMapping("/{id}/my-booking")
    public ResponseEntity<DepartureBookingResponse> myBooking(@PathVariable Long id) {
        User user = currentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Optional<DepartureBookingResponse> booking = departureService.myBooking(id, user.getId());
        return booking.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** Paid co-travellers (first names only). Only paid travellers on this trip can see it. */
    @GetMapping("/{id}/travellers")
    public ResponseEntity<List<DepartureTravellerResponse>> travellers(@PathVariable Long id) {
        User user = currentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(departureService.travellers(id, user.getId()));
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElse(null);
    }
}
