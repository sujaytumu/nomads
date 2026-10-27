package com.tripfactory.nomad.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tripfactory.nomad.api.dto.TripNoteRequest;
import com.tripfactory.nomad.api.dto.TripNoteResponse;
import com.tripfactory.nomad.domain.entity.TripNote;
import com.tripfactory.nomad.domain.entity.TripRequest;
import com.tripfactory.nomad.domain.enums.TripStatus;
import com.tripfactory.nomad.repository.TripNoteRepository;
import com.tripfactory.nomad.repository.TripRequestRepository;
import com.tripfactory.nomad.service.TripNoteService;
import com.tripfactory.nomad.service.engine.JournalExport;
import com.tripfactory.nomad.service.exception.BadRequestException;
import com.tripfactory.nomad.service.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TripNoteServiceImpl implements TripNoteService {

    static final int MAX_NOTES_PER_TRIP = 200;

    private final TripNoteRepository noteRepository;
    private final TripRequestRepository tripRequestRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TripNoteResponse> list(Long tripId) {
        requireTrip(tripId);
        return noteRepository.findByTripRequestIdOrderByDayNumberAscCreatedAtAsc(tripId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TripNoteResponse add(Long tripId, TripNoteRequest request) {
        TripRequest trip = requireTrip(tripId);
        if (trip.getStatus() != TripStatus.CONFIRMED) {
            throw new BadRequestException("The journal opens once the trip is booked");
        }
        if (noteRepository.countByTripRequestId(tripId) >= MAX_NOTES_PER_TRIP) {
            throw new BadRequestException("This journal is full (" + MAX_NOTES_PER_TRIP + " notes)");
        }
        TripNote note = new TripNote();
        note.setTripRequest(trip);
        apply(note, request);
        return toResponse(noteRepository.save(note));
    }

    @Override
    @Transactional
    public TripNoteResponse update(Long tripId, Long noteId, TripNoteRequest request) {
        requireTrip(tripId);
        TripNote note = noteRepository.findByIdAndTripRequestId(noteId, tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));
        apply(note, request);
        return toResponse(noteRepository.save(note));
    }

    @Override
    @Transactional
    public void delete(Long tripId, Long noteId) {
        requireTrip(tripId);
        TripNote note = noteRepository.findByIdAndTripRequestId(noteId, tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));
        noteRepository.delete(note);
    }

    @Override
    @Transactional(readOnly = true)
    public Export exportText(Long tripId) {
        TripRequest trip = requireTrip(tripId);
        List<JournalExport.Entry> entries = noteRepository.findByTripRequestIdOrderByDayNumberAscCreatedAtAsc(tripId).stream()
                .map(n -> new JournalExport.Entry(n.getDayNumber(), n.getMood(), n.getText(), n.getCreatedAt()))
                .collect(Collectors.toList());
        return new Export(JournalExport.fileName(trip.getCity()), JournalExport.toText(trip.getCity(), trip.getTripDate(), entries));
    }

    private void apply(TripNote note, TripNoteRequest request) {
        String text = request.text() == null ? "" : request.text().trim();
        if (text.isEmpty()) {
            throw new BadRequestException("Write something first");
        }
        note.setText(text);
        note.setDayNumber(request.dayNumber() == null ? 1 : request.dayNumber());
        String mood = request.mood() == null ? null : request.mood().trim();
        note.setMood(mood == null || mood.isEmpty() ? null : mood);
    }

    private TripRequest requireTrip(Long tripId) {
        return tripRequestRepository.findById(tripId).orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
    }

    private TripNoteResponse toResponse(TripNote n) {
        return new TripNoteResponse(n.getId(), n.getDayNumber(), n.getMood(), n.getText(), n.getCreatedAt(), n.getUpdatedAt());
    }
}
