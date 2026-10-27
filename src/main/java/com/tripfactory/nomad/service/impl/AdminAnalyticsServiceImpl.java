package com.tripfactory.nomad.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tripfactory.nomad.domain.entity.Payment;
import com.tripfactory.nomad.domain.entity.TripRequest;
import com.tripfactory.nomad.domain.entity.User;
import com.tripfactory.nomad.repository.PaymentRepository;
import com.tripfactory.nomad.repository.TripRequestRepository;
import com.tripfactory.nomad.repository.UserRepository;
import com.tripfactory.nomad.service.AdminAnalyticsService;
import com.tripfactory.nomad.service.engine.AnalyticsEngine;
import com.tripfactory.nomad.service.engine.AnalyticsEngine.PaymentRow;
import com.tripfactory.nomad.service.engine.AnalyticsEngine.TripRow;
import com.tripfactory.nomad.service.engine.AnalyticsEngine.UserRow;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

    private final UserRepository userRepository;
    private final TripRequestRepository tripRequestRepository;
    private final PaymentRepository paymentRepository;

    @Value("${nomad.zone:Asia/Kolkata}")
    private String zone;

    @Override
    @Transactional(readOnly = true)
    public AnalyticsEngine.Snapshot snapshot(int days) {
        ZoneId z = ZoneId.of(zone);
        List<UserRow> users = userRepository.findAll().stream()
                .map((User u) -> new UserRow(day(u.getCreatedAt())))
                .toList();
        List<TripRow> trips = tripRequestRepository.findAll().stream()
                .map((TripRequest t) -> new TripRow(t.getId(), t.getCity(), t.getStatus(), t.getInterest(), day(t.getCreatedAt())))
                .toList();
        List<PaymentRow> payments = paymentRepository.findAll().stream()
                .filter(p -> p.getTripRequest() != null)
                .map((Payment p) -> new PaymentRow(p.getTripRequest().getId(), p.getPaymentStatus(), p.getAmount(), day(p.getCreatedAt())))
                .toList();
        return AnalyticsEngine.compute(users, trips, payments, LocalDate.now(z), days);
    }

    private static LocalDate day(LocalDateTime t) {
        return t == null ? null : t.toLocalDate();
    }
}
