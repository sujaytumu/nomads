package com.tripfactory.nomad.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tripfactory.nomad.api.dto.PlaceResponse;
import com.tripfactory.nomad.api.dto.PlaceSearchResponse;
import com.tripfactory.nomad.domain.entity.Place;
import com.tripfactory.nomad.domain.enums.InterestType;
import com.tripfactory.nomad.repository.PlaceRepository;
import com.tripfactory.nomad.service.PlaceSearchService;
import com.tripfactory.nomad.service.engine.PlaceSearchEngine;
import com.tripfactory.nomad.service.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

/**
 * The places table is small (hundreds of rows), so the whole table is loaded and searched in memory. That is
 * what makes exact facet counts and relevance ranking simple; if it ever grows to many thousands of rows, move
 * the filtering into the database and keep the engine for ranking.
 */
@Service
@RequiredArgsConstructor
public class PlaceSearchServiceImpl implements PlaceSearchService {

    private final PlaceRepository placeRepository;

    @Override
    @Transactional(readOnly = true)
    public PlaceSearchResponse search(String text, String city, InterestType category, Double minRating, String sort,
                                      Double lat, Double lon, int page, int size) {
        List<PlaceSearchEngine.Item> all = new ArrayList<>();
        for (Place p : placeRepository.findAll()) {
            all.add(new PlaceSearchEngine.Item(p.getId(), p.getName(), p.getCity(), p.getLatitude(), p.getLongitude(),
                    p.getCategory(), p.getRating() == null ? 0.0 : p.getRating(), p.getImageUrl()));
        }

        PlaceSearchEngine.Result result = PlaceSearchEngine.search(all, new PlaceSearchEngine.Query(
                text, city, category, minRating, PlaceSearchEngine.Sort.parse(sort), lat, lon, page, size));

        List<PlaceSearchResponse.Item> items = new ArrayList<>();
        for (PlaceSearchEngine.Hit h : result.hits()) {
            PlaceSearchEngine.Item i = h.item();
            items.add(new PlaceSearchResponse.Item(i.id(), i.name(), i.city(), i.latitude(), i.longitude(),
                    i.category(), i.rating(), i.imageUrl(), h.distanceKm()));
        }
        return new PlaceSearchResponse(items, result.page(), result.size(), result.totalItems(), result.totalPages(),
                new PlaceSearchResponse.Facets(result.categoryFacets(), result.cityFacets()));
    }

    @Override
    @Transactional(readOnly = true)
    public PlaceResponse getPlace(Long id) {
        Place p = placeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Place not found"));
        PlaceResponse r = new PlaceResponse();
        r.setId(p.getId());
        r.setName(p.getName());
        r.setCity(p.getCity());
        r.setLatitude(p.getLatitude());
        r.setLongitude(p.getLongitude());
        r.setCategory(p.getCategory());
        r.setRating(p.getRating());
        r.setImageUrl(p.getImageUrl());
        return r;
    }
}
