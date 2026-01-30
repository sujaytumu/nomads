package com.tripfactory.nomad.api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tripfactory.nomad.api.dto.TripResponse;
import com.tripfactory.nomad.domain.entity.TripPlan;
import com.tripfactory.nomad.domain.entity.TripRequest;
import com.tripfactory.nomad.repository.TripPlanRepository;
import com.tripfactory.nomad.repository.TripRequestRepository;
import com.tripfactory.nomad.service.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/share")
@RequiredArgsConstructor
public class ShareController {

    private final TripRequestRepository tripRequestRepository;
    private final TripPlanRepository tripPlanRepository;

    @GetMapping("/{token}")
    public ResponseEntity<TripResponse> getSharedTrip(@PathVariable String token) {
        TripRequest tripRequest = tripRequestRepository.findByShareToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Shared trip not found"));
        List<TripPlan> plans = tripPlanRepository.findByTripRequestIdOrderByDayNumberAscStartTimeAsc(
                tripRequest.getId());

        TripResponse response = new TripResponse();
        response.setTripRequestId(tripRequest.getId());
        response.setUserId(tripRequest.getUser().getId());
        response.setCity(tripRequest.getCity());
        response.setShareToken(tripRequest.getShareToken());
        response.setStatus(tripRequest.getStatus());
        response.setCreatedAt(tripRequest.getCreatedAt());

        response.setPlans(plans.stream().map(plan -> {
            com.tripfactory.nomad.api.dto.TripPlanItemResponse item = new com.tripfactory.nomad.api.dto.TripPlanItemResponse();
            item.setDayNumber(plan.getDayNumber());
            item.setPlaceId(plan.getPlace().getId());
            item.setPlaceName(plan.getPlace().getName());
            item.setStartTime(plan.getStartTime());
            item.setEndTime(plan.getEndTime());
            item.setDistanceFromPrevious(plan.getDistanceFromPrevious());
            return item;
        }).toList());

        return ResponseEntity.ok(response);
    }
}