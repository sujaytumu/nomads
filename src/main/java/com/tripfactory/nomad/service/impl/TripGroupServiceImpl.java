package com.tripfactory.nomad.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.tripfactory.nomad.api.dto.TripGroupMemberResponse;
import com.tripfactory.nomad.api.dto.TripGroupResponse;
import com.tripfactory.nomad.domain.entity.TripGroup;
import com.tripfactory.nomad.domain.entity.TripRequest;
import com.tripfactory.nomad.repository.TripGroupRepository;
import com.tripfactory.nomad.repository.TripRequestRepository;
import com.tripfactory.nomad.service.TripGroupService;
import com.tripfactory.nomad.service.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TripGroupServiceImpl implements TripGroupService {

    private final TripGroupRepository tripGroupRepository;
    private final TripRequestRepository tripRequestRepository;

    @Override
    public TripGroupResponse getGroup(Long groupId) {
        TripGroup group = tripGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        TripGroupResponse response = new TripGroupResponse();
        response.setId(group.getId());
        response.setCity(group.getCity());
        response.setInterest(group.getInterest());
        response.setWeekendType(group.getWeekendType());
        response.setStatus(group.getStatus());
        response.setCreatedAt(group.getCreatedAt());
        response.setSize(tripRequestRepository.countByGroupId(groupId));
        return response;
    }

    @Override
    public List<TripGroupMemberResponse> getGroupMembers(Long groupId) {
        TripGroup group = tripGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        List<TripRequest> members = tripRequestRepository.findGroupMembers(group.getId());
        return members.stream().map(trip -> {
            TripGroupMemberResponse response = new TripGroupMemberResponse();
            response.setTripRequestId(trip.getId());
            response.setUserId(trip.getUser().getId());
            response.setName(trip.getUser().getName());
            response.setCity(trip.getUser().getCity());
            response.setInterestType(trip.getUser().getInterestType());
            response.setTripStatus(trip.getStatus());
            response.setJoinedAt(trip.getCreatedAt());
            return response;
        }).collect(Collectors.toList());
    }
}