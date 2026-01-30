package com.tripfactory.nomad.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tripfactory.nomad.domain.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTripRequestId(Long tripRequestId);

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
}