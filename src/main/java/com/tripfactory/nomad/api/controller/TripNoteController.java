package com.tripfactory.nomad.api.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tripfactory.nomad.api.dto.TripNoteRequest;
import com.tripfactory.nomad.api.dto.TripNoteResponse;
import com.tripfactory.nomad.service.TripNoteService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** A trip's journal. Only the trip's owner (or an admin) can read or write it. */
@RestController
@RequestMapping("/api/trips/{tripId}/notes")
@RequiredArgsConstructor
public class TripNoteController {

    private final TripNoteService tripNoteService;

    @GetMapping
    @PreAuthorize("@authz.canAccessTrip(#tripId)")
    public ResponseEntity<List<TripNoteResponse>> list(@PathVariable Long tripId) {
        return ResponseEntity.ok(tripNoteService.list(tripId));
    }

    @PostMapping
    @PreAuthorize("@authz.canAccessTrip(#tripId)")
    public ResponseEntity<TripNoteResponse> add(@PathVariable Long tripId, @Valid @RequestBody TripNoteRequest request) {
        return new ResponseEntity<>(tripNoteService.add(tripId, request), HttpStatus.CREATED);
    }

    @PutMapping("/{noteId}")
    @PreAuthorize("@authz.canAccessTrip(#tripId)")
    public ResponseEntity<TripNoteResponse> update(@PathVariable Long tripId, @PathVariable Long noteId,
                                                   @Valid @RequestBody TripNoteRequest request) {
        return ResponseEntity.ok(tripNoteService.update(tripId, noteId, request));
    }

    @DeleteMapping("/{noteId}")
    @PreAuthorize("@authz.canAccessTrip(#tripId)")
    public ResponseEntity<Void> delete(@PathVariable Long tripId, @PathVariable Long noteId) {
        tripNoteService.delete(tripId, noteId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export")
    @PreAuthorize("@authz.canAccessTrip(#tripId)")
    public ResponseEntity<byte[]> export(@PathVariable Long tripId) {
        TripNoteService.Export export = tripNoteService.exportText(tripId);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(export.fileName()).build().toString())
                .body(export.text().getBytes(StandardCharsets.UTF_8));
    }
}
