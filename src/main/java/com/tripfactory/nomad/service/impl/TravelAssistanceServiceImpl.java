package com.tripfactory.nomad.service.impl;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tripfactory.nomad.api.dto.PickupAdminUpdateRequest;
import com.tripfactory.nomad.api.dto.PickupConfirmRequest;
import com.tripfactory.nomad.api.dto.TravelAssistanceResponse;
import com.tripfactory.nomad.domain.entity.TravelAssistance;
import com.tripfactory.nomad.domain.entity.TripRequest;
import com.tripfactory.nomad.domain.entity.Vehicle;
import com.tripfactory.nomad.domain.enums.AssistanceStatus;
import com.tripfactory.nomad.domain.enums.AvailabilityStatus;
import com.tripfactory.nomad.domain.enums.TravelMode;
import com.tripfactory.nomad.domain.enums.VehicleType;
import com.tripfactory.nomad.repository.TravelAssistanceRepository;
import com.tripfactory.nomad.repository.TripRequestRepository;
import com.tripfactory.nomad.repository.VehicleRepository;
import com.tripfactory.nomad.service.TravelAssistanceService;
import com.tripfactory.nomad.service.NotificationService;
import com.tripfactory.nomad.service.exception.BadRequestException;
import com.tripfactory.nomad.service.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TravelAssistanceServiceImpl implements TravelAssistanceService {

    private final TravelAssistanceRepository travelAssistanceRepository;
    private final TripRequestRepository tripRequestRepository;
    private final VehicleRepository vehicleRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public TravelAssistanceResponse assignAssistance(Long tripRequestId) {
        TripRequest tripRequest = tripRequestRepository.findById(tripRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        if (!Boolean.TRUE.equals(tripRequest.getPickupRequired())) {
            throw new BadRequestException("Pickup not required for this trip");
        }

        travelAssistanceRepository.findByTripRequestId(tripRequestId).ifPresent(existing -> {
            throw new BadRequestException("Travel assistance already assigned");
        });

        int groupSize = tripRequest.getTravelMode() == TravelMode.GROUP ? 4 : 1;
        List<VehicleType> vehicleTypes = resolveVehicleTypes(groupSize);
        List<Vehicle> candidates = vehicleRepository.findByAvailabilityStatusAndVehicleTypeIn(
                AvailabilityStatus.AVAILABLE, vehicleTypes);

        if (candidates.isEmpty()) {
            throw new BadRequestException("No vehicles available");
        }

        Vehicle vehicle = candidates.get(0);
        vehicle.setAvailabilityStatus(AvailabilityStatus.ASSIGNED);
        vehicleRepository.save(vehicle);

        TravelAssistance assistance = new TravelAssistance();
        assistance.setTripRequest(tripRequest);
        assistance.setPickupLocation(tripRequest.getCity() + " Pickup Point");
        assistance.setPickupTime(LocalDateTime.now().plusHours(2));
        assistance.setVehicle(vehicle);
        assistance.setRouteMapUrl(null);
        assistance.setStatus(AssistanceStatus.ASSIGNED);

        TravelAssistance saved = travelAssistanceRepository.save(assistance);
        String phone = saved.getTripRequest().getUser().getPhoneNumber();
        if (phone != null && !phone.isBlank()) {
            notificationService.sendSms(phone,
                "NOMAD: Pickup assigned at " + saved.getPickupTime() + " for trip "
                    + saved.getTripRequest().getId());
        }
        return toResponse(saved);
    }

    @Override
    public TravelAssistanceResponse getAssistance(Long tripRequestId) {
        TravelAssistance assistance = travelAssistanceRepository.findByTripRequestId(tripRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Travel assistance not found"));
        return toResponse(assistance);
    }

    @Override
    @Transactional
    public TravelAssistanceResponse confirmPickup(Long tripRequestId, PickupConfirmRequest request) {
        TravelAssistance assistance = travelAssistanceRepository.findByTripRequestId(tripRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Travel assistance not found"));

        assistance.setPickupTime(request.getPickupTime());
        assistance.setStatus(AssistanceStatus.CONFIRMED);
        TravelAssistance saved = travelAssistanceRepository.save(assistance);

        String email = saved.getTripRequest().getUser().getEmail();
        notificationService.sendEmail(email, "NOMAD Pickup अपडेट",
            "Pickup time confirmed at " + saved.getPickupTime() + " for trip "
                + saved.getTripRequest().getId());
        String phone = saved.getTripRequest().getUser().getPhoneNumber();
        if (phone != null && !phone.isBlank()) {
            notificationService.sendSms(phone,
                "NOMAD: Pickup confirmed at " + saved.getPickupTime() + " for trip "
                    + saved.getTripRequest().getId());
        }
        return toResponse(saved);
    }

    @Override
    @Transactional
    public TravelAssistanceResponse updatePickupAdmin(Long tripRequestId, PickupAdminUpdateRequest request) {
        TravelAssistance assistance = travelAssistanceRepository.findByTripRequestId(tripRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Travel assistance not found"));

        if (request.getPickupTime() != null) {
            assistance.setPickupTime(request.getPickupTime());
        }
        if (request.getStatus() != null) {
            assistance.setStatus(request.getStatus());
        }
        if (request.getRouteMapUrl() != null) {
            assistance.setRouteMapUrl(request.getRouteMapUrl());
        }
        if (request.getVehicleId() != null && !request.getVehicleId().equals(assistance.getVehicle().getId())) {
            Vehicle newVehicle = vehicleRepository.findById(request.getVehicleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
            if (newVehicle.getAvailabilityStatus() != AvailabilityStatus.AVAILABLE) {
                throw new BadRequestException("Selected vehicle is not available");
            }
            Vehicle oldVehicle = assistance.getVehicle();
            oldVehicle.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
            vehicleRepository.save(oldVehicle);

            newVehicle.setAvailabilityStatus(AvailabilityStatus.ASSIGNED);
            vehicleRepository.save(newVehicle);
            assistance.setVehicle(newVehicle);
        }

        TravelAssistance saved = travelAssistanceRepository.save(assistance);
        String email = saved.getTripRequest().getUser().getEmail();
        notificationService.sendEmail(email, "NOMAD Pickup अपडेट",
            "Pickup details updated for trip " + saved.getTripRequest().getId());
        String phone = saved.getTripRequest().getUser().getPhoneNumber();
        if (phone != null && !phone.isBlank()) {
            notificationService.sendSms(phone,
                "NOMAD: Pickup updated for trip " + saved.getTripRequest().getId());
        }
        return toResponse(saved);
    }

    private List<VehicleType> resolveVehicleTypes(int groupSize) {
        if (groupSize <= 3) {
            return Arrays.asList(VehicleType.CAB, VehicleType.MINI_BUS, VehicleType.BUS);
        }
        if (groupSize <= 6) {
            return Arrays.asList(VehicleType.MINI_BUS, VehicleType.BUS);
        }
        return Arrays.asList(VehicleType.BUS);
    }

    private TravelAssistanceResponse toResponse(TravelAssistance assistance) {
        TravelAssistanceResponse response = new TravelAssistanceResponse();
        response.setTripRequestId(assistance.getTripRequest().getId());
        response.setPickupLocation(assistance.getPickupLocation());
        response.setPickupTime(assistance.getPickupTime());
        response.setVehicleId(assistance.getVehicle().getId());
        response.setVehicleType(assistance.getVehicle().getVehicleType());
        response.setDriverName(assistance.getVehicle().getDriverName());
        response.setVehicleNumber(assistance.getVehicle().getVehicleNumber());
        response.setRouteMapUrl(assistance.getRouteMapUrl());
        response.setStatus(assistance.getStatus());
        return response;
    }
}