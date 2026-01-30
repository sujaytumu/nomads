package com.tripfactory.nomad.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.tripfactory.nomad.domain.enums.PaymentStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PaymentCreateResponse {

    private Long paymentId;
    private Long tripRequestId;
    private BigDecimal amount;
    private String razorpayOrderId;
    private PaymentStatus paymentStatus;
    private LocalDateTime createdAt;
}