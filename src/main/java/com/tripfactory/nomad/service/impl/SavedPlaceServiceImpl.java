package com.tripfactory.nomad.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tripfactory.nomad.api.dto.PlaceSearchResponse;
import com.tripfactory.nomad.domain.entity.Place;
import com.tripfactory.nomad.domain.entity.SavedPlace;
import com.tripfactory.nomad.domain.entity.User;
import com.tripfactory.nomad.repository.PlaceRepository;
import com.tripfactory.nomad.repository.SavedPlaceRepository;
import com.tripfactory.nomad.repository.UserRepository;
import com.tripfactory.nomad.service.SavedPlaceService;
import com.tripfactory.nomad.service.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SavedPlaceServiceImpl implements SavedPlaceService {

    private final SavedPlaceRepository savedPlaceRepository;
    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PlaceSearchResponse.Item> list(String email) {
        User user = requireUser(email);
        List<SavedPlace> saved = savedPlaceRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        if (saved.isEmpty()) return List.of();
        Map<Long, Place> byId = placeRepository.findAllById(saved.stream().map(SavedPlace::getPlaceId).toList())
                .stream().collect(Collectors.toMap(Place::getId, Function.identity()));
        List<PlaceSearchResponse.Item> out = new ArrayList<>();
        for (SavedPlace s : saved) {
            Place p = byId.get(s.getPlaceId());
            if (p == null) continue;   // the place was deleted since it was saved
            out.add(new PlaceSearchResponse.Item(p.getId(), p.getName(), p.getCity(), p.getLatitude() == null ? 0.0 : p.getLatitude(), p.getLongitude() == null ? 0.0 : p.getLongitude(),
                    p.getCategory(), p.getRating() == null ? 0.0 : p.getRating(), p.getImageUrl(), null));
        }
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> ids(String email) {
        User user = requireUser(email);
        return savedPlaceRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(SavedPlace::getPlaceId).toList();
    }

    @Override
    @Transactional
    public void add(String email, Long placeId) {
        User user = requireUser(email);
        if (!placeRepository.existsById(placeId)) {
            throw new ResourceNotFoundException("Place not found");
        }
        if (savedPlaceRepository.existsByUserIdAndPlaceId(user.getId(), placeId)) return;
        SavedPlace s = new SavedPlace();
        s.setUser(user);
        s.setPlaceId(placeId);
        savedPlaceRepository.save(s);
    }

    @Override
    @Transactional
    public void remove(String email, Long placeId) {
        User user = requireUser(email);
        savedPlaceRepository.deleteByUserIdAndPlaceId(user.getId(), placeId);
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
