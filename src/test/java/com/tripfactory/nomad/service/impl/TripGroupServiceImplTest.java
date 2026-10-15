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
import com.tripfactory.nomad.repository.GroupDepartureRepository;
import com.tripfactory.nomad.repository.TripGroupRepository;
import com.tripfactory.nomad.repository.TripRequestRepository;
import com.tripfactory.nomad.service.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class TripGroupServiceImplTest {

    @Mock
    private TripGroupRepository tripGroupRepository;

    @Mock
    private TripRequestRepository tripRequestRepository;

    @Mock
    private GroupDepartureRepository groupDepartureRepository;

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

    @Test
    void getGroupMembers_strangerTrip_showsOnlyPaidTravellersByFirstName() {
        TripGroup group = new TripGroup();
        group.setId(8L);
        when(tripGroupRepository.findById(8L)).thenReturn(Optional.of(group));
        when(groupDepartureRepository.existsByGroupId(8L)).thenReturn(true);

        TripRequest paid = memberRequest(1L, 31L, "Ravi Kumar", TripStatus.CONFIRMED);
        TripRequest unpaid = memberRequest(2L, 32L, "Meera Nair", TripStatus.PAYMENT_PENDING);
        when(tripRequestRepository.findGroupMembers(8L)).thenReturn(List.of(paid, unpaid));

        List<TripGroupMemberResponse> members = tripGroupService.getGroupMembers(8L);

        assertThat(members).hasSize(1);
        assertThat(members.get(0).getName()).isEqualTo("Ravi");
        assertThat(members.get(0).getUserId()).isNull();
    }

    @Test
    void getGroup_strangerTrip_sizeCountsOnlyPaidTravellers() {
        TripGroup group = new TripGroup();
        group.setId(9L);
        group.setStatus(GroupStatus.CLOSED);
        when(tripGroupRepository.findById(9L)).thenReturn(Optional.of(group));
        when(groupDepartureRepository.existsByGroupId(9L)).thenReturn(true);
        when(tripRequestRepository.countByGroupIdAndStatus(9L, TripStatus.CONFIRMED)).thenReturn(2L);

        assertThat(tripGroupService.getGroup(9L).getSize()).isEqualTo(2L);
        verify(tripRequestRepository, never()).countByGroupId(9L);
    }

    private TripRequest memberRequest(Long tripId, Long userId, String name, TripStatus status) {
        User user = new User();
        user.setId(userId);
        user.setName(name);
        user.setCity("Pune");
        user.setInterestType(InterestType.FOOD);
        TripRequest request = new TripRequest();
        request.setId(tripId);
        request.setUser(user);
        request.setStatus(status);
        request.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        return request;
    }
}
