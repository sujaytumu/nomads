package com.tripfactory.nomad.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tripfactory.nomad.api.dto.TripGroupMemberResponse;
import com.tripfactory.nomad.api.dto.TripGroupResponse;
import com.tripfactory.nomad.domain.entity.TripGroup;
import com.tripfactory.nomad.domain.entity.TripRequest;
import com.tripfactory.nomad.domain.entity.User;
import com.tripfactory.nomad.domain.enums.GroupStatus;
import com.tripfactory.nomad.domain.enums.InterestType;
import com.tripfactory.nomad.domain.enums.TripStatus;
import com.tripfactory.nomad.domain.enums.WeekendType;
import com.tripfactory.nomad.repository.TripGroupRepository;
import com.tripfactory.nomad.repository.TripRequestRepository;
import com.tripfactory.nomad.service.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class TripGroupServiceImplTest {

    @Mock
    private TripGroupRepository tripGroupRepository;

    @Mock
    private TripRequestRepository tripRequestRepository;

    @InjectMocks
    private TripGroupServiceImpl tripGroupService;

    @Test
    void getGroup_returnsSummary() {
        TripGroup group = new TripGroup();
        group.setId(10L);
        group.setCity("Jaipur");
        group.setInterest(InterestType.CULTURE);
        group.setWeekendType(WeekendType.TWO_DAY);
        group.setStatus(GroupStatus.READY);
        group.setCreatedAt(LocalDateTime.now());

        when(tripGroupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(tripRequestRepository.countByGroupId(10L)).thenReturn(3L);

        TripGroupResponse response = tripGroupService.getGroup(10L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getCity()).isEqualTo("Jaipur");
        assertThat(response.getInterest()).isEqualTo(InterestType.CULTURE);
        assertThat(response.getWeekendType()).isEqualTo(WeekendType.TWO_DAY);
        assertThat(response.getStatus()).isEqualTo(GroupStatus.READY);
        assertThat(response.getSize()).isEqualTo(3L);
    }

    @Test
    void getGroup_throwsWhenMissing() {
        when(tripGroupRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> tripGroupService.getGroup(99L));
    }

    @Test
    void getGroupMembers_mapsMembers() {
        TripGroup group = new TripGroup();
        group.setId(7L);
        when(tripGroupRepository.findById(7L)).thenReturn(Optional.of(group));

        User user = new User();
        user.setId(21L);
        user.setName("Asha");
        user.setCity("Jaipur");
        user.setInterestType(InterestType.NATURE);

        TripRequest request = new TripRequest();
        request.setId(55L);
        request.setUser(user);
        request.setStatus(TripStatus.REQUESTED);
        request.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));

        when(tripRequestRepository.findGroupMembers(7L)).thenReturn(List.of(request));

        List<TripGroupMemberResponse> members = tripGroupService.getGroupMembers(7L);

        assertThat(members).hasSize(1);
        TripGroupMemberResponse member = members.get(0);
        assertThat(member.getTripRequestId()).isEqualTo(55L);
        assertThat(member.getUserId()).isEqualTo(21L);
        assertThat(member.getName()).isEqualTo("Asha");
        assertThat(member.getCity()).isEqualTo("Jaipur");
        assertThat(member.getInterestType()).isEqualTo(InterestType.NATURE);
        assertThat(member.getTripStatus()).isEqualTo(TripStatus.REQUESTED);
        assertThat(member.getJoinedAt()).isEqualTo(LocalDateTime.of(2025, 1, 1, 10, 0));
    }
}
