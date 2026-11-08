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
import com.tripfactory.nomad.service.InboxService;
import com.tripfactory.nomad.service.NotificationService;
import com.tripfactory.nomad.service.exception.BadRequestException;
import com.tripfactory.nomad.service.exception.ResourceNotFoundException;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import com.tripfactory.nomad.config.RazorpayProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.tripfactory.nomad.security.AuthorizationService;
import org.springframework.security.access.AccessDeniedException;
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
    private final AuthorizationService authorizationService;
    private final ItineraryEmailService itineraryEmailService;
    private final InboxService inboxService;
    private final com.tripfactory.nomad.repository.PaymentEventRepository paymentEventRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);
    @Value("${nomad.dev-payments:false}")
    private boolean devPayments;

    @Override
    @Transactional
    public PaymentCreateResponse createOrder(PaymentCreateRequest request) {
        // Lock the trip first: a double tap or a retry waits here and then sees the first request's result.
        tripRequestRepository.lockById(request.getTripRequestId());
        TripRequest tripRequest = tripRequestRepository.findById(request.getTripRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        // The controller already checks access, but this method is also called
        // directly (package enrollment), so enforce it here as well - same as
        // verifyPayment and cancelBooking do.
        if (!authorizationService.canAccessTrip(tripRequest.getId())) {
            throw new AccessDeniedException("Not authorized to pay for this trip");
        }

        // Only a planned trip, or one whose payment was started but not finished,
        // can be paid for. Without this, a second order on a booked trip would
        // flip it back to PAYMENT_PENDING, and a cancelled trip would come back to life.
        if (tripRequest.getStatus() == TripStatus.CONFIRMED) {
            throw new BadRequestException("This trip is already paid and booked");
        }
        if (tripRequest.getStatus() == TripStatus.CANCELLED) {
            throw new BadRequestException("This trip was cancelled and can't be paid for");
        }

        // The amount is never taken from the client - it's always derived from
        // the trip's own estimatedCost, which the server itself already set
        // when the trip was created (Trip Planner) or when a package was
        // enrolled in. Trusting a client-supplied amount here would let a
        // tampered request pay less than the trip actually costs.
        if (tripRequest.getEstimatedCost() == null) {
            throw new BadRequestException("This trip has no cost set yet - cannot create a payment for it");
        }
        BigDecimal amount = tripRequest.getEstimatedCost();

        // An unfinished order for the same amount is reused instead of opening a second one.
        Payment open = paymentRepository
                .findFirstByTripRequestIdAndPaymentStatusOrderByIdDesc(tripRequest.getId(), PaymentStatus.CREATED)
                .orElse(null);
        if (open != null && open.getAmount() != null && open.getAmount().compareTo(amount) == 0) {
            audit(tripRequest.getId(), open.getId(), "ORDER_REUSED", "Same unpaid order returned, amount " + amount);
            if (tripRequest.getStatus() != TripStatus.PAYMENT_PENDING) {
                tripRequest.setStatus(TripStatus.PAYMENT_PENDING);
                tripRequestRepository.save(tripRequest);
            }
            return toResponse(open);
        }

        long amountPaise = amount.multiply(new BigDecimal("100")).longValue();
        if (devPayments) {
            log.info("devPayments enabled - creating fake order for tripId={}", tripRequest.getId());
            String fakeOrderId = "dev_order_" + System.currentTimeMillis();
            Payment payment = new Payment();
            payment.setTripRequest(tripRequest);
            payment.setAmount(amount);
            payment.setRazorpayOrderId(fakeOrderId);
            payment.setPaymentStatus(PaymentStatus.CREATED);
            Payment saved = paymentRepository.save(payment);
            audit(tripRequest.getId(), saved.getId(), "ORDER_CREATED", "Dev order, amount " + amount);

            tripRequest.setStatus(TripStatus.PAYMENT_PENDING);
            tripRequestRepository.save(tripRequest);

            return toResponse(saved);
        }

        Order order = createOrder(amountPaise, tripRequest.getId());

        Payment payment = new Payment();
        payment.setTripRequest(tripRequest);
        payment.setAmount(amount);
        payment.setRazorpayOrderId(order.get("id"));
        payment.setPaymentStatus(PaymentStatus.CREATED);
        Payment saved = paymentRepository.save(payment);
        audit(tripRequest.getId(), saved.getId(), "ORDER_CREATED", "Order " + saved.getRazorpayOrderId() + ", amount " + amount);

        tripRequest.setStatus(TripStatus.PAYMENT_PENDING);
        tripRequestRepository.save(tripRequest);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public PaymentCreateResponse verifyPayment(PaymentVerifyRequest request) {
        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        if (!authorizationService.canAccessTrip(payment.getTripRequest().getId())) {
            throw new AccessDeniedException("Not authorized to verify this payment");
        }

        // Verifying twice (a retry, a slow network) must not email or text the person again.
        if (payment.getPaymentStatus() == PaymentStatus.CAPTURED) {
            audit(payment.getTripRequest().getId(), payment.getId(), "VERIFY_DUPLICATE", "Already paid, nothing repeated");
            return toResponse(payment);
        }

        boolean valid;
        if (devPayments && payment.getRazorpayOrderId() != null && payment.getRazorpayOrderId().startsWith("dev_order_")) {
            log.info("devPayments enabled - skipping real signature check for fake order {}", payment.getRazorpayOrderId());
            valid = true;
        } else {
            valid = verifySignature(request.getRazorpayOrderId(), request.getRazorpayPaymentId(),
                request.getRazorpaySignature());
        }
        if (!valid) {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            audit(payment.getTripRequest().getId(), payment.getId(), "VERIFY_FAILED", "Signature did not match");
            throw new BadRequestException("Payment verification failed");
        }

        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setPaymentStatus(PaymentStatus.CAPTURED);
        Payment saved = paymentRepository.save(payment);
        audit(saved.getTripRequest().getId(), saved.getId(), "VERIFIED", "Paid " + saved.getAmount());

        TripRequest tripRequest = saved.getTripRequest();
        boolean firstConfirmation = tripRequest.getStatus() != TripStatus.CONFIRMED;
        tripRequest.setStatus(TripStatus.CONFIRMED);
        tripRequestRepository.save(tripRequest);

        sendConfirmationEmail(tripRequest);
        String phone = tripRequest.getUser().getPhoneNumber();
        if (phone != null && !phone.isBlank()) {
            notificationService.sendSms(phone, "NOMAD: Payment confirmed for trip " + tripRequest.getId());
        }
        if (firstConfirmation) {
            notifyBooked(tripRequest);
        }

        return toResponse(saved);
    }

    /** Writes one line to the payment trail. Best effort: the trail must never break a payment. */
    private void audit(Long tripId, Long paymentId, String type, String detail) {
        try {
            com.tripfactory.nomad.domain.entity.PaymentEvent e = new com.tripfactory.nomad.domain.entity.PaymentEvent();
            e.setTripRequestId(tripId);
            e.setPaymentId(paymentId);
            e.setType(type);
            e.setDetail(detail != null && detail.length() > 300 ? detail.substring(0, 300) : detail);
            paymentEventRepository.save(e);
        } catch (Exception ex) {
            log.warn("Could not record payment event {} for trip {}: {}", type, tripId, ex.getMessage());
        }
    }

    private void notifyBooked(TripRequest tripRequest) {
        pushInbox(tripRequest, "BOOKING_CONFIRMED", "Booking confirmed \uD83C\uDF89",
            "Your " + cityOf(tripRequest) + " weekend is booked. Set your trip date and arrange pickup.", "/bookings");
    }

    private static String cityOf(TripRequest tripRequest) {
        String city = tripRequest.getCity();
        return city == null || city.isBlank() ? "weekend" : city;
    }

    /** In-app notification. Best effort: a problem here must never undo or fail the payment/cancellation. */
    private void pushInbox(TripRequest tripRequest, String type, String title, String body, String link) {
        try {
            inboxService.push(tripRequest.getUser().getId(), type, title, body, link);
        } catch (Exception ex) {
            log.warn("Could not add inbox notification {} for trip {}: {}", type, tripRequest.getId(), ex.getMessage());
        }
    }

    /**
     * The one email of a booking: the itinerary PDF. A failed email must never
     * roll back the payment that already succeeded (the service never throws).
     */
    private void sendConfirmationEmail(TripRequest tripRequest) {
        if (!itineraryEmailService.sendBookingEmail(tripRequest)) {
            log.warn("Booking email for trip {} was not sent; the user can resend it from My Trips",
                tripRequest.getId());
        }
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

            if ("payment.captured".equals(event) && payment.getPaymentStatus() == PaymentStatus.CAPTURED) {
                // Razorpay sends the same event more than once, and the app may have verified it already.
                audit(payment.getTripRequest().getId(), payment.getId(), "WEBHOOK_DUPLICATE", event);
            } else if ("payment.captured".equals(event)) {
                audit(payment.getTripRequest().getId(), payment.getId(), "WEBHOOK_CAPTURED", "Paid " + payment.getAmount());
                payment.setPaymentStatus(PaymentStatus.CAPTURED);
                payment.setRazorpayPaymentId(paymentId);
                paymentRepository.save(payment);

                TripRequest tripRequest = payment.getTripRequest();
                boolean firstConfirmation = tripRequest.getStatus() != TripStatus.CONFIRMED;
                tripRequest.setStatus(TripStatus.CONFIRMED);
                tripRequestRepository.save(tripRequest);

                sendConfirmationEmail(tripRequest);
                String phone = tripRequest.getUser().getPhoneNumber();
                if (phone != null && !phone.isBlank()) {
                    notificationService.sendSms(phone,
                        "NOMAD: Payment confirmed for trip " + tripRequest.getId());
                }
                if (firstConfirmation) {
                    notifyBooked(tripRequest);
                }
            } else if ("payment.failed".equals(event) && payment.getPaymentStatus() == PaymentStatus.CAPTURED) {
                // A failed attempt that arrives after a successful one must never undo a paid booking.
                audit(payment.getTripRequest().getId(), payment.getId(), "WEBHOOK_FAILED_IGNORED", "Payment was already captured");
            } else if ("payment.failed".equals(event)) {
                payment.setPaymentStatus(PaymentStatus.FAILED);
                payment.setRazorpayPaymentId(paymentId);
                paymentRepository.save(payment);
                audit(payment.getTripRequest().getId(), payment.getId(), "WEBHOOK_FAILED", "Payment failed");

                TripRequest tripRequest = payment.getTripRequest();
                if (tripRequest.getStatus() == TripStatus.CONFIRMED) {
                    // Another attempt for this trip was paid: keep the booking.
                    return;
                }
                tripRequest.setStatus(TripStatus.CANCELLED);
                tripRequestRepository.save(tripRequest);
                String phone = tripRequest.getUser().getPhoneNumber();
                if (phone != null && !phone.isBlank()) {
                    notificationService.sendSms(phone,
                        "NOMAD: Payment failed for trip " + tripRequest.getId());
                }
                pushInbox(tripRequest, "PAYMENT_FAILED", "Payment didn't go through",
                    "Your " + cityOf(tripRequest) + " trip was not booked. You can plan it again any time.", "/trip-planner");
            }
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("Webhook processing failed");
        }
    }

    @Override
    @Transactional
    public PaymentCreateResponse cancelBooking(Long tripRequestId) {
        TripRequest tripRequest = tripRequestRepository.findById(tripRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        if (!authorizationService.canAccessTrip(tripRequestId)) {
            throw new AccessDeniedException("Not authorized to cancel this trip");
        }

        if (tripRequest.getStatus() == TripStatus.CANCELLED) {
            throw new BadRequestException("This trip is already cancelled");
        }

        // A trip can have several payment attempts (an abandoned or failed order
        // followed by a retry), so look for the one that was actually charged
        // instead of assuming there is exactly one row.
        Payment payment = paymentRepository
                .findFirstByTripRequestIdAndPaymentStatusOrderByIdDesc(tripRequestId, PaymentStatus.CAPTURED)
                .orElse(null);

        if (payment != null && payment.getPaymentStatus() == PaymentStatus.CAPTURED) {
            boolean isDevOrder = payment.getRazorpayOrderId() != null
                    && payment.getRazorpayOrderId().startsWith("dev_order_");
            if (devPayments && isDevOrder) {
                log.info("devPayments enabled - skipping real Razorpay refund for fake order {}",
                    payment.getRazorpayOrderId());
            } else {
                try {
                    org.json.JSONObject refundRequest = new org.json.JSONObject();
                    razorpayClient.payments.refund(payment.getRazorpayPaymentId(), refundRequest);
                } catch (Exception ex) {
                    log.error("Razorpay refund failed for payment {}: {}", payment.getRazorpayPaymentId(), ex.getMessage());
                    throw new BadRequestException("Refund could not be processed - please try again or contact support");
                }
            }
            payment.setPaymentStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);
        }

        tripRequest.setStatus(TripStatus.CANCELLED);
        tripRequestRepository.save(tripRequest);

        boolean refunded = payment != null;
        pushInbox(tripRequest, "BOOKING_CANCELLED", "Trip cancelled",
            "Your " + cityOf(tripRequest) + " trip was cancelled." + (refunded ? " Your refund is on its way." : ""), "/bookings");

        return payment != null ? toResponse(payment) : null;
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