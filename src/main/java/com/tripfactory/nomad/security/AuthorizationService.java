package com.tripfactory.nomad.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.tripfactory.nomad.domain.entity.TripRequest;
import com.tripfactory.nomad.domain.entity.User;
import com.tripfactory.nomad.domain.enums.TripStatus;
import com.tripfactory.nomad.domain.enums.UserRole;
import com.tripfactory.nomad.repository.GroupDepartureRepository;
import com.tripfactory.nomad.repository.TripRequestRepository;
import com.tripfactory.nomad.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component("authz")
@RequiredArgsConstructor
public class AuthorizationService {

    private final UserRepository userRepository;
    private final TripRequestRepository tripRequestRepository;
    private final GroupDepartureRepository groupDepartureRepository;

    public boolean canAccessUser(Long userId) {
        User current = getCurrentUser();
        return current != null && (current.getRole() == UserRole.ADMIN || current.getId().equals(userId));
    }

    public boolean canAccessTrip(Long tripRequestId) {
        if (tripRequestId == null) return false;
        User current = getCurrentUser();
        if (current == null) {
            return false;
        }
        if (current.getRole() == UserRole.ADMIN) {
            return true;
        }
        TripRequest trip = tripRequestRepository.findById(tripRequestId).orElse(null);
        return trip != null && trip.getUser().getId().equals(current.getId());
    }

    public boolean canAccessGroup(Long groupId) {
        User current = getCurrentUser();
        if (current == null) {
            return false;
        }
        if (current.getRole() == UserRole.ADMIN) {
            return true;
        }
        return isGroupMember(groupId, current.getId());
    }

    /**
     * Single membership rule used by both the REST endpoints and the chat
     * WebSocket handshake. For a scheduled group trip (people who have never
     * met) only travellers who have actually paid get in - someone who merely
     * reserved a seat, or cancelled, must not read the chat or see who is going.
     */
    public boolean isGroupMember(Long groupId, Long userId) {
        if (groupId == null || userId == null) {
            return false;
        }
        if (groupDepartureRepository.existsByGroupId(groupId)) {
            return tripRequestRepository.existsByGroupIdAndUserIdAndStatus(groupId, userId, TripStatus.CONFIRMED);
        }
        return tripRequestRepository.existsByGroupIdAndUserId(groupId, userId);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            return null;
        }
        String email = auth.getName();
        return userRepository.findByEmail(email).orElse(null);
    }
}