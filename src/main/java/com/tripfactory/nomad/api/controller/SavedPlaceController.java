package com.tripfactory.nomad.api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tripfactory.nomad.api.dto.PlaceSearchResponse;
import com.tripfactory.nomad.service.SavedPlaceService;

import lombok.RequiredArgsConstructor;

/** The caller's hearted places. Every route is scoped to the signed-in user. */
@RestController
@RequestMapping("/api/me/saved-places")
@RequiredArgsConstructor
public class SavedPlaceController {

    private final SavedPlaceService savedPlaceService;

    @GetMapping
    public ResponseEntity<List<PlaceSearchResponse.Item>> list() {
        return ResponseEntity.ok(savedPlaceService.list(email()));
    }

    @GetMapping("/ids")
    public ResponseEntity<List<Long>> ids() {
        return ResponseEntity.ok(savedPlaceService.ids(email()));
    }

    @PostMapping("/{placeId}")
    public ResponseEntity<Void> add(@PathVariable Long placeId) {
        savedPlaceService.add(email(), placeId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{placeId}")
    public ResponseEntity<Void> remove(@PathVariable Long placeId) {
        savedPlaceService.remove(email(), placeId);
        return ResponseEntity.noContent().build();
    }

    private String email() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
