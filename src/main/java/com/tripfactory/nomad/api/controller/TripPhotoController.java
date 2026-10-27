package com.tripfactory.nomad.api.controller;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.tripfactory.nomad.api.dto.TripPhotoRequest;
import com.tripfactory.nomad.api.dto.TripPhotoResponse;
import com.tripfactory.nomad.domain.entity.TripPhoto;
import com.tripfactory.nomad.domain.entity.TripRequest;
import com.tripfactory.nomad.repository.TripPhotoRepository;
import com.tripfactory.nomad.repository.TripRequestRepository;
import com.tripfactory.nomad.service.exception.BadRequestException;
import com.tripfactory.nomad.service.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

/** Photos on a trip. Only the trip's owner (or an admin) can add or remove them. */
@RestController
@RequestMapping("/api/trips/{tripId}/photos")
@RequiredArgsConstructor
public class TripPhotoController {

    static final int MAX_PHOTOS = 12;
    private static final Pattern IMAGE = Pattern.compile("^data:image/(jpeg|png|webp);base64,[A-Za-z0-9+/=]+$");

    private final TripPhotoRepository photoRepository;
    private final TripRequestRepository tripRequestRepository;

    @GetMapping
    @PreAuthorize("@authz.canAccessTrip(#tripId)")
    @Transactional(readOnly = true)
    public ResponseEntity<List<TripPhotoResponse>> list(@PathVariable Long tripId) {
        return ResponseEntity.ok(photoRepository.findByTripId(tripId).stream().map(TripPhotoController::toResponse).toList());
    }

    @PostMapping
    @PreAuthorize("@authz.canAccessTrip(#tripId)")
    @Transactional
    public ResponseEntity<TripPhotoResponse> add(@PathVariable Long tripId, @Valid @RequestBody TripPhotoRequest request) {
        TripRequest trip = tripRequestRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        if (!IMAGE.matcher(request.getDataUrl()).matches()) {
            throw new BadRequestException("That file isn't a supported image (use JPEG, PNG or WebP)");
        }
        if (photoRepository.countByTripId(tripId) >= MAX_PHOTOS) {
            throw new BadRequestException("A trip can hold up to " + MAX_PHOTOS + " photos");
        }
        TripPhoto photo = new TripPhoto();
        photo.setTripRequest(trip);
        photo.setDataUrl(request.getDataUrl());
        String caption = request.getCaption() == null ? "" : request.getCaption().trim();
        photo.setCaption(caption.isEmpty() ? null : caption);
        return new ResponseEntity<>(toResponse(photoRepository.save(photo)), HttpStatus.CREATED);
    }

    @DeleteMapping("/{photoId}")
    @PreAuthorize("@authz.canAccessTrip(#tripId)")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long tripId, @PathVariable Long photoId) {
        TripPhoto photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("Photo not found"));
        if (!photo.getTripRequest().getId().equals(tripId)) {
            throw new ResourceNotFoundException("Photo not found");
        }
        photoRepository.delete(photo);
        return ResponseEntity.noContent().build();
    }

    static TripPhotoResponse toResponse(TripPhoto p) {
        TripPhotoResponse r = new TripPhotoResponse();
        r.setId(p.getId());
        r.setCaption(p.getCaption());
        r.setDataUrl(p.getDataUrl());
        r.setCreatedAt(p.getCreatedAt());
        return r;
    }
}
