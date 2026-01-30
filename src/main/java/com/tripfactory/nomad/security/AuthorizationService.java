package com.tripfactory.nomad.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.tripfactory.nomad.domain.entity.TripRequest;
import com.tripfactory.nomad.domain.entity.User;
import com.tripfactory.nomad.domain.enums.UserRole;
import com.tripfactory.nomad.repository.TripRequestRepository;
import com.tripfactory.nomad.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component("authz")
@RequiredArgsConstructor
public class AuthorizationService {

    private final UserRepository userRepository;
    private final TripRequestRepository tripRequestRepository;

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
        return tripRequestRepository.existsByGroupIdAndUserId(groupId, current.getId());
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