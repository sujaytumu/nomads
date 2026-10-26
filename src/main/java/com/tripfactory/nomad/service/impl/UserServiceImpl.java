package com.tripfactory.nomad.service.impl;

import org.springframework.stereotype.Service;

import com.tripfactory.nomad.api.dto.UserCreateRequest;
import com.tripfactory.nomad.api.dto.UserResponse;
import com.tripfactory.nomad.api.dto.UserUpdateRequest;
import com.tripfactory.nomad.domain.entity.User;
import com.tripfactory.nomad.repository.UserRepository;
import com.tripfactory.nomad.service.UserService;
import com.tripfactory.nomad.service.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserResponse createUser(UserCreateRequest request) {
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setCity(request.getCity());
        user.setLatitude(request.getLatitude());
        user.setLongitude(request.getLongitude());
        user.setInterestType(request.getInterestType());
        user.setTravelPreference(request.getTravelPreference());
        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    @Override
    public UserResponse getUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toResponse(user);
    }

    @Override
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setName(request.getName());
        user.setCity(request.getCity());
        user.setPhoneNumber(request.getPhoneNumber());
        if (request.getUpiId() != null) {
            String upi = request.getUpiId().trim();
            if (upi.isEmpty()) {
                user.setUpiId(null);
            } else if (!upi.matches("^[A-Za-z0-9._\\-]{2,64}@[A-Za-z][A-Za-z0-9]{1,63}$")) {
                throw new com.tripfactory.nomad.service.exception.BadRequestException("That UPI ID doesn't look right (example: name@bank)");
            } else {
                user.setUpiId(upi);
            }
        }
        user.setLatitude(request.getLatitude());
        user.setLongitude(request.getLongitude());
        user.setInterestType(request.getInterestType());
        user.setTravelPreference(request.getTravelPreference());
        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    private UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setUpiId(user.getUpiId());
        response.setCity(user.getCity());
        response.setLatitude(user.getLatitude());
        response.setLongitude(user.getLongitude());
        response.setInterestType(user.getInterestType());
        response.setTravelPreference(user.getTravelPreference());
        response.setRole(user.getRole());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}