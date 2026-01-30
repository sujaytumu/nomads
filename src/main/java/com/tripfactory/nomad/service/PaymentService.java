package com.tripfactory.nomad.service;

import com.tripfactory.nomad.api.dto.PaymentCreateRequest;
import com.tripfactory.nomad.api.dto.PaymentCreateResponse;
import com.tripfactory.nomad.api.dto.PaymentVerifyRequest;

public interface PaymentService {

    PaymentCreateResponse createOrder(PaymentCreateRequest request);

    PaymentCreateResponse verifyPayment(PaymentVerifyRequest request);

    void handleWebhook(String payload, String signature);
}