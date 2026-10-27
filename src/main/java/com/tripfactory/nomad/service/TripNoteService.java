package com.tripfactory.nomad.service;

import java.util.List;

import com.tripfactory.nomad.api.dto.TripNoteRequest;
import com.tripfactory.nomad.api.dto.TripNoteResponse;

/** The trip journal. Callers are expected to have checked that the user may access the trip. */
public interface TripNoteService {

    List<TripNoteResponse> list(Long tripId);

    /** Notes are for booked trips only. */
    TripNoteResponse add(Long tripId, TripNoteRequest request);

    TripNoteResponse update(Long tripId, Long noteId, TripNoteRequest request);

    void delete(Long tripId, Long noteId);

    /** The whole journal as a plain-text diary. */
    Export exportText(Long tripId);

    record Export(String fileName, String text) {
    }
}
