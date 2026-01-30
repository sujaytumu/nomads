package com.tripfactory.nomad.api.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tripfactory.nomad.api.dto.PackageDetailResponse;
import com.tripfactory.nomad.api.dto.PackageSummaryResponse;
import com.tripfactory.nomad.api.dto.PaymentCreateRequest;
import com.tripfactory.nomad.api.dto.PackageEnrollRequest;
import com.tripfactory.nomad.api.dto.PaymentCreateResponse;
import com.tripfactory.nomad.domain.entity.TripRequest;
import com.tripfactory.nomad.domain.entity.User;
import com.tripfactory.nomad.repository.TripRequestRepository;
import com.tripfactory.nomad.repository.UserRepository;
import com.tripfactory.nomad.service.PackageService;
import com.tripfactory.nomad.service.PaymentService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/packages")
@RequiredArgsConstructor
public class PackageController {

    private final PackageService packageService;
    private final TripRequestRepository tripRequestRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;

    @GetMapping("/homepage")
    public ResponseEntity<List<PackageSummaryResponse>> homepagePackages() {
        return ResponseEntity.ok(packageService.getHomepagePackages());
    }

    @GetMapping("/all")
    public ResponseEntity<List<PackageSummaryResponse>> allPackages() {
        return ResponseEntity.ok(packageService.getAllPackages());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PackageDetailResponse> packageDetails(@PathVariable Long id) {
        PackageDetailResponse d = packageService.getPackageById(id);
        if (d == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(d);
    }

    @PostMapping("/{id}/enroll")
    public ResponseEntity<PaymentCreateResponse> enrollPackage(@PathVariable Long id, @Valid @RequestBody PackageEnrollRequest request) {
        // create a TripRequest for the current user and the package
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

        PackageDetailResponse d = packageService.getPackageById(id);
        if (d == null) return ResponseEntity.notFound().build();

        TripRequest trip = new TripRequest();
        trip.setUser(user);
        trip.setCity(d.getPlaces() != null && !d.getPlaces().isEmpty() ? d.getPlaces().get(0).getCity() : user.getCity());
        trip.setWeekendType(com.tripfactory.nomad.domain.enums.WeekendType.TWO_DAY);
        trip.setInterest(com.tripfactory.nomad.domain.enums.InterestType.NATURE);
        trip.setTravelMode(com.tripfactory.nomad.domain.enums.TravelMode.SOLO);
        trip.setPickupRequired(Boolean.FALSE);
        trip = tripRequestRepository.save(trip);

        // delegate to payment service
        PaymentCreateRequest payReq = new PaymentCreateRequest();
        payReq.setTripRequestId(trip.getId());
        payReq.setAmount(request.getAmount());
        PaymentCreateResponse resp = paymentService.createOrder(payReq);
        return new ResponseEntity<>(resp, HttpStatus.CREATED);
    }
}
