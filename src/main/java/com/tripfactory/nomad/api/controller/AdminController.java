package com.tripfactory.nomad.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.lang.NonNull;

import jakarta.validation.Valid;

import com.tripfactory.nomad.api.dto.PlaceCreateRequest;
import com.tripfactory.nomad.api.dto.PlaceResponse;
import com.tripfactory.nomad.api.dto.VehicleCreateRequest;
import com.tripfactory.nomad.domain.entity.Place;
import com.tripfactory.nomad.domain.entity.Vehicle;
import com.tripfactory.nomad.repository.PlaceRepository;
import com.tripfactory.nomad.repository.VehicleRepository;
import com.tripfactory.nomad.service.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final PlaceRepository placeRepository;
    private final VehicleRepository vehicleRepository;

    @PostMapping("/places")
    public ResponseEntity<PlaceResponse> createPlace(@Valid @RequestBody PlaceCreateRequest request) {
        Place place = new Place();
        place.setName(request.getName());
        place.setCity(request.getCity());
        place.setLatitude(request.getLatitude());
        place.setLongitude(request.getLongitude());
        place.setCategory(request.getCategory());
        place.setRating(request.getRating());

        Place saved = placeRepository.save(place);
        return new ResponseEntity<>(toResponse(saved), HttpStatus.CREATED);
    }

    @GetMapping("/places")
    public ResponseEntity<List<PlaceResponse>> getPlaces() {
        List<PlaceResponse> responses = placeRepository.findAll().stream().map(this::toResponse).toList();
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/places/{id}")
    public ResponseEntity<Void> deletePlace(@PathVariable @NonNull Long id) {
        if (!placeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Place not found");
        }
        placeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/vehicles")
    public ResponseEntity<Vehicle> createVehicle(@Valid @RequestBody VehicleCreateRequest request) {
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleType(request.getVehicleType());
        vehicle.setCapacity(request.getCapacity());
        vehicle.setDriverName(request.getDriverName());
        vehicle.setVehicleNumber(request.getVehicleNumber());
        vehicle.setAvailabilityStatus(request.getAvailabilityStatus());
        Vehicle saved = vehicleRepository.save(vehicle);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @GetMapping("/vehicles")
    public ResponseEntity<List<Vehicle>> getVehicles() {
        return ResponseEntity.ok(vehicleRepository.findAll());
    }

    @DeleteMapping("/vehicles/{id}")
    public ResponseEntity<Void> deleteVehicle(@PathVariable @NonNull Long id) {
        if (!vehicleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Vehicle not found");
        }
        vehicleRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private PlaceResponse toResponse(Place place) {
        PlaceResponse response = new PlaceResponse();
        response.setId(place.getId());
        response.setName(place.getName());
        response.setCity(place.getCity());
        response.setLatitude(place.getLatitude());
        response.setLongitude(place.getLongitude());
        response.setCategory(place.getCategory());
        response.setRating(place.getRating());
        return response;
    }
}