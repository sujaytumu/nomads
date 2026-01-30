package com.tripfactory.nomad.service;

import com.tripfactory.nomad.api.dto.UserCreateRequest;
import com.tripfactory.nomad.api.dto.UserResponse;
import com.tripfactory.nomad.api.dto.UserUpdateRequest;

public interface UserService {

    UserResponse createUser(UserCreateRequest request);

    UserResponse getUser(Long id);

    UserResponse updateUser(Long id, UserUpdateRequest request);
}