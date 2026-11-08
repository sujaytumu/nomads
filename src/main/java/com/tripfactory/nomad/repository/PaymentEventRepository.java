package com.tripfactory.nomad.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.tripfactory.nomad.domain.entity.PaymentEvent;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {

    List<PaymentEvent> findByTripRequestIdOrderByIdAsc(Long tripRequestId);

    List<PaymentEvent> findAllByOrderByIdDesc(Pageable pageable);
}
