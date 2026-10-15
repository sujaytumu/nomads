package com.tripfactory.nomad.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.tripfactory.nomad.api.dto.TripGroupMemberResponse;
import com.tripfactory.nomad.api.dto.TripGroupResponse;
import com.tripfactory.nomad.domain.entity.TripGroup;
import com.tripfactory.nomad.domain.entity.TripRequest;
import com.tripfactory.nomad.domain.enums.TripStatus;
import com.tripfactory.nomad.repository.GroupDepartureRepository;
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
    private final GroupDepartureRepository groupDepartureRepository;

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
        // A scheduled group trip's size is its paid travellers - unpaid
        // reservations are not "in the group" until they pay.
        response.setSize(groupDepartureRepository.existsByGroupId(groupId)
                ? tripRequestRepository.countByGroupIdAndStatus(groupId, TripStatus.CONFIRMED)
                : tripRequestRepository.countByGroupId(groupId));
        return response;
    }

    @Override
    public List<TripGroupMemberResponse> getGroupMembers(Long groupId) {
        TripGroup group = tripGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        List<TripRequest> members = tripRequestRepository.findGroupMembers(group.getId());
        // Strangers travelling together: only people who have paid are members,
        // and they see each other by first name only (no user ids / surnames).
        boolean strangerTrip = groupDepartureRepository.existsByGroupId(group.getId());
        if (strangerTrip) {
            members = members.stream()
                    .filter(trip -> trip.getStatus() == TripStatus.CONFIRMED)
                    .collect(Collectors.toList());
        }
        return members.stream().map(trip -> {
            TripGroupMemberResponse response = new TripGroupMemberResponse();
            response.setTripRequestId(trip.getId());
            response.setUserId(strangerTrip ? null : trip.getUser().getId());
            response.setName(strangerTrip ? firstName(trip.getUser().getName()) : trip.getUser().getName());
            response.setCity(trip.getUser().getCity());
            response.setInterestType(trip.getUser().getInterestType());
            response.setTripStatus(trip.getStatus());
            response.setJoinedAt(trip.getCreatedAt());
            return response;
        }).collect(Collectors.toList());
    }

    private static String firstName(String fullName) {
        String name = fullName == null ? "" : fullName.trim();
        int space = name.indexOf(' ');
        return space > 0 ? name.substring(0, space) : name;
    }
}
