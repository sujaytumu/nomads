package com.tripfactory.nomad.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import com.razorpay.RazorpayClient;
import com.tripfactory.nomad.api.dto.PaymentCreateRequest;
import com.tripfactory.nomad.api.dto.PaymentCreateResponse;
import com.tripfactory.nomad.config.RazorpayProperties;
import com.tripfactory.nomad.domain.entity.Payment;
import com.tripfactory.nomad.domain.entity.TripRequest;
import com.tripfactory.nomad.domain.entity.User;
import com.tripfactory.nomad.domain.enums.PaymentStatus;
import com.tripfactory.nomad.domain.enums.TripStatus;
import com.tripfactory.nomad.repository.PaymentRepository;
import com.tripfactory.nomad.repository.TripRequestRepository;
import com.tripfactory.nomad.security.AuthorizationService;
import com.tripfactory.nomad.service.NotificationService;
import com.tripfactory.nomad.service.exception.BadRequestException;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    private static final long TRIP_ID = 12L;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private TripRequestRepository tripRequestRepository;

    @Mock
    private RazorpayClient razorpayClient;

    @Mock
    private RazorpayProperties razorpayProperties;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private ReceiptPdfGenerator receiptPdfGenerator;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void useFakeOrders() {
        // Dev mode creates fake orders, so no real Razorpay call is ever made.
        ReflectionTestUtils.setField(paymentService, "devPayments", true);
    }

    private TripRequest trip(TripStatus status) {
        User user = new User();
        user.setEmail("a@b.com");

        TripRequest trip = new TripRequest();
        trip.setId(TRIP_ID);
        trip.setUser(user);
        trip.setStatus(status);
        trip.setEstimatedCost(new BigDecimal("1500"));
        return trip;
    }

    private PaymentCreateRequest request() {
        PaymentCreateRequest request = new PaymentCreateRequest();
        request.setTripRequestId(TRIP_ID);
        return request;
    }

    @Test
    void createOrder_plannedTrip_startsPayment() {
        TripRequest trip = trip(TripStatus.PLANNED);
        when(tripRequestRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(authorizationService.canAccessTrip(TRIP_ID)).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentCreateResponse response = paymentService.createOrder(request());

        assertThat(response.getRazorpayOrderId()).startsWith("dev_order_");
        assertThat(response.getAmount()).isEqualByComparingTo("1500");
        assertThat(trip.getStatus()).isEqualTo(TripStatus.PAYMENT_PENDING);
    }

    @Test
    void createOrder_pendingTrip_canBeRetried() {
        // "Continue payment" in the UI relies on this staying allowed.
        TripRequest trip = trip(TripStatus.PAYMENT_PENDING);
        when(tripRequestRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(authorizationService.canAccessTrip(TRIP_ID)).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentCreateResponse response = paymentService.createOrder(request());

        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.CREATED);
        assertThat(trip.getStatus()).isEqualTo(TripStatus.PAYMENT_PENDING);
    }

    @Test
    void createOrder_confirmedTrip_isRejectedAndStaysConfirmed() {
        TripRequest trip = trip(TripStatus.CONFIRMED);
        when(tripRequestRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(authorizationService.canAccessTrip(TRIP_ID)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> paymentService.createOrder(request()));

        assertThat(trip.getStatus()).isEqualTo(TripStatus.CONFIRMED);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void createOrder_cancelledTrip_isRejectedAndStaysCancelled() {
        TripRequest trip = trip(TripStatus.CANCELLED);
        when(tripRequestRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(authorizationService.canAccessTrip(TRIP_ID)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> paymentService.createOrder(request()));

        assertThat(trip.getStatus()).isEqualTo(TripStatus.CANCELLED);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void createOrder_someoneElsesTrip_isRejected() {
        TripRequest trip = trip(TripStatus.PLANNED);
        when(tripRequestRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(authorizationService.canAccessTrip(TRIP_ID)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> paymentService.createOrder(request()));

        assertThat(trip.getStatus()).isEqualTo(TripStatus.PLANNED);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void cancelBooking_refundsTheCapturedPayment_evenWithOtherAttempts() {
        // The trip has an abandoned attempt AND a captured one; cancelling must
        // find the captured payment rather than fail on "more than one result".
        TripRequest trip = trip(TripStatus.CONFIRMED);
        Payment captured = new Payment();
        captured.setTripRequest(trip);
        captured.setAmount(new BigDecimal("1500"));
        captured.setRazorpayOrderId("dev_order_123");
        captured.setPaymentStatus(PaymentStatus.CAPTURED);

        when(tripRequestRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(authorizationService.canAccessTrip(TRIP_ID)).thenReturn(true);
        when(paymentRepository.findFirstByTripRequestIdAndPaymentStatusOrderByIdDesc(TRIP_ID, PaymentStatus.CAPTURED))
                .thenReturn(Optional.of(captured));

        paymentService.cancelBooking(TRIP_ID);

        assertThat(captured.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(trip.getStatus()).isEqualTo(TripStatus.CANCELLED);
        verify(notificationService).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void cancelBooking_unpaidTrip_justCancels() {
        TripRequest trip = trip(TripStatus.PAYMENT_PENDING);
        when(tripRequestRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(authorizationService.canAccessTrip(TRIP_ID)).thenReturn(true);
        when(paymentRepository.findFirstByTripRequestIdAndPaymentStatusOrderByIdDesc(TRIP_ID, PaymentStatus.CAPTURED))
                .thenReturn(Optional.empty());

        paymentService.cancelBooking(TRIP_ID);

        assertThat(trip.getStatus()).isEqualTo(TripStatus.CANCELLED);
        verify(paymentRepository, never()).save(any(Payment.class));
    }
}
