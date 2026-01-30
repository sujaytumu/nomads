package com.tripfactory.nomad.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tripfactory.nomad.api.dto.PickupAdminUpdateRequest;
import com.tripfactory.nomad.api.dto.PickupConfirmRequest;
import com.tripfactory.nomad.domain.entity.TravelAssistance;
import com.tripfactory.nomad.domain.entity.TripRequest;
import com.tripfactory.nomad.domain.entity.User;
import com.tripfactory.nomad.domain.entity.Vehicle;
import com.tripfactory.nomad.domain.enums.AssistanceStatus;
import com.tripfactory.nomad.domain.enums.AvailabilityStatus;
import com.tripfactory.nomad.domain.enums.VehicleType;
import com.tripfactory.nomad.repository.TravelAssistanceRepository;
import com.tripfactory.nomad.repository.TripRequestRepository;
import com.tripfactory.nomad.repository.VehicleRepository;
import com.tripfactory.nomad.service.NotificationService;
import com.tripfactory.nomad.service.exception.BadRequestException;
import com.tripfactory.nomad.service.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class TravelAssistanceServiceImplTest {

    @Mock
    private TravelAssistanceRepository travelAssistanceRepository;

    @Mock
    private TripRequestRepository tripRequestRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TravelAssistanceServiceImpl travelAssistanceService;

    @Test
    void confirmPickup_setsConfirmedStatus() {
        User user = new User();
        user.setEmail("a@b.com");
        user.setPhoneNumber("+911234567890");

        TripRequest tripRequest = new TripRequest();
        tripRequest.setId(12L);
        tripRequest.setUser(user);

        Vehicle vehicle = new Vehicle();
        vehicle.setId(99L);
        vehicle.setVehicleType(VehicleType.CAB);

        TravelAssistance assistance = new TravelAssistance();
        assistance.setTripRequest(tripRequest);
        assistance.setVehicle(vehicle);
        assistance.setStatus(AssistanceStatus.ASSIGNED);

        when(travelAssistanceRepository.findByTripRequestId(12L)).thenReturn(Optional.of(assistance));
        when(travelAssistanceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PickupConfirmRequest request = new PickupConfirmRequest();
        request.setPickupTime(LocalDateTime.of(2025, 1, 2, 10, 0));

        travelAssistanceService.confirmPickup(12L, request);

        assertThat(assistance.getStatus()).isEqualTo(AssistanceStatus.CONFIRMED);
        assertThat(assistance.getPickupTime()).isEqualTo(LocalDateTime.of(2025, 1, 2, 10, 0));
        verify(notificationService).sendEmail(any(), any(), any());
        verify(notificationService).sendSms(any(), any());
    }

    @Test
    void updatePickupAdmin_reassignsVehicle() {
        User user = new User();
        user.setEmail("a@b.com");

        TripRequest tripRequest = new TripRequest();
        tripRequest.setId(42L);
        tripRequest.setUser(user);

        Vehicle oldVehicle = new Vehicle();
        oldVehicle.setId(1L);
        oldVehicle.setAvailabilityStatus(AvailabilityStatus.ASSIGNED);

        Vehicle newVehicle = new Vehicle();
        newVehicle.setId(2L);
        newVehicle.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        newVehicle.setVehicleType(VehicleType.MINI_BUS);

        TravelAssistance assistance = new TravelAssistance();
        assistance.setTripRequest(tripRequest);
        assistance.setVehicle(oldVehicle);
        assistance.setStatus(AssistanceStatus.ASSIGNED);

        when(travelAssistanceRepository.findByTripRequestId(42L)).thenReturn(Optional.of(assistance));
        when(vehicleRepository.findById(2L)).thenReturn(Optional.of(newVehicle));
        when(travelAssistanceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PickupAdminUpdateRequest request = new PickupAdminUpdateRequest();
        request.setVehicleId(2L);
        request.setRouteMapUrl("https://maps.example/route");
        request.setStatus(AssistanceStatus.EN_ROUTE);

        travelAssistanceService.updatePickupAdmin(42L, request);

        assertThat(assistance.getVehicle().getId()).isEqualTo(2L);
        assertThat(oldVehicle.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
        assertThat(newVehicle.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.ASSIGNED);
        assertThat(assistance.getRouteMapUrl()).isEqualTo("https://maps.example/route");
        assertThat(assistance.getStatus()).isEqualTo(AssistanceStatus.EN_ROUTE);
    }

    @Test
    void updatePickupAdmin_throwsWhenVehicleUnavailable() {
        TripRequest tripRequest = new TripRequest();
        tripRequest.setId(42L);

        Vehicle oldVehicle = new Vehicle();
        oldVehicle.setId(1L);
        oldVehicle.setAvailabilityStatus(AvailabilityStatus.ASSIGNED);

        Vehicle newVehicle = new Vehicle();
        newVehicle.setId(2L);
        newVehicle.setAvailabilityStatus(AvailabilityStatus.ASSIGNED);

        TravelAssistance assistance = new TravelAssistance();
        assistance.setTripRequest(tripRequest);
        assistance.setVehicle(oldVehicle);

        when(travelAssistanceRepository.findByTripRequestId(42L)).thenReturn(Optional.of(assistance));
        when(vehicleRepository.findById(2L)).thenReturn(Optional.of(newVehicle));

        PickupAdminUpdateRequest request = new PickupAdminUpdateRequest();
        request.setVehicleId(2L);

        assertThrows(BadRequestException.class,
                () -> travelAssistanceService.updatePickupAdmin(42L, request));
    }

    @Test
    void confirmPickup_throwsWhenMissing() {
        when(travelAssistanceRepository.findByTripRequestId(404L)).thenReturn(Optional.empty());
        PickupConfirmRequest request = new PickupConfirmRequest();
        request.setPickupTime(LocalDateTime.now());
        assertThrows(ResourceNotFoundException.class,
                () -> travelAssistanceService.confirmPickup(404L, request));
    }
}
