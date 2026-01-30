package com.tripfactory.nomad.service.impl;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tripfactory.nomad.api.dto.PaymentCreateRequest;
import com.tripfactory.nomad.api.dto.PaymentCreateResponse;
import com.tripfactory.nomad.api.dto.PaymentVerifyRequest;
import com.tripfactory.nomad.domain.entity.Payment;
import com.tripfactory.nomad.domain.entity.TripRequest;
import com.tripfactory.nomad.domain.enums.PaymentStatus;
import com.tripfactory.nomad.domain.enums.TripStatus;
import com.tripfactory.nomad.repository.PaymentRepository;
import com.tripfactory.nomad.repository.TripRequestRepository;
import com.tripfactory.nomad.service.PaymentService;
import com.tripfactory.nomad.service.NotificationService;
import com.tripfactory.nomad.service.exception.BadRequestException;
import com.tripfactory.nomad.service.exception.ResourceNotFoundException;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import com.tripfactory.nomad.config.RazorpayProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final TripRequestRepository tripRequestRepository;
    private final RazorpayClient razorpayClient;
    private final RazorpayProperties razorpayProperties;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);
    @Value("${nomad.dev-payments:false}")
    private boolean devPayments;

    @Override
    @Transactional
    public PaymentCreateResponse createOrder(PaymentCreateRequest request) {
        TripRequest tripRequest = tripRequestRepository.findById(request.getTripRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        if (request.getAmount() == null) {
            throw new BadRequestException("amount is required");
        }

        long amountPaise = request.getAmount().multiply(new BigDecimal("100")).longValue();
        if (devPayments) {
            log.info("devPayments enabled - creating fake order for tripId={}", tripRequest.getId());
            String fakeOrderId = "dev_order_" + System.currentTimeMillis();
            Payment payment = new Payment();
            payment.setTripRequest(tripRequest);
            payment.setAmount(request.getAmount());
            payment.setRazorpayOrderId(fakeOrderId);
            payment.setPaymentStatus(PaymentStatus.CREATED);
            Payment saved = paymentRepository.save(payment);

            tripRequest.setStatus(TripStatus.PAYMENT_PENDING);
            tripRequestRepository.save(tripRequest);

            return toResponse(saved);
        }

        Order order = createOrder(amountPaise, tripRequest.getId());

        Payment payment = new Payment();
        payment.setTripRequest(tripRequest);
        payment.setAmount(request.getAmount());
        payment.setRazorpayOrderId(order.get("id"));
        payment.setPaymentStatus(PaymentStatus.CREATED);
        Payment saved = paymentRepository.save(payment);

        tripRequest.setStatus(TripStatus.PAYMENT_PENDING);
        tripRequestRepository.save(tripRequest);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public PaymentCreateResponse verifyPayment(PaymentVerifyRequest request) {
        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        boolean valid = verifySignature(request.getRazorpayOrderId(), request.getRazorpayPaymentId(),
            request.getRazorpaySignature());
        if (!valid) {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new BadRequestException("Payment verification failed");
        }

        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setPaymentStatus(PaymentStatus.CAPTURED);
        Payment saved = paymentRepository.save(payment);

        TripRequest tripRequest = saved.getTripRequest();
        tripRequest.setStatus(TripStatus.CONFIRMED);
        tripRequestRepository.save(tripRequest);

        notificationService.sendEmail(tripRequest.getUser().getEmail(), "NOMAD Payment Confirmed",
            "Payment confirmed for trip " + tripRequest.getId());
        String phone = tripRequest.getUser().getPhoneNumber();
        if (phone != null && !phone.isBlank()) {
            notificationService.sendSms(phone, "NOMAD: Payment confirmed for trip " + tripRequest.getId());
        }

        return toResponse(saved);
    }

    @Override
    @Transactional
    public void handleWebhook(String payload, String signature) {
        String secret = razorpayProperties.getWebhookSecret();
        if (secret == null || secret.isBlank()) {
            throw new BadRequestException("Webhook secret not configured");
        }
        try {
            boolean valid = Utils.verifyWebhookSignature(payload, signature, secret);
            if (!valid) {
                throw new BadRequestException("Invalid webhook signature");
            }

            JsonNode root = objectMapper.readTree(payload);
            String event = root.path("event").asText();
            JsonNode paymentEntity = root.path("payload").path("payment").path("entity");
            String orderId = paymentEntity.path("order_id").asText();
            String paymentId = paymentEntity.path("id").asText();

            Payment payment = paymentRepository.findByRazorpayOrderId(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

            if ("payment.captured".equals(event)) {
                payment.setPaymentStatus(PaymentStatus.CAPTURED);
                payment.setRazorpayPaymentId(paymentId);
                paymentRepository.save(payment);

                TripRequest tripRequest = payment.getTripRequest();
                tripRequest.setStatus(TripStatus.CONFIRMED);
                tripRequestRepository.save(tripRequest);

                notificationService.sendEmail(tripRequest.getUser().getEmail(), "NOMAD Payment Confirmed",
                    "Payment confirmed for trip " + tripRequest.getId());
                String phone = tripRequest.getUser().getPhoneNumber();
                if (phone != null && !phone.isBlank()) {
                    notificationService.sendSms(phone,
                        "NOMAD: Payment confirmed for trip " + tripRequest.getId());
                }
            } else if ("payment.failed".equals(event)) {
                payment.setPaymentStatus(PaymentStatus.FAILED);
                payment.setRazorpayPaymentId(paymentId);
                paymentRepository.save(payment);

                TripRequest tripRequest = payment.getTripRequest();
                tripRequest.setStatus(TripStatus.CANCELLED);
                tripRequestRepository.save(tripRequest);
                String phone = tripRequest.getUser().getPhoneNumber();
                if (phone != null && !phone.isBlank()) {
                    notificationService.sendSms(phone,
                        "NOMAD: Payment failed for trip " + tripRequest.getId());
                }
            }
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("Webhook processing failed");
        }
    }

    private Order createOrder(long amountPaise, Long tripId) {
        try {
            log.debug("Creating Razorpay order: amountPaise={} tripId={}", amountPaise, tripId);
            Map<String, Object> options = new HashMap<>();
            options.put("amount", amountPaise);
            options.put("currency", "INR");
            options.put("receipt", "trip_" + tripId);
            org.json.JSONObject jsonOptions = new org.json.JSONObject();
            for (Map.Entry<String, Object> entry : options.entrySet()) {
                jsonOptions.put(entry.getKey(), entry.getValue());
            }
            return razorpayClient.orders.create(jsonOptions);
        } catch (Exception ex) {
            log.error("Razorpay order creation failed", ex);
            String msg = ex.getMessage() == null ? "Failed to create Razorpay order" : "Failed to create Razorpay order: " + ex.getMessage();
            throw new BadRequestException(msg);
        }
    }

    private boolean verifySignature(String orderId, String paymentId, String signature) {
        try {
            Map<String, String> attributes = new HashMap<>();
            attributes.put("razorpay_order_id", orderId);
            attributes.put("razorpay_payment_id", paymentId);
            attributes.put("razorpay_signature", signature);
            org.json.JSONObject jsonAttributes = new org.json.JSONObject();
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                jsonAttributes.put(entry.getKey(), entry.getValue());
            }
            return Utils.verifyPaymentSignature(jsonAttributes, razorpayProperties.getKeySecret());
        } catch (Exception ex) {
            return false;
        }
    }

    private PaymentCreateResponse toResponse(Payment payment) {
        PaymentCreateResponse response = new PaymentCreateResponse();
        response.setPaymentId(payment.getId());
        response.setTripRequestId(payment.getTripRequest().getId());
        response.setAmount(payment.getAmount());
        response.setRazorpayOrderId(payment.getRazorpayOrderId());
        response.setPaymentStatus(payment.getPaymentStatus());
        response.setCreatedAt(payment.getCreatedAt());
        return response;
    }
}