package com.tripfactory.nomad.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tripfactory.nomad.domain.entity.Payment;
import com.tripfactory.nomad.domain.enums.PaymentStatus;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // A trip can have several payment rows (abandoned/failed orders plus a
    // retry), so never look one up by trip id alone - that would throw as soon
    // as there are two. Ask for the one with the status you need.
    Optional<Payment> findFirstByTripRequestIdAndPaymentStatusOrderByIdDesc(Long tripRequestId, PaymentStatus paymentStatus);

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
}
