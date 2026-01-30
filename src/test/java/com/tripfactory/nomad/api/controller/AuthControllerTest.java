package com.tripfactory.nomad.api.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripfactory.nomad.api.dto.AuthLoginRequest;
import com.tripfactory.nomad.api.dto.AuthRegisterRequest;
import com.tripfactory.nomad.api.dto.AuthResponse;
import com.tripfactory.nomad.service.AuthService;
import com.tripfactory.nomad.service.jwt.JwtService;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_returnsCreated() throws Exception {
        AuthResponse resp = new AuthResponse();
        resp.setToken("token123");
        com.tripfactory.nomad.api.dto.UserResponse user = new com.tripfactory.nomad.api.dto.UserResponse();
        user.setId(1L);
        user.setName("testuser");
        resp.setUser(user);
        Mockito.when(authService.register(any(AuthRegisterRequest.class))).thenReturn(resp);

        AuthRegisterRequest req = new AuthRegisterRequest();
        req.setName("testuser");
        req.setEmail("test@example.com");
        req.setPassword("password");
        req.setCity("Bangalore");
        req.setLatitude(12.97);
        req.setLongitude(77.59);
        req.setInterestType(com.tripfactory.nomad.domain.enums.InterestType.FOOD);
        req.setTravelPreference(com.tripfactory.nomad.domain.enums.TravelPreference.SOLO);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").value("token123"));
    }

    @Test
    void login_returnsOk() throws Exception {
        AuthResponse resp = new AuthResponse();
        resp.setToken("token456");
        com.tripfactory.nomad.api.dto.UserResponse user2 = new com.tripfactory.nomad.api.dto.UserResponse();
        user2.setId(2L);
        user2.setName("testuser");
        resp.setUser(user2);
        Mockito.when(authService.login(any(AuthLoginRequest.class))).thenReturn(resp);

        AuthLoginRequest req = new AuthLoginRequest();
        req.setEmail("test@example.com");
        req.setPassword("password");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("token456"));
    }
}
