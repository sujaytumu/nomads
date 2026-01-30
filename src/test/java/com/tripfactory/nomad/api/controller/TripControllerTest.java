package com.tripfactory.nomad.api.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripfactory.nomad.api.dto.TripCreateRequest;
import com.tripfactory.nomad.api.dto.TripResponse;
import com.tripfactory.nomad.service.TripService;
import com.tripfactory.nomad.service.jwt.JwtService;

@WebMvcTest(TripController.class)
@AutoConfigureMockMvc(addFilters = false)
public class TripControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TripService tripService;

    @MockBean
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createTrip_returnsCreated() throws Exception {
        TripResponse resp = new TripResponse();
        resp.setTripRequestId(1L);
        resp.setCity("Test City");
        Mockito.when(tripService.createTrip(any(TripCreateRequest.class))).thenReturn(resp);

        TripCreateRequest req = new TripCreateRequest();
        req.setUserId(1L);
        req.setCity("Test City");

        mockMvc.perform(post("/api/trips/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tripRequestId").value(1))
            .andExpect(jsonPath("$.city").value("Test City"));
    }

    @Test
    void getTrip_returnsOk() throws Exception {
        TripResponse resp = new TripResponse();
        resp.setTripRequestId(2L);
        resp.setCity("Existing City");
        Mockito.when(tripService.getTrip(eq(2L))).thenReturn(resp);

        mockMvc.perform(get("/api/trips/2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.city").value("Existing City"));
    }

    @Test
    void getTripsByUser_returnsOk() throws Exception {
        TripResponse t = new TripResponse();
        t.setTripRequestId(3L);
        t.setCity("User City");
        Mockito.when(tripService.getTripsByUser(eq(10L))).thenReturn(List.of(t));

        mockMvc.perform(get("/api/trips/user/10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].city").value("User City"));
    }
}
